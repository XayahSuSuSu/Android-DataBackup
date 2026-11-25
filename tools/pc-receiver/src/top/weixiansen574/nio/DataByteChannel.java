package top.weixiansen574.nio;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;

public class DataByteChannel implements ByteChannel, DataInput, DataOutput {
    private final ByteChannel origin;
    private final ByteBuffer buffer = ByteBuffer.allocate(8);

    public DataByteChannel(ByteChannel origin) {
        this.origin = origin;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return origin.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return origin.write(src);
    }

    @Override
    public boolean isOpen() {
        return origin.isOpen();
    }

    @Override
    public void close() throws IOException {
        origin.close();
    }

    public void readFully(ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            if (origin.read(dst) == -1) {
                throw new EOFException();
            }
        }
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(ByteBuffer.wrap(b));
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        readFully(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public int skipBytes(int n) throws IOException {
        ByteBuffer skipBuffer = ByteBuffer.allocate(n);
        return origin.read(skipBuffer);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        buffer.clear().limit(1);
        readFully(buffer);
        buffer.flip();
        return buffer.get();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        buffer.clear().limit(2);
        readFully(buffer);
        buffer.flip();
        return buffer.getShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    @Override
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public int readInt() throws IOException {
        buffer.clear().limit(4);
        readFully(buffer);
        buffer.flip();
        return buffer.getInt();
    }

    @Override
    public long readLong() throws IOException {
        buffer.clear().limit(8);
        readFully(buffer);
        buffer.flip();
        return buffer.getLong();
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException("readLine() is not implemented.");
    }

    @Override
    public String readUTF() throws IOException {
        int length = readUnsignedShort();
        byte[] bytes = new byte[length];
        readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void write(int b) throws IOException {
        writeByte(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void writeFully(ByteBuffer src) throws IOException {
        while (src.hasRemaining()) {
            int written = origin.write(src);
            if (written < 0) {
                throw new IOException("Channel closed during write");
            }
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeFully(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        writeByte(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        buffer.clear();
        buffer.put((byte) v);
        buffer.flip();
        writeFully(buffer);
    }

    @Override
    public void writeShort(int v) throws IOException {
        buffer.clear();
        buffer.putShort((short) v);
        buffer.flip();
        writeFully(buffer);
    }

    @Override
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        buffer.clear();
        buffer.putInt(v);
        buffer.flip();
        writeFully(buffer);
    }

    @Override
    public void writeLong(long v) throws IOException {
        buffer.clear();
        buffer.putLong(v);
        buffer.flip();
        writeFully(buffer);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        write(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void writeChars(String s) throws IOException {
        for (char c : s.toCharArray()) {
            writeChar(c);
        }
    }

    @Override
    public void writeUTF(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65535) {
            throw new IOException("String too long");
        }
        writeShort(bytes.length);
        write(bytes);
    }
}
