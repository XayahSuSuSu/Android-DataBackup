package com.xayah.databackup.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.main.router.currentRoute
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.StateTokens
import com.xayah.databackup.util.ConstantUtil

@Composable
fun GuideTopBar(title: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorScheme.onSurfaceVariant(),
            modifier = Modifier
                .size(CommonTokens.IconMediumSize)
                .paddingBottom(CommonTokens.PaddingSmall)
        )
        TopBarTitle(text = title)
    }
}

@ExperimentalMaterial3Api
@Composable
fun SlotScope.MainTopBar(scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = navController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = MainRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Crossfade(targetState = currentRoute, label = StateTokens.CrossFadeLabel) { route ->
                if ((route in routes).not())
                    ArrowBackButton {
                        navController.popBackStack()
                    }
            }
        },
    )
}
