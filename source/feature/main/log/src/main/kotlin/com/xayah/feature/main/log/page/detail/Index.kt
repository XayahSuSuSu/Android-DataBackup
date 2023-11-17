package com.xayah.feature.main.log.page.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.theme.JetbrainsMonoFamily
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromString
import com.xayah.feature.main.log.logCardShimmer

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageDetail(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val contentItems by viewModel.contentItems.collectAsState()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Load)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromString(uiState.name),
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.loading,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                        }

                        if (targetState) {
                            items(count = 9) { _ ->
                                Row(Modifier.animateItemPlacement()) {
                                    LabelSmallText(
                                        modifier = Modifier
                                            .paddingHorizontal(PaddingTokens.Level3)
                                            .logCardShimmer(true),
                                        text = "ShimmerShimmerShimmer"
                                    )
                                }
                            }
                        } else {
                            items(count = contentItems.size, key = { it }) { index ->
                                Row(Modifier.animateItemPlacement()) {
                                    LabelSmallText(
                                        modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                        text = contentItems[index],
                                        fontFamily = JetbrainsMonoFamily
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                        }
                    }
                }
            }
        }
    }
}
