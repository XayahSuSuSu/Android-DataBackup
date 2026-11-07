package com.xayah.databackup.service.restore.rustic

import android.system.Os
import android.system.OsConstants
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.entity.backup.ApkInstallSource
import com.xayah.databackup.util.PathHelper
import com.xayah.libnative.RusticWrapper
import java.io.File
import java.util.UUID

/**
 * Restores an application's APKs from a Rustic snapshot.
 *
 * Extracts all selected APKs into a temporary directory unique to each invocation,
 * then delegates installation to the supplied installation operation. Attempts to remove that directory
 * after either success or failure, without following symbolic links during cleanup.
 */
internal class RusticRestoreApkHelper(
    private val mCacheDir: File,
    private val mInstallApks: (String, List<File>, ApkInstallSource?) -> Unit,
) {
    @WorkerThread
    fun restore(repositoryPath: String, password: String, snapshotId: String, packageName: String, userId: Int, apkPaths: List<String>) {
        require(apkPaths.isNotEmpty()) { "No APK sources selected" }
        val cacheDir = PathHelper.getCacheDir(PathHelper.CACHE_SUBDIR_RESTORE_APK, mCacheDir)
        val staging = File(cacheDir, UUID.randomUUID().toString())
        check(staging.mkdir()) { "Failed to create APK staging directory: $staging" }
        val result = runCatching {
            val apks = apkPaths.mapIndexed { index, path ->
                val apk = File(staging, index.toString())
                RusticWrapper.restoreSnapshot(repositoryPath, password, "$snapshotId:$path", apk.path)
                check(!apk.isSymbolicLink() && apk.isFile && apk.length() > 0) { "Failed to extract APK: $path" }
                apk
            }
            val manifestPath = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getRusticManifestFileRelativePath())
            val serialized = RusticWrapper.readSnapshotTextFiles(repositoryPath, password, snapshotId, listOf(manifestPath))
            val files = requireNotNull(Moshi.Builder().build().adapter<Map<String, String>>().fromJson(serialized))
            val manifest = RusticRestoreMetadataHelper().deserializeManifest(files.getValue(manifestPath))
            val app = manifest.apps.single { it.packageName == packageName && it.userId == userId }
            mInstallApks(packageName, apks, app.installSource)
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
