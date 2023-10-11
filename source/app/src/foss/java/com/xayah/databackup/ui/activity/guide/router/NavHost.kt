package com.xayah.databackup.ui.activity.guide.router

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.activity.guide.page.Intro
import com.xayah.databackup.ui.activity.guide.page.env.PageEnv
import com.xayah.databackup.ui.activity.guide.page.intro.PageIntro

@ExperimentalMaterial3Api
@Composable
fun GuideNavHost(navController: NavHostController, viewModel: GuideViewModel) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = GuideRoutes.Intro.route,
    ) {
        composable(GuideRoutes.Intro.route) {
            LaunchedEffect(null) {
                viewModel.toUiState(Intro(context.getString(R.string.welcome_to_use)))
            }
            PageIntro()
        }

        composable(GuideRoutes.Env.route) {
            PageEnv(viewModel)
        }
    }
}
