package com.xayah.core.rootservice.service

import android.app.usage.StorageStats
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.UserHandle
import android.widget.Toast
import com.topjohnwu.superuser.ipc.RootService
import com.xayah.core.rootservice.IRemoteRootService
import com.xayah.core.rootservice.impl.RemoteRootServiceImpl
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.rootservice.parcelables.StatFsParcelable
import com.xayah.core.rootservice.util.ExceptionUtil.tryOnScope
import com.xayah.core.rootservice.util.withMainContext
import com.xayah.core.util.PathUtil
import com.xayah.core.util.model.ShellResult
import kotlinx.coroutines.isActive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RemoteRootService(private val context: Context) {
    private var mService: IRemoteRootService? = null
    private var mConnection: ServiceConnection? = null
    private var isFirstConnection = true
    private val intent by lazy {
        Intent().apply {
            component = ComponentName(context.packageName, RemoteRootService::class.java.name)
            addCategory(RootService.CATEGORY_DAEMON_MODE)
        }
    }

    class RemoteRootService : RootService() {
        override fun onBind(intent: Intent): IBinder = RemoteRootServiceImpl()
    }

    private suspend fun bindService(): IRemoteRootService = suspendCoroutine { continuation ->
        if (mService == null) {
            mConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mService = IRemoteRootService.Stub.asInterface(service)
                    if (continuation.context.isActive) continuation.resume(mService!!)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Service disconnected."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                }

                override fun onBindingDied(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Binding died."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                }

                override fun onNullBinding(name: ComponentName) {
                    mService = null
                    mConnection = null
                    val msg = "Null binding."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                }
            }
            RootService.bind(intent, mConnection!!)
        } else {
            mService
        }
    }

    /**
     * Destroy the service.
     */
    fun destroyService(killDaemon: Boolean = false) {
        if (killDaemon) {
            if (mConnection != null) {
                RootService.unbind(mConnection!!)
            }
            RootService.stopOrTask(intent)
        }

        mConnection = null
        mService = null
    }

    private suspend fun getService(): IRemoteRootService {
        return tryOnScope(
            block = {
                withMainContext {
                    if (mService == null) {
                        val msg = "Service is null."
                        if (isFirstConnection)
                            isFirstConnection = false
                        else
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        bindService()
                    } else if (mService!!.asBinder().isBinderAlive.not()) {
                        mService = null
                        val msg = "Service is dead."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        bindService()
                    } else {
                        mService!!
                    }
                }
            },
            onException = {
                withMainContext {
                    mService = null
                    val msg = it.message
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    bindService()
                }
            }
        )
    }

    suspend fun testService(): IRemoteRootService = getService()

    suspend fun readStatFs(path: String): StatFsParcelable = getService().readStatFs(path)

    suspend fun mkdirs(path: String): Boolean = getService().mkdirs(path)

    suspend fun copyRecursively(path: String, targetPath: String, overwrite: Boolean): Boolean = getService().copyRecursively(path, targetPath, overwrite)

    suspend fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean = getService().copyTo(path, targetPath, overwrite)

    suspend fun renameTo(src: String, dst: String): Boolean = getService().renameTo(src, dst)

    suspend fun writeText(text: String, path: String, context: Context): Boolean {
        var state = true
        val tmpFilePath = "${context.cacheDir.path}/tmp"
        val tmpFile = File(tmpFilePath)
        tmpFile.writeText(text)
        if (getService().mkdirs(path).not()) state = false
        if (getService().copyTo(tmpFilePath, path, true).not()) state = false
        tmpFile.deleteRecursively()
        return state
    }

    suspend fun writeBytes(bytes: ByteArray, dst: String): Boolean {
        var isSuccess = true
        val tmpFilePath = "${context.cacheDir.path}/tmp"
        val tmpFile = File(tmpFilePath)
        tmpFile.writeBytes(bytes)
        getService().mkdirs(PathUtil.getParentPath(dst))
        isSuccess = isSuccess and getService().copyTo(tmpFilePath, dst, true)
        tmpFile.deleteRecursively()
        return isSuccess
    }

    suspend fun exists(path: String): Boolean = getService().exists(path)

    suspend fun createNewFile(path: String): Boolean = getService().createNewFile(path)

    suspend fun deleteRecursively(path: String): Boolean = getService().deleteRecursively(path)

    suspend fun listFilePaths(path: String): List<String> = getService().listFilePaths(path)

    private fun readFromParcel(pfd: ParcelFileDescriptor, onRead: (Parcel) -> Unit) {
        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        val bytes = stream.readBytes()
        val parcel = Parcel.obtain()

        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)

        onRead(parcel)

        parcel.recycle()
    }

    suspend fun readText(path: String): String {
        val pfd = getService().readText(path)
        var text: String? = null
        readFromParcel(pfd) {
            text = it.readString()
        }
        return text ?: ""
    }

    suspend fun readBytes(src: String): ByteArray {
        val pfd = getService().readBytes(src)
        var bytes = ByteArray(0)
        readFromParcel(pfd) {
            bytes = ByteArray(it.readInt())
            it.readByteArray(bytes)
        }
        return bytes
    }

    suspend fun calculateSize(path: String): Long = getService().calculateSize(path)

    suspend fun clearEmptyDirectoriesRecursively(path: String) = getService().clearEmptyDirectoriesRecursively(path)

    suspend fun getInstalledPackagesAsUser(flags: Int, userId: Int): List<PackageInfo> {
        val pfd = getService().getInstalledPackagesAsUser(flags, userId)
        val packages = mutableListOf<PackageInfo>()
        readFromParcel(pfd) {
            it.readTypedList(packages, PackageInfo.CREATOR)
        }
        return packages
    }

    suspend fun getPackageSourceDir(packageName: String, userId: Int): List<String> = getService().getPackageSourceDir(packageName, userId)

    suspend fun queryInstalled(packageName: String, userId: Int): Boolean = getService().queryInstalled(packageName, userId)

    suspend fun getPackageUid(packageName: String, userId: Int): Int = getService().getPackageUid(packageName, userId)

    suspend fun getUserHandle(userId: Int): UserHandle = getService().getUserHandle(userId)

    suspend fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats? = getService().queryStatsForPackage(packageInfo, user)

    suspend fun getUsers(): List<UserInfo> = getService().users

    suspend fun walkFileTree(path: String): List<PathParcelable> {
        val pfd = getService().walkFileTree(path)
        val list = mutableListOf<PathParcelable>()
        readFromParcel(pfd) {
            it.readTypedList(list, PathParcelable.CREATOR)
        }
        return list
    }

    suspend fun getPackageArchiveInfo(path: String): PackageInfo? = getService().getPackageArchiveInfo(path)

    @OptIn(ExperimentalSerializationApi::class)
    suspend inline fun <reified T> writeProtoBuf(data: T, dst: String): ShellResult = run {
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        val bytes = ProtoBuf.encodeToByteArray(data)
        writeBytes(bytes = bytes, dst = dst).also {
            isSuccess = it
            if (isSuccess) {
                out.add("Succeed to write configs: $dst")
            } else {
                out.add("Failed to write configs: $dst")
            }
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend inline fun <reified T> readProtoBuf(src: String): T = run {
        val bytes = readBytes(src = src)
        ProtoBuf.decodeFromByteArray<T>(bytes)
    }
}
