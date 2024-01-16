package com.xayah.feature.guide.premium.page.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.guide.common.LocalMainViewModel
import com.xayah.feature.guide.common.MainUiIntent
import com.xayah.feature.guide.common.MainUiState
import com.xayah.feature.guide.premium.GuideRoutes
import com.xayah.feature.guide.premium.R
import com.xayah.feature.guide.premium.UpdateCard
import com.xayah.feature.guide.premium.UpdateCardShimmer

@ExperimentalMaterial3Api
@Composable
fun PageUpdate(navController: NavHostController) {
    val mainViewModel = LocalMainViewModel.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val releases by viewModel.releases.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(null) {
        mainViewModel.emitIntent(
            MainUiIntent.SetUiState(
                state = MainUiState(
                    isInitializing = false,
                    snackbarHostState = viewModel.snackbarHostState,
                    topBarTitle = StringResourceToken.fromStringId(R.string.update_records),
                    topBarIcon = ImageVectorToken.fromVector(Icons.Rounded.Notifications),
                    fabIcon = ImageVectorToken.fromVector(Icons.Rounded.ArrowForward),
                    onFabClick = {
                        navController.navigate(GuideRoutes.Env.route)
                    }
                )
            )
        )
    }

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    AnimatedContent(
        targetState = uiState.isInitializing,
        label = AnimationTokens.AnimatedContentLabel
    ) { targetState ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)) {
            item {
                Spacer(modifier = Modifier.height(PaddingTokens.Level2))
            }

            if (targetState) {
                items(count = 9) {
                    UpdateCardShimmer()
                }
            } else {
                items(items = releases, key = { it.url }) {
                    UpdateCard(shimmering = false, content = it.content, version = it.name, link = it.url) {
                        viewModel.emitEffect(IndexUiEffect.ShowSnackbar(message = context.getString(R.string.no_browser)))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(PaddingTokens.Level2))
            }
        }
    }
}
