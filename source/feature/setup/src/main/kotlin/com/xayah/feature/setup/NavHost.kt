package com.xayah.feature.setup

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.util.LocalNavController
import com.xayah.feature.setup.page.one.PageOne
import com.xayah.feature.setup.page.two.PageTwo

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SetupGraph() {
    val navController = LocalNavController.current!!
    AnimatedNavHost(
        navController = navController,
        startDestination = SetupRoutes.One.route,
    ) {
        composable(SetupRoutes.One.route) {
            PageOne()
        }
        composable(SetupRoutes.Two.route) {
            PageTwo()
        }
    }
}

