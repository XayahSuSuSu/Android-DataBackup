package com.xayah.databackup.librootservice;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.app.usage.StorageStats;
import com.xayah.databackup.librootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    List<String> listFilesPath(String path);
    boolean mkdirs(String path);
    boolean copyTo(String path, String targetPath, boolean overwrite);
    int countFiles(String path);
    long countSize(String path, String regex);
    String readText(String path);
    byte[] readBytes(String path);
    ParcelFileDescriptor readByDescriptor(String path);
    boolean closeMemoryFile();
    boolean writeText(String path, String text);
    boolean writeBytes(String path, in byte[] bytes);
    boolean writeByDescriptor(String path, in ParcelFileDescriptor descriptor);
    boolean initActionLogFile(String path);
    boolean appendActionLog(String text);

    StatFsParcelable readStatFs(String path);

    void initializeService();
    UserHandle getUserHandle(int userId);
    List<UserInfo> getUsers();
    boolean offerInstalledPackagesAsUser(int flags, int userId);
    List<PackageInfo> pollInstalledPackages();
    List<PackageInfo> getSuspendedPackages();
    PackageInfo getPackageArchiveInfo(String path);
    boolean queryInstalled(String packageName, int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
    boolean grantRuntimePermission(String packageName, String permName, int userId);
    List<String> displayPackageFilePath(String packageName, int userId);
    boolean setPackagesSuspended(in String[] packageNames, boolean suspended);
    int getPackageUid(String packageName, int userId);
    long getPackageLongVersionCode(String packageName, int userId);
    void setApplicationEnabledSetting(in String packageName, in int newState, int flags, int userId);
    int getApplicationEnabledSetting(in String packageName, int userId);
}
