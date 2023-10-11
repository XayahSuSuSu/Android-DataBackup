package com.xayah.databackup.ui.activity.main.page.cloud.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xayah.databackup.ui.activity.main.page.cloud.PageAccount
import com.xayah.databackup.ui.activity.main.page.cloud.PageCreateAccount
import com.xayah.databackup.ui.activity.main.page.cloud.PageMain
import com.xayah.databackup.ui.activity.main.page.cloud.PageMount

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
        composable(CloudRoutes.CreateAccount.route) {
            PageCreateAccount(navController)
        }
        composable(CloudRoutes.Mount.route) {
            PageMount(navController)
        }
    }
}
