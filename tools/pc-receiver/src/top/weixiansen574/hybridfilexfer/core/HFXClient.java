package top.weixiansen574.hybridfilexfer.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;
import top.weixiansen574.hybridfilexfer.core.callback.ClientCallBack;
import top.weixiansen574.hybridfilexfer.core.callback.ConnectServerCallback;
import top.weixiansen574.nio.DataByteChannel;

public abstract class HFXClient extends HFXService {

    public static class FileStat {
        public final boolean exists;
        public final boolean isDirectory;
        public final long size;
        public final long lastModified;

        public FileStat(boolean exists, boolean isDirectory, long size, long lastModified) {
            this.exists = exists;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }
    }

    protected final String serverControllerAddress;
    protected final int serverPort;
    protected final String homeDir;
    protected boolean isRun = true;
    protected ClientCallBack callBack;

    public HFXClient(String serverControllerAddress, int serverPort, String homeDir) {
        this.serverControllerAddress = serverControllerAddress;
        this.serverPort = serverPort;
        this.homeDir = homeDir;
    }

    public boolean connect(ConnectServerCallback callback) throws IOException {
        try {
            callback.onConnectingControlChannel(serverControllerAddress, serverPort);
            ctChannel = new DataByteChannel(SocketChannel
                    .open(new InetSocketAddress(serverControllerAddress, serverPort)));

            ctChannel.write(CLIENT_HEADER.getBytes(StandardCharsets.UTF_8));
            ctChannel.writeInt(VERSION_CODE);
            if (!ctChannel.readBoolean()) {
                callback.onVersionMismatch(VERSION_CODE, ctChannel.readInt());
                ctChannel.close();
                return false;
            }
        } catch (IOException e) {
            callback.onConnectControlFailed();
            return false;
        }

        int ipCount = ctChannel.readInt();
        String[] names = new String[ipCount];
        InetAddress[] addresses = new InetAddress[ipCount];
        InetAddress[] bindAddresses = new InetAddress[ipCount];

        for (int i = 0; i < ipCount; i++) {
            String name = ctChannel.readUTF();
            byte[] address = new byte[ctChannel.readByte()];
            ctChannel.readFully(address);
            InetAddress inetAddress = InetAddress.getByAddress(address);
            byte l46 = ctChannel.readByte();
            InetAddress bindAddress = null;
            if (l46 != 0) {
                byte[] bAddress = new byte[l46];
                ctChannel.readFully(bAddress);
                bindAddress = InetAddress.getByAddress(bAddress);
            }
            names[i] = name;
            addresses[i] = inetAddress;
            bindAddresses[i] = bindAddress;
        }

        connections = new ArrayList<>(ipCount);
        for (int i = 0; i < ipCount; i++) {
            SocketChannel socketChannel;
            String name = names[i];
            InetAddress inetAddress = addresses[i];
            InetAddress bindAddress = bindAddresses[i];
            callback.onConnectingTransferChannel(name, inetAddress, bindAddress);
            try {
                if (bindAddress == null) {
                    socketChannel = SocketChannel.open(new InetSocketAddress(inetAddress, serverPort));
                } else {
                    socketChannel = SocketChannel.open();
                    socketChannel.bind(new InetSocketAddress(bindAddress, 0));
                    socketChannel.connect(new InetSocketAddress(inetAddress, serverPort));
                }
                tuneDataSocket(socketChannel);
                connections.add(new TransferConnection(name, new DataByteChannel(socketChannel)));
            } catch (IOException e) {
                callback.onConnectTransferChannelFailed(name, inetAddress, e);
                try {
                    ctChannel.writeBoolean(false);
                    ctChannel.writeUTF(name);
                } catch (IOException ignored) {
                }
                closeAllConnections();
                return false;
            }
            ctChannel.writeBoolean(true);
            ctChannel.writeUTF(name);
            ctChannel.readBoolean();
        }

        int bufferCount = ctChannel.readInt();
        for (int i = 0; i < bufferCount; i++) {
            ByteBuffer buffer = createBuffer(FileBlock.BLOCK_SIZE);
            if (buffer != null) {
                buffers.add(buffer);
            } else {
                String arch = System.getProperty("os.arch");
                long availableMemoryMB = getAvailableMemoryMB();
                freeBuffers();
                callback.onOOM(i, bufferCount, availableMemoryMB, arch);
                ctChannel.writeBoolean(false);
                closeAllConnections();
                return false;
            }
        }
        ctChannel.writeBoolean(true);
        if (!ctChannel.readBoolean()) {
            freeBuffers();
            callback.onRemoteOOM();
            closeAllConnections();
            return false;
        }

        // V400: PC 端启用虚拟 POSIX 根路径沙箱，固定上报 UNIX(0) 与 "/"
        ctChannel.writeInt(Directory.FILE_SYSTEM_UNIX);
        ctChannel.writeUTF("/");

        List<String> channelNames = new ArrayList<>(connections.size());
        for (TransferConnection connection : connections) {
            channelNames.add(connection.iName);
        }
        callback.onConnectSuccess(channelNames);
        return true;
    }

