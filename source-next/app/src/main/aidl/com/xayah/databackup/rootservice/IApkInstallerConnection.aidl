package com.xayah.databackup.rootservice;

import com.xayah.databackup.rootservice.IApkInstaller;

interface IApkInstallerConnection {
    void connect(String token, int userId, IApkInstaller installer);
}
