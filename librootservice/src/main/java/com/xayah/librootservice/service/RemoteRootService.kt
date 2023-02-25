package com.xayah.librootservice.service

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

class RemoteRootService : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return RemoteRootServiceIPC()
    }
}
