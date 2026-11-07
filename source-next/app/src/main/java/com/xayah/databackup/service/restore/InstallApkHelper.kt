package com.xayah.databackup.service.restore

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import android.content.pm.PackageManagerHidden
import android.content.pm.SessionParamsHidden
import android.os.Build
import androidx.annotation.WorkerThread
import com.xayah.hiddenapi.castTo
import java.io.FileInputStream

/**
 * Installs an app's base and split APKs in a single session using the calling process's identity.
 * The caller is responsible for closing the APK input streams after installation completes or fails.
 */
internal class InstallApkHelper(
    private val mInstaller: PackageInstaller,
    private val mInstallingPackageName: String? = null,
) {
    // APKs are installed sequentially, allowing the buffer to be reused across files and apps.
    private val mCopyBuffer by lazy { ByteArray(64 * 1024) }

    private fun createSessionParams(packageName: String, apkInputs: List<FileInputStream>): PackageInstaller.SessionParams =
        PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apkInputs.sumOf { it.channel.size() })
            castTo<SessionParamsHidden>().apply {
                // Android 9+ supports overriding the installer package; older versions use PackageInstaller's package name.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mInstallingPackageName != null) {
                    installerPackageName = mInstallingPackageName
                }
                installFlags = installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING or PackageManagerHidden.INSTALL_ALLOW_TEST
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    installFlags = installFlags or PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
                }
            }
        }

    @WorkerThread
    @SuppressLint("RequestInstallPackagesPolicy")
    fun install(
        packageName: String,
        apkInputs: List<FileInputStream>,
        onSessionCreated: (Int) -> Unit = {},
    ) {
        require(packageName.isNotBlank())
        require(apkInputs.isNotEmpty() && apkInputs.all { it.channel.size() > 0 }) { "Missing or empty APK files" }
        val params = createSessionParams(packageName, apkInputs)
        val sessionId = mInstaller.createSession(params)
        runCatching {
            onSessionCreated(sessionId)
            mInstaller.openSession(sessionId).use { session ->
                val buffer = mCopyBuffer
                apkInputs.forEachIndexed { index, input ->
                    session.openWrite(index.toString(), 0, input.channel.size()).use { output ->
                        var count = input.read(buffer)
                        while (count != -1) {
                            output.write(buffer, 0, count)
                            count = input.read(buffer)
                        }
                        session.fsync(output)
                    }
                }
                val receiver = ApkInstallResultReceiver()
                session.commit(receiver.sender)
                val result = receiver.await()
                check(result.hasExtra(PackageInstaller.EXTRA_STATUS)) { "Missing APK installation status" }
                val status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
                if (status != PackageInstaller.STATUS_SUCCESS) {
                    val message = "APK installation failed ($status): ${result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}"
                    if (status > PackageInstaller.STATUS_SUCCESS) {
                        throw ApkInstallFailure(message)
                    }
                    // The installation is waiting for user confirmation; retrying could cause a duplicate installation.
                    error(message)
                }
            }
        }.onFailure { error ->
            runCatching {
                mInstaller.abandonSession(sessionId)
            }.onFailure { cleanup ->
                error.addSuppressed(cleanup)
            }
        }.getOrThrow()
    }
}

/** A confirmed installation failure that allows a safe retry with another installer. */
internal class ApkInstallFailure(message: String) : IllegalStateException(message)
