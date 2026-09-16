package com.xayah.databackup.service.restore

import android.system.Os
import android.system.OsConstants
import androidx.annotation.WorkerThread
import com.xayah.databackup.util.PathHelper
import com.xayah.libnative.Rustic
import java.io.File
import java.util.UUID

/**
 * Restores an application's APKs from a Rustic snapshot.
 *
 * Extracts all selected APKs into a temporary directory unique to each invocation,
 * then delegates installation to [InstallApkHelper]. Attempts to remove that directory
 * after either success or failure, without following symbolic links during cleanup.
 */
internal class RestoreApkHelper(
    private val mCacheDir: File,
    private val mInstaller: InstallApkHelper,
) {
    @WorkerThread
    fun restore(repositoryPath: String, password: String, snapshotId: String, packageName: String, apkPaths: List<String>) {
        require(apkPaths.isNotEmpty()) { "No APK sources selected" }
        val cacheDir = PathHelper.getCacheDir(PathHelper.CACHE_SUBDIR_RESTORE_APK, mCacheDir)
        val staging = File(cacheDir, UUID.randomUUID().toString())
        check(staging.mkdir()) { "Failed to create APK staging directory: $staging" }
        val result = runCatching {
            val apks = apkPaths.mapIndexed { index, path ->
                val apk = File(staging, index.toString())
                Rustic.restoreSnapshot(repositoryPath, password, "$snapshotId:$path", apk.path)
                check(!apk.isSymbolicLink() && apk.isFile && apk.length() > 0) { "Failed to extract APK: $path" }
                apk
            }
            mInstaller.install(packageName, apks)
        }

        runCatching {
            deleteStaging(staging)
        }.onFailure { cleanup ->
            result.exceptionOrNull()?.addSuppressed(cleanup) ?: throw cleanup
        }

        result.getOrThrow()
    }

    private fun deleteStaging(file: File) {
        // File.walk follows directory symlinks. Only delete entries inside our staging tree.
        if (!file.isSymbolicLink() && file.isDirectory) {
            checkNotNull(file.listFiles()) { "Failed to list APK staging directory: $file" }.forEach(::deleteStaging)
        }
        check(file.delete()) { "Failed to clean APK staging path: $file" }
    }

    private fun File.isSymbolicLink(): Boolean = OsConstants.S_ISLNK(Os.lstat(path).st_mode)
}
