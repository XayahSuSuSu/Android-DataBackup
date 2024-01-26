package com.xayah.core.rootservice;

import com.xayah.core.rootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    StatFsParcelable readStatFs(String path);
    boolean mkdirs(String path);
    boolean copyRecursively(String path, String targetPath, boolean overwrite);
    boolean copyTo(String path, String targetPath, boolean overwrite);
    boolean renameTo(String src, String dst);
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    List<String> listFilePaths(String path);
    ParcelFileDescriptor readText(String path);
    ParcelFileDescriptor readBytes(String path);
    long calculateSize(String path);
    void clearEmptyDirectoriesRecursively(String path);

    ParcelFileDescriptor getInstalledPackagesAsUser(int flags, int userId);
    PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId);
    void grantRuntimePermission(String packageName, String permName, in UserHandle user);
    void revokeRuntimePermission(String packageName, String permName, in UserHandle user);
    int getPermissionFlags(String packageName, String permName, in UserHandle user);
    void updatePermissionFlags(String packageName, String permName, in UserHandle user, int flagMask, int flagValues);
    List<String> getPackageSourceDir(String packageName, int userId);
    boolean queryInstalled(String packageName, int userId);
    int getPackageUid(String packageName, int userId);
    UserHandle getUserHandle(int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
    List<UserInfo> getUsers();
    ParcelFileDescriptor walkFileTree(String path);
    PackageInfo getPackageArchiveInfo(String path);
    String getPackageSsaidAsUser(String packageName, int uid, int userId);
    void setPackageSsaidAsUser(String packageName, int uid, int userId, String ssaid);
}
