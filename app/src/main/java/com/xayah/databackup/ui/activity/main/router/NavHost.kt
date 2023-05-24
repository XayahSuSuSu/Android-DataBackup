package com.xayah.databackup.ui.activity.main.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.component.GuideScaffold
import com.xayah.databackup.ui.activity.main.component.MainScaffold
import com.xayah.databackup.ui.activity.main.page.guide.GuideUiState
import com.xayah.databackup.ui.activity.main.page.guide.GuideViewModel
import com.xayah.databackup.ui.activity.main.page.guide.PageEnv
import com.xayah.databackup.ui.activity.main.page.guide.PageIntro
import com.xayah.databackup.ui.activity.main.page.guide.PageUpdate

fun NavHostController.navigateAndPopBackStack(route: String) {
    navigate(route) { popBackStack() }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ScaffoldNavHost(scaffoldNavController: NavHostController) {
    NavHost(
        navController = scaffoldNavController,
        startDestination = ScaffoldRoutes.Main.route,
    ) {
        composable(ScaffoldRoutes.Guide.route) {
            val navController = rememberNavController()
            val guideViewModel = hiltViewModel<GuideViewModel>()
            GuideScaffold(
                scaffoldNavController = scaffoldNavController,
                navController = navController,
                viewModel = guideViewModel
            ) {
                GuideNavHost(navController = navController, viewModel = guideViewModel)
            }
        }

        composable(ScaffoldRoutes.Main.route) {
            val navController = rememberNavController()
            MainScaffold(scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()) {
                MainNavHost(navController = navController)
            }
        }
    }
}

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
                viewModel.toUiState(GuideUiState.Intro(context.getString(R.string.welcome_to_use)))
            }
            PageIntro()
        }

        composable(GuideRoutes.Update.route) {
            LaunchedEffect(null) {
                viewModel.toUiState(GuideUiState.Update(context.getString(R.string.update_records)))
            }
            PageUpdate()
        }

        composable(GuideRoutes.Env.route) {
            PageEnv(viewModel)
        }
    }
}

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = MainRoutes.Home.route,
    ) {
        composable(MainRoutes.Home.route) {
        }
        composable(MainRoutes.Cloud.route) {
        }
        composable(MainRoutes.Backup.route) {
        }
        composable(MainRoutes.Restore.route) {
        }
    }
}
