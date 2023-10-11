package com.xayah.databackup.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.activity.main.router.currentRoute
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.util.ConstantUtil

@ExperimentalMaterial3Api
@Composable
fun CloudTopBar(scrollBehavior: TopAppBarScrollBehavior?, cloudNavController: NavHostController) {
    val context = LocalContext.current
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = cloudNavController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = CloudRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (currentRoute != CloudRoutes.Main.route) {
                Crossfade(targetState = currentRoute, label = AnimationTokens.CrossFadeLabel) { route ->
                    if ((route in routes).not())
                        ArrowBackButton {
                            cloudNavController.popBackStack()
                        }
                }
            }
        },
        actions = {}
    )
}