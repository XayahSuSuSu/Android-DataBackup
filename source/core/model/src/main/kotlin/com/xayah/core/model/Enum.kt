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

enum class StorageType {
    INTERNAL,
    EXTERNAL,
    CUSTOM,
}
