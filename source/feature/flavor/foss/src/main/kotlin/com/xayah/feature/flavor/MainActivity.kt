package com.xayah.feature.flavor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.feature.guide.common.GuideScaffold
import com.xayah.feature.guide.common.LocalMainViewModel
import com.xayah.feature.guide.common.MainViewModel
import com.xayah.feature.flavor.foss.GuideGraph
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DataBackupTheme {
                val navController = rememberNavController()
                val viewModel = hiltViewModel<MainViewModel>(this)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                GuideScaffold(
                    isInitializing = uiState.isInitializing,
                    snackbarHostState = uiState.snackbarHostState,
                    topBarTitle = uiState.topBarTitle,
                    topBarIcon = uiState.topBarIcon,
                    fabIcon = uiState.fabIcon,
                    onFabClick = uiState.onFabClick,
                ) {
                    CompositionLocalProvider(LocalMainViewModel provides viewModel) {
                        GuideGraph(navController = navController)
                    }
                }
            }
        }
    }
}