    public abstract ByteBuffer createBuffer(int size);

    public abstract long getAvailableMemoryMB();

    public void start(ClientCallBack transferFileCallback) throws Exception {
        this.callBack = transferFileCallback;
        try {
            while (isRun) {
                short id = ctChannel.readShort();
                switch (id) {
                    case ControllerIdentifiers.LIST_FILES:
                        handleListFiles();
                        break;
                    case ControllerIdentifiers.DELETE_FILE:
                        handleDeleteFile();
                        break;
                    case ControllerIdentifiers.MKDIR:
                        handleMkdir();
                        break;
                    case ControllerIdentifiers.STAT_FILE:
                        handleStatFile();
                        break;
                    case ControllerIdentifiers.RENAME_FILE:
                        handleRenameFile();
                        break;
                    case ControllerIdentifiers.DETACH:
                        handleDetach();
                        break;
                    case ControllerIdentifiers.REQUEST_RECEIVE:
                        handleReceiveFiles();
                        break;
                    case ControllerIdentifiers.REQUEST_SEND:
                        handleSendFiles();
                        break;
                    case ControllerIdentifiers.STREAM_BEGIN:
                        handleStreamBegin();
                        break;
                    case ControllerIdentifiers.STREAM_APPEND_CHUNK:
                        handleStreamAppendChunk();
                        break;
                    case ControllerIdentifiers.STREAM_COMMIT:
                        handleStreamCommit();
                        break;
                    case ControllerIdentifiers.STREAM_ABORT:
                        handleStreamAbort();
                        break;
                    case ControllerIdentifiers.STREAM_RESUME:
                        handleStreamResume();
                        break;
                    case ControllerIdentifiers.STREAM_CONTINUOUS:
                        handleStreamContinuous();
                        break;
                    case ControllerIdentifiers.SHUTDOWN:
                        handleShutdown();
                        break;
                    default:
                        throw new IOException("Unknown controller identifier: " + id);
                }
            }
        } finally {
            onSessionClosed();
            closeAllConnections();
            freeBuffers();
        }
    }

    private void handleStatFile() throws Exception {
        String path = ctChannel.readUTF();
        FileStat stat = statLocalFile(path);
        ctChannel.writeBoolean(stat.exists);
        ctChannel.writeBoolean(stat.isDirectory);
        ctChannel.writeLong(stat.size);
        ctChannel.writeLong(stat.lastModified);
    }

    private void handleRenameFile() throws Exception {
        String src = ctChannel.readUTF();
        String dst = ctChannel.readUTF();
        ctChannel.writeBoolean(renameLocalFile(src, dst));
    }

    private void handleDetach() {
        isRun = false;
        try {
            ctChannel.writeBoolean(true);
        } catch (IOException ignored) {
        }
        if (callBack != null) {
            callBack.onExit();
        }
    }

    private void handleDeleteFile() throws Exception {
        ctChannel.writeBoolean(deleteLocalFile(ctChannel.readUTF()));
    }

    private void handleMkdir() throws Exception {
        String parent = ctChannel.readUTF();
        String child = ctChannel.readUTF();
        ctChannel.writeBoolean(mkdir(parent, child));
    }

    private void handleShutdown() {
        isRun = false;
        if (callBack != null) {
            callBack.onExit();
        }
    }

    private void handleListFiles() throws Exception {
        String path = ctChannel.readUTF();
        List<RemoteFile> files = listFiles(path);
        if (files != null) {
            ctChannel.writeInt(files.size());
            for (RemoteFile file : files) {
                writeFile(file);
            }
        } else {
            ctChannel.writeInt(-1);
        }
    }

    private void writeFile(RemoteFile file) throws IOException {
        ctChannel.writeUTF(file.getName());
        ctChannel.writeUTF(file.getPath());
        ctChannel.writeLong(file.lastModified());
        ctChannel.writeLong(file.getSize());
        ctChannel.writeBoolean(file.isDirectory());
    }

    protected void handleReceiveFiles() throws IOException {
        callBack.onReceiving();
        isRun = receiveFiles(callBack);
    }

