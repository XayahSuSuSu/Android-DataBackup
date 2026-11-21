package top.weixiansen574.hybridfilexfer.jdkcore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingDeque;

import top.weixiansen574.hybridfilexfer.core.FileBlock;
import top.weixiansen574.hybridfilexfer.core.WriteFileCall;

public class StreamWriteFileCall extends WriteFileCall {
    private final StreamSession session;
    private final long chunkOffset;
    private final long expectedChunkLength;

    public StreamWriteFileCall(
            LinkedBlockingDeque<ByteBuffer> buffers,
            int dequeCount,
            StreamSession session,
            long chunkOffset,
            long expectedChunkLength
    ) {
        super(buffers, dequeCount);
        this.session = session;
        this.chunkOffset = chunkOffset;
        this.expectedChunkLength = expectedChunkLength;
    }

    @Override
    public Void call() throws Exception {
        long writtenBytes = 0;
        try {
            FileBlock block = takeBlock();
            while (block != null) {
                if (block.isFile && block.data != null) {
                    ByteBuffer data = block.data;
                    data.flip();
                    int len = data.remaining();
                    if (len > 0) {
                        session.writeAt(chunkOffset + block.getStartPosition(), data);
                        writtenBytes += len;
                    }
                    recycleBuffer(block.data);
                }
                block = takeBlock();
            }
            if (!canceled && writtenBytes != expectedChunkLength) {
                throw new IOException("Chunk written length mismatch: expected=" + expectedChunkLength + ", actual=" + writtenBytes);
            }
        } catch (Exception e) {
            cancel();
            throw e;
        }
        return null;
    }

    @Override
    protected void createParentDirIfNotExists(String path) {
    }

    @Override
    protected void tryMkdirs(String path) {
    }

    @Override
    protected FileChannel createAndOpenFile(String path, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void closeFile() {
    }

    @Override
    protected boolean setFileLastModified(String path, long time) {
        return true;
    }
}

