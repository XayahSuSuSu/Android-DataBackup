package com.xayah.feature.main.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.currentRoute
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.value
import com.xayah.feature.main.home.backup.PageBackup
import com.xayah.feature.main.home.cloud.page.account.PageCloudAccount
import com.xayah.feature.main.home.cloud.page.list.PageCloudList
import com.xayah.feature.main.home.common.BottomBar
import com.xayah.feature.main.home.common.TopBar
import com.xayah.feature.main.home.common.settings.PageSettings
import com.xayah.feature.main.home.restore.PageRestore

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
private fun HomeScaffold(navController: NavHostController, snackbarHostState: SnackbarHostState, content: @Composable BoxScope.() -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val currentRoute = navController.currentRoute()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (currentRoute in HomeRoutes.RouteList.map { it.route }) {
                TopBar(scrollBehavior = scrollBehavior, title = HomeRoutes.ofTitle(currentRoute).value)
            } else {
                SecondaryTopBar(
                    scrollBehavior = scrollBehavior,
                    title = StringResourceToken.fromString(HomeRoutes.ofTitle(currentRoute).value),
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        },
        bottomBar = {
            BottomBar(currentRoute = currentRoute, navController = navController, routeList = HomeRoutes.RouteList)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun HomeGraph() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    HomeScaffold(navController = navController, snackbarHostState = snackbarHostState) {
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
            composable(HomeRoutes.Cloud.route) {
                PageCloudList(navController = navController)
            }
            composable(HomeRoutes.CloudAccount.route) {
                PageCloudAccount(navController = navController)
            }
            composable(HomeRoutes.Settings.route) {
                PageSettings(snackbarHostState = snackbarHostState)
            }
        }
    }
}
