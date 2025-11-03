package com.xayah.databackup.rootservice;

interface ICallback {
    void onProgress(long bytesWritten, long speed);
}
