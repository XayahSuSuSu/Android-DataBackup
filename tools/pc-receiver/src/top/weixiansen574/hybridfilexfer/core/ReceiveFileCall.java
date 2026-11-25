package top.weixiansen574.hybridfilexfer.core;

import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;
import top.weixiansen574.nio.DataByteChannel;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

public class ReceiveFileCall implements Callable<Void> {
    private final WriteFileCall writeFileCall;
    private final int tIndex;
    private final DataByteChannel channel;
    private final TransferFileCallback callback;
    private final TransferConnection connection;
    private final String iName;

    public ReceiveFileCall(int tIndex, TransferConnection connection,WriteFileCall writeFileCall,TransferFileCallback callback) {
        this.writeFileCall = writeFileCall;
        this.tIndex = tIndex;
        this.connection = connection;
        this.channel = connection.channel;
        this.callback = callback;
        iName = connection.iName;
        connection.resetTotalTrafficInfo();
        //this.dis = dataInputStream;
    }

    @Override
    public Void call() throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            while (true) {
                short header = channel.readShort();
                switch (header) {
                    case TransferIdentifiers.FOLDER: {
                        int fileIndex = channel.readInt();
                        String path = channel.readUTF();
                        long lastModified = channel.readLong();
                        writeFileCall.putBlock(new FileBlock(false, fileIndex, path, lastModified, 0, 0, null), tIndex);
                        break;
                    }
                    case TransferIdentifiers.FILE: {
                        int fileIndex = channel.readInt();
                        String path = channel.readUTF();
                        long lastModified = channel.readLong();
                        long totalSize = channel.readLong();
                        int index = channel.readInt();
                        int length = channel.readInt();
                        callback.onFileDownloading(iName, path,
                                index * (long) FileBlock.BLOCK_SIZE + length,
                                totalSize);
                        ByteBuffer buffer = writeFileCall.getBuffer();
                        boolean enqueued = false;
                        try {
                            buffer.clear();
                            buffer.limit(length);
                            int read;
                            while (buffer.hasRemaining()) {
                                read = channel.read(buffer);
                                if (read == -1) {
                                    throw new EOFException();
                                }
                                connection.addDownloadedBytes(read);
                            }
                            writeFileCall.putBlock(new FileBlock(true, fileIndex, path, lastModified, totalSize, index, buffer), tIndex);
                            enqueued = true;
                        } finally {
                            if (!enqueued && buffer != null) {
                                writeFileCall.recycleBuffer(buffer);
                            }
                        }
                        break;
                    }
                    case TransferIdentifiers.EOF:
                        //System.out.println(iName + " 接收完成");
                        writeFileCall.finishChannel(tIndex);
                        callback.onChannelComplete(iName,
                                connection.getTotalTraffic().downloadTraffic,
                                System.currentTimeMillis() - startTime);
                        return null;
                    case TransferIdentifiers.END_OF_INTERRUPTED:
                        //System.out.println("传输通道：" + iName + " 已中断，因其他通道断开");
                        writeFileCall.cancel();
                        callback.onChannelError(iName,
                                TransferFileCallback.ERROR_TYPE_INTERRUPT,null);
                        return null;
                    case TransferIdentifiers.END_OF_READ_ERROR:
                        //System.out.println("传输通道：" + iName + " 已取消传输，因为读取文件时发生异常");
                        writeFileCall.cancel();
                        callback.onChannelError(iName,
                                TransferFileCallback.ERROR_TYPE_READ_ERROR,null);
                        return null;
                    case TransferIdentifiers.END_OF_WRITE_ERROR:
                        //System.out.println("传输通道：" + iName + " 已取消传输，因为写入文件时发生异常");
                        writeFileCall.cancel();
                        callback.onChannelError(iName,
                                TransferFileCallback.ERROR_TYPE_WRITE_ERROR,null);
                        return null;
                }
            }
        } catch (IOException e){
            writeFileCall.finishChannel(tIndex);
            callback.onChannelError(iName,
                    TransferFileCallback.ERROR_TYPE_EXCEPTION,e.toString());
            throw e;
        }
    }
}
