package com.xayah.feature.flavor.premium

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xayah.feature.flavor.premium.page.env.PageEnv
import com.xayah.feature.flavor.premium.page.intro.PageIntro
import com.xayah.feature.flavor.premium.page.update.PageUpdate

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun GuideGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = GuideRoutes.Intro.route,
        enterTransition = { scaleIn() + fadeIn() },
        popEnterTransition = { scaleIn() + fadeIn() },
        exitTransition = { scaleOut() + fadeOut() },
        popExitTransition = { scaleOut() + fadeOut() },
    ) {
        composable(GuideRoutes.Intro.route) {
            PageIntro(navController = navController)
        }
        composable(GuideRoutes.Update.route) {
            PageUpdate(navController = navController)
        }
        composable(GuideRoutes.Env.route) {
            PageEnv()
        }
    }
}
