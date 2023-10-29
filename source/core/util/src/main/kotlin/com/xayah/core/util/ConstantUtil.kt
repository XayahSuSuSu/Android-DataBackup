package com.xayah.core.util

object ConstantUtil {
    const val DefaultPathParent = "/storage/emulated/0"
    const val DefaultPathChild = "DataBackup"
    const val DefaultPath = "${DefaultPathParent}/${DefaultPathChild}"
    val SupportedExternalStorageFormat = listOf(
        "sdfat",
        "fuseblk",
        "exfat",
        "ntfs",
        "ext4",
        "f2fs"
    )
}