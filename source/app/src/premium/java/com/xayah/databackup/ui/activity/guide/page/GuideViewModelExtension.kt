package com.xayah.databackup.ui.activity.guide.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import com.xayah.databackup.ui.activity.guide.router.GuideRoutes

class Intro(title: String = "") : GuideUiState(
    title = title,
    icon = Icons.Rounded.AccountCircle,
    fabIcon = Icons.Rounded.ArrowForward,
    onFabClick = { navController ->
        navController.navigate(GuideRoutes.Update.route)
    }
)
