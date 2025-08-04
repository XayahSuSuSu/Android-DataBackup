package com.xayah.databackup.rootservice;

import com.xayah.databackup.parcelables.BytesParcelable;

interface IRemoteRootService {
    ParcelFileDescriptor getInstalledAppInfos();
    ParcelFileDescriptor getInstalledAppStorages();
    List<UserInfo> getUsers();
    List<BytesParcelable> getPrivilegedConfiguredNetworks();
    int[] addNetworks(in List<BytesParcelable> configs);
}
