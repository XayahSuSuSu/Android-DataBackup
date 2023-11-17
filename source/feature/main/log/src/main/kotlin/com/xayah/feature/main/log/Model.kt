package com.xayah.feature.main.log

data class LogCardItem(
    val name: String,
    val sizeBytes: Double,
    val timestamp: Long,
    val path: String,
)