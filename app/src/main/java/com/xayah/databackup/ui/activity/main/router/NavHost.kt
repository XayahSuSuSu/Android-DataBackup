package com.xayah.databackup.ui.activity.main.router

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.GuideScaffold
import com.xayah.databackup.ui.component.MainScaffold
import com.xayah.databackup.ui.activity.main.page.guide.GuideUiState
import com.xayah.databackup.ui.activity.main.page.guide.GuideViewModel
import com.xayah.databackup.ui.activity.main.page.guide.PageEnv
import com.xayah.databackup.ui.activity.main.page.guide.PageIntro
import com.xayah.databackup.ui.activity.main.page.guide.PageUpdate
import com.xayah.databackup.ui.activity.main.page.main.PageBackup
import com.xayah.databackup.ui.activity.main.router.MainRoutes.Backup
import com.xayah.databackup.ui.activity.main.router.MainRoutes.Cloud
import com.xayah.databackup.ui.activity.main.router.MainRoutes.Restore
import com.xayah.databackup.ui.activity.main.router.MainRoutes.Settings
import com.xayah.databackup.ui.component.SlotScope

fun NavHostController.navigateAndPopBackStack(route: String) {
    navigate(route) { popBackStack() }
}

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SlotScope.ScaffoldNavHost(scaffoldNavController: NavHostController) {
    NavHost(
        navController = scaffoldNavController,
        startDestination = ScaffoldRoutes.Main.route,
    ) {
        composable(ScaffoldRoutes.Guide.route) {
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

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Backup.route,
    ) {
        composable(Backup.route) {
            PageBackup()
        }
        composable(Restore.route) {
        }
        composable(Cloud.route) {
        }
        composable(Settings.route) {
        }
    }
}
