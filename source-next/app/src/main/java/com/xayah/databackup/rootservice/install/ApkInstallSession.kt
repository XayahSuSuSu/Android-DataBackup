package com.xayah.databackup.rootservice.install

import android.content.Context
import android.os.IBinder
import com.xayah.databackup.entity.backup.ApkInstallSource
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.rootservice.IApkInstallerConnection
import com.xayah.databackup.service.restore.ApkInstallProcess
import java.io.Closeable
import java.io.File

/**
 * Manages APK installer processes in the root service for a single restore task.
 * Each target user and installer identity has a separate process, and no process is shared across tasks.
 */
internal class ApkInstallSession(
    private val mContext: Context,
    private val mSystemContext: Context,
    val action: String,
    private val mClientUserId: Int,
    private val mOwner: IBinder,
) : IApkInstallerConnection.Stub(), Closeable {
    private val mLock = Any()
    private val mInstallerByUserAndMode = mutableMapOf<Pair<Int, Boolean>, ApkInstallProcess>()
    private var mIsClosed = false
    private val mOwnerDeathRecipient = IBinder.DeathRecipient { close() }

    val isClosed: Boolean
        get() = synchronized(mLock) { mIsClosed }

    init {
        mOwner.linkToDeath(mOwnerDeathRecipient, 0)
    }

    fun install(userId: Int, packageName: String, apks: List<File>, source: ApkInstallSource?) {
        if (source?.isPlayStore == true) {
            val playInstaller = getInstaller(userId, true)
            if (playInstaller.canInstallWithPlay(packageName, apks)) {
                // Retry only after a confirmed installation failure; a lost response may hide a successful installation.
                if (playInstaller.install(packageName, apks) == null) {
                    return
                }
            }
        }
        val error = getInstaller(userId, false).install(packageName, apks)
        check(error == null) { error.orEmpty() }
    }

    private fun getInstaller(userId: Int, isPlay: Boolean): ApkInstallProcess = synchronized(mLock) {
        val key = userId to isPlay
        check(!mIsClosed) { "APK install session is closed" }
        if (mInstallerByUserAndMode[key]?.isClosed == true) {
            mInstallerByUserAndMode.remove(key)?.close()
        }
        mInstallerByUserAndMode.getOrPut(key) {
            ApkInstallProcess(
                mSystemContext = mSystemContext,
                mUserId = userId,
                mAppApkPath = mContext.applicationInfo.sourceDir,
                mAppPackageName = mContext.packageName,
                mClientUserId = mClientUserId,
                mAction = action,
                mIsPlay = isPlay,
            )
        }
    }

    override fun connect(token: String, userId: Int, installer: IApkInstaller) {
        synchronized(mLock) {
            if (mIsClosed) {
                return
            }
            mInstallerByUserAndMode.filterKeys { it.first == userId }.values.forEach { it.connect(token, installer) }
        }
    }

    override fun close() {
        val installers = synchronized(mLock) {
            if (mIsClosed) {
                return
            }
            mIsClosed = true
            val activeInstallers = mInstallerByUserAndMode.values.toList()
            mInstallerByUserAndMode.clear()
            activeInstallers
        }
        runCatching { mOwner.unlinkToDeath(mOwnerDeathRecipient, 0) }
        installers.forEach { it.close() }
    }
}
