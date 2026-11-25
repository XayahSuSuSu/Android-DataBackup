package top.weixiansen574.hybridfilexfer.jdkcore;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import top.weixiansen574.hybridfilexfer.core.HFXClient;
import top.weixiansen574.hybridfilexfer.core.ReadFileCall;
import top.weixiansen574.hybridfilexfer.core.WriteFileCall;
import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;

public class JdkHFXClient extends HFXClient {
    private final VirtualPathResolver resolver;
    private final Map<String, StreamSession> activeStreams = new ConcurrentHashMap<>();

    public JdkHFXClient(String serverControllerAddress, int serverPort, String backupRootDir) throws IOException {
        super(serverControllerAddress, serverPort, "/");
        this.resolver = new VirtualPathResolver(Paths.get(backupRootDir));
    }

    public VirtualPathResolver getResolver() {
        return resolver;
    }

    @Override
    public ByteBuffer createBuffer(int size) {
        try {
            return ByteBuffer.allocateDirect(size);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    @Override
    public long getAvailableMemoryMB() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    @Override
    protected FileStat statLocalFile(String virtualPath) throws Exception {
        Path p = resolver.resolve(virtualPath);
        if (!Files.exists(p)) {
            return new FileStat(false, false, 0L, 0L);
        }
        boolean isDir = Files.isDirectory(p);
        long size = isDir ? 0L : Files.size(p);
        long lastMod = Files.getLastModifiedTime(p).toMillis();
        return new FileStat(true, isDir, size, lastMod);
    }

    @Override
    protected boolean renameLocalFile(String srcVirtualPath, String dstVirtualPath) throws Exception {
        Path src = resolver.resolve(srcVirtualPath);
        Path dst = resolver.resolve(dstVirtualPath);
        if (!Files.exists(src) || resolver.isRoot(src)) {
            return false;
        }
        Path parent = dst.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    protected boolean deleteLocalFile(String virtualPath) throws Exception {
        Path target = resolver.resolve(virtualPath);
        if (resolver.isRoot(target)) {
            return false;
        }
        return deleteRecursive(target.toFile());
    }

    private boolean deleteRecursive(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    @Override
    protected boolean mkdir(String parentVirtualPath, String child) throws Exception {
        Path target;
        if (child == null || child.isEmpty()) {
            target = resolver.resolve(parentVirtualPath);
        } else {
            String combined = parentVirtualPath.endsWith("/") ? (parentVirtualPath + child) : (parentVirtualPath + "/" + child);
            target = resolver.resolve(combined);
        }
        if (Files.exists(target)) {
            return Files.isDirectory(target);
        }
        Files.createDirectories(target);
        return true;
    }

    @Override
    protected List<RemoteFile> listFiles(String virtualPath) throws Exception {
        Path dir = resolver.resolve(virtualPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return null;
        }
        File[] files = dir.toFile().listFiles();
        if (files == null) {
            return null;
        }
        List<RemoteFile> list = new ArrayList<>(files.length);
        for (File file : files) {
            if (file.getName().endsWith(".hfxtmp")) {
                continue;
            }
            String vPath = resolver.toVirtualPath(file.toPath());
            list.add(new RemoteFile(file.getName(), vPath, file.lastModified(), file.isDirectory() ? 0L : file.length(), file.isDirectory()));
        }
        return list;
    }

    @Override
    protected RemoteFile resolveRemoteFile(String virtualPath) throws IOException {
        Path real = resolver.resolve(virtualPath);
        File file = real.toFile();
        String vPath = resolver.toVirtualPath(real);
        return new RemoteFile(file.getName(), vPath, file.lastModified(), file.isDirectory() ? 0L : file.length(), file.isDirectory());
    }

    @Override
    protected boolean receiveFiles(TransferFileCallback callback) throws IOException {
        JdkWriteFileCall writeFileCall = new JdkWriteFileCall(buffers, connections.size(), resolver);
        return receiveWithWriteCall(
                writeFileCall,
                callback,
                writeFileCall::commitAtomicFiles,
                writeFileCall::cleanupTmpFiles
        );
    }

    @Override
    protected WriteFileCall createWriteFileCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount) {
        return new JdkWriteFileCall(buffers, dequeCount, resolver);
    }

    @Override
    protected ReadFileCall createReadFileCall(LinkedBlockingDeque<ByteBuffer> buffers, List<RemoteFile> files, Directory localDir, Directory remoteDir, int operateThreadCount) {
        return new JdkReadFileCall(buffers, files, localDir, remoteDir, operateThreadCount, resolver);
    }

    @Override
    protected void onStreamBegin(String streamId, String targetVirtualPath, long lastModified) throws Exception {
        Path finalPath = resolver.resolve(targetVirtualPath);
        StreamSession old = activeStreams.remove(streamId);
        if (old != null) {
            old.closeHandleOnly();
        }
        StreamSession session = new StreamSession(streamId, finalPath, lastModified, false);
        activeStreams.put(streamId, session);
    }

    @Override
    protected long onStreamResume(String streamId, String targetVirtualPath, long lastModified) throws Exception {
        Path finalPath = resolver.resolve(targetVirtualPath);
        StreamSession existing = activeStreams.get(streamId);
        if (existing != null) {
            return existing.currentSize();
        }
        StreamSession session = new StreamSession(streamId, finalPath, lastModified, true);
        activeStreams.put(streamId, session);
        return session.currentSize();
    }

    @Override
    protected boolean onStreamAppendChunk(String streamId, long chunkOffset, long chunkLength, long expectedCrc32) throws IOException {
        StreamSession session = activeStreams.get(streamId);
        if (session == null) {
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF("Stream session not found: " + streamId);
            return true;
        }
        StreamWriteFileCall writeCall = new StreamWriteFileCall(buffers, connections.size(), session, chunkOffset, chunkLength);
        return receiveWithWriteCall(
                writeCall,
                callBack,
                () -> {
                    long actualCrc32 = session.computeCrc32(chunkOffset, chunkLength);
                    if (actualCrc32 != expectedCrc32) {
                        throw new IOException("Chunk CRC32 mismatch at offset " + chunkOffset
                                + ": expected=" + Long.toHexString(expectedCrc32)
                                + ", actual=" + Long.toHexString(actualCrc32));
                    }
                },
                null
        );
    }

    @Override
    protected void onStreamCommit(String streamId, long expectedTotalSize) throws Exception {
        StreamSession session = activeStreams.remove(streamId);
        if (session == null) {
            throw new IOException("Stream session not found for commit: " + streamId);
        }
        try {
            session.commit(expectedTotalSize);
        } catch (Exception e) {
            session.closeHandleOnly();
            throw e;
        }
    }

    @Override
    protected void onStreamAbort(String streamId) {
        StreamSession session = activeStreams.remove(streamId);
        if (session != null) {
            session.abort();
        }
    }

    @Override
    protected boolean onStreamContinuous(String targetVirtualPath, long lastModified, boolean verifyCrc32) throws IOException {
        return ContinuousStreamReceiver.receiveContinuousStream(
                ctChannel,
                connections,
                buffers,
                resolver,
                targetVirtualPath,
                lastModified,
                verifyCrc32,
                callBack
        );
    }

    @Override
    protected void onSessionClosed() {
        // 断线仅关闭句柄，不删 .hfxtmp，保留以便手机重连后 STREAM_RESUME 续传
        for (StreamSession session : activeStreams.values()) {
            session.closeHandleOnly();
        }
        activeStreams.clear();
    }
}
