package com.xayah.databackup.util

object ConstantUtil {
    const val DefaultBackupParent = "/storage/emulated/0"
    const val DefaultBackupChild = "DataBackup"
    const val DefaultBackupSavePath = "${DefaultBackupParent}/${DefaultBackupChild}"
    val SupportedExternalStorageFormat = listOf(
        "sdfat",
        "fuseblk",
        "exfat",
        "ntfs",
        "ext4",
        "f2fs"
    )
}
