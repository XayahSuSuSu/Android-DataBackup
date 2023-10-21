package com.xayah.databackup.ui.activity.main.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xayah.databackup.ui.activity.main.page.MainUiState
import com.xayah.databackup.ui.activity.main.page.MainViewModel
import com.xayah.databackup.ui.activity.main.page.backup.PageBackup
import com.xayah.databackup.ui.activity.main.page.log.LogViewModel
import com.xayah.databackup.ui.activity.main.page.log.PageLog
import com.xayah.databackup.ui.activity.main.page.reload.PageReload
import com.xayah.databackup.ui.activity.main.page.reload.ReloadViewModel
import com.xayah.databackup.ui.activity.main.page.restore.PageRestore
import com.xayah.databackup.ui.activity.main.page.settings.PageSettings
import com.xayah.databackup.ui.activity.main.page.tree.PageTree
import com.xayah.databackup.ui.activity.main.page.tree.TreeViewModel

fun NavHostController.navigateAndPopBackStack(route: String) {
    navigate(route) { popBackStack() }
}

fun NavHostController.navigateAndPopAllStack(route: String) {
    navigate(route) {
        repeat(currentBackStack.value.size - 1) {
            popBackStack()
        }
    }
}

@Composable
fun NavHostController.currentRoute(): String? {
    val navBackStackEntry by currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun MainNavHost(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(
        navController = navController,
        startDestination = MainRoutes.Backup.route,
    ) {
        composable(MainRoutes.Backup.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Main(scrollBehavior))
            }
            PageBackup()
        }
        composable(MainRoutes.Restore.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Main(scrollBehavior))
            }
            PageRestore()
        }
        composable(MainRoutes.Cloud.route) {
            ComposablePageCloud(viewModel = viewModel)
        }
        composable(MainRoutes.Settings.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Main(scrollBehavior))
            }
            PageSettings()
        }
        composable(MainRoutes.Tree.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val treeViewModel = hiltViewModel<TreeViewModel>()

            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Tree(scrollBehavior = scrollBehavior, viewModel = treeViewModel))
            }
            PageTree(viewModel = treeViewModel)
        }
        composable(MainRoutes.Log.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val logViewModel = hiltViewModel<LogViewModel>()

            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Log(scrollBehavior = scrollBehavior, viewModel = logViewModel))
            }
            PageLog(viewModel = logViewModel)
        }
        composable(MainRoutes.Reload.route) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            val reloadViewModel = hiltViewModel<ReloadViewModel>()

            LaunchedEffect(null) {
                viewModel.toUiState(MainUiState.Reload(scrollBehavior = scrollBehavior, viewModel = reloadViewModel))
            }
            PageReload(viewModel = reloadViewModel)
        }
    }
}
