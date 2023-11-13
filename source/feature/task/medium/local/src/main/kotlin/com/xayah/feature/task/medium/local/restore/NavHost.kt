package com.xayah.feature.task.medium.local.restore

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
import com.xayah.feature.task.medium.local.restore.list.PageList
import com.xayah.feature.task.medium.local.restore.processing.PageProcessing

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun TaskMediumRestoreGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = TaskPackagesRestoreRoutes.List.route,
        enterTransition = { scaleIn() + fadeIn() },
        popEnterTransition = { scaleIn() + fadeIn() },
        exitTransition = { scaleOut() + fadeOut() },
        popExitTransition = { scaleOut() + fadeOut() },
    ) {
        composable(TaskPackagesRestoreRoutes.List.route) {
            PageList(navController = navController)
        }

        composable(TaskPackagesRestoreRoutes.Processing.route) {
            PageProcessing(navController = navController)
        }
    }
}
