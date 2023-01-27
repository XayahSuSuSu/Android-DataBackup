package com.xayah.databackup.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.topjohnwu.superuser.nio.FileSystemManager
import com.xayah.materialyoufileexplorer.IRemoteFileSystemService

class RemoteFileSystemConnection : ServiceConnection {
    private var onServiceConnected: (fileSystemManager: FileSystemManager) -> Unit = {}

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        val ipc: IRemoteFileSystemService = IRemoteFileSystemService.Stub.asInterface(p1)
        val binder: IBinder = ipc.fileSystemService
        onServiceConnected(FileSystemManager.getRemote(binder))
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}

    fun setOnServiceConnected(callback: (fileSystemManager: FileSystemManager) -> Unit) {
        onServiceConnected = callback
    }
}
