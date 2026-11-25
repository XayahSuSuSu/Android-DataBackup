package top.weixiansen574.hybridfilexfer.core;

import java.nio.ByteBuffer;

public class ContinuousStreamBlock implements Comparable<ContinuousStreamBlock> {
    public static final ContinuousStreamBlock END_POINT = new ContinuousStreamBlock(-1L, 0, 0, null);
    public static final ContinuousStreamBlock INTERRUPT = new ContinuousStreamBlock(-2L, 0, 0, null);
    public static final ContinuousStreamBlock READ_ERROR = new ContinuousStreamBlock(-3L, 0, 0, null);
    public static final ContinuousStreamBlock WRITE_ERROR = new ContinuousStreamBlock(-4L, 0, 0, null);

    public final long offset;
    public final int length;
    public final int crc32;
    public final ByteBuffer data;

    public ContinuousStreamBlock(long offset, int length, int crc32, ByteBuffer data) {
        this.offset = offset;
        this.length = length;
        this.crc32 = crc32;
        this.data = data;
    }

    @Override
    public int compareTo(ContinuousStreamBlock other) {
        return Long.compare(this.offset, other.offset);
    }
}

