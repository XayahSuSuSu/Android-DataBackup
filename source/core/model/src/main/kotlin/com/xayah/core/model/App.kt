package com.xayah.core.model

data class App(
    val id: Long,
    val packageName: String,
    val label: String,
    val preserveId: Long,
    val isSystemApp: Boolean,
    val selectionFlag: Int,
    val selected: Boolean,
)
