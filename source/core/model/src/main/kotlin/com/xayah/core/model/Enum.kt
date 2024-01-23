package com.xayah.core.model

const val TAR_SUFFIX = "tar"
const val ZSTD_SUFFIX = "tar.zst"
const val LZ4_SUFFIX = "tar.lz4"

enum class CompressionType(val type: String, val suffix: String, val compressPara: String, val decompressPara: String) {
    TAR("tar", TAR_SUFFIX, "", ""),
    ZSTD("zstd", ZSTD_SUFFIX, "zstd -r -T0 --ultra -1 -q --priority=rt", "zstd"),
    LZ4("lz4", LZ4_SUFFIX, "zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4", "zstd");

    companion object
}

enum class DataType(val type: String) {
    PACKAGE_APK("apk"),
    PACKAGE_USER("user"),
    PACKAGE_USER_DE("user_de"),
    PACKAGE_DATA("data"),
    PACKAGE_OBB("obb"),
    PACKAGE_MEDIA("media"),            // /data/media/$user_id/Android/media
    PACKAGE_CONFIG("config"),          // Json file for reloading
    MEDIA_MEDIA("media"),
    MEDIA_CONFIG("config");

    companion object
}

enum class OpType {
    BACKUP,
    RESTORE;

    companion object
}

enum class TaskType {
    PACKAGE,
    MEDIA;

    companion object
}

enum class StorageType {
    INTERNAL,
    EXTERNAL,
    CUSTOM,
}

enum class SortType {
    ASCENDING,
    DESCENDING;

    companion object
}

enum class OperationState {
    IDLE,
    PROCESSING,
    UPLOADING,
    SKIP,
    DONE,
    ERROR
}

enum class ProcessingState {
    Idle,
    Processing,
    DONE,
}

enum class EmojiString(val emoji: String) {
    PARTY_POPPER("🎉"),
    ALARM_CLOCK("⏰"),
    SPARKLING_HEART("💖"),
    BROKEN_HEART("💔"),
    SWEAT_DROPLETS("💦"),
}

enum class CloudType {
    FTP,
    WEBDAV,
    SMB,
}

enum class SmbVersion(val text: String) {
    SMB_2_0_2("2.0.2"),
    SMB_2_1("2.1"),
    SMB_3_0("3.0"),
    SMB_3_0_2("3.0.2"),
    SMB_3_1_1("3.1.1"),
}

enum class DataState {
    Selected,
    NotSelected,
    Disabled,
}
