package com.xayah.core.ui.model

data class TopBarState(
    val progress: Float = 1f,
    val title: String = "",
    val indeterminate: Boolean = false
)

data class RefreshState(
    val progress: Float = 0f,
    val user: String = "",
    val pkg: String = "",
)
