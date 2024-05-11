package com.xayah.databackup.index

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.component.LocalActionsState
import com.xayah.core.ui.component.rememberActionsState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.feature.main.cloud.account.PageCloudAccount
import com.xayah.feature.main.dashboard.PageDashboard
import com.xayah.feature.main.restore.PageRestore
import com.xayah.feature.main.settings.PageSettings

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MainIndexGraph() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionsState = rememberActionsState()

    CompositionLocalProvider(LocalActionsState provides actionsState) {
        MainIndexScaffold(navController = navController, snackbarHostState = snackbarHostState) {
            AnimatedNavHost(
                navController = navController,
                startDestination = MainIndexRoutes.Dashboard.route,
            ) {
                composable(MainIndexRoutes.Dashboard.route) {
                    PageDashboard()
                }
                composable(MainIndexRoutes.Local.route) {}
                composable(MainIndexRoutes.Cloud.route) {}
                composable(MainRoutes.CloudAccount.route) {
                    PageCloudAccount(navController = navController, snackbarHostState = snackbarHostState)
                }
                composable(MainIndexRoutes.Settings.route) {
                    PageSettings()
                }
            }
        }
    }
}
