package com.xayah.databackup.index

import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken

data class BottomBarItem(
    val label: StringResourceToken,
    val iconToken: ImageVectorToken,
    val route: String,
)
