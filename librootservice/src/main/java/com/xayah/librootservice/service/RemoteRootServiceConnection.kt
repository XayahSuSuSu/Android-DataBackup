package com.xayah.librootservice.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.xayah.librootservice.IRemoteRootService

class RemoteRootServiceConnection : ServiceConnection {
    private var onServiceConnected: (ipc: IRemoteRootService) -> Unit = {}

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        onServiceConnected(IRemoteRootService.Stub.asInterface(service))
    }

    override fun onServiceDisconnected(name: ComponentName) {}

    fun setOnServiceConnected(callback: (ipc: IRemoteRootService) -> Unit) {
        onServiceConnected = callback
    }
}
