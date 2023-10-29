package com.xayah.feature.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.util.currentRoute
import com.xayah.core.ui.util.value
import com.xayah.feature.home.backup.PageBackup
import com.xayah.feature.home.common.BottomBar
import com.xayah.feature.home.common.TopBar
import com.xayah.feature.home.common.settings.PageSettings
import com.xayah.feature.home.restore.PageRestore

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
private fun HomeScaffold(navController: NavHostController, content: @Composable BoxScope.() -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val currentRoute = navController.currentRoute()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(scrollBehavior = scrollBehavior, title = HomeRoutes.ofTitle(currentRoute).value)
        },
        bottomBar = {
            BottomBar(currentRoute = currentRoute, navController = navController, routeList = HomeRoutes.RouteList)
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun HomeGraph() {
    val navController = rememberNavController()
    HomeScaffold(navController) {
        NavHost(
            navController = navController,
            startDestination = HomeRoutes.Backup.route,
            enterTransition = { scaleIn() + fadeIn() },
            popEnterTransition = { scaleIn() + fadeIn() },
            exitTransition = { scaleOut() + fadeOut() },
            popExitTransition = { scaleOut() + fadeOut() },
        ) {
            composable(HomeRoutes.Backup.route) {
                PageBackup()
            }
            composable(HomeRoutes.Restore.route) {
                PageRestore()
            }
            composable(HomeRoutes.Settings.route) {
                PageSettings()
            }
        }
    }
}
