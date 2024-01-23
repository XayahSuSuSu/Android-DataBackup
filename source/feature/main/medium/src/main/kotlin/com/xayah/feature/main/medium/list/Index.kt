package com.xayah.feature.main.medium.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.AddIconButton
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.pullrefresh.PullRefreshIndicator
import com.xayah.core.ui.material3.pullrefresh.pullRefresh
import com.xayah.core.ui.material3.pullrefresh.rememberPullRefreshState
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.medium.MediaCard
import com.xayah.feature.main.medium.R

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageMedium() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val navController = LocalNavController.current!!
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarState by viewModel.topBarState.collectAsStateWithLifecycle()
    val mediumState by viewModel.mediumState.collectAsStateWithLifecycle()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.emitIntent(IndexUiIntent.OnRefresh) })
    val enabled = topBarState.progress == 1f
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                topBarState = topBarState,
                actions = {
                    AddIconButton {
                        viewModel.emitIntent(IndexUiIntent.AddMedia(context = context))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                        SearchBar(
                            modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                            enabled = enabled,
                            placeholder = StringResourceToken.fromStringId(R.string.search_bar_hint_medium),
                            onTextChange = {
                                viewModel.emitIntent(IndexUiIntent.FilterByKey(key = it))
                            }
                        )
                    }

                    items(items = mediumState, key = { it.path }) { item ->
                        AnimatedContent(
                            modifier = Modifier
                                .animateItemPlacement()
                                .paddingHorizontal(PaddingTokens.Level4),
                            targetState = item,
                            label = AnimationTokens.AnimatedContentLabel
                        ) { targetState ->
                            Row {
                                val name = targetState.name
                                val path = targetState.path
                                val displayStatsFormat = targetState.mediaInfo.displayBytes.toDouble().formatSize()
                                MediaCard(
                                    name = name,
                                    path = path,
                                    cardSelected = false,
                                    onCardClick = {
                                        viewModel.emitIntent(IndexUiIntent.ToPageMediaDetail(navController, targetState))
                                    },
                                    onCardLongClick = {},
                                ) {
                                    RoundChip(text = displayStatsFormat) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                        viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.data_size)}: $displayStatsFormat"))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level4))
                    }
                }

                PullRefreshIndicator(refreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
