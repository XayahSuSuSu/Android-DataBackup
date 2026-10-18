package com.xayah.databackup.rootservice;

interface IApkInstallCallback {
    void onSessionCreated(int sessionId);
    void onFinished(String error);
}
