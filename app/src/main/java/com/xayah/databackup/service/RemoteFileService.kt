package com.xayah.databackup.service

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class RemoteFileService : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return RemoteFileIPC()
    }
}