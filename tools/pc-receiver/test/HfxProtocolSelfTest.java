import top.weixiansen574.hybridfilexfer.core.ControllerIdentifiers;
import top.weixiansen574.hybridfilexfer.core.FileBlock;
import top.weixiansen574.hybridfilexfer.core.HFXService;
import top.weixiansen574.hybridfilexfer.core.ReadFileCall;
import top.weixiansen574.hybridfilexfer.core.TransferConnection;
import top.weixiansen574.hybridfilexfer.core.TransferIdentifiers;
import top.weixiansen574.hybridfilexfer.core.WriteFileCall;
import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;
import top.weixiansen574.hybridfilexfer.core.bean.ServerNetInterface;
import top.weixiansen574.hybridfilexfer.core.bean.TrafficInfo;
import top.weixiansen574.hybridfilexfer.core.callback.ClientCallBack;
import top.weixiansen574.hybridfilexfer.core.callback.ConnectServerCallback;
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;
import top.weixiansen574.hybridfilexfer.jdkcore.JdkHFXClient;
import top.weixiansen574.hybridfilexfer.jdkcore.VirtualPathResolver;
import top.weixiansen574.nio.DataByteChannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.CRC32;

public class HfxProtocolSelfTest {
    private static final int TEST_PORT = 15740;

    static class TestServer extends HFXService {
        private ServerSocketChannel ssc;

        void startAndAccept(int port, int channels) throws IOException {
            ssc = ServerSocketChannel.open();
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            ssc.bind(new InetSocketAddress("127.0.0.1", port));

            SocketChannel ct = ssc.accept();
            ctChannel = new DataByteChannel(ct);
            byte[] header = new byte[4];
            ctChannel.readFully(header);
            if (!CLIENT_HEADER.equals(new String(header, StandardCharsets.UTF_8))) {
                throw new IOException("Bad header");
            }
            int ver = ctChannel.readInt();
            if (ver != VERSION_CODE) {
                ctChannel.writeBoolean(false);
                ctChannel.writeInt(VERSION_CODE);
                throw new IOException("Version mismatch: " + ver);
            }
            ctChannel.writeBoolean(true);

            List<ServerNetInterface> ifaces = new ArrayList<>();
            InetAddress lo = InetAddress.getByName("127.0.0.1");
            for (int i = 0; i < channels; i++) {
                ifaces.add(new ServerNetInterface("CH_" + i, lo, null));
            }
            ctChannel.writeInt(ifaces.size());
            for (ServerNetInterface ni : ifaces) {
                ctChannel.writeUTF(ni.name);
                byte[] addr = ni.address.getAddress();
                ctChannel.writeByte(addr.length);
                ctChannel.write(addr);
                ctChannel.writeByte(0);
            }
            connections = new ArrayList<>();
            for (int i = 0; i < ifaces.size(); i++) {
                boolean ok = ctChannel.readBoolean();
                String name = ctChannel.readUTF();
                if (!ok) throw new IOException("Channel connect failed: " + name);
                SocketChannel dc = ssc.accept();
                connections.add(new TransferConnection(name, new DataByteChannel(dc)));
                ctChannel.writeBoolean(true);
            }
            ctChannel.writeInt(16); // remote buffers
            if (!ctChannel.readBoolean()) throw new IOException("PC OOM");
            for (int i = 0; i < 16; i++) {
                buffers.add(ByteBuffer.allocateDirect(FileBlock.BLOCK_SIZE));
            }
            ctChannel.writeBoolean(true);
            int fs = ctChannel.readInt();
            String home = ctChannel.readUTF();
            if (fs != Directory.FILE_SYSTEM_UNIX || !"/".equals(home)) {
                throw new IOException("Unexpected virtual root: fs=" + fs + ", home=" + home);
            }
        }

