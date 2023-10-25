package com.xayah.databackup.ui.activity.main.page.cloud.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xayah.databackup.ui.activity.main.page.cloud.PageAccount
import com.xayah.databackup.ui.activity.main.page.cloud.PageAccountDetail
import com.xayah.databackup.ui.activity.main.page.cloud.PageMain
import com.xayah.databackup.ui.activity.main.page.cloud.PageMount
import com.xayah.databackup.ui.activity.main.page.reload.PageReload
import com.xayah.databackup.ui.activity.main.page.reload.ReloadViewModel

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun CloudNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = CloudRoutes.Main.route,
    ) {
        composable(CloudRoutes.Main.route) {
            PageMain(navController)
        }
        composable(CloudRoutes.Account.route) {
            PageAccount(navController)
        }
        composable(
            route = "${CloudRoutes.AccountDetail.route}?$AccountDetailArg={$AccountDetailArg}",
            arguments = listOf(navArgument(AccountDetailArg) { nullable = true })
        ) { backStackEntry ->
            val entityName = backStackEntry.arguments?.getString(AccountDetailArg)
            PageAccountDetail(navController, entityName)
        }
        composable(CloudRoutes.Mount.route) {
            PageMount(navController)
        }

        composable(
            route = "${CloudRoutes.Reload.route}?$ReloadArg={$ReloadArg}",
            arguments = listOf(navArgument(ReloadArg) {
                type = NavType.BoolType
            })
        ) {
            val reloadViewModel = hiltViewModel<ReloadViewModel>()
            PageReload(viewModel = reloadViewModel)
        }
    }
}
