package com.xayah.feature.main.packages.redesigned.restore.processing

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.route.MainRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalAnimationApi
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@Composable
fun PackagesRestoreProcessingGraph() {
    val localNavController = rememberNavController()
    val viewModel = hiltViewModel<IndexViewModel>()

    AnimatedNavHost(
        navController = localNavController,
        startDestination = MainRoutes.PackagesRestoreProcessingSetup.route,
    ) {
        composable(MainRoutes.PackagesRestoreProcessing.route) {
            PagePackagesRestoreProcessing(localNavController = localNavController, viewModel = viewModel)
        }
        composable(MainRoutes.PackagesRestoreProcessingSetup.route) {
            PagePackagesRestoreProcessingSetup(localNavController = localNavController, viewModel = viewModel)
        }
    }
}
