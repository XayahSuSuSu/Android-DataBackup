package com.xayah.databackup.util

import com.xayah.databackup.ui.activity.main.router.MainRoutes

object ConstantUtil {
    const val DefaultBackupParent = "/storage/emulated/0"
    const val DefaultBackupChild = "DataBackup"
    const val DefaultBackupSavePath = "${DefaultBackupParent}/${DefaultBackupChild}"
    const val DefaultBackupUserId = 0

    const val DefaultRestoreParent = "/storage/emulated/0"
    const val DefaultRestoreChild = "DataBackup"
    const val DefaultRestoreSavePath = "${DefaultRestoreParent}/${DefaultRestoreChild}"
    const val DefaultRestoreUserId = 0

    val SupportedExternalStorageFormat = listOf(
        "sdfat",
        "fuseblk",
        "exfat",
        "ntfs",
        "ext4",
        "f2fs"
    )

    val MainBottomBarRoutes = listOf(
        MainRoutes.Backup.route,
        MainRoutes.Restore.route,
        MainRoutes.Cloud.route,
        MainRoutes.Settings.route,
    )

    const val ClipDataLabel = "DataBackupClipData"
}
