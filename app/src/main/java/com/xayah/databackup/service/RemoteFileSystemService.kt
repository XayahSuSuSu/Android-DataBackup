package com.xayah.databackup.service

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import com.xayah.materialyoufileexplorer.IRemoteFileSystemService

class RemoteFileSystemIPC : IRemoteFileSystemService.Stub() {
    override fun getFileSystemService(): IBinder {
        return FileSystemManager.getService()
    }

}

class RemoteFileSystemService : RootService() {
    override fun onBind(intent: Intent): IBinder {
        return RemoteFileSystemIPC()
    }
}
