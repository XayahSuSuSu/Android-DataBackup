package com.xayah.databackup.ui.activity.operation.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.xayah.databackup.ui.activity.operation.page.media.backup.PageMediaBackupList
import com.xayah.databackup.ui.activity.operation.page.media.restore.PageMediaRestoreList
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupCompletion
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupList
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupManifest
import com.xayah.databackup.ui.activity.operation.page.packages.backup.PackageBackupProcessing
import com.xayah.databackup.ui.activity.operation.page.packages.restore.PackageRestoreCompletion
import com.xayah.databackup.ui.activity.operation.page.packages.restore.PackageRestoreList
import com.xayah.databackup.ui.activity.operation.page.packages.restore.PackageRestoreManifest
import com.xayah.databackup.ui.activity.operation.page.packages.restore.PackageRestoreProcessing
import com.xayah.databackup.ui.component.LocalSlotScope


@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun OperationNavHost(startDestination: String) {
    val navController = LocalSlotScope.current!!.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        packageBackupGraph()
        packageRestoreGraph()

        composable(OperationRoutes.MediaBackup.route) {
            PageMediaBackupList()
        }

        composable(OperationRoutes.MediaRestore.route) {
            PageMediaRestoreList()
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
fun NavGraphBuilder.packageBackupGraph() {
    navigation(
        startDestination = OperationRoutes.PackageBackupList.route,
        route = OperationRoutes.PackageBackup.route
    ) {
        composable(OperationRoutes.PackageBackupList.route) {
            PackageBackupList()
        }
        composable(OperationRoutes.PackageBackupManifest.route) {
            PackageBackupManifest()
        }
        composable(OperationRoutes.PackageBackupProcessing.route) {
            PackageBackupProcessing()
        }
        composable(OperationRoutes.PackageBackupCompletion.route) {
            PackageBackupCompletion()
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
fun NavGraphBuilder.packageRestoreGraph() {
    navigation(
        startDestination = OperationRoutes.PackageRestoreList.route,
        route = OperationRoutes.PackageRestore.route
    ) {
        composable(OperationRoutes.PackageRestoreList.route) {
            PackageRestoreList()
        }
        composable(OperationRoutes.PackageRestoreManifest.route) {
            PackageRestoreManifest()
        }
        composable(OperationRoutes.PackageRestoreProcessing.route) {
            PackageRestoreProcessing()
        }
        composable(OperationRoutes.PackageRestoreCompletion.route) {
            PackageRestoreCompletion()
        }
    }
}