    protected void handleSendFiles() throws IOException {
        int listSize = ctChannel.readInt();
        List<RemoteFile> fileList = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            String virtualPath = ctChannel.readUTF();
            fileList.add(resolveRemoteFile(virtualPath));
        }
        Directory remoteDir = new Directory(ctChannel.readUTF(), ctChannel.readInt());
        Directory localDir = new Directory(ctChannel.readUTF(), Directory.FILE_SYSTEM_UNIX);
        callBack.onSending();
        isRun = sendFiles(fileList, localDir, remoteDir, callBack);
    }

    protected void handleStreamBegin() throws IOException {
        String streamId = ctChannel.readUTF();
        String targetVirtualPath = ctChannel.readUTF();
        long lastModified = ctChannel.readLong();
        try {
            onStreamBegin(streamId, targetVirtualPath, lastModified);
            ctChannel.writeBoolean(true);
        } catch (Exception e) {
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(e.toString());
        }
    }

    protected void handleStreamResume() throws IOException {
        String streamId = ctChannel.readUTF();
        String targetVirtualPath = ctChannel.readUTF();
        long lastModified = ctChannel.readLong();
        try {
            long currentSize = onStreamResume(streamId, targetVirtualPath, lastModified);
            ctChannel.writeBoolean(true);
            ctChannel.writeLong(currentSize);
        } catch (Exception e) {
            ctChannel.writeBoolean(false);
            ctChannel.writeLong(0L);
            ctChannel.writeUTF(e.toString());
        }
    }

    protected void handleStreamAppendChunk() throws IOException {
        String streamId = ctChannel.readUTF();
        long chunkOffset = ctChannel.readLong();
        long chunkLength = ctChannel.readLong();
        long expectedCrc32 = ctChannel.readLong();
        isRun = onStreamAppendChunk(streamId, chunkOffset, chunkLength, expectedCrc32);
    }

    protected void handleStreamCommit() throws IOException {
        String streamId = ctChannel.readUTF();
        long expectedTotalSize = ctChannel.readLong();
        try {
            onStreamCommit(streamId, expectedTotalSize);
            ctChannel.writeBoolean(true);
        } catch (Exception e) {
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(e.toString());
        }
    }

    protected void handleStreamAbort() throws IOException {
        String streamId = ctChannel.readUTF();
        try {
            onStreamAbort(streamId);
            ctChannel.writeBoolean(true);
        } catch (Exception e) {
            ctChannel.writeBoolean(false);
        }
    }

    protected void handleStreamContinuous() throws IOException {
        String targetVirtualPath = ctChannel.readUTF();
        long lastModified = ctChannel.readLong();
        boolean verifyCrc32 = ctChannel.readBoolean();
        callBack.onReceiving();
        isRun = onStreamContinuous(targetVirtualPath, lastModified, verifyCrc32);
    }

    protected void tuneDataSocket(SocketChannel sc) {
        try { sc.setOption(StandardSocketOptions.TCP_NODELAY, true); } catch (Exception ignored) {}
        try { sc.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024 * 1024); } catch (Exception ignored) {}
        try { sc.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024 * 1024); } catch (Exception ignored) {}
    }

    protected void closeAllConnections() {
        if (ctChannel != null) {
            try {
                ctChannel.close();
            } catch (IOException ignored) {
            }
        }
        if (connections != null) {
            for (TransferConnection transferConnection : connections) {
                try {
                    transferConnection.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    protected void freeBuffers() {
        buffers.clear();
    }

    protected void onSessionClosed() {
    }

    protected abstract FileStat statLocalFile(String virtualPath) throws Exception;

    protected abstract boolean renameLocalFile(String srcVirtualPath, String dstVirtualPath) throws Exception;

    protected abstract boolean deleteLocalFile(String virtualPath) throws Exception;

    protected abstract boolean mkdir(String parentVirtualPath, String child) throws Exception;

    protected abstract List<RemoteFile> listFiles(String virtualPath) throws Exception;

    protected abstract RemoteFile resolveRemoteFile(String virtualPath) throws IOException;

    protected abstract void onStreamBegin(String streamId, String targetVirtualPath, long lastModified) throws Exception;

    protected abstract long onStreamResume(String streamId, String targetVirtualPath, long lastModified) throws Exception;

    protected abstract boolean onStreamAppendChunk(String streamId, long chunkOffset, long chunkLength, long expectedCrc32) throws IOException;

    protected abstract void onStreamCommit(String streamId, long expectedTotalSize) throws Exception;

    protected abstract void onStreamAbort(String streamId) throws Exception;

    protected abstract boolean onStreamContinuous(String targetVirtualPath, long lastModified, boolean verifyCrc32) throws IOException;
}
