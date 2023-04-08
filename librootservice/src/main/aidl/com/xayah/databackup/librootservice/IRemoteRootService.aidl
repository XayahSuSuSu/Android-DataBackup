package com.xayah.databackup.librootservice;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.app.usage.StorageStats;
import com.xayah.databackup.librootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    boolean checkConnection();

    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    List<String> listFilesPath(String path);
    boolean mkdirs(String path);
    boolean copyTo(String path, String targetPath, boolean overwrite);
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
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);
    boolean offerInstalledPackagesAsUser(int flags, int userId);
    List<PackageInfo> pollInstalledPackages();
    boolean queryInstalled(String packageName, int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
    boolean grantRuntimePermission(String packageName, String permName, int userId);
    List<String> displayPackageFilePath(String packageName, int userId);
    boolean setPackagesSuspended(in String[] packageNames, boolean suspended);
    int getPackageUid(String packageName, int userId);
    long getPackageLongVersionCode(String packageName, int userId);
}
