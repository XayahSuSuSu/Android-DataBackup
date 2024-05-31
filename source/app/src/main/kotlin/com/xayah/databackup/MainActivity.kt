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
import com.xayah.feature.main.cloud.PageCloud
import com.xayah.feature.main.cloud.add.PageCloudAddAccount
import com.xayah.feature.main.cloud.add.PageFTPSetup
import com.xayah.feature.main.cloud.add.PageSMBSetup
import com.xayah.feature.main.cloud.add.PageWebDAVSetup
import com.xayah.feature.main.dashboard.PageDashboard
import com.xayah.feature.main.directory.PageDirectory
import com.xayah.feature.main.packages.backup.detail.PagePackagesBackupDetail
import com.xayah.feature.main.packages.backup.list.PagePackagesBackupList
import com.xayah.feature.main.packages.backup.processing.PackagesBackupProcessingGraph
import com.xayah.feature.main.packages.restore.detail.PagePackagesRestoreDetail
import com.xayah.feature.main.packages.restore.list.PagePackagesRestoreList
import com.xayah.feature.main.packages.restore.processing.PackagesRestoreProcessingGraph
import com.xayah.feature.main.restore.PageRestore
import com.xayah.feature.main.restore.reload.PageReload
import com.xayah.feature.main.settings.PageSettings
import com.xayah.feature.main.settings.backup.PageBackupSettings
import com.xayah.feature.main.settings.restore.PageRestoreSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalLayoutApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        runBlocking {
            runCatching {
                BaseUtil.initializeEnvironment(context = this@MainActivity)
            }
        }

        setContent {
            DataBackupTheme {
                val navController = rememberNavController()
                CompositionLocalProvider(LocalNavController provides navController) {
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = MainRoutes.Dashboard.route,
                    ) {
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
                        composable(MainRoutes.Reload.route) {
                            PageReload()
                        }
                        composable(MainRoutes.BackupSettings.route) {
                            PageBackupSettings()
                        }
                        composable(MainRoutes.RestoreSettings.route) {
                            PageRestoreSettings()
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
