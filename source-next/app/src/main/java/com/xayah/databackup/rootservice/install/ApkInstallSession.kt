package com.xayah.databackup.rootservice.install

import android.content.Context
import android.os.IBinder
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.rootservice.IApkInstallerConnection
import com.xayah.databackup.service.restore.ApkInstallProcess
import java.io.Closeable

/**
 * Manages APK installer processes in the root service for a single restore task.
 * Each target user has a separate installer process, and no process is shared across tasks.
 */
internal class ApkInstallSession(
    private val mContext: Context,
    private val mSystemContext: Context,
    val action: String,
    private val mClientUserId: Int,
    private val mOwner: IBinder,
) : IApkInstallerConnection.Stub(), Closeable {
    private val mLock = Any()
    private val mInstallersByUser = mutableMapOf<Int, ApkInstallProcess>()
    private var mIsClosed = false
    private val mOwnerDeathRecipient = IBinder.DeathRecipient { close() }

    val isClosed: Boolean
        get() = synchronized(mLock) { mIsClosed }

    init {
        mOwner.linkToDeath(mOwnerDeathRecipient, 0)
    }

    fun getInstaller(userId: Int): ApkInstallProcess = synchronized(mLock) {
        check(!mIsClosed) { "APK install session is closed" }
        if (mInstallersByUser[userId]?.isClosed == true) {
            mInstallersByUser.remove(userId)?.close()
        }
        mInstallersByUser.getOrPut(userId) {
            ApkInstallProcess(
                mSystemContext = mSystemContext,
                mUserId = userId,
                mAppApkPath = mContext.applicationInfo.sourceDir,
                mAppPackageName = mContext.packageName,
                mClientUserId = mClientUserId,
                mAction = action,
            )
        }
    }

    override fun connect(token: String, userId: Int, installer: IApkInstaller) {
        synchronized(mLock) {
            if (mIsClosed) {
                return
            }
            mInstallersByUser[userId]?.connect(token, installer)
        }
    }

    override fun close() {
        val installers = synchronized(mLock) {
            if (mIsClosed) {
                return
            }
            mIsClosed = true
            val activeInstallers = mInstallersByUser.values.toList()
            mInstallersByUser.clear()
            activeInstallers
        }
        runCatching { mOwner.unlinkToDeath(mOwnerDeathRecipient, 0) }
        installers.forEach { it.close() }
    }
}
