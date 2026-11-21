package top.weixiansen574.hybridfilexfer.core;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;

public abstract class ReadFileCall implements Callable<Void> {
    public static final FileBlock END_POINT = new FileBlock(true, -1, "END_POINT", 0, 0, -1, null);
    public static final FileBlock INTERRUPT = new FileBlock(true, -1, "INTERRUPT", 0, 0, -1, null);
    public static final FileBlock READ_ERROR = new FileBlock(true, -1, "READ_ERROR", 0, 0, -1, null);
    public static final FileBlock WRITE_ERROR = new FileBlock(true, -1, "WRITE_ERROR", 0, 0, -1, null);

    protected final LinkedBlockingDeque<FileBlock> deque = new LinkedBlockingDeque<>();
    protected final LinkedBlockingDeque<ByteBuffer> buffers;
    protected final List<RemoteFile> files;
    protected final Directory localDir;
    protected final Directory remoteDir;
    protected final int operateThreadCount;
    protected int fileIndex = -1;
    protected volatile boolean cancelled = false;
    private volatile Thread workerThread;

    public ReadFileCall(LinkedBlockingDeque<ByteBuffer> buffers, List<RemoteFile> files, Directory localDir, Directory remoteDir, int operateThreadCount) {
        this.buffers = buffers;
        this.files = files;
        this.localDir = localDir;
        this.remoteDir = remoteDir;
        this.operateThreadCount = operateThreadCount;
    }

    @Override
    public Void call() throws Exception {
        workerThread = Thread.currentThread();
        try {
            produceBlocks();
            if (!cancelled) {
                for (int i = 0; i < operateThreadCount; i++) {
                    deque.add(END_POINT);
                }
            }
        } catch (InterruptedException e) {
            if (!cancelled) {
                for (int i = 0; i < operateThreadCount; i++) {
                    deque.add(READ_ERROR);
                }
                throw e;
            }
        } catch (Exception e) {
            if (!cancelled) {
                for (int i = 0; i < operateThreadCount; i++) {
                    deque.add(READ_ERROR);
                }
            }
            throw e;
        }
        return null;
    }

    protected void produceBlocks() throws Exception {
        for (RemoteFile file : files) {
            if (cancelled) return;
            if (!fileExists(file.getPath())) {
                continue;
            }
            readToDeque(file);
            if (file.isDirectory()) {
                listFilesAndRead(file);
            }
        }
    }

    private void listFilesAndRead(RemoteFile folder) throws Exception {
        List<RemoteFile> subFiles = listFiles(folder.getPath());
        if (subFiles != null) {
            for (RemoteFile file : subFiles) {
                if (cancelled) return;
                readToDeque(file);
                if (file.isDirectory()) {
                    listFilesAndRead(file);
                }
            }
        }
    }

    private void readToDeque(RemoteFile file) throws Exception {
        if (cancelled) return;
        fileIndex++;
        if (file.isDirectory()) {
            deque.add(new FileBlock(false,
                    fileIndex, localDir.generateTransferPath(file.getPath(), remoteDir),
                    file.lastModified(), 0, 0, null));
            return;
        }
        FileChannel channel = openFile(file.getPath());
        try {
            long length = channel.size();
            long lastModified = file.lastModified();
            String transferPath = localDir.generateTransferPath(file.getPath(), remoteDir);
            readChannelToDeque(channel, transferPath, lastModified, length);
        } finally {
            try {
                closeFile();
            } catch (Exception ignored) {
            }
        }
    }

    protected void readChannelToDeque(FileChannel channel, String transferPath, long lastModified, long length) throws Exception {
        long remaining = length;
        if (length == 0) {
            ByteBuffer buffer = buffers.take();
            if (cancelled) {
                recycleBuffer(buffer);
                return;
            }
            buffer.clear();
            buffer.limit(0);
            deque.add(new FileBlock(true, fileIndex, transferPath, lastModified, 0, 0, buffer));
            return;
        }
        int i = 0;
        while (remaining > 0) {
            if (cancelled) return;
            int blkSize = (int) Math.min(remaining, FileBlock.BLOCK_SIZE);
            ByteBuffer buffer = buffers.take();
            if (cancelled) {
                recycleBuffer(buffer);
                return;
            }
            boolean enqueued = false;
            try {
                buffer.clear();
                buffer.limit(blkSize);
                while (buffer.hasRemaining()) {
                    if (cancelled) return;
                    int r = channel.read(buffer);
                    if (r == -1) {
                        throw new EOFException("Unexpected EOF while reading " + transferPath);
                    }
                }
                deque.add(new FileBlock(true, fileIndex, transferPath, lastModified, length, i, buffer));
                enqueued = true;
            } finally {
                if (!enqueued) {
                    recycleBuffer(buffer);
                }
            }
            remaining -= blkSize;
            i++;
        }
    }

    public void recycleBuffer(ByteBuffer buffer) {
        if (buffer != null) {
            buffers.add(buffer);
        }
    }

    public FileBlock takeBlock() throws InterruptedException {
        return deque.take();
    }

    public void shutdownByWriteError() {
        cancelled = true;
        Thread t = workerThread;
        if (t != null) t.interrupt();
        recycleAllBuffer();
        for (int i = 0; i < operateThreadCount; i++) {
            deque.addFirst(WRITE_ERROR);
        }
    }

    public void shutdownByConnectionBreak() {
        cancelled = true;
        Thread t = workerThread;
        if (t != null) t.interrupt();
        recycleAllBuffer();
        for (int i = 0; i < operateThreadCount; i++) {
            deque.addFirst(INTERRUPT);
        }
    }

    private void recycleAllBuffer() {
        FileBlock fileBlock;
        while ((fileBlock = deque.poll()) != null) {
            if (fileBlock.data != null) {
                recycleBuffer(fileBlock.data);
            }
        }
    }

    protected abstract boolean fileExists(String path) throws Exception;

    protected abstract List<RemoteFile> listFiles(String path) throws Exception;

    protected abstract FileChannel openFile(String path) throws Exception;

    protected abstract void closeFile() throws Exception;
}
