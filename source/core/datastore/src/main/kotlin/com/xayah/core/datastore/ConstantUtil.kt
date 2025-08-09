package com.xayah.core.datastore

object ConstantUtil {
    const val STORAGE_EMULATED_PATH = "/storage/emulated"
    private const val DEFAULT_USER_ID = 0
    const val DEFAULT_PATH_PARENT = "${STORAGE_EMULATED_PATH}/${DEFAULT_USER_ID}"
    const val DEFAULT_PATH_CHILD = "DataBackup"
    const val DEFAULT_PATH = "${DEFAULT_PATH_PARENT}/${DEFAULT_PATH_CHILD}"
    const val DEFAULT_IDLE_TIMEOUT = -1
    const val DEFAULT_TIMEOUT = 30000
    const val CONFIGURATIONS_KEY_BLACKLIST = "blacklist"
    const val CONFIGURATIONS_KEY_CLOUD = "cloud"
    const val CONFIGURATIONS_KEY_FILE = "file"
    const val CONFIGURATIONS_KEY_LABEL = "label"
    const val FTP_ANONYMOUS_USERNAME = "anonymous" // https://www.rfc-editor.org/rfc/rfc1635
    const val FTP_ANONYMOUS_PASSWORD = "guest"
    const val LANGUAGE_SYSTEM = "auto"
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

    const val DOC_LINK = "https://DataBackupOfficial.github.io"
    const val GITHUB_LINK = "https://github.com/XayahSuSuSu/Android-DataBackup"
    const val CHAT_LINK = "https://t.me/databackupchat"
    const val DONATE_BMAC_LINK = "https://buymeacoffee.com/xayahsususu"
    const val DONATE_PAYPAL_LINK = "https://paypal.me/XayahSuSuSu"
    const val DONATE_AFD_LINK = "https://afdian.net/a/XayahSuSuSu"

    const val FLAVOR_PREMIUM = "premium"
}
