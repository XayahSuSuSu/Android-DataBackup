package com.xayah.databackup.rootservice;

import android.os.ParcelFileDescriptor;
import com.xayah.databackup.rootservice.IApkInstallCallback;

interface IApkInstaller {
    void install(String packageName, in List<ParcelFileDescriptor> apks, IApkInstallCallback callback);
}
