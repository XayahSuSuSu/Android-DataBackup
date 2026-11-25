package top.weixiansen574.hybridfilexfer.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class WriteFileCall implements Callable<Void> {
    protected final LinkedBlockingDeque<ByteBuffer> buffers;
    private final boolean[] channelFinished;
    private final ArrayList<LinkedList<FileBlock>> dequeArray;
    protected boolean canceled = false;

    public WriteFileCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount) {
        this.buffers = buffers;
        dequeArray = new ArrayList<>(dequeCount);
        channelFinished = new boolean[dequeCount];
        for (int i = 0; i < dequeCount; i++) {
            dequeArray.add(new LinkedList<>());
        }
    }

    @Override
    public Void call() throws Exception {
        FileBlock lastBlock = null;
        FileChannel lastChannel = null;
        try {
            FileBlock block = takeBlock();
            long cursor = 0;

            while (block != null) {
                if (block.isDirectory()) {
                    String file = block.path;
                    tryMkdirs(file);
                    setLastModified(file, block.lastModified);
                    block = takeBlock();
                    continue;
                }
                createParentDirIfNotExists(block.path);
                FileChannel channel;
                if (lastBlock == null || !lastBlock.path.equals(block.path)) {
                    if (lastChannel != null) {
                        closeFile();
                        lastChannel = null;
                        setLastModified(lastBlock.path, lastBlock.lastModified);
                    }
                    channel = createAndOpenFile(block.path, block.totalSize);
                    cursor = 0;
                } else {
                    channel = lastChannel;
                }
                if (cursor != block.getStartPosition()) {
                    cursor = block.getStartPosition();
                    channel.position(cursor);
                }

                ByteBuffer data = block.data;
                data.flip();
                while (data.hasRemaining()) {
                    int written = channel.write(data);
                    if (written < 0) {
                        throw new IOException("Disk write EOF for " + block.path);
                    }
                    cursor += written;
                }
                recycleBuffer(block.data);
                lastBlock = block;
                lastChannel = channel;
                block = takeBlock();
            }
            if (lastChannel != null) {
                closeFile();
                lastChannel = null;
                if (lastBlock != null) {
                    setLastModified(lastBlock.path, lastBlock.lastModified);
                }
            }
        } catch (Exception e) {
            cancel();
            throw e;
        } finally {
            if (lastChannel != null) {
                try {
                    closeFile();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public ByteBuffer getBuffer() throws InterruptedException {
        return buffers.take();
    }

    public void recycleBuffer(ByteBuffer buffer) {
        if (buffer != null) {
            buffers.add(buffer);
        }
    }

    public synchronized void finishChannel(int tIndex) {
        channelFinished[tIndex] = true;
        notifyAll();
    }

    public synchronized void cancel() {
        canceled = true;
        for (LinkedList<FileBlock> deque : dequeArray) {
            for (FileBlock fileBlock : deque) {
                if (fileBlock.data != null) {
                    buffers.add(fileBlock.data);
                }
            }
            deque.clear();
        }
        notifyAll();
    }

    public synchronized void putBlock(FileBlock block, int tIndex) {
        if (canceled) {
            if (block != null && block.data != null) {
                buffers.add(block.data);
            }
            return;
        }
        dequeArray.get(tIndex).add(block);
        notifyAll();
    }

    protected synchronized FileBlock takeBlock() throws InterruptedException {
        while (true) {
            if (canceled) {
                return null;
            }
            FileBlock block = tryTakeBlockInternal();
            if (block != null) return block;

            if (allChannelsFinished() && allQueuesEmpty()) {
                return null;
            }

            wait();
        }
    }

    public synchronized FileBlock tryTakeBlockInternal() {
        FileBlock minHead = null;
        int mdqIndex = -1;

        for (int i = 0; i < dequeArray.size(); i++) {
            LinkedList<FileBlock> deque = dequeArray.get(i);
            if (!deque.isEmpty()) {
                FileBlock head = deque.getFirst();
                if (minHead == null || head.compareTo(minHead) < 0) {
                    minHead = head;
                    mdqIndex = i;
                }
            }
        }
        if (minHead != null) {
            dequeArray.get(mdqIndex).removeFirst();
        }
        return minHead;
    }

    private boolean allChannelsFinished() {
        for (boolean finished : channelFinished) {
            if (!finished) return false;
        }
        return true;
    }

    private boolean allQueuesEmpty() {
        for (LinkedList<FileBlock> deque : dequeArray) {
            if (!deque.isEmpty()) return false;
        }
        return true;
    }

    private void setLastModified(String file, long time) throws Exception {
        if (!setFileLastModified(file, time)) {
            System.out.println("Warning! file cannot set last modified:" + file);
        }
    }

    protected abstract void createParentDirIfNotExists(String path) throws Exception;

    protected abstract void tryMkdirs(String path) throws Exception;

    protected abstract FileChannel createAndOpenFile(String path, long length) throws Exception;

    protected abstract void closeFile() throws Exception;

    protected abstract boolean setFileLastModified(String path, long time) throws Exception;
}
