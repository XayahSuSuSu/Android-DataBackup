package com.xayah.databackup.util

import com.xayah.databackup.ui.activity.main.router.MainRoutes

object ConstantUtil {
    const val DefaultPathParent = "/storage/emulated/0"
    const val DefaultPathChild = "DataBackup"
    const val DefaultPath = "${DefaultPathParent}/${DefaultPathChild}"

    const val DefaultBackupUserId = 0

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

    val DefaultMediaList = listOf(
        Pair("Pictures","/storage/emulated/0/Pictures"),
        Pair("Music","/storage/emulated/0/Music"),
        Pair("DCIM","/storage/emulated/0/DCIM"),
        Pair("Download","/storage/emulated/0/Download"),
    )

    val FlavorFeatureFoss = "foss"
}
