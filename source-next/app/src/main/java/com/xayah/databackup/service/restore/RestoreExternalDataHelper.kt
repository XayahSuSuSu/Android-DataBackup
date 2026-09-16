package com.xayah.databackup.service.restore

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.IVold
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.UserHandleHidden
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.WorkerThread
import androidx.annotation.RequiresApi
import com.xayah.libnative.NativeLib
import com.xayah.libnative.NativeLib.SELINUX_ANDROID_RESTORECON_FORCE
import com.xayah.libnative.NativeLib.SELINUX_ANDROID_RESTORECON_RECURSE
import com.xayah.libnative.Rustic
import java.io.File

/**
 * Restores external app data in place without rollback.
 * The caller must serialize restores and provide a callback to stop the app.
 * Prepares destination directories, preserves their roots when clearing contents,
 * and repairs destination metadata even if extraction fails.
 *
 * External directory handling references AOSP installd's clearAppData/destroyAppData
 * and vold's setupAppDir/fixupAppDir.
 *
 * see [InstalldNativeService.cpp](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/native/cmds/installd/InstalldNativeService.cpp)
 *
 * see [Utils.cpp](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:system/vold/Utils.cpp)
 */
internal class RestoreExternalDataHelper {
    private companion object {
        val SNAPSHOT_ID_PATTERN = Regex("[0-9a-fA-F]{64}")

        const val PROC_MOUNTS_PATH = "/proc/mounts"
        const val PROC_FILESYSTEMS_PATH = "/proc/filesystems"
        const val PROPERTY_SDCARDFS_ENABLED = "external_storage.sdcardfs.enabled"
        const val MOUNT_PATH_INDEX = 1
        const val MOUNT_TYPE_INDEX = 2
        const val MOUNT_SOURCE_INDEX = 0
        const val MOUNT_OPTIONS_INDEX = 3
        const val FILESYSTEM_SDCARDFS = "sdcardfs"
        const val FILESYSTEM_ESDFS = "esdfs"
        const val FILESYSTEM_FUSE = "fuse"
        val DERIVED_FILESYSTEMS = setOf(FILESYSTEM_SDCARDFS, FILESYSTEM_ESDFS)
        val EMULATED_FILESYSTEMS = DERIVED_FILESYSTEMS + FILESYSTEM_FUSE
        const val MOUNT_OPTION_UNSHARED_OBB = "unshared_obb"

        const val BACKING_STORAGE_ROOT = "/data/media"
        const val RUNTIME_STORAGE_ROOT = "/mnt/runtime/default/emulated"
        const val EMULATED_STORAGE_ROOT = "/storage/emulated"
        const val USER_MOUNT_ROOT = "/mnt/user"
        const val EMULATED_DIRECTORY = "emulated"
        const val ANDROID_DIRECTORY = "Android"
        const val CACHE_DIRECTORY = "cache"
        const val OBB_DIRECTORY = "obb"

        const val VOLD_SERVICE_NAME = "vold"
        const val XATTR_POSIX_ACL_DEFAULT = "system.posix_acl_default"
        const val XATTR_POSIX_ACL_ACCESS = "system.posix_acl_access"

    }

    private data class RestoreEntry(
        val snapshotSelector: String,
        val targetDir: File,
        val backingDir: File,
        val storagePath: String,
    )

