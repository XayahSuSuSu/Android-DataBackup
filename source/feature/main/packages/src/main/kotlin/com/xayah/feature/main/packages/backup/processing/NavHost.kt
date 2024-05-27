package com.xayah.feature.main.packages.backup.processing

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

@ExperimentalAnimationApi
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackagesBackupProcessingGraph() {
    val localNavController = rememberNavController()
    val viewModel = hiltViewModel<IndexViewModel>()

    AnimatedNavHost(
        navController = localNavController,
        startDestination = MainRoutes.PackagesBackupProcessingSetup.route,
    ) {
        composable(MainRoutes.PackagesBackupProcessing.route) {
            PagePackagesBackupProcessing(viewModel = viewModel)
        }
        composable(MainRoutes.PackagesBackupProcessingSetup.route) {
            PagePackagesBackupProcessingSetup(localNavController = localNavController, viewModel = viewModel)
        }
    }
}
