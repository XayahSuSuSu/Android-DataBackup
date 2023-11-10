package com.xayah.librootservice;

import com.xayah.librootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    StatFsParcelable readStatFs(String path);
    boolean mkdirs(String path);
    boolean copyRecursively(String path, String targetPath, boolean overwrite);
    boolean copyTo(String path, String targetPath, boolean overwrite);
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    List<String> listFilePaths(String path);
    ParcelFileDescriptor readText(String path);
    ParcelFileDescriptor readBytes(String path);
    long calculateSize(String path);
    void clearEmptyDirectoriesRecursively(String path);

    ParcelFileDescriptor getInstalledPackagesAsUser(int flags, int userId);
    List<String> getPackageSourceDir(String packageName, int userId);
    boolean queryInstalled(String packageName, int userId);
    int getPackageUid(String packageName, int userId);
    UserHandle getUserHandle(int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
    List<UserInfo> getUsers();
    ParcelFileDescriptor walkFileTree(String path);
    PackageInfo getPackageArchiveInfo(String path);
}
