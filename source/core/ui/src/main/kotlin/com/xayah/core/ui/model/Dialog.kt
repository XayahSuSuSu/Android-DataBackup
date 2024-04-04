package com.xayah.core.ui.model

data class DialogRadioItem<T>(
    val enum: T,
    val title: StringResourceToken,
    val desc: StringResourceToken
)
