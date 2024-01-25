package com.xayah.core.datastore

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
        "f2fs",
        "texfat",
    )
    val DefaultMediaList = listOf(
        "Pictures" to "/storage/emulated/0/Pictures",
        "Music" to "/storage/emulated/0/Music",
        "DCIM" to "/storage/emulated/0/DCIM",
        "Download" to "/storage/emulated/0/Download",
    )
}