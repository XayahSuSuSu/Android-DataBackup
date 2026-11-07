package com.xayah.databackup.rootservice.install

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.app.AppGlobals
import android.content.Context
import android.content.ContextHidden
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstallerHidden
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.UserHandleHidden
import android.system.Os
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.xayah.databackup.rootservice.IApkInstallCallback
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.service.restore.ApkInstallFailure
import com.xayah.databackup.service.restore.ApkInstallProcess.Companion.GOOGLE_PACKAGE_INSTALLER_PACKAGE_NAME
import com.xayah.databackup.service.restore.ApkInstallProcess.Companion.GOOGLE_PLAY_PACKAGE_NAME
import com.xayah.databackup.service.restore.InstallApkHelper
import com.xayah.hiddenapi.castTo
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

/**
 * Entry point for the child process that handles APK installation requests under Google Play or the system UID for the target user.
 */
@Keep
internal object ApkInstallerMain {
    internal const val EXTRA_BINDER = "installer"
    internal const val EXTRA_TOKEN = "token"
    internal const val EXTRA_USER_ID = "userId"
    private const val MAX_OUTPUT_LENGTH = 8192
    private const val OWNER_MONITOR_THREAD_NAME = "apk-installer-owner"

    // Send the cross-user bootstrap broadcast as root before switching to the installer identity.
    @SuppressLint("MissingPermission")
    @JvmStatic
    fun main(args: Array<String>) {
        val result = runCatching {
            check(Os.getuid() == 0) { "APK installer must start as root" }
            require(args.size == 6)
            val targetUserId = args[0].toInt()
            val appPackageName = args[1]
            val clientUserId = args[2].toInt()
            val action = args[3]
            val token = args[4]
            val isPlay = args[5].toBooleanStrict()
            Looper.prepareMainLooper()
            val systemContext: Context = ActivityThread.systemMain().systemContext
            val installerContext = systemContext.castTo<ContextHidden>().createPackageContextAsUser(
                if (isPlay) GOOGLE_PLAY_PACKAGE_NAME else appPackageName,
                0,
                UserHandleHidden.of(if (isPlay) targetUserId else clientUserId),
            )
            val installer = if (isPlay) {
                installerContext.packageManager.packageInstaller
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                createSystemInstaller(appPackageName, targetUserId)
            } else {
                error("System APK installer requires Android 9 or later")
            }
            val worker = InstallWorker(installer, isPlay)
            val extras = Bundle().apply {
                putBinder(EXTRA_BINDER, worker)
                putString(EXTRA_TOKEN, token)
                putInt(EXTRA_USER_ID, targetUserId)
            }
            // Send the installer Binder to the app's permission-protected receiver while still running as root.
            systemContext.sendBroadcastAsUser(
                Intent(action).setPackage(appPackageName).putExtras(extras),
                UserHandleHidden.of(clientUserId),
            )
            Os.setuid(if (isPlay) installerContext.applicationInfo.uid else android.os.Process.SYSTEM_UID)
            worker.startOwnerMonitor()
            Looper.loop()
        }
        result.exceptionOrNull()?.printStackTrace(System.err)
        exitProcess(if (result.isSuccess) 0 else 1)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createSystemInstaller(appPackageName: String, userId: Int): PackageInstaller {
        val installerService = AppGlobals.getPackageManager().getPackageInstaller()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PackageInstallerHidden(installerService, appPackageName, null, userId).castTo<PackageInstaller>()
        } else {
            PackageInstallerHidden(installerService, appPackageName, userId).castTo<PackageInstaller>()
        }
    }

    private class InstallWorker(private val mInstaller: PackageInstaller, isPlay: Boolean) : IApkInstaller.Stub() {
        private val mHelper = InstallApkHelper(mInstaller, if (isPlay) null else GOOGLE_PACKAGE_INSTALLER_PACKAGE_NAME)
        private val mHandler = Handler(Looper.getMainLooper())
        private val mIsBusy = AtomicBoolean(false)
        private val mSessionId = AtomicInteger(-1)

        override fun install(packageName: String, apks: MutableList<ParcelFileDescriptor>, callback: IApkInstallCallback) {
            try {
                check(getCallingUid() == 0) { "Only the root service may install APKs" }
                check(mIsBusy.compareAndSet(false, true)) { "APK installer is busy" }
                check(mHandler.post { installAndNotify(packageName, apks, callback) }) { "APK installer is stopping" }
            } catch (error: Exception) {
                apks.forEach { runCatching { it.close() } }
                throw IllegalStateException(error.message, error)
            }
        }

        private fun installAndNotify(packageName: String, apks: List<ParcelFileDescriptor>, callback: IApkInstallCallback) {
            val inputs = apks.map { ParcelFileDescriptor.AutoCloseInputStream(it) }
            val installation = runCatching {
                mHelper.install(packageName, inputs) { sessionId ->
                    mSessionId.set(sessionId)
                    callback.onSessionCreated(sessionId)
                }
            }
            inputs.forEach { runCatching { it.close() } }
            mSessionId.set(-1)
            mIsBusy.set(false)
            runCatching {
                val error = installation.exceptionOrNull()
                if (error != null && error !is ApkInstallFailure) {
                    // The installation may have committed despite the error. Exit without a reply to prevent a duplicate installation.
                    error.printStackTrace(System.err)
                    exitProcess(1)
                }
                val message = error?.let { (it.message ?: it.javaClass.simpleName).take(MAX_OUTPUT_LENGTH) }
                callback.onFinished(message)
            }.onFailure { exitProcess(1) }
        }

        fun startOwnerMonitor() {
            Thread({
                runCatching {
                    while (System.`in`.read() != -1) {
                        // Only EOF is meaningful; requests arrive through Binder.
                    }
                }
                val sessionId = mSessionId.get()
                if (sessionId >= 0) {
                    runCatching { mInstaller.abandonSession(sessionId) }
                }
                exitProcess(0)
            }, OWNER_MONITOR_THREAD_NAME).apply { isDaemon = true }.start()
        }
    }
}
