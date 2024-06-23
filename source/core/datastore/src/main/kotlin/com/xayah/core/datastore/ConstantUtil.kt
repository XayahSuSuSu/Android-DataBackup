package com.xayah.core.datastore

object ConstantUtil {
    private const val DATA_MEDIA_PATH = "/data/media"
    private const val DEFAULT_USER_ID = 0
    const val DEFAULT_PATH_PARENT = "${DATA_MEDIA_PATH}/${DEFAULT_USER_ID}"
    const val DEFAULT_PATH_CHILD = "DataBackup"
    const val DEFAULT_PATH = "${DEFAULT_PATH_PARENT}/${DEFAULT_PATH_CHILD}"
    const val DEFAULT_TIMEOUT = 30000
    const val CONFIGURATIONS_KEY_BLACKLIST = "blacklist"
    const val CONFIGURATIONS_KEY_CLOUD = "cloud"
    const val CONFIGURATIONS_KEY_FILE = "file"
    const val FTP_ANONYMOUS_USERNAME = "anonymous" // https://www.rfc-editor.org/rfc/rfc1635
    const val FTP_ANONYMOUS_PASSWORD = "guest"
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
        "Pictures" to "${DEFAULT_PATH_PARENT}/Pictures",
        "Music" to "${DEFAULT_PATH_PARENT}/Music",
        "DCIM" to "${DEFAULT_PATH_PARENT}/DCIM",
        "Download" to "${DEFAULT_PATH_PARENT}/Download",
    )
}
