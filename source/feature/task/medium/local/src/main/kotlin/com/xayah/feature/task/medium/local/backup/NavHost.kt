package com.xayah.feature.task.medium.local.backup

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.feature.task.medium.local.backup.list.PageList
import com.xayah.feature.task.medium.local.backup.processing.PageProcessing

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun TaskMediumBackupGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TaskMediumBackupRoutes.List.route,
        enterTransition = { scaleIn() + fadeIn() },
        popEnterTransition = { scaleIn() + fadeIn() },
        exitTransition = { scaleOut() + fadeOut() },
        popExitTransition = { scaleOut() + fadeOut() },
    ) {
        composable(TaskMediumBackupRoutes.List.route) {
            PageList(navController = navController)
        }

        composable(TaskMediumBackupRoutes.Processing.route) {
            PageProcessing(navController = navController)
        }
    }
}
