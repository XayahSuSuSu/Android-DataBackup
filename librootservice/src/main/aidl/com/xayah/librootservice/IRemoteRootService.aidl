package com.xayah.librootservice;

import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.app.usage.StorageStats;

interface IRemoteRootService {
    boolean exists(String path);
    boolean createNewFile(String path);
    boolean mkdirs(String path);
    String readText(String path);
    byte[] readBytes(String path);
    boolean writeText(String path, String text);
    boolean writeBytes(String path, in byte[] bytes);
    boolean initActionLogFile(String path);
    boolean appendActionLog(String text);

    void initializeService();
    UserHandle getUserHandle(int userId);
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);
    List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId);
    StorageStats queryStatsForPackage(in PackageInfo packageInfo, in UserHandle user);
}
