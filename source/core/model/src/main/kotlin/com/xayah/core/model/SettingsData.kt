package com.xayah.core.model

val DEFAULT_COMPRESSION_TYPE = CompressionType.ZSTD
const val DEFAULT_APPS_UPDATE_TIME = 0L

data class SettingsData(
    val compressionType: CompressionType = DEFAULT_COMPRESSION_TYPE,
    val appsUpdateTime: Long = DEFAULT_APPS_UPDATE_TIME,
)
