package com.xayah.librootservice;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.app.usage.StorageStats;

interface IRemoteRootService {
    boolean checkConnection();

    boolean exists(String path);
    boolean createNewFile(String path);
    boolean deleteRecursively(String path);
    boolean mkdirs(String path);
    long countSize(String path);
    String readText(String path);
    byte[] readBytes(String path);
    boolean writeText(String path, String text);
    boolean writeBytes(String path, in byte[] bytes);
    boolean initActionLogFile(String path);
    boolean appendActionLog(String text);

    void initializeService();
    UserHandle getUserHandle(int userId);
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);
    boolean offerInstalledPackagesAsUser(int flags, int userId);
    List<PackageInfo> pollInstalledPackages();
    boolean queryInstalled(String packageName, int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
}
