package top.weixiansen574.hybridfilexfer.core;

import java.io.File;
import java.nio.ByteBuffer;

public class FileBlock implements Comparable<FileBlock> {
    public static final int BLOCK_SIZE = 1024*1024;//1MB

    public final boolean isFile;
    public final int fileIndex;
    public final String path;
    public final long lastModified;
    public final long totalSize;
    public final int index;
    public final ByteBuffer data;

    public FileBlock(boolean isFile, int fileIndex, String path, long lastModified, long totalSize, int index, ByteBuffer data) {
        this.isFile = isFile;
        this.fileIndex = fileIndex;
        this.path = path;
        this.lastModified = lastModified;
        this.totalSize = totalSize;
        this.index = index;
        this.data = data;
    }

    public long getStartPosition(){
        return  BLOCK_SIZE * ((long) index);
    }

    public long calcBlockCount(){
        return totalSize / BLOCK_SIZE + 1;
    }

    public boolean isFile(){
        return isFile;
    }

    public boolean isDirectory(){
        return !isFile;
    }

    public int getLength(){
        if (data == null){
            return -1;
        }
        return data.position();
    }

    @Override
    public int compareTo(FileBlock other) {
        if (this.fileIndex != other.fileIndex) {
            return Integer.compare(this.fileIndex, other.fileIndex);
        }
        // 如果 nameIndex 相同，比较 index
        return Integer.compare(this.index, other.index);
    }
}
