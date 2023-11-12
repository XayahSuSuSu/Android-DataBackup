package com.xayah.databackup.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.model.OpType
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.LocalNavController
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.feature.directory.PageDirectory
import com.xayah.feature.home.HomeGraph
import com.xayah.feature.task.packages.local.backup.TaskPackagesBackupGraph
import com.xayah.feature.task.packages.local.restore.TaskPackagesRestoreGraph
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

        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()
                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    val navController = rememberNavController()
                    CompositionLocalProvider(LocalNavController provides navController) {
                        NavHost(
                            navController = navController,
                            startDestination = MainRoutes.Home.route,
                        ) {
                            composable(MainRoutes.Home.route) {
                                HomeGraph()
                            }
                            composable(route = MainRoutes.Directory.route) {
                                PageDirectory()
                            }
                            composable(route = MainRoutes.TaskPackages.route) { backStackEntry ->
                                when (OpType.of(backStackEntry.arguments?.getString(MainRoutes.ArgOpType))) {
                                    OpType.BACKUP -> TaskPackagesBackupGraph()
                                    OpType.RESTORE -> TaskPackagesRestoreGraph()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
