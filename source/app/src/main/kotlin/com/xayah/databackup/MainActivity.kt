package com.xayah.databackup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.command.BaseUtil
import com.xayah.databackup.index.MainIndexGraph
import com.xayah.feature.main.directory.PageDirectory
import com.xayah.feature.main.log.LogGraph
import com.xayah.feature.main.medium.detail.PageMediaDetail
import com.xayah.feature.main.medium.list.PageMedium
import com.xayah.feature.main.packages.detail.PagePackageDetail
import com.xayah.feature.main.packages.list.PagePackages
import com.xayah.feature.main.packages.redesigned.backup.detail.PagePackagesBackupDetail
import com.xayah.feature.main.packages.redesigned.backup.list.PagePackagesBackupList
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
                    NavHost(
                        navController = navController,
                        startDestination = MainRoutes.Index.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween()) +
                                    slideIntoContainer(animationSpec = tween(), towards = AnimatedContentTransitionScope.SlideDirection.Start)
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween()) +
                                    slideOutOfContainer(animationSpec = tween(), towards = AnimatedContentTransitionScope.SlideDirection.Start)
                        },
                    ) {
                        composable(MainRoutes.Index.route) {
                            MainIndexGraph()
                        }
                        composable(MainRoutes.PackagesBackupList.route) {
                            PagePackagesBackupList()
                        }
                        composable(MainRoutes.PackagesBackupDetail.route) {
                            PagePackagesBackupDetail()
                        }
                        composable(MainRoutes.Settings.route) {
                            PageSettings()
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
