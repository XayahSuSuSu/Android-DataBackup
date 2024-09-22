package com.xayah.core.model

data class File(
    val id: Long,
    val name: String,
    val path: String,
    val preserveId: Long,
    val selected: Boolean,
)
