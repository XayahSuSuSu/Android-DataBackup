package com.xayah.databackup.rootservice;

import com.xayah.databackup.parcelables.BytesParcelable;
import com.xayah.databackup.parcelables.StatFsParcelable;
import com.xayah.databackup.parcelables.FilePathParcelable;

interface IRemoteRootService {
    ParcelFileDescriptor getInstalledAppInfos();
    ParcelFileDescriptor getInstalledAppStorages();
    List<UserInfo> getUsers();
    List<BytesParcelable> getPrivilegedConfiguredNetworks();
    int[] addNetworks(in List<BytesParcelable> configs);
    StatFsParcelable readStatFs(String path);
    List<FilePathParcelable> listFilePaths(String path, boolean listFiles, boolean listDirs);
    ParcelFileDescriptor readText(String path);
    long calculateTreeSize(String path);
}
