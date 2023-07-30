package com.xayah.databackup.ui.activity.operation.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.xayah.databackup.ui.activity.operation.page.packageBackup.PackageBackupList
import com.xayah.databackup.ui.component.SlotScope


@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SlotScope.OperationNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = OperationRoutes.PackageBackup.route,
    ) {
        packageBackupGraph()
    }
}

@ExperimentalMaterial3Api
fun NavGraphBuilder.packageBackupGraph() {
    navigation(
        startDestination = OperationRoutes.PackageBackupList.route,
        route = OperationRoutes.PackageBackup.route
    ) {
        composable(OperationRoutes.PackageBackupList.route) {
            PackageBackupList()
        }
    }
}
