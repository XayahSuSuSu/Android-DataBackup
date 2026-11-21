package top.weixiansen574.hybridfilexfer.core.bean;

import java.io.File;

public class RemoteFile {
    protected final String name;
    protected final String path;
    protected final long lastModified;
    protected final long size;
    protected final boolean isDirectory;
    //public final String name;
    //public final String path;
    //public final long lastModified;
    //public final long size;
    //public final boolean isDirectory;

    public RemoteFile(File file) {
        this.path = file.getPath();
        this.isDirectory = file.isDirectory();
        this.name = file.getName();
        this.lastModified = file.lastModified();
        this.size = file.length();
    }

    public RemoteFile(String name, String path, long lastModified, long size, boolean isDirectory){
        this.name = name;
        this.path = path;
        this.lastModified = lastModified;
        this.size = size;
        this.isDirectory = isDirectory;
    }

    public RemoteFile(RemoteFile file){
        this.name = file.name;
        this.path = file.path;
        this.lastModified = file.lastModified;
        this.size = file.size;
        this.isDirectory = file.isDirectory;
    }


    @Override
    public String toString() {
        return "RemoteFile{" +
                "path='" + path + '\'' +
                ", isDirectory=" + isDirectory +
                '}';
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getName() {
        return name;
    }

    public long lastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

}
