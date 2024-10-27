package com.xayah.core.rootservice;

import com.xayah.core.rootservice.parcelables.StatFsParcelable;
import com.xayah.core.rootservice.parcelables.StorageStatsParcelable;
import com.xayah.core.model.database.PackagePermission;

interface IRemoteRootService {
    StatFsParcelable readStatFs(String path);
    boolean mkdirs(String path);
    boolean copyRecursively(String path, String targetPath, boolean overwrite);
    boolean copyTo(String path, String targetPath, boolean overwrite);
    boolean renameTo(String src, String dst);
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    List<String> listFilePaths(String path, boolean listFiles, boolean listDirs);
    ParcelFileDescriptor readText(String path);
    ParcelFileDescriptor readBytes(String path);
    long calculateSize(String path);
    void clearEmptyDirectoriesRecursively(String path);
    void setAllPermissions(String src);
    int[] getUidGid(String path);

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
    StorageStatsParcelable queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
    List<UserInfo> getUsers();
    ParcelFileDescriptor walkFileTree(String path);
    PackageInfo getPackageArchiveInfo(String path);
    String getPackageSsaidAsUser(String packageName, int uid, int userId);
    void setPackageSsaidAsUser(String packageName, int uid, int userId, String ssaid);
    void setDisplayPowerMode(int mode);
    int getScreenOffTimeout();
    void setScreenOffTimeout(int timeout);
    void forceStopPackageAsUser(String packageName, int userId);
    void setApplicationEnabledSetting(in String packageName, in int newState, int flags, int userId, String callingPackage);
    int getApplicationEnabledSetting(in String packageName, int userId);
    List<PackagePermission> getPermissions(in PackageInfo packageInfo);
    void setOpsMode(int code, int uid, String packageName, int mode);

    String calculateMD5(String src);
}
