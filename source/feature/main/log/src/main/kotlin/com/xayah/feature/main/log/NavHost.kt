package com.xayah.feature.main.log

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.AnimatedNavHost
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
    AnimatedNavHost(
        navController = navController,
        startDestination = LogRoutes.List.route,
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