    @WorkerThread
    fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        app: ApplicationInfo,
        sources: Map<String, String>,
        stopApp: () -> Unit,
    ) {
        require(snapshotId.matches(SNAPSHOT_ID_PATTERN)) { "A full snapshot ID is required" }
        val userId = UserHandleHidden.getUserId(app.uid)
        val runtimeVolume = File("$RUNTIME_STORAGE_ROOT/$userId")
        val mounts = File(PROC_MOUNTS_PATH).readLines().map { it.split(' ') }
        // Inspect this volume, not unrelated sdcardfs mounts on another storage device.
        val runtimeMount = mounts.filter {
            it.size > MOUNT_OPTIONS_INDEX &&
                    (runtimeVolume.path == it[MOUNT_PATH_INDEX] || runtimeVolume.path.startsWith(it[MOUNT_PATH_INDEX].trimEnd('/') + "/"))
        }.maxByOrNull { it[MOUNT_PATH_INDEX].length }
        val derivedPermissions = runtimeMount?.get(MOUNT_TYPE_INDEX) in DERIVED_FILESYSTEMS
        // Android 7–10 uses the mounted sdcard/FUSE or sdcardfs view to derive permissions.
        // Android 11–17 without a derived-permission filesystem uses vold for ownership, modes and project IDs.
        // See https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:system/core/sdcard/sdcard.c
        val useVold = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !derivedPermissions && !isSdcardfsUsed()
        val vold = if (useVold) getVold() else null
        val volume = if (useVold) File("$BACKING_STORAGE_ROOT/$userId") else runtimeVolume
        checkAncestors(volume)
        if (!useVold) {
            check(runtimeMount != null && runtimeMount[MOUNT_TYPE_INDEX] in EMULATED_FILESYSTEMS) {
                "External storage is not mounted: $volume"
            }
            if (derivedPermissions) {
                check(runtimeMount[MOUNT_SOURCE_INDEX] == BACKING_STORAGE_ROOT) { "Unexpected external storage backing path" }
            }
        }
        // Legacy sdcard always grafts Android/obb onto /data/media/obb. sdcardfs does the
        // same unless mounted with unshared_obb (AOSP enables it starting with Android 10).
        // See https://cs.android.com/android/platform/superproject/+/android-10.0.0_r1:system/vold/model/EmulatedVolume.cpp
        val sharedObb = !useVold && (runtimeMount?.get(MOUNT_TYPE_INDEX) == FILESYSTEM_FUSE ||
                MOUNT_OPTION_UNSHARED_OBB !in runtimeMount!![MOUNT_OPTIONS_INDEX].split(','))
        val repository = File(repositoryPath).canonicalPath
        val plan = sources.map { (kind, source) ->
            val relative = "$ANDROID_DIRECTORY/$kind/${app.packageName}"
            val backing = if (kind == OBB_DIRECTORY && sharedObb) {
                File("$BACKING_STORAGE_ROOT/$OBB_DIRECTORY/${app.packageName}")
            } else File("$BACKING_STORAGE_ROOT/$userId/$relative")
            val mounted = File(runtimeVolume, relative)
            val target = if (useVold) backing else mounted
            // Authenticate and validate every selected tree before modifying any destination.
            val snapshotSelector = "$snapshotId:$source"
            Rustic.validateExternalSnapshot(repositoryPath, password, snapshotSelector)
            validateDestination(volume, target)
            if (!useVold) {
                check(backing.canonicalPath == backing.absolutePath) { "Invalid external storage backing path: $backing" }
                statOrNull(backing)?.let { walk(backing, it.st_dev) { } }
            }
            listOf(
                backing,
                mounted,
                File("$EMULATED_STORAGE_ROOT/$userId/$relative"),
                File("$USER_MOUNT_ROOT/$userId/$EMULATED_DIRECTORY/$userId/$relative"),
            ).forEach { alias ->
                val path = alias.canonicalPath
                check(repository != path && !repository.startsWith("$path/")) { "Repository is inside the restore destination" }
            }
            RestoreEntry(snapshotSelector, target, backing, "$EMULATED_STORAGE_ROOT/$userId/$relative/")
        }
        stopApp()
        plan.forEach { entry ->
            val target = entry.targetDir
            if (vold != null) vold.setupAppDir(entry.storagePath, app.uid)
            else check(target.mkdirs() || target.isDirectory) { "Failed to prepare external storage: $target" }
            val stat = Os.lstat(target.path)
            check(OsConstants.S_ISDIR(stat.st_mode)) { "Unprepared external data directory: $target" }
            if (vold != null) check(stat.st_uid == app.uid) { "Storage volume does not match the backup destination: $target" }
            // Validate the whole destination before deleting; never follow links or cross devices.
            walk(target, stat.st_dev) { }
            val result = runCatching {
                children(target).forEach { delete(it, stat.st_dev) }
                Rustic.restoreExternalSnapshot(repositoryPath, password, entry.snapshotSelector, target.path)
            }
            // Repair metadata even if extraction fails, preserving the extraction error if both fail.
            var failure = result.exceptionOrNull()
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vold != null) {
                    repairPermissions(entry, vold, app.uid, stat.st_dev)
                }
            }.onFailure { repair -> failure?.addSuppressed(repair) ?: run { failure = repair } }
            // SELinux labels belong to the lower filesystem on every supported Android version.
            // FUSE/sdcardfs exposes genfscon labels; do not run restorecon on the mounted view.
            runCatching {
                restoreLabels(entry.backingDir)
            }.onFailure { repair -> failure?.addSuppressed(repair) ?: run { failure = repair } }
            failure?.let { throw it }
        }
    }

    private fun validateDestination(volume: File, target: File) {
        // Missing Android/category directories will be prepared by the storage service/filesystem.
        listOf(File(volume, ANDROID_DIRECTORY), target.parentFile!!).forEach { ancestor ->
            statOrNull(ancestor)?.let {
                check(OsConstants.S_ISDIR(it.st_mode)) { "Invalid external storage ancestor: $ancestor" }
            }
        }
        statOrNull(target)?.let {
            check(OsConstants.S_ISDIR(it.st_mode)) { "Invalid external data root: $target" }
            walk(target, it.st_dev) { }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun repairPermissions(entry: RestoreEntry, vold: IVold, uid: Int, device: Long) {
        val target = entry.targetDir
        // fixupAppDir repairs one directory and its immediate children, not an entire tree.
        // Parent first: a cache subtree must retain its distinct external-cache project ID.
        walk(target, device) { directory ->
            val suffix = directory.relativeTo(target).invariantSeparatorsPath
            vold.fixupAppDir(entry.storagePath + if (suffix.isEmpty()) "" else "$suffix/", uid)
            // Rustic removes source xattrs, including inherited ACLs. vold only sets
            // the package root's default ACL. Recreate the inheritance that mkdir/open
            // would receive from that system-generated ACL (including additional app GIDs).
            val defaultAcl = Os.getxattr(target.path, XATTR_POSIX_ACL_DEFAULT)
            Os.setxattr(directory.path, XATTR_POSIX_ACL_DEFAULT, defaultAcl, 0)
            children(directory).forEach { child ->
                Os.setxattr(child.path, XATTR_POSIX_ACL_ACCESS, defaultAcl, 0)
            }
        }
    }

    /** Repairs SELinux labels on the actual backing directory using the destination system's policy on all supported versions. */
    private fun restoreLabels(backing: File) {
        checkAncestors(backing)
        walk(backing, Os.lstat(backing.path).st_dev) { }
        // Resolve labels from the destination system's file_contexts; never hard-code a context.
        check(NativeLib.selinuxAndroidRestorecon(backing.path, SELINUX_ANDROID_RESTORECON_RECURSE or SELINUX_ANDROID_RESTORECON_FORCE) == 0) {
            "Failed to restore external storage SELinux labels: $backing"
        }
    }

    private fun getVold(): IVold = checkNotNull(IVold.Stub.asInterface(ServiceManager.getService(VOLD_SERVICE_NAME))) {
        "Storage daemon is unavailable"
    }

    /** Matches vold's IsSdcardfsUsed(), including when this mount namespace hides its mounts. */
    private fun isSdcardfsUsed(): Boolean =
        SystemProperties.getBoolean(PROPERTY_SDCARDFS_ENABLED, true)
                && File(PROC_FILESYSTEMS_PATH).useLines { lines -> lines.any { it.substringAfterLast('\t') == FILESYSTEM_SDCARDFS } }

    /**
     * Requires the directory and all its ancestors to exist as directories, rejecting symbolic links via lstat.
     */
    private fun checkAncestors(directory: File) {
        directory.parentFile?.let { checkAncestors(it) }
        check(OsConstants.S_ISDIR(Os.lstat(directory.path).st_mode)) { "Unprepared external storage: $directory" }
    }

    private fun children(file: File) = checkNotNull(file.listFiles()) { "Failed to list $file" }

    private fun statOrNull(file: File) = runCatching {
        Os.lstat(file.path)
    }.getOrElse { error ->
        if (error is ErrnoException && error.errno == OsConstants.ENOENT) null else throw error
    }

    private fun walk(file: File, device: Long, directory: (File) -> Unit) {
        val stat = Os.lstat(file.path)
        check(stat.st_dev == device) { "Refusing to cross a mount point: $file" }
        check(OsConstants.S_ISDIR(stat.st_mode) || OsConstants.S_ISREG(stat.st_mode)) { "Unsupported external data entry: $file" }
        check(!OsConstants.S_ISREG(stat.st_mode) || stat.st_nlink == 1L) { "Refusing to modify an externally linked file: $file" }
        if (OsConstants.S_ISDIR(stat.st_mode)) {
            directory(file)
            // Each vold call also fixes ancestors. Visit cache last so later calls cannot reset
            // its root to the ordinary external-data project ID.
            children(file).sortedBy { it.name == CACHE_DIRECTORY }.forEach { walk(it, device, directory) }
        }
    }

    private fun delete(file: File, device: Long) {
        val stat = Os.lstat(file.path)
        check(stat.st_dev == device) { "Refusing to cross a mount point: $file" }
        if (OsConstants.S_ISDIR(stat.st_mode)) children(file).forEach { delete(it, device) }
        check(file.delete()) { "Failed to delete $file" }
    }
}
