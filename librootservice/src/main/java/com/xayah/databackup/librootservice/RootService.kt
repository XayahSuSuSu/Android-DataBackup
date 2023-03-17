package com.xayah.databackup.librootservice

import android.annotation.SuppressLint
import android.app.usage.StorageStats
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import android.os.MemoryFile
import android.os.MemoryFileHidden
import android.os.ParcelFileDescriptor
import android.os.UserHandle
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.databackup.librootservice.service.RemoteRootService
import com.xayah.databackup.librootservice.service.RemoteRootServiceConnection
import java.io.FileInputStream

class RootService {
    object Instance {
        val instance = com.xayah.databackup.librootservice.RootService()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    private lateinit var ipc: IRemoteRootService
    private lateinit var connection: RemoteRootServiceConnection

    fun isInitialize(): Boolean {
        var needInitialize = this::ipc.isInitialized
        if (needInitialize) {
            needInitialize = try {
                checkConnection()
            } catch (e: Exception) {
                false
            }
        }
        return needInitialize
    }

    /**
     * 初始化Service
     */
    fun initialize(context: Context, onServiceConnected: () -> Unit) {
        val intent = Intent(context, RemoteRootService::class.java)
        connection = RemoteRootServiceConnection().apply {
            setOnServiceConnected {
                ipc = it
                onServiceConnected()
            }
        }
        RootService.bind(intent, connection)
    }

    private fun checkConnection(): Boolean {
        return ipc.checkConnection()
    }

    fun exists(path: String): Boolean {
        return ipc.exists(path)
    }

    fun createNewFile(path: String): Boolean {
        return ipc.createNewFile(path)
    }

    fun deleteRecursively(path: String): Boolean {
        return ipc.deleteRecursively(path)
    }

    fun mkdirs(path: String): Boolean {
        return ipc.mkdirs(path)
    }

    fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean {
        return ipc.copyTo(path, targetPath, overwrite)
    }

    fun countSize(path: String): Long {
        return ipc.countSize(path)
    }

    fun readText(path: String): String {
        return ipc.readText(path)
    }

    fun readBytes(path: String): ByteArray {
        return ipc.readBytes(path)
    }

    private fun readByDescriptor(path: String): ParcelFileDescriptor {
        return ipc.readByDescriptor(path)
    }

    private fun closeMemoryFile(): Boolean {
        return ipc.closeMemoryFile()
    }

    fun readTextByDescriptor(path: String): String {
        var text = ""
        try {
            val fileDescriptor = readByDescriptor(path)
            val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
            text = String(fileInputStream.readBytes())
            closeMemoryFile()
        } catch (_: Exception) {
        }
        return text
    }

    fun readBytesByDescriptor(path: String): ByteArray {
        var bytes = ByteArray(0)
        try {
            val fileDescriptor = readByDescriptor(path)
            val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
            bytes = fileInputStream.readBytes()
            closeMemoryFile()
        } catch (_: Exception) {
        }
        return bytes
    }

    fun writeText(path: String, text: String): Boolean {
        return ipc.writeText(path, text)
    }

    fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return ipc.writeBytes(path, bytes)
    }

    private fun writeByDescriptor(path: String, descriptor: ParcelFileDescriptor): Boolean {
        return ipc.writeByDescriptor(path, descriptor)
    }

    @SuppressLint("NewApi")
    fun writeBytesByDescriptor(path: String, bytes: ByteArray) {
        val memoryFile = MemoryFile("memoryFileDataBackupWrite", bytes.size)
        val fileDescriptor = MemoryFileHidden.getFileDescriptor(memoryFile)
        memoryFile.writeBytes(bytes, 0, 0, bytes.size)
        writeByDescriptor(path, ParcelFileDescriptor.dup(fileDescriptor))
        memoryFile.close()
    }

    fun initActionLogFile(path: String): Boolean {
        return ipc.initActionLogFile(path)
    }

    fun appendActionLog(text: String): Boolean {
        return ipc.appendActionLog(text)
    }

    fun getUserHandle(userId: Int): UserHandle {
        return ipc.getUserHandle(userId)
    }

    fun getUsers(
        excludePartial: Boolean,
        excludeDying: Boolean,
        excludePreCreated: Boolean
    ): MutableList<UserInfo> {
        return ipc.getUsers(excludePartial, excludeDying, excludePreCreated)
    }

    fun offerInstalledPackagesAsUser(flags: Int, userId: Int): Boolean {
        return ipc.offerInstalledPackagesAsUser(flags, userId)
    }

    fun pollInstalledPackages(): MutableList<PackageInfo> {
        return ipc.pollInstalledPackages()
    }

    fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats {
        return ipc.queryStatsForPackage(packageInfo, user)
    }

    fun queryInstalled(packageName: String, userId: Int): Boolean {
        return ipc.queryInstalled(packageName, userId)
    }

    fun grantRuntimePermission(
        packageName: String,
        permName: String,
        userId: Int
    ): Boolean {
        return ipc.grantRuntimePermission(packageName, permName, userId)
    }
}
