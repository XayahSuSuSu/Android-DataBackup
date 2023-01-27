package com.xayah.databackup.util

import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import com.xayah.materialyoufileexplorer.service.RemoteFileSystemConnection
import com.xayah.materialyoufileexplorer.service.RemoteFileSystemService

class SuFile {
    object Instance {
        val instance = SuFile()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    lateinit var manager: FileSystemManager
    private val isInitialized
        get() = this::manager.isInitialized

    /**
     * 初始化实例
     */
    fun initialize(context: Context, onServiceConnected: () -> Unit = {}) {
        val intent = Intent(context, RemoteFileSystemService::class.java)
        val remoteFileSystemConnection = RemoteFileSystemConnection().apply {
            setOnServiceConnected {
                manager = it
                onServiceConnected()
            }
        }
        RootService.bind(intent, remoteFileSystemConnection)
    }

    /**
     * 使用之前需要先进行`initialize()`初始化
     */
    fun File(path: String): ExtendedFile {
        if (isInitialized) {
            return manager.getFile(path)
        } else {
            throw UninitializedPropertyAccessException("The service has not been initialized.")
        }
    }
}