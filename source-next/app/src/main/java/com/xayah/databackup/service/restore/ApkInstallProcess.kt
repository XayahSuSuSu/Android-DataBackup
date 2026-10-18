package com.xayah.databackup.service.restore

import android.Manifest
import android.content.Context
import android.content.ContextHidden
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.UserHandleHidden
import com.xayah.databackup.rootservice.IApkInstallCallback
import com.xayah.databackup.rootservice.IApkInstaller
import com.xayah.databackup.rootservice.install.ApkInstallerMain
import com.xayah.hiddenapi.castTo
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages an APK installer process and its Binder connection for a single target user throughout a restore task.
 */
internal class ApkInstallProcess(
    private val mSystemContext: Context,
    private val mUserId: Int,
    private val mAppApkPath: String,
    private val mAppPackageName: String,
    private val mClientUserId: Int,
    private val mAction: String,
) : Closeable {
    private val mLock = Any()
    private val mToken = UUID.randomUUID().toString()
    private val mConnection = CompletableFuture<IApkInstaller>()
    private val mProcessMonitorCompletion = CountDownLatch(1)
    private val mSessionId = AtomicInteger(-1)
    private var mProcess: Process? = null
    private var mIsClosed = false
    private var mPendingResult: CompletableFuture<String?>? = null
    private var mRemoteBinder: IBinder? = null
    private val mWorkerDeathRecipient = IBinder.DeathRecipient { failPendingRequests("APK installer process died") }

    val isClosed: Boolean
        get() = synchronized(mLock) { mIsClosed || mRemoteBinder?.isBinderAlive == false }

    private fun getPlayApplicationInfo(): ApplicationInfo? = runCatching {
        mSystemContext.packageManager.castTo<PackageManagerHidden>().getPackageInfoAsUser(GOOGLE_PLAY_PACKAGE_NAME, 0, mUserId)
    }.getOrElse { error ->
        if (error is PackageManager.NameNotFoundException) {
            return@getOrElse null
        }
        throw error
    }?.applicationInfo

    private fun hasPermission(permission: String, uid: Int): Boolean =
        // Check the target user's UID rather than the root service's own user or calling Binder identity.
        mSystemContext.checkPermission(permission, -1, uid) == PackageManager.PERMISSION_GRANTED

    private fun canInstallWithPlay(packageName: String, apks: List<File>): Boolean {
        val playApplicationInfo = getPlayApplicationInfo() ?: return false
        if (!hasPermission(Manifest.permission.INSTALL_PACKAGES, playApplicationInfo.uid)) {
            return false
        }
        // Read the base APK metadata to check its testOnly flag and target SDK before choosing an installer.
        val apkApplicationInfo = apks.firstNotNullOfOrNull { apk ->
            mSystemContext.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
                ?.takeIf { it.packageName == packageName }?.applicationInfo
        } ?: return false
        val isTestOnly = (apkApplicationInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0
        return canUsePlayInstaller(
            sdkInt = Build.VERSION.SDK_INT,
            hasInstallPermission = true,
            isTestOnly = isTestOnly,
            hasTestInstallPermission = isTestOnly && hasPermission(INSTALL_TEST_ONLY_PACKAGE_PERMISSION, playApplicationInfo.uid),
        ) { apkApplicationInfo.targetSdkVersion }
    }

    fun connect(token: String, installer: IApkInstaller) = synchronized(mLock) {
        if (token == mToken && !mIsClosed && mProcess != null && !mConnection.isDone) {
            val binder = installer.asBinder()
            binder.linkToDeath(mWorkerDeathRecipient, 0)
            mRemoteBinder = binder
            mConnection.complete(installer)
        }
    }

    fun install(packageName: String, apks: List<File>) {
        require(packageName.isNotBlank())
        require(apks.isNotEmpty() && apks.all { it.isFile && it.length() > 0 }) { "Missing or empty APK files" }
        synchronized(mLock) { check(!mIsClosed) { "APK installer is closed" } }
        if (canInstallWithPlay(packageName, apks)) {
            installWithWorker(packageName, apks)
        } else {
            installAsShell(packageName, apks)
        }
    }

    private fun installAsShell(packageName: String, apks: List<File>) {
        val inputs = ArrayList<FileInputStream>(apks.size)
        try {
            apks.forEach { inputs.add(it.inputStream()) }
            InstallApkHelper(getShellInstaller()).install(packageName, inputs)
        } finally {
            inputs.forEach { runCatching { it.close() } }
        }
    }

    private fun installWithWorker(packageName: String, apks: List<File>) {
        val descriptors = ArrayList<ParcelFileDescriptor>(apks.size)
        val result = CompletableFuture<String?>()
        val installationError: String?
        try {
            val installer = getInstaller()
            synchronized(mLock) {
                check(!mIsClosed && mPendingResult == null) { "APK installer is unavailable or busy" }
                mPendingResult = result
            }
            apks.forEach { descriptors.add(ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)) }
            installer.install(packageName, descriptors, object : IApkInstallCallback.Stub() {
                override fun onSessionCreated(sessionId: Int) {
                    mSessionId.set(sessionId)
                }

                override fun onFinished(error: String?) {
                    mSessionId.set(-1)
                    result.complete(error)
                }
            })
            installationError = result.get(INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        } catch (error: Exception) {
            // Never replay a request: a lost reply can mean the installation already committed.
            runCatching { close() }.onFailure { error.addSuppressed(it) }
            if (error is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            throw error
        } finally {
            descriptors.forEach { runCatching { it.close() } }
            synchronized(mLock) { mPendingResult = null }
        }
        check(installationError == null) { installationError.orEmpty() }
    }

    private fun getInstaller(): IApkInstaller {
        synchronized(mLock) {
            check(!mIsClosed) { "APK installer is closed" }
            if (mProcess == null) {
                val process = ProcessBuilder(
                    "/system/bin/app_process",
                    "/system/bin",
                    ApkInstallerMain::class.java.name,
                    mUserId.toString(),
                    mAppPackageName,
                    mClientUserId.toString(),
                    mAction,
                    mToken,
                ).apply {
                    environment()["CLASSPATH"] = mAppApkPath
                    redirectErrorStream(true)
                }.start()
                mProcess = process
                Thread({ monitorProcess(process) }, "$PROCESS_MONITOR_THREAD_NAME_PREFIX$mUserId").apply { isDaemon = true }.start()
            }
        }
        return mConnection.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun monitorProcess(process: Process) {
        try {
            val output = readProcessOutput(process)
            failPendingRequests("APK installer exited (${process.waitFor()}): $output")
        } catch (error: Exception) {
            failPendingRequests("Cannot read APK installer output: ${error.javaClass.simpleName}")
        } finally {
            mProcessMonitorCompletion.countDown()
        }
    }

    private fun readProcessOutput(process: Process): String {
        return process.inputStream.reader().use { reader ->
            val output = StringBuilder()
            val buffer = CharArray(1024)
            var count = reader.read(buffer)
            while (count != -1) {
                val remaining = MAX_OUTPUT_LENGTH - output.length
                if (remaining > 0) {
                    output.appendRange(buffer, 0, minOf(count, remaining))
                }
                // Continue reading and discard excess output to prevent the pipe from filling and blocking the worker.
                count = reader.read(buffer)
            }
            output.toString()
        }
    }

    private fun failPendingRequests(message: String) = synchronized(mLock) {
        val error = IllegalStateException(message)
        mConnection.completeExceptionally(error)
        mPendingResult?.completeExceptionally(error)
    }

    override fun close() {
        val process = synchronized(mLock) {
            if (mIsClosed) {
                return
            }
            mIsClosed = true
            failPendingRequests("APK installer closed")
            mProcess
        }
        if (process != null) {
            stopProcess(process)
        }
        val sessionId = mSessionId.getAndSet(-1)
        if (sessionId >= 0) {
            runCatching { getShellInstaller().abandonSession(sessionId) }
        }
        runCatching { mRemoteBinder?.unlinkToDeath(mWorkerDeathRecipient, 0) }
    }

    private fun stopProcess(process: Process) {
        // The worker exits when stdin closes, either during cleanup or when the root parent dies.
        runCatching { process.outputStream.close() }
        runCatching {
            if (!mProcessMonitorCompletion.await(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy()
                mProcessMonitorCompletion.await(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        }.onFailure { error ->
            if (error is InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        // The monitor may stop after an output read error while the process is still running.
        runCatching { process.destroy() }
        runCatching { process.inputStream.close() }
        runCatching { process.errorStream.close() }
    }

    private fun getShellInstaller(): PackageInstaller =
        mSystemContext.castTo<ContextHidden>()
            .createPackageContextAsUser(SHELL_PACKAGE_NAME, 0, UserHandleHidden.of(mUserId))
            .packageManager.packageInstaller

    companion object {
        private const val INSTALL_TEST_ONLY_PACKAGE_PERMISSION = "android.permission.INSTALL_TEST_ONLY_PACKAGE"
        private const val GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending"
        private const val SHELL_PACKAGE_NAME = "com.android.shell"
        private const val MAX_OUTPUT_LENGTH = 8192
        private const val PROCESS_MONITOR_THREAD_NAME_PREFIX = "apk-installer-"
        private const val START_TIMEOUT_SECONDS = 30L
        private const val INSTALL_TIMEOUT_MINUTES = 10L
        private const val CLEANUP_TIMEOUT_SECONDS = 5L
    }
}

/**
 * Checks whether Play has the permissions required by the APK and meets the device's target SDK threshold.
 * Returns false to select shell installation when permission is missing or, on Android 14+, the target SDK is too low or unknown.
 * Call before starting installation; switching installers after a failure could repeat an already committed installation.
 */
internal fun canUsePlayInstaller(
    sdkInt: Int,
    hasInstallPermission: Boolean,
    isTestOnly: Boolean,
    hasTestInstallPermission: Boolean,
    readTargetSdk: () -> Int?,
): Boolean {
    // Android clears INSTALL_ALLOW_TEST when the installer lacks INSTALL_TEST_ONLY_PACKAGE.
    if (!hasInstallPermission || (isTestOnly && !hasTestInstallPermission)) {
        return false
    }
    if (sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }
    // Use target SDK 23 as the minimum on Android 14 and 24 on Android 15+.
    // On Android 15+, route APKs targeting 23 through shell even if the system allows them.
    // https://cs.android.com/android/platform/superproject/+/android-15.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=565
    val minimumTargetSdk = if (sdkInt == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Build.VERSION_CODES.M else Build.VERSION_CODES.N
    val targetSdk = readTargetSdk() ?: return false
    return targetSdk >= minimumTargetSdk
}
