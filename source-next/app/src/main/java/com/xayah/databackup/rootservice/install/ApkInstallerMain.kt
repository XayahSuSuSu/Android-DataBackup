package com.xayah.databackup.rootservice.install

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.content.ContextHidden
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.UserHandleHidden
import android.system.Os
import androidx.annotation.Keep
import com.xayah.databackup.rootservice.IApkInstallCallback
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.service.restore.InstallApkHelper
import com.xayah.hiddenapi.castTo
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

/**
 * Entry point for the child process that handles APK installation requests under the target user's Google Play UID.
 */
@Keep
internal object ApkInstallerMain {
    internal const val EXTRA_BINDER = "installer"
    internal const val EXTRA_TOKEN = "token"
    internal const val EXTRA_USER_ID = "userId"
    private const val GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending"
    private const val MAX_OUTPUT_LENGTH = 8192
    private const val OWNER_MONITOR_THREAD_NAME = "apk-installer-owner"

    // Send the cross-user bootstrap broadcast as root before switching to the target user's Google Play UID.
    @SuppressLint("MissingPermission")
    @JvmStatic
    fun main(args: Array<String>) {
        val result = runCatching {
            check(Os.getuid() == 0) { "APK installer must start as root" }
            require(args.size == 5)
            val targetUserId = args[0].toInt()
            val appPackageName = args[1]
            val clientUserId = args[2].toInt()
            val action = args[3]
            val token = args[4]
            Looper.prepareMainLooper()
            val systemContext: Context = ActivityThread.systemMain().systemContext
            val installerContext = systemContext.castTo<ContextHidden>().createPackageContextAsUser(
                GOOGLE_PLAY_PACKAGE_NAME, 0, UserHandleHidden.of(targetUserId),
            )
            val worker = InstallWorker(installerContext.packageManager.packageInstaller)
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
            Os.setuid(installerContext.applicationInfo.uid)
            worker.startOwnerMonitor()
            Looper.loop()
        }
        result.exceptionOrNull()?.printStackTrace(System.err)
        exitProcess(if (result.isSuccess) 0 else 1)
    }

    private class InstallWorker(private val mInstaller: PackageInstaller) : IApkInstaller.Stub() {
        private val mHelper = InstallApkHelper(mInstaller)
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
