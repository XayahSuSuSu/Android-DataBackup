package com.xayah.databackup.rootservice

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextHidden
import android.content.pm.PackageInstaller
import android.content.pm.PackageManagerHidden
import android.content.pm.SessionParamsHidden
import android.os.Build
import android.os.UserHandleHidden
import androidx.annotation.WorkerThread
import com.xayah.hiddenapi.castTo
import java.io.File

/**
 * Installs base and split APKs in a single session.
 * Blocks until installation completes or times out
 */
internal class ApkInstaller(systemContext: Context, userId: Int) {
    companion object {
        private const val GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending"
        private const val SHELL_PACKAGE_NAME = "com.android.shell"
    }

    private val mInstaller = getPackageInstallerForUser(systemContext, userId)

    private fun getPackageInstallerForUser(systemContext: Context, userId: Int): PackageInstaller {
        val installerPackage = if (systemContext.isPackageInstalled(GOOGLE_PLAY_PACKAGE_NAME, userId)) {
            GOOGLE_PLAY_PACKAGE_NAME
        } else {
            SHELL_PACKAGE_NAME
        }
        return systemContext.castTo<ContextHidden>()
            .createPackageContextAsUser(installerPackage, 0, UserHandleHidden.of(userId)).packageManager.packageInstaller
    }

    private fun Context.isPackageInstalled(packageName: String, userId: Int): Boolean =
        runCatching { packageManager.castTo<PackageManagerHidden>().getPackageInfoAsUser(packageName, 0, userId) }.isSuccess

    @SuppressLint("RequestInstallPackagesPolicy")
    @WorkerThread
    fun install(packageName: String, apks: List<File>) {
        require(packageName.isNotBlank())
        require(apks.isNotEmpty() && apks.all { it.isFile && it.length() > 0 }) { "Missing or empty APK files" }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apks.sumOf { it.length() })
            castTo<SessionParamsHidden>().apply {
                installFlags = installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING or PackageManagerHidden.INSTALL_ALLOW_TEST

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    installFlags = installFlags or PackageManagerHidden.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
                }
            }
        }
        val sessionId = mInstaller.createSession(params)
        runCatching {
            mInstaller.openSession(sessionId).use { session ->
                apks.forEachIndexed { index, apk ->
                    session.openWrite(index.toString(), 0, apk.length()).use { output ->
                        apk.inputStream().use { it.copyTo(output) }
                        session.fsync(output)
                    }
                }
                val receiver = InstallResultReceiver()
                session.commit(receiver.sender)
                val result = receiver.await()
                val status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                check(status == PackageInstaller.STATUS_SUCCESS) {
                    "APK installation failed ($status): ${result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}"
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
