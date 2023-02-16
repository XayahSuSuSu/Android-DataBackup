package com.xayah.databackup;

interface IRemoteFileService {
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean mkdirs(String path);
    String readText(String path);
    byte[] readBytes(String path);
    boolean writeText(String path, String text);
    boolean writeBytes(String path, in byte[] bytes);
}
