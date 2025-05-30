package com.xayah.databackup.rootservice;

interface IRemoteRootService {
    ParcelFileDescriptor getInstalledAppInfos();
    ParcelFileDescriptor getInstalledAppStorages();
    List<UserInfo> getUsers();
}
