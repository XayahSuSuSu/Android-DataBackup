package com.xayah.databackup.service.restore

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfoHidden
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.WorkerThread
import com.xayah.libnative.NativeLib
import com.xayah.libnative.NativeLib.SELINUX_ANDROID_RESTORECON_FORCE
import com.xayah.libnative.NativeLib.SELINUX_ANDROID_RESTORECON_RECURSE
import com.xayah.libnative.Rustic
import com.xayah.hiddenapi.castTo
import java.io.File

/**
 * Restores internal app data in place without rollback.
 * The caller must serialize restores and provide a callback to stop the app.
 * Keeps system-created data roots and repairs directory metadata after restoration,
 * including when extraction fails.
 *
 * Directory metadata handling references AOSP installd's
 * createAppDataDirs/createAppDataLocked:
 *
 * see [InstalldNativeService.cpp](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/native/cmds/installd/InstalldNativeService.cpp)
 */
internal class RestoreInternalDataHelper {
    private data class RestoreEntry(
        val snapshotSelector: String,
        val targetDir: File,
        val isCe: Boolean,
        val sourceUid: Int,
    )

    @WorkerThread
    fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        app: ApplicationInfo,
        seInfo: String,
        sources: Map<Boolean, String>,
        stopApp: () -> Unit,
    ) {
        require(snapshotId.matches(Regex("[0-9a-fA-F]{64}"))) { "A full snapshot ID is required" }
        val plan = sources.map { (isCe, path) ->
            val targetPath = if (isCe) app.castTo<ApplicationInfoHidden>().credentialProtectedDataDir else app.deviceProtectedDataDir
            check(!targetPath.isNullOrBlank()) { "Package has no ${if (isCe) "CE" else "DE"} data directory" }
            val target = File(targetPath)
            val stat = Os.lstat(target.path)
            check(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == app.uid) { "Unprepared app data directory: $target" }
            // Preserve the system-created data directory's encryption policy and inode.
            // Require it to exist instead of recreating it with mkdirs().
            val snapshotSelector = "$snapshotId:$path"
            RestoreEntry(snapshotSelector, target, isCe, Rustic.readSnapshotDirectoryUid(repositoryPath, password, snapshotSelector))
        }
        stopApp()
        plan.forEach { entry ->
            val target = entry.targetDir
            // Record the filesystem device ID so cleanup can reject entries on another filesystem.
            val device = Os.lstat(target.path).st_dev
            // Save the installed app's legacy lib symlink target so it can be restored after extraction.
            val lib = File(target, "lib")
            val libTarget = statOrNull(lib)?.takeIf { OsConstants.S_ISLNK(it.st_mode) }?.let { Os.readlink(lib.path) }
            val result = runCatching {
                // Remove stale databases/preferences without following links. Preserve system-created cache roots.
                children(target).forEach { child ->
                    check(Os.lstat(child.path).st_dev == device) { "Refusing to cross a mount point: $child" }
                    if (child.name in listOf("cache", "code_cache") && OsConstants.S_ISDIR(Os.lstat(child.path).st_mode)) {
                        children(child).forEach { delete(it, device) }
                    } else {
                        delete(child, device)
                    }
                }
                Rustic.restoreSnapshot(
                    repositoryPath,
                    password,
                    entry.snapshotSelector,
                    target.path,
                    Rustic.RestoreOptions(numericId = true, verifyExisting = true),
                )
            }
            // Repair ownership, permissions, and SELinux labels even if extraction fails.
            // Preserve the extraction error if metadata repair also fails.
            runCatching {
                check(NativeLib.chownAppDir(target.path, app.uid, entry.sourceUid) == 0) { "Failed to restore app data ownership: $target" }
                Os.chown(target.path, app.uid, app.uid)
                Os.chmod(target.path, if (app.targetSdkVersion >= 24) 448 else 489) // 0700 / 0751
                val appId = app.uid % 100000
                val cacheGid = if (android.os.Build.VERSION.SDK_INT >= 26 && appId in 10000..19999) app.uid + 10000 else app.uid
                listOf("cache", "code_cache").forEach { name ->
                    val cache = File(target, name)
                    if (statOrNull(cache) == null) Os.mkdir(cache.path, 448)
                    check(OsConstants.S_ISDIR(Os.lstat(cache.path).st_mode)) { "Invalid cache directory: $cache" }
                    Os.chown(cache.path, app.uid, cacheGid)
                    Os.chmod(cache.path, if (android.os.Build.VERSION.SDK_INT >= 26) 1529 else 505) // 02771 / 0771
                }
                if (entry.isCe) {
                    check(
                        NativeLib.writePathInode(target.path, "cache", "user.inode_cache") == 0 &&
                                NativeLib.writePathInode(target.path, "code_cache", "user.inode_code_cache") == 0
                    ) {
                        "Failed to write cache inode attributes: $target"
                    }
                }
                check(NativeLib.chownAppDir(target.path, app.uid, app.uid) == 0) { "Failed to restore app data ownership: $target" }
                // Legacy lib symlinks must point at the installed APK, not the source device's APK path.
                if (libTarget != null) {
                    if (statOrNull(lib) != null) delete(lib, device)
                    Os.symlink(libTarget, lib.path)
                } else if (statOrNull(lib)?.let { OsConstants.S_ISLNK(it.st_mode) } == true) {
                    check(lib.delete()) { "Failed to remove obsolete library link: $lib" }
                }
                check(
                    NativeLib.selinuxAndroidRestoreconPkgdir(
                        target.path, seInfo, app.uid, SELINUX_ANDROID_RESTORECON_RECURSE or SELINUX_ANDROID_RESTORECON_FORCE
                    ) == 0
                ) {
                    "Failed to restore data SELinux labels: $target"
                }
            }.onFailure { repair -> result.exceptionOrNull()?.addSuppressed(repair) ?: throw repair }
            result.getOrThrow()
        }
    }

    private fun children(file: File): Array<File> = checkNotNull(file.listFiles()) { "Failed to list $file" }

    private fun statOrNull(file: File) = runCatching { Os.lstat(file.path) }.getOrElse { error ->
        if (error is ErrnoException && error.errno == OsConstants.ENOENT) null
        else throw error
    }

    private fun delete(file: File, device: Long) {
        val stat = Os.lstat(file.path)
        check(stat.st_dev == device) { "Refusing to cross a mount point: $file" }
        if (OsConstants.S_ISDIR(stat.st_mode)) {
            children(file).forEach { delete(it, device) }
        }
        check(file.delete()) { "Failed to delete $file" }
    }
}
