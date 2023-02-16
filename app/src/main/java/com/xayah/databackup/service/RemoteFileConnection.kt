package com.xayah.databackup.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.xayah.databackup.IRemoteFileService

class RemoteFileConnection : ServiceConnection {
    private var onServiceConnected: (ipc: IRemoteFileService) -> Unit = {}

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        onServiceConnected(IRemoteFileService.Stub.asInterface(p1))
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}

    fun setOnServiceConnected(callback: (ipc: IRemoteFileService) -> Unit) {
        onServiceConnected = callback
    }
}
