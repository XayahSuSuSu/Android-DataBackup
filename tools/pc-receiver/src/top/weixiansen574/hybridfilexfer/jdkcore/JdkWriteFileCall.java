package top.weixiansen574.hybridfilexfer.jdkcore;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import top.weixiansen574.hybridfilexfer.core.WriteFileCall;

public class JdkWriteFileCall extends WriteFileCall {
    private static class PendingEntry {
        final Path tmpPath;
        long lastModified;

        PendingEntry(Path tmpPath, long lastModified) {
            this.tmpPath = tmpPath;
            this.lastModified = lastModified;
        }
    }

    private final VirtualPathResolver resolver;
    private final Map<Path, PendingEntry> pendingAtomicFiles = new LinkedHashMap<>();
    private RandomAccessFile currentRaf;
    private FileChannel currentChannel;

    public JdkWriteFileCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount, VirtualPathResolver resolver) {
        super(buffers, dequeCount);
        this.resolver = resolver;
    }

    @Override
    protected void createParentDirIfNotExists(String path) throws Exception {
        Path finalPath = resolver.resolve(path);
        Path parent = finalPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    @Override
    protected void tryMkdirs(String path) throws Exception {
        Path dirPath = resolver.resolve(path);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }

    @Override
    protected FileChannel createAndOpenFile(String path, long length) throws Exception {
        Path finalPath = resolver.resolve(path);
        Path tmpPath = finalPath.resolveSibling(finalPath.getFileName().toString() + ".hfxtmp");
        PendingEntry existing = pendingAtomicFiles.get(finalPath);
        if (existing == null) {
            pendingAtomicFiles.put(finalPath, new PendingEntry(tmpPath, 0L));
        }
        currentRaf = new RandomAccessFile(tmpPath.toFile(), "rw");
        if (currentRaf.length() != length) {
            currentRaf.setLength(length);
        }
        currentChannel = currentRaf.getChannel();
        return currentChannel;
    }

    @Override
    protected void closeFile() throws Exception {
        if (currentChannel != null) {
            currentChannel.close();
            currentChannel = null;
        }
        if (currentRaf != null) {
            currentRaf.close();
            currentRaf = null;
        }
    }

    @Override
    protected boolean setFileLastModified(String path, long time) throws Exception {
        Path finalPath = resolver.resolve(path);
        PendingEntry entry = pendingAtomicFiles.get(finalPath);
        if (entry != null) {
            entry.lastModified = time;
            return true;
        }
        if (Files.exists(finalPath) && time > 0) {
            Files.setLastModifiedTime(finalPath, FileTime.fromMillis(time));
        }
        return true;
    }

    public void commitAtomicFiles() throws IOException {
        for (Map.Entry<Path, PendingEntry> e : pendingAtomicFiles.entrySet()) {
            Path finalPath = e.getKey();
            PendingEntry pending = e.getValue();
            if (Files.exists(pending.tmpPath)) {
                if (pending.lastModified > 0) {
                    try {
                        Files.setLastModifiedTime(pending.tmpPath, FileTime.fromMillis(pending.lastModified));
                    } catch (IOException ignored) {
                    }
                }
                try {
                    Files.move(pending.tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(pending.tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        pendingAtomicFiles.clear();
    }

    public void cleanupTmpFiles() {
        for (PendingEntry pending : pendingAtomicFiles.values()) {
            try {
                Files.deleteIfExists(pending.tmpPath);
            } catch (IOException ignored) {
            }
        }
        pendingAtomicFiles.clear();
    }
}
