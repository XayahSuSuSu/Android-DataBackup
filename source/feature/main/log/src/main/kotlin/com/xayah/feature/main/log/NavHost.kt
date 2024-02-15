package com.xayah.feature.main.log

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.feature.main.log.page.detail.PageDetail
import com.xayah.feature.main.log.page.list.PageList
import com.xayah.feature.main.log.page.logcat.PageLogcat

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun LogGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LogRoutes.List.route,
        enterTransition = { scaleIn() + fadeIn() },
        popEnterTransition = { scaleIn() + fadeIn() },
        exitTransition = { scaleOut() + fadeOut() },
        popExitTransition = { scaleOut() + fadeOut() },
    ) {
        composable(LogRoutes.List.route) {
            PageList(navController = navController)
        }
        composable(LogRoutes.Detail.route) {
            PageDetail(navController = navController)
        }
        composable(LogRoutes.Logcat.route) {
            PageLogcat(navController = navController)
        }
    }
}
