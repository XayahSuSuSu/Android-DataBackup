package com.xayah.databackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.command.BaseUtil
import com.xayah.databackup.index.MainIndexGraph
import com.xayah.feature.main.cloud.redesigned.PageCloud
import com.xayah.feature.main.cloud.redesigned.add.PageCloudAddAccount
import com.xayah.feature.main.cloud.redesigned.add.PageFTPSetup
import com.xayah.feature.main.cloud.redesigned.add.PageSMBSetup
import com.xayah.feature.main.cloud.redesigned.add.PageWebDAVSetup
import com.xayah.feature.main.dashboard.PageDashboard
import com.xayah.feature.main.directory.PageDirectory
import com.xayah.feature.main.log.LogGraph
import com.xayah.feature.main.medium.detail.PageMediaDetail
import com.xayah.feature.main.medium.list.PageMedium
import com.xayah.feature.main.packages.detail.PagePackageDetail
import com.xayah.feature.main.packages.list.PagePackages
import com.xayah.feature.main.packages.redesigned.backup.detail.PagePackagesBackupDetail
import com.xayah.feature.main.packages.redesigned.backup.list.PagePackagesBackupList
import com.xayah.feature.main.packages.redesigned.backup.processing.PackagesBackupProcessingGraph
import com.xayah.feature.main.packages.redesigned.restore.detail.PagePackagesRestoreDetail
import com.xayah.feature.main.packages.redesigned.restore.list.PagePackagesRestoreList
import com.xayah.feature.main.packages.redesigned.restore.processing.PackagesRestoreProcessingGraph
import com.xayah.feature.main.restore.PageRestore
import com.xayah.feature.main.settings.redesigned.PageSettings
import com.xayah.feature.main.settings.redesigned.backup.PageBackupSettings
import com.xayah.feature.main.settings.redesigned.restore.PageRestoreSettings
import com.xayah.feature.main.task.detail.medium.PageTaskMediaDetail
import com.xayah.feature.main.task.detail.packages.PageTaskPackageDetail
import com.xayah.feature.main.task.list.PageTaskList
import com.xayah.feature.main.tree.PageTree
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalLayoutApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        runCatching {
            BaseUtil.initializeEnvironment(context = this)
        }

        setContent {
            DataBackupTheme {
                val navController = rememberNavController()
                CompositionLocalProvider(LocalNavController provides navController) {
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = MainRoutes.Dashboard.route,
                    ) {
                        composable(MainRoutes.Index.route) {
                            MainIndexGraph()
                        }
                        composable(MainRoutes.Dashboard.route) {
                            PageDashboard()
                        }
                        composable(MainRoutes.Cloud.route) {
                            PageCloud()
                        }
                        composable(MainRoutes.CloudAddAccount.route) {
                            PageCloudAddAccount()
                        }
                        composable(MainRoutes.FTPSetup.route) {
                            PageFTPSetup()
                        }
                        composable(MainRoutes.WebDAVSetup.route) {
                            PageWebDAVSetup()
                        }
                        composable(MainRoutes.SMBSetup.route) {
                            PageSMBSetup()
                        }
                        composable(MainRoutes.PackagesBackupList.route) {
                            PagePackagesBackupList()
                        }
                        composable(MainRoutes.PackagesBackupDetail.route) {
                            PagePackagesBackupDetail()
                        }
                        composable(MainRoutes.PackagesBackupProcessingGraph.route) {
                            PackagesBackupProcessingGraph()
                        }
                        composable(MainRoutes.PackagesRestoreList.route) {
                            PagePackagesRestoreList()
                        }
                        composable(MainRoutes.PackagesRestoreDetail.route) {
                            PagePackagesRestoreDetail()
                        }
                        composable(MainRoutes.PackagesRestoreProcessingGraph.route) {
                            PackagesRestoreProcessingGraph()
                        }
                        composable(MainRoutes.Settings.route) {
                            PageSettings()
                        }
                        composable(MainRoutes.Restore.route) {
                            PageRestore()
                        }
                        composable(MainRoutes.BackupSettings.route) {
                            PageBackupSettings()
                        }
                        composable(MainRoutes.RestoreSettings.route) {
                            PageRestoreSettings()
                        }
                        composable(MainRoutes.Packages.route) {
                            PagePackages()
                        }
                        composable(MainRoutes.PackageDetail.route) {
                            PagePackageDetail()
                        }
                        composable(MainRoutes.Medium.route) {
                            PageMedium()
                        }
                        composable(MainRoutes.MediumDetail.route) {
                            PageMediaDetail()
                        }
                        composable(MainRoutes.TaskList.route) {
                            PageTaskList()
                        }
                        composable(MainRoutes.TaskPackageDetail.route) {
                            PageTaskPackageDetail()
                        }
                        composable(MainRoutes.TaskMediaDetail.route) {
                            PageTaskMediaDetail()
                        }
                        composable(route = MainRoutes.Log.route) {
                            LogGraph()
                        }
                        composable(route = MainRoutes.Tree.route) {
                            PageTree()
                        }
                        composable(route = MainRoutes.Directory.route) {
                            PageDirectory()
                        }
                    }
                }
            }
        }
    }
}
