package com.xayah.feature.home.common.model

import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken

data class SettingsInfoItem(
    val icon: ImageVectorToken,
    val title: StringResourceToken,
    val content: StringResourceToken,
    val onWarning: Boolean = false,
    val onClick: (() -> Unit)? = null,
)
