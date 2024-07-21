package com.xayah.core.ui.model

data class DialogRadioItem<T>(
    val enum: T? = null,
    val title: String,
    val desc: String? = null,
)

data class DialogCheckBoxItem<T>(
    val enum: T? = null,
    val title: String,
    val desc: String? = null,
)
