package com.xayah.feature.main.home

import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken

data class ActivityCardItem(
    val label: StringResourceToken,
    val icon: ImageVectorToken,
    val onClick: () -> Unit,
)

data class UtilityChipItem(
    val label: StringResourceToken,
    val icon: ImageVectorToken,
    val onClick: () -> Unit,
)
