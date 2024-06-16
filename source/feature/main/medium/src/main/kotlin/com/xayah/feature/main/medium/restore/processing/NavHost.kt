package com.xayah.feature.main.medium.restore.processing

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

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumRestoreProcessingGraph() {
    val localNavController = rememberNavController()
    val viewModel = hiltViewModel<IndexViewModel>()

    AnimatedNavHost(
        navController = localNavController,
        startDestination = MainRoutes.MediumRestoreProcessingSetup.route,
    ) {
        composable(MainRoutes.MediumRestoreProcessing.route) {
            PageMediumRestoreProcessing(viewModel = viewModel)
        }
        composable(MainRoutes.MediumRestoreProcessingSetup.route) {
            PageMediumRestoreProcessingSetup(localNavController = localNavController, viewModel = viewModel)
        }
    }
}
