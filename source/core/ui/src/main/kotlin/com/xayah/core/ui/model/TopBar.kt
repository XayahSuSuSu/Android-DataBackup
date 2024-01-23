package com.xayah.core.ui.model

import com.xayah.core.ui.util.fromString

data class TopBarState(
    val progress: Float = 1f,
    val title: StringResourceToken = StringResourceToken.fromString(""),
    val indeterminate: Boolean = false
)
