package com.xayah.librootservice;

import com.xayah.librootservice.parcelables.StatFsParcelable;

interface IRemoteRootService {
    StatFsParcelable readStatFs(String path);
    boolean mkdirs(String path);
    boolean copyTo(String path, String targetPath, boolean overwrite);

    ParcelFileDescriptor getInstalledPackagesAsUser(int flags, int userId);
}
