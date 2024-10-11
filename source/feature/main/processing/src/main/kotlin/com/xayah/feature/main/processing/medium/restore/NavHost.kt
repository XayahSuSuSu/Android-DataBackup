package com.xayah.feature.main.processing.medium.restore

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.route.MainRoutes
import com.xayah.feature.main.processing.PageProcessing
import com.xayah.feature.main.processing.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumRestoreProcessingGraph() {
    val localNavController = rememberNavController()
    val viewModel = hiltViewModel<RestoreViewModelImpl>()

    AnimatedNavHost(
        navController = localNavController,
        startDestination = MainRoutes.MediumRestoreProcessingSetup.route,
    ) {
        composable(MainRoutes.MediumRestoreProcessing.route) {
            PageProcessing(
                topBarTitleId = { state ->
                    when (state) {
                        OperationState.PROCESSING -> R.string.processing
                        OperationState.DONE -> R.string.restore_completed
                        else -> R.string.restore
                    }
                },
                finishedTitleId = R.string.restore_completed,
                finishedSubtitleId = R.string.args_files_restored,
                finishedWithErrorsSubtitleId = R.string.args_files_restored_and_failed,
                viewModel = viewModel
            )
        }
        composable(MainRoutes.MediumRestoreProcessingSetup.route) {
            PageMediumRestoreProcessingSetup(localNavController = localNavController, viewModel = viewModel)
        }
    }
}
