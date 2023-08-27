package com.xayah.databackup.ui.activity.operation.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupCompletion
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupList
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupManifest
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupProcessing
import com.xayah.databackup.ui.component.SlotScope


@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SlotScope.OperationNavHost() {
    NavHost(
        navController = navController,
        startDestination = OperationRoutes.PackageBackup.route,
    ) {
        packageBackupGraph(slotScope = this@OperationNavHost)
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
fun NavGraphBuilder.packageBackupGraph(slotScope: SlotScope) {
    navigation(
        startDestination = OperationRoutes.PackageBackupList.route,
        route = OperationRoutes.PackageBackup.route
    ) {
        composable(OperationRoutes.PackageBackupList.route) {
            slotScope.PackageBackupList()
        }

        composable(OperationRoutes.PackageBackupManifest.route) {
            slotScope.PackageBackupManifest()
        }

        composable(OperationRoutes.PackageBackupProcessing.route) {
            slotScope.PackageBackupProcessing()
        }
        composable(OperationRoutes.PackageBackupCompletion.route) {
            PackageBackupCompletion()
        }
    }
}