        boolean mkdir(String parent, String child) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.MKDIR);
            ctChannel.writeUTF(parent);
            ctChannel.writeUTF(child);
            return ctChannel.readBoolean();
        }

        boolean exists(String path) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STAT_FILE);
            ctChannel.writeUTF(path);
            boolean ex = ctChannel.readBoolean();
            ctChannel.readBoolean();
            ctChannel.readLong();
            ctChannel.readLong();
            return ex;
        }

        boolean rename(String src, String dst) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.RENAME_FILE);
            ctChannel.writeUTF(src);
            ctChannel.writeUTF(dst);
            return ctChannel.readBoolean();
        }

        boolean delete(String path) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.DELETE_FILE);
            ctChannel.writeUTF(path);
            return ctChannel.readBoolean();
        }

        void uploadFile(File localFile, String remoteDir) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.REQUEST_RECEIVE);
            Directory lDir = new Directory(localFile.getParentFile().getAbsolutePath(), Directory.getCurrentFileSystem());
            Directory rDir = new Directory(remoteDir, Directory.FILE_SYSTEM_UNIX);
            boolean ok = sendFiles(Collections.singletonList(new RemoteFile(localFile)), lDir, rDir, noopCallback());
            if (!ok) throw new IOException("uploadFile failed");
        }

        void streamBegin(String streamId, String targetPath, long lastMod) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STREAM_BEGIN);
            ctChannel.writeUTF(streamId);
            ctChannel.writeUTF(targetPath);
            ctChannel.writeLong(lastMod);
            if (!ctChannel.readBoolean()) {
                throw new IOException("STREAM_BEGIN failed: " + ctChannel.readUTF());
            }
        }

        long streamResume(String streamId, String targetPath, long lastMod) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STREAM_RESUME);
            ctChannel.writeUTF(streamId);
            ctChannel.writeUTF(targetPath);
            ctChannel.writeLong(lastMod);
            boolean ok = ctChannel.readBoolean();
            long size = ctChannel.readLong();
            if (!ok) {
                throw new IOException("STREAM_RESUME failed: " + ctChannel.readUTF());
            }
            return size;
        }

        boolean streamAppendChunk(String streamId, File chunkFile, long offset, long crc32) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STREAM_APPEND_CHUNK);
            ctChannel.writeUTF(streamId);
            ctChannel.writeLong(offset);
            ctChannel.writeLong(chunkFile.length());
            ctChannel.writeLong(crc32);

            final boolean[] writeErr = {false};
            ReadFileCall readCall = new ReadFileCall(buffers, Collections.emptyList(), null, null, connections.size()) {
                private RandomAccessFile raf;
                private FileChannel ch;

                @Override
                protected void produceBlocks() throws Exception {
                    fileIndex = 0;
                    raf = new RandomAccessFile(chunkFile, "r");
                    ch = raf.getChannel();
                    try {
                        readChannelToDeque(ch, streamId, 0L, chunkFile.length());
                    } finally {
                        closeFile();
                    }
                }

                @Override protected boolean fileExists(String path) { return true; }
                @Override protected List<RemoteFile> listFiles(String path) { return null; }
                @Override protected FileChannel openFile(String path) { return ch; }
                @Override protected void closeFile() throws Exception {
                    if (ch != null) { ch.close(); ch = null; }
                    if (raf != null) { raf.close(); raf = null; }
                }
            };
            TransferFileCallback cb = new NoopTransferCallback() {
                @Override public void onWriteFileError(String message) { writeErr[0] = true; }
            };
            boolean ok = sendWithReadCall(readCall, cb);
            return ok && !writeErr[0];
        }

        void streamCommit(String streamId, long totalSize) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STREAM_COMMIT);
            ctChannel.writeUTF(streamId);
            ctChannel.writeLong(totalSize);
            if (!ctChannel.readBoolean()) {
                throw new IOException("STREAM_COMMIT failed: " + ctChannel.readUTF());
            }
        }

        void sendContinuous(String targetPath, byte[] csData) throws IOException {
            ctChannel.writeShort(ControllerIdentifiers.STREAM_CONTINUOUS);
            ctChannel.writeUTF(targetPath);
            ctChannel.writeLong(System.currentTimeMillis());
            ctChannel.writeBoolean(true);
            if (!ctChannel.readBoolean()) throw new IOException("PC rejected STREAM_CONTINUOUS");

            int blkSize = FileBlock.BLOCK_SIZE;
            CRC32 blkCrc = new CRC32();
            ByteBuffer hdr = ByteBuffer.allocate(18);
            for (int off = 0, chIdx = 0; off < csData.length; off += blkSize, chIdx++) {
                int len = Math.min(blkSize, csData.length - off);
                blkCrc.reset();
                blkCrc.update(csData, off, len);
                DataByteChannel dc = connections.get(chIdx % connections.size()).channel;
                hdr.clear();
                hdr.putShort(TransferIdentifiers.STREAM_BLOCK);
                hdr.putLong(off);
                hdr.putInt(len);
                hdr.putInt((int) blkCrc.getValue());
                hdr.flip();
                dc.writeFully(hdr);
                dc.writeFully(ByteBuffer.wrap(csData, off, len));
            }
            for (TransferConnection conn : connections) {
                conn.channel.writeShort(TransferIdentifiers.EOF);
            }
            if (!ctChannel.readBoolean()) throw new IOException("PC write/CRC failed: " + ctChannel.readUTF());
            ctChannel.writeBoolean(true);
            ctChannel.writeLong(csData.length);
            if (!ctChannel.readBoolean()) throw new IOException("PC commit failed: " + ctChannel.readUTF());
        }

        void detach() {
            try {
                ctChannel.writeShort(ControllerIdentifiers.DETACH);
                ctChannel.readBoolean();
            } catch (Exception ignored) {
            }
            closeAll();
        }

        void closeAll() {
            try { if (ctChannel != null) ctChannel.close(); } catch (Exception ignored) {}
            if (connections != null) {
                for (TransferConnection c : connections) {
                    try { c.close(); } catch (Exception ignored) {}
                }
            }
            try { if (ssc != null) ssc.close(); } catch (Exception ignored) {}
            buffers.clear();
        }

        @Override
        protected WriteFileCall createWriteFileCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected ReadFileCall createReadFileCall(LinkedBlockingDeque<ByteBuffer> buffers, List<RemoteFile> files, Directory localDir, Directory remoteDir, int operateThreadCount) {
            return new ReadFileCall(buffers, files, localDir, remoteDir, operateThreadCount) {
                private RandomAccessFile raf;
                private FileChannel ch;
                @Override protected boolean fileExists(String path) { return new File(path).exists(); }
                @Override protected List<RemoteFile> listFiles(String path) { return null; }
                @Override protected FileChannel openFile(String path) throws Exception {
                    raf = new RandomAccessFile(path, "r");
                    ch = raf.getChannel();
                    return ch;
                }
                @Override protected void closeFile() throws Exception {
                    if (ch != null) { ch.close(); ch = null; }
                    if (raf != null) { raf.close(); raf = null; }
                }
            };
        }
    }

    static class NoopTransferCallback implements TransferFileCallback {
        @Override public void onFileUploading(String iName, String path, long targetSize, long totalSize) {}
        @Override public void onFileDownloading(String iName, String path, long targetSize, long totalSize) {}
        @Override public void onSpeedInfo(List<TrafficInfo> trafficInfoList) {}
        @Override public void onChannelComplete(String iName, long traffic, long time) {}
        @Override public void onChannelError(String iName, int errorType, String message) {}
        @Override public void onReadFileError(String message) {}
        @Override public void onWriteFileError(String message) {}
        @Override public void onComplete(boolean isUpload, long traffic, long time) {}
        @Override public void onIncomplete() {}
    }

    static TransferFileCallback noopCallback() { return new NoopTransferCallback(); }

    static Thread startClientThread(Path pcRoot) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(150);
                JdkHFXClient client = new JdkHFXClient("127.0.0.1", TEST_PORT, pcRoot.toString());
                boolean connected = client.connect(new ConnectServerCallback() {
                    @Override public void onConnectingControlChannel(String address, int port) {}
                    @Override public void onVersionMismatch(int localVersion, int remoteVersion) {}
                    @Override public void onConnectControlFailed() {}
                    @Override public void onConnectingTransferChannel(String name, InetAddress inetAddress, InetAddress bindAddress) {}
                    @Override public void onConnectTransferChannelFailed(String name, InetAddress inetAddress, Exception e) {}
                    @Override public void onOOM(int createdBuffers, int requiredBuffers, long maxMemoryMB, String osArch) {}
                    @Override public void onRemoteOOM() {}
                    @Override public void onConnectSuccess(List<String> channelNames) {}
                });
                if (connected) {
                    client.start(new ClientCallBack() {
                        @Override public void onReceiving() {}
                        @Override public void onSending() {}
                        @Override public void onExit() {}
                        @Override public void onFileUploading(String iName, String path, long targetSize, long totalSize) {}
                        @Override public void onFileDownloading(String iName, String path, long targetSize, long totalSize) {}
                        @Override public void onSpeedInfo(List<TrafficInfo> trafficInfoList) {}
                        @Override public void onChannelComplete(String iName, long traffic, long time) {}
                        @Override public void onChannelError(String iName, int errorType, String message) {}
                        @Override public void onReadFileError(String message) {}
                        @Override public void onWriteFileError(String message) {}
                        @Override public void onComplete(boolean isUpload, long traffic, long time) {}
                        @Override public void onIncomplete() {}
                    });
                }
            } catch (Exception ignored) {
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    static byte[] sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[1024 * 1024];
        try (java.io.InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        return md.digest();
    }

    static long crc32(File file) throws Exception {
        CRC32 crc = new CRC32();
        byte[] buf = new byte[1024 * 1024];
        try (java.io.InputStream in = Files.newInputStream(file.toPath())) {
            int n;
            while ((n = in.read(buf)) != -1) {
                crc.update(buf, 0, n);
            }
        }
        return crc.getValue();
    }

    public static void main(String[] args) throws Exception {
        Path tempRoot = Files.createTempDirectory("hfx_v400_test_");
        Path pcRoot = tempRoot.resolve("pc_root");
        Path phoneTmp = tempRoot.resolve("phone_tmp");
        Files.createDirectories(pcRoot);
        Files.createDirectories(phoneTmp);

        // Test 1: VirtualPathResolver traversal check + regular upload/stat/rename/delete
        VirtualPathResolver resolver = new VirtualPathResolver(pcRoot);
        boolean blocked = false;
        try {
            resolver.resolve("/../../evil.txt");
        } catch (SecurityException e) {
            blocked = true;
        }
        if (!blocked) throw new AssertionError("Path traversal was not blocked!");

        TestServer server = new TestServer();
        Thread clientThread = startClientThread(pcRoot);
        server.startAndAccept(TEST_PORT, 2);

        if (!server.mkdir("/apps/com.test/user_0", "")) throw new AssertionError("mkdir failed");
        File smallFile = phoneTmp.resolve("config.json").toFile();
        Files.write(smallFile.toPath(), "{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
        server.uploadFile(smallFile, "/apps/com.test/user_0");
        if (!server.exists("/apps/com.test/user_0/config.json")) throw new AssertionError("uploaded file missing");
        if (!server.rename("/apps/com.test/user_0/config.json", "/apps/com.test/user_0/config2.json")) throw new AssertionError("rename failed");
        if (!server.delete("/apps/com.test/user_0/config2.json")) throw new AssertionError("delete failed");
        System.out.println("[PASS] Test 1: Path traversal guard + regular upload/stat/rename/delete");

        // Test 2 & 3: Streaming chunked upload (5 chunks x 4MB = 20MB, with CRC mismatch retry on chunk 1)
        int chunkSize = 4 * 1024 * 1024;
        int numChunks = 5;
        Random rnd = new Random(42);
        Path fullExpected = phoneTmp.resolve("full_expected.bin");
        List<File> chunkFiles = new ArrayList<>();
        List<Long> chunkCrcs = new ArrayList<>();
        try (FileOutputStream fos = new FileOutputStream(fullExpected.toFile())) {
            byte[] buf = new byte[chunkSize];
            for (int i = 0; i < numChunks; i++) {
                rnd.nextBytes(buf);
                fos.write(buf);
                File cf = phoneTmp.resolve("chunk_" + i + ".part").toFile();
                Files.write(cf.toPath(), buf);
                chunkFiles.add(cf);
                chunkCrcs.add(crc32(cf));
            }
        }

        String streamId = UUID.randomUUID().toString();
        String targetVirtual = "/apps/com.tencent.mm/user_0/user.tar.zst";
        server.streamBegin(streamId, targetVirtual, System.currentTimeMillis());
        for (int i = 0; i < numChunks; i++) {
            long offset = (long) i * chunkSize;
            if (i == 1) {
                boolean badOk = server.streamAppendChunk(streamId, chunkFiles.get(i), offset, chunkCrcs.get(i) ^ 0x1234L);
                if (badOk) throw new AssertionError("Corrupted CRC32 chunk should have been rejected!");
            }
            boolean ok = server.streamAppendChunk(streamId, chunkFiles.get(i), offset, chunkCrcs.get(i));
            if (!ok) throw new AssertionError("Chunk " + i + " failed");
        }
        server.streamCommit(streamId, (long) numChunks * chunkSize);
        Path pcFinal = pcRoot.resolve("apps/com.tencent.mm/user_0/user.tar.zst");
        if (!Arrays.equals(sha256(fullExpected), sha256(pcFinal))) {
            throw new AssertionError("SHA-256 mismatch after streamCommit!");
        }
        if (Files.exists(pcRoot.resolve("apps/com.tencent.mm/user_0/user.tar.zst.hfxtmp"))) {
            throw new AssertionError("Residual .hfxtmp found!");
        }
        System.out.println("[PASS] Test 2 & 3: Multi-channel streaming chunks + CRC32 tamper rejection + atomic commit SHA-256 match");

        // Test 4: Mid-stream disconnect + STREAM_RESUME + SHA-256 match
        String resumeStreamId = UUID.randomUUID().toString();
        String resumeTarget = "/apps/com.tencent.mm/user_0/data.tar.zst";
        server.streamBegin(resumeStreamId, resumeTarget, System.currentTimeMillis());
        for (int i = 0; i < 2; i++) {
            if (!server.streamAppendChunk(resumeStreamId, chunkFiles.get(i), (long) i * chunkSize, chunkCrcs.get(i))) {
                throw new AssertionError("Pre-disconnect chunk failed");
            }
        }
        // Simulate abrupt disconnect
        server.closeAll();
        clientThread.join(3000);

        // Reconnect and resume
        TestServer server2 = new TestServer();
        Thread clientThread2 = startClientThread(pcRoot);
        server2.startAndAccept(TEST_PORT, 2);
        long resumedSize = server2.streamResume(resumeStreamId, resumeTarget, System.currentTimeMillis());
        if (resumedSize != 2L * chunkSize) {
            throw new AssertionError("Unexpected resumed size: " + resumedSize);
        }
        for (int i = 2; i < numChunks; i++) {
            if (!server2.streamAppendChunk(resumeStreamId, chunkFiles.get(i), (long) i * chunkSize, chunkCrcs.get(i))) {
                throw new AssertionError("Post-resume chunk failed");
            }
        }
        server2.streamCommit(resumeStreamId, (long) numChunks * chunkSize);
        Path pcResumedFinal = pcRoot.resolve("apps/com.tencent.mm/user_0/data.tar.zst");
        if (!Arrays.equals(sha256(fullExpected), sha256(pcResumedFinal))) {
            throw new AssertionError("SHA-256 mismatch after STREAM_RESUME!");
        }
        System.out.println("[PASS] Test 4: Mid-stream disconnect + STREAM_RESUME + SHA-256 match");

        // Test 5: V401 Continuous Zero-Disk Stream (STREAM_CONTINUOUS = 25) with inline CRC32
        byte[] csData = Files.readAllBytes(fullExpected);
        server2.sendContinuous("/apps/com.tencent.mm/user_0/continuous.tar.zst", csData);
        Path pcContinuousFinal = pcRoot.resolve("apps/com.tencent.mm/user_0/continuous.tar.zst");
        if (!Arrays.equals(sha256(fullExpected), sha256(pcContinuousFinal))) {
            throw new AssertionError("SHA-256 mismatch after STREAM_CONTINUOUS!");
        }
        System.out.println("[PASS] Test 5: V401 STREAM_CONTINUOUS zero-disk stream + inline CRC32 + SHA-256 match");

        server2.detach();
        clientThread2.join(3000);
        System.out.println("ALL V401 PROTOCOL SELF-TESTS PASSED!");
    }
}

