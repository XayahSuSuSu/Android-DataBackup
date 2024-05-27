package com.xayah.core.datastore

object ConstantUtil {
    const val DEFAULT_PATH_PARENT = "/storage/emulated/0"
    const val DEFAULT_PATH_CHILD = "DataBackup"
    const val DEFAULT_PATH = "${DEFAULT_PATH_PARENT}/${DEFAULT_PATH_CHILD}"
    val SupportedExternalStorageFormat = listOf(
        "sdfat",
        "fuseblk",
        "exfat",
        "ntfs",
        "ext4",
        "f2fs",
        "texfat",
    )
}
