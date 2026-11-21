package top.weixiansen574.hybridfilexfer.jdkcore;

import top.weixiansen574.hybridfilexfer.core.ContinuousStreamBlock;
import top.weixiansen574.hybridfilexfer.core.SpeedMonitorThread;
import top.weixiansen574.hybridfilexfer.core.TransferConnection;
import top.weixiansen574.hybridfilexfer.core.TransferIdentifiers;
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;
import top.weixiansen574.nio.DataByteChannel;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class ContinuousStreamReceiver {

    private static class ContinuousWriteCall implements Callable<Void> {
        private final LinkedBlockingDeque<ByteBuffer> buffers;
        private final boolean[] channelFinished;
        private final ArrayList<LinkedList<ContinuousStreamBlock>> dequeArray;
        private final FileChannel channel;
        private boolean canceled = false;

        ContinuousWriteCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount, FileChannel channel) {
            this.buffers = buffers;
            this.channel = channel;
            this.channelFinished = new boolean[dequeCount];
            this.dequeArray = new ArrayList<>(dequeCount);
            for (int i = 0; i < dequeCount; i++) {
                this.dequeArray.add(new LinkedList<>());
            }
        }

        ByteBuffer getBuffer() throws InterruptedException {
            return buffers.take();
        }

        void recycleBuffer(ByteBuffer buf) {
            if (buf != null) buffers.add(buf);
        }

        synchronized void finishChannel(int idx) {
            channelFinished[idx] = true;
            notifyAll();
        }

        synchronized void cancel() {
            canceled = true;
            for (LinkedList<ContinuousStreamBlock> dq : dequeArray) {
                for (ContinuousStreamBlock b : dq) {
                    if (b.data != null) buffers.add(b.data);
                }
                dq.clear();
            }
            notifyAll();
        }

        synchronized void putBlock(ContinuousStreamBlock block, int idx) {
            if (canceled) {
                if (block != null && block.data != null) buffers.add(block.data);
                return;
            }
            dequeArray.get(idx).add(block);
            notifyAll();
        }

        private synchronized ContinuousStreamBlock takeBlock() throws InterruptedException {
            while (true) {
                if (canceled) return null;
                ContinuousStreamBlock minHead = null;
                int minIdx = -1;
                for (int i = 0; i < dequeArray.size(); i++) {
                    LinkedList<ContinuousStreamBlock> dq = dequeArray.get(i);
                    if (!dq.isEmpty()) {
                        ContinuousStreamBlock head = dq.getFirst();
                        if (minHead == null || head.compareTo(minHead) < 0) {
                            minHead = head;
                            minIdx = i;
                        }
                    }
                }
                if (minHead != null) {
                    dequeArray.get(minIdx).removeFirst();
                    return minHead;
                }
                boolean allDone = true;
                for (boolean f : channelFinished) {
                    if (!f) {
                        allDone = false;
                        break;
                    }
                }
                if (allDone) return null;
                wait();
            }
        }

        @Override
        public Void call() throws Exception {
            long cursor = 0L;
            try {
                ContinuousStreamBlock block = takeBlock();
                while (block != null) {
                    ByteBuffer data = block.data;
                    try {
                        if (cursor != block.offset) {
                            channel.position(block.offset);
                            cursor = block.offset;
                        }
                        data.flip();
                        while (data.hasRemaining()) {
                            int w = channel.write(data);
                            if (w < 0) throw new IOException("Disk write EOF");
                            cursor += w;
                        }
                    } finally {
                        recycleBuffer(data);
                    }
                    block = takeBlock();
                }
            } catch (Exception e) {
                cancel();
                throw e;
            }
            return null;
        }
    }

    private static class ContinuousReceiveCall implements Callable<Void> {
        private final int tIndex;
        private final TransferConnection connection;
        private final ContinuousWriteCall writeCall;
        private final TransferFileCallback callback;
        private final boolean verifyCrc32;

        ContinuousReceiveCall(int tIndex, TransferConnection connection, ContinuousWriteCall writeCall,
                              TransferFileCallback callback, boolean verifyCrc32) {
            this.tIndex = tIndex;
            this.connection = connection;
            this.writeCall = writeCall;
            this.callback = callback;
            this.verifyCrc32 = verifyCrc32;
            connection.resetTotalTrafficInfo();
        }

        @Override
        public Void call() throws Exception {
            long startTime = System.currentTimeMillis();
            DataByteChannel channel = connection.channel;
            ByteBuffer metaBuf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
            CRC32 crc32 = verifyCrc32 ? new CRC32() : null;
            try {
                while (true) {
                    short header = channel.readShort();
                    if (header == TransferIdentifiers.STREAM_BLOCK) {
                        metaBuf.clear();
                        channel.readFully(metaBuf);
                        metaBuf.flip();
                        long offset = metaBuf.getLong();
                        int length = metaBuf.getInt();
                        int expectedCrc = metaBuf.getInt();

                        ByteBuffer buffer = writeCall.getBuffer();
                        boolean enqueued = false;
                        try {
                            buffer.clear();
                            buffer.limit(length);
                            while (buffer.hasRemaining()) {
                                int r = channel.read(buffer);
                                if (r == -1) throw new EOFException("Unexpected EOF on " + connection.iName);
                                connection.addDownloadedBytes(r);
                            }
                            if (verifyCrc32) {
                                ByteBuffer dup = buffer.duplicate();
                                dup.flip();
                                crc32.reset();
                                crc32.update(dup);
                                int actualCrc = (int) crc32.getValue();
                                if (actualCrc != expectedCrc) {
                                    throw new IOException("Inline CRC32 mismatch at offset " + offset
                                            + " on " + connection.iName);
                                }
                            }
                            writeCall.putBlock(new ContinuousStreamBlock(offset, length, expectedCrc, buffer), tIndex);
                            enqueued = true;
                        } finally {
                            if (!enqueued && buffer != null) {
                                writeCall.recycleBuffer(buffer);
                            }
                        }
                    } else if (header == TransferIdentifiers.EOF) {
                        writeCall.finishChannel(tIndex);
                        callback.onChannelComplete(connection.iName,
                                connection.getTotalTraffic().downloadTraffic,
                                System.currentTimeMillis() - startTime);
                        return null;
                    } else if (header == TransferIdentifiers.END_OF_INTERRUPTED
                            || header == TransferIdentifiers.END_OF_READ_ERROR
                            || header == TransferIdentifiers.END_OF_WRITE_ERROR) {
                        writeCall.cancel();
                        int errType = header == TransferIdentifiers.END_OF_READ_ERROR
                                ? TransferFileCallback.ERROR_TYPE_READ_ERROR
                                : (header == TransferIdentifiers.END_OF_WRITE_ERROR
                                ? TransferFileCallback.ERROR_TYPE_WRITE_ERROR
                                : TransferFileCallback.ERROR_TYPE_INTERRUPT);
                        callback.onChannelError(connection.iName, errType, null);
                        return null;
                    } else {
                        throw new IOException("Unexpected frame header in continuous stream: " + header);
                    }
                }
            } catch (Exception e) {
                writeCall.finishChannel(tIndex);
                callback.onChannelError(connection.iName, TransferFileCallback.ERROR_TYPE_EXCEPTION, e.toString());
                throw e;
            }
        }
    }

    public static boolean receiveContinuousStream(
            DataByteChannel ctChannel,
            List<TransferConnection> connections,
            LinkedBlockingDeque<ByteBuffer> buffers,
            VirtualPathResolver resolver,
            String targetVirtualPath,
            long lastModified,
            boolean verifyCrc32,
            TransferFileCallback callback
    ) throws IOException {
        Path finalPath;
        Path tmpPath;
        RandomAccessFile raf;
        FileChannel fileChannel;
        try {
            finalPath = resolver.resolve(targetVirtualPath);
            tmpPath = finalPath.resolveSibling(finalPath.getFileName().toString() + ".hfxtmp");
            Path parent = finalPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            raf = new RandomAccessFile(tmpPath.toFile(), "rw");
            raf.setLength(0L);
            fileChannel = raf.getChannel();
        } catch (Exception e) {
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(e.toString());
            return true;
        }

        ctChannel.writeBoolean(true);

        long startTime = System.currentTimeMillis();
        ContinuousWriteCall writeCall = new ContinuousWriteCall(buffers, connections.size(), fileChannel);
        SpeedMonitorThread speedMonitorThread = new SpeedMonitorThread(connections, callback);
        speedMonitorThread.setName("SpeedMonitor");
        speedMonitorThread.start();

        List<FutureTask<Void>> recvTasks = new ArrayList<>(connections.size());
        for (int i = 0; i < connections.size(); i++) {
            TransferConnection conn = connections.get(i);
            FutureTask<Void> task = new FutureTask<>(new ContinuousReceiveCall(i, conn, writeCall, callback, verifyCrc32));
            recvTasks.add(task);
            Thread t = new Thread(task, "DL_CS_" + conn.iName);
            t.start();
        }
        FutureTask<Void> writeTask = new FutureTask<>(writeCall);
        Thread wt = new Thread(writeTask, "FileWrite_CS");
        wt.start();

        String writeOrVerifyErr = null;
        boolean channelBroken = false;
        try {
            writeTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Throwable cause = e.getCause();
            writeOrVerifyErr = cause != null ? cause.toString() : e.toString();
            writeCall.cancel();
        }

        for (FutureTask<Void> task : recvTasks) {
            try {
                task.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                String msg = cause != null ? cause.toString() : e.toString();
                if (msg.contains("CRC32 mismatch")) {
                    if (writeOrVerifyErr == null) writeOrVerifyErr = msg;
                } else {
                    channelBroken = true;
                }
                task.cancel(true);
            }
        }
        speedMonitorThread.cancel();

        if (writeOrVerifyErr != null) {
            closeAndDeleteTmp(fileChannel, raf, tmpPath);
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(writeOrVerifyErr);
            callback.onWriteFileError(writeOrVerifyErr);
            return true;
        }

        if (channelBroken) {
            closeAndDeleteTmp(fileChannel, raf, tmpPath);
            try {
                ctChannel.writeBoolean(true);
            } catch (IOException ignored) {
            }
            callback.onIncomplete();
            return false;
        }

        // 阶段 1：通知手机端所有数据已落盘
        ctChannel.writeBoolean(true);

        // 阶段 2：等待手机端确认 tar|zstd 退出码 == 0 并下发期望总长度
        boolean commitOk = ctChannel.readBoolean();
        long expectedTotalSize = ctChannel.readLong();
        if (!commitOk) {
            String errMsg = ctChannel.readUTF();
            closeAndDeleteTmp(fileChannel, raf, tmpPath);
            callback.onReadFileError(errMsg);
            ctChannel.writeBoolean(true);
            return true;
        }

        // 阶段 3：核对长度并原子重命名
        try {
            long actualSize = fileChannel.size();
            if (actualSize != expectedTotalSize) {
                throw new IOException("Continuous stream size mismatch: expected=" + expectedTotalSize + ", actual=" + actualSize);
            }
            fileChannel.close();
            raf.close();
            if (lastModified > 0) {
                try {
                    Files.setLastModifiedTime(tmpPath, FileTime.fromMillis(lastModified));
                } catch (IOException ignored) {
                }
            }
            try {
                Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }
            ctChannel.writeBoolean(true);

            long totalDownload = 0;
            for (TransferConnection c : connections) {
                totalDownload += c.resetTotalTrafficInfo().downloadTraffic;
            }
            callback.onComplete(false, totalDownload, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            closeAndDeleteTmp(fileChannel, raf, tmpPath);
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(e.toString());
            callback.onWriteFileError(e.toString());
        }
        return true;
    }

    private static void closeAndDeleteTmp(FileChannel ch, RandomAccessFile raf, Path tmpPath) {
        try { if (ch != null) ch.close(); } catch (IOException ignored) {}
        try { if (raf != null) raf.close(); } catch (IOException ignored) {}
        try { Files.deleteIfExists(tmpPath); } catch (IOException ignored) {}
    }
}

