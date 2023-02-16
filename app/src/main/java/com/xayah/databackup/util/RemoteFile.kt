package com.xayah.databackup.util

import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.databackup.IRemoteFileService
import com.xayah.databackup.service.RemoteFileConnection
import com.xayah.databackup.service.RemoteFileService

/**
 * RemoteFile
 */
class RemoteFile {
    object Instance {
        val instance = RemoteFile()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    private lateinit var ipc: IRemoteFileService
    private lateinit var connection: RemoteFileConnection

    /**
     * 初始化Service
     */
    fun initialize(context: Context, onServiceConnected: () -> Unit) {
        val intent = Intent(context, RemoteFileService::class.java)
        connection = RemoteFileConnection().apply {
            setOnServiceConnected {
                ipc = it
                onServiceConnected()
            }
        }
        RootService.bind(intent, connection)
    }

    /**
     * 取消绑定Service
     */
    fun destroy() {
        if (this::connection.isInitialized)
            RootService.unbind(connection)
    }

    fun exists(path: String): Boolean {
        return ipc.exists(path)
    }

    fun createNewFile(path: String): Boolean {
        return ipc.createNewFile(path)
    }

    fun mkdirs(path: String): Boolean {
        return ipc.mkdirs(path)
    }

    fun readText(path: String): String {
        return ipc.readText(path)
    }

    fun readBytes(path: String): ByteArray {
        return ipc.readBytes(path)
    }

    fun writeText(path: String, text: String): Boolean {
        return ipc.writeText(path, text)
    }

    fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return ipc.writeBytes(path, bytes)
    }
}
