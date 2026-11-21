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
import java.util.zip.CRC32;

public class StreamSession {
    public final String streamId;
    public final Path finalPath;
    public final Path tmpPath;
    public final long lastModified;
    private final RandomAccessFile raf;
    private final FileChannel channel;
    private boolean closed = false;

    public StreamSession(String streamId, Path finalPath, long lastModified, boolean resume) throws IOException {
        this.streamId = streamId;
        this.finalPath = finalPath.toAbsolutePath().normalize();
        this.tmpPath = this.finalPath.resolveSibling(this.finalPath.getFileName().toString() + ".hfxtmp");
        this.lastModified = lastModified;
        Path parent = this.finalPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        if (resume && !Files.exists(this.tmpPath)) {
            throw new IOException("tmp file not found for resume: " + this.tmpPath);
        }
        this.raf = new RandomAccessFile(this.tmpPath.toFile(), "rw");
        if (!resume) {
            this.raf.setLength(0L);
        }
        this.channel = this.raf.getChannel();
    }

    public synchronized long currentSize() throws IOException {
        return channel.size();
    }

    public synchronized void writeAt(long offset, ByteBuffer data) throws IOException {
        channel.position(offset);
        while (data.hasRemaining()) {
            int written = channel.write(data);
            if (written < 0) {
                throw new IOException("Disk write EOF for stream " + streamId);
            }
        }
    }

    public synchronized long computeCrc32(long offset, long length) throws IOException {
        CRC32 crc32 = new CRC32();
        if (length == 0) {
            return crc32.getValue();
        }
        byte[] buf = new byte[(int) Math.min(1024 * 1024, length)];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        long remaining = length;
        long pos = offset;
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            bb.clear();
            bb.limit(toRead);
            channel.position(pos);
            while (bb.hasRemaining()) {
                int r = channel.read(bb);
                if (r == -1) {
                    throw new IOException("Unexpected EOF while computing CRC32 at offset " + pos);
                }
            }
            crc32.update(buf, 0, toRead);
            pos += toRead;
            remaining -= toRead;
        }
        return crc32.getValue();
    }

    public synchronized void commit(long expectedTotalSize) throws IOException {
        long actualSize = channel.size();
        if (actualSize != expectedTotalSize) {
            throw new IOException("Stream size mismatch: expected=" + expectedTotalSize + ", actual=" + actualSize);
        }
        channel.force(true);
        closeHandleOnly();
        if (lastModified > 0) {
            try {
                Files.setLastModifiedTime(tmpPath, FileTime.fromMillis(lastModified));
            } catch (IOException ignored) {
            }
        }
        try {
            Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized void abort() {
        closeHandleOnly();
        try {
            Files.deleteIfExists(tmpPath);
        } catch (IOException ignored) {
        }
    }

    public synchronized void closeHandleOnly() {
        if (closed) return;
        closed = true;
        try {
            channel.close();
        } catch (IOException ignored) {
        }
        try {
            raf.close();
        } catch (IOException ignored) {
        }
    }
}

