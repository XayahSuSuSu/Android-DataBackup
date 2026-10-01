package com.xayah.databackup.rootservice;

interface IRestoreCallback {
    void onStarted(String id);
    void onCompleted(String id, boolean skipped);
    void onFailed(String id);
}
