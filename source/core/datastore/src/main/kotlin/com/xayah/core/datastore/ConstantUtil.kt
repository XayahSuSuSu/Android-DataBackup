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

    /**
     * Media file extensions (lowercase, without dot) shared by the
     * storage scanner and the thumbnail generator.
     */
    val MediaImageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "heic", "heif", "tif", "tiff", "svg", "avif", "dng"
    )
    val MediaVideoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv",
        "webm", "3gp", "ts", "m2ts", "mpg", "mpeg", "m4v"
    )
    val MediaAudioExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "oga",
        "m4a", "opus", "amr", "mid", "midi", "wma", "aiff"
    )

    /**
     * Saved Wi-Fi network configs (require root).
     * Paths vary by Android version, so all known locations are probed
     * and only the ones that exist on the device are added.
     * Display name to absolute path.
     */
    val KnownWifiConfigs = listOf(
        "WiFi-WifiConfigStore" to "/data/misc/apexdata/com.android.wifi/WifiConfigStore.xml",
        "WiFi-WifiConfigStoreSoftAp" to "/data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml",
        "WiFi-WifiConfigStore-legacy" to "/data/misc/wifi/WifiConfigStore.xml",
        "WiFi-WifiConfigStoreSoftAp-legacy" to "/data/misc/wifi/WifiConfigStoreSoftAp.xml",
        "WiFi-wpa_supplicant" to "/data/misc/wifi/wpa_supplicant.conf",
        "WiFi-p2p_supplicant" to "/data/misc/wifi/p2p_supplicant.conf",
    )

    const val DOC_LINK = "https://DataBackupOfficial.github.io"
    const val GITHUB_LINK = "https://github.com/XayahSuSuSu/Android-DataBackup"
    const val CHAT_LINK = "https://t.me/databackupchat"
    const val DONATE_BMAC_LINK = "https://buymeacoffee.com/xayahsususu"
    const val DONATE_PAYPAL_LINK = "https://paypal.me/XayahSuSuSu"
    const val DONATE_AFD_LINK = "https://afdian.net/a/XayahSuSuSu"

    const val FLAVOR_PREMIUM = "premium"
}
