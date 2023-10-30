package com.xayah.databackup.librootservice

import android.annotation.SuppressLint
import android.app.usage.StorageStats
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import android.os.*
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.databackup.librootservice.parcelables.StatFsParcelable
import com.xayah.databackup.librootservice.service.RemoteRootServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SuppressLint("NewApi")
class RootService {
    object Instance {
        val instance = com.xayah.databackup.librootservice.RootService()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    private val mConnection: RemoteRootServiceConnection = RemoteRootServiceConnection()
    private var mLatch: CountDownLatch? = null
    private var mService: IRemoteRootService? = null

    class RemoteRootService : RootService() {
        override fun onBind(intent: Intent): IBinder {
            return RemoteRootServiceImpl()
        }
    }

    inner class RemoteRootServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = IRemoteRootService.Stub.asInterface(service)
            mLatch?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mLatch?.countDown()
        }

        override fun onBindingDied(name: ComponentName) {
            mService = null
            mLatch?.countDown()
        }

        override fun onNullBinding(name: ComponentName) {
            mService = null
            mLatch?.countDown()
        }
    }

    private fun getService(): IRemoteRootService {
        if (mService == null) {
            runBlocking { bindService("com.xayah.databackup") {} }
        } else if (mService!!.asBinder().isBinderAlive.not()) {
            mService = null
            runBlocking { bindService("com.xayah.databackup") {} }
        } else {
            mService!!
        }
        return mService!!
    }

    suspend fun bindService(pkg: String, onTimeOut: () -> Unit): IRemoteRootService? {
        if (mService == null) {
            if (mLatch == null || mLatch?.count == 0L) {
                mLatch = CountDownLatch(1)
                withContext(Dispatchers.Main) {
                    val intent = Intent().apply {
                        component = ComponentName(pkg, RemoteRootService::class.java.name)
                    }
                    RootService.bind(intent, mConnection)
                }
            }

            // Wait for binding
            try {
                withContext(Dispatchers.IO) {
                    mLatch?.await(30, TimeUnit.SECONDS)
                }
            } catch (_: Exception) {
            }

            if (mLatch?.count != 0L) {
                onTimeOut()
            }
        }
        return mService
    }

    fun exists(path: String): Boolean {
        return getService().exists(path)
    }

    fun createNewFile(path: String): Boolean {
        return getService().createNewFile(path)
    }

    fun deleteRecursively(path: String): Boolean {
        return getService().deleteRecursively(path)
    }

    fun listFilesPath(path: String): MutableList<String> {
        return getService().listFilesPath(path)
    }

    fun mkdirs(path: String): Boolean {
        return getService().mkdirs(path)
    }

    fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean {
        return getService().copyTo(path, targetPath, overwrite)
    }

    fun countFiles(path: String): Int {
        return getService().countFiles(path)
    }

    fun countSize(path: String, regex: String = ""): Long {
        return getService().countSize(path, regex)
    }

    fun readText(path: String): String {
        return getService().readText(path)
    }

    fun readBytes(path: String): ByteArray {
        return getService().readBytes(path)
    }

    private fun readByDescriptor(path: String): ParcelFileDescriptor {
        return getService().readByDescriptor(path)
    }

    private fun closeMemoryFile(): Boolean {
        return getService().closeMemoryFile()
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
        return getService().writeText(path, text)
    }

    fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return getService().writeBytes(path, bytes)
    }

    private fun writeByDescriptor(path: String, descriptor: ParcelFileDescriptor): Boolean {
        return getService().writeByDescriptor(path, descriptor)
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
        return getService().initActionLogFile(path)
    }

    fun appendActionLog(text: String): Boolean {
        return getService().appendActionLog(text)
    }

    fun readStatFs(path: String): StatFsParcelable {
        return getService().readStatFs(path)
    }

    fun getUserHandle(userId: Int): UserHandle {
        return getService().getUserHandle(userId)
    }

    fun getUsers(): MutableList<UserInfo> {
        return getService().users
    }

    fun offerInstalledPackagesAsUser(flags: Int, userId: Int): Boolean {
        return getService().offerInstalledPackagesAsUser(flags, userId)
    }

    fun pollInstalledPackages(): MutableList<PackageInfo> {
        return getService().pollInstalledPackages()
    }

    fun getSuspendedPackages(): MutableList<PackageInfo> {
        return getService().suspendedPackages
    }

    fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats {
        return getService().queryStatsForPackage(packageInfo, user)
    }

    fun getPackageArchiveInfo(path: String): PackageInfo? {
        return getService().getPackageArchiveInfo(path)
    }

    fun queryInstalled(packageName: String, userId: Int): Boolean {
        return getService().queryInstalled(packageName, userId)
    }

    fun grantRuntimePermission(packageName: String, permName: String, userId: Int): Boolean {
        return getService().grantRuntimePermission(packageName, permName, userId)
    }

    fun displayPackageFilePath(packageName: String, userId: Int): List<String> {
        return getService().displayPackageFilePath(packageName, userId)
    }

    fun setPackagesSuspended(packageNames: Array<String>, suspended: Boolean): Boolean {
        return getService().setPackagesSuspended(packageNames, suspended)
    }

    fun getPackageUid(packageName: String, userId: Int): Int {
        return getService().getPackageUid(packageName, userId)
    }

    fun getPackageLongVersionCode(packageName: String, userId: Int): Long {
        return getService().getPackageLongVersionCode(packageName, userId)
    }

    fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int, userId: Int) {
        getService().setApplicationEnabledSetting(packageName, newState, flags, userId)
    }

    fun getApplicationEnabledSetting(packageName: String, userId: Int): Int {
        return getService().getApplicationEnabledSetting(packageName, userId)
    }
}
