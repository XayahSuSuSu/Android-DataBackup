package com.xayah.feature.main.tree

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.theme.JetbrainsMonoFamily
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageTree() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val contentItems by viewModel.contentItems.collectAsState()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Refresh)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.directory_structure),
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = uiState.refreshing,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    Column {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                        ) {
                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                            FilterChip(
                                enabled = targetState.not(),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.FilterList),
                                selectedIndex = uiState.filterIndex,
                                list = uiState.filterList,
                                onSelected = { index, _ ->
                                    viewModel.launchOnIO {
                                        viewModel.emitState(uiState.copy(filterIndex = index))
                                        viewModel.emitIntent(IndexUiIntent.Refresh)
                                    }
                                },
                                onClick = {}
                            )

                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .paddingHorizontal(PaddingTokens.Level4)
                                .paddingBottom(PaddingTokens.Level5)
                        ) {
                            LazyColumn(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                item {
                                    Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                                }

                                if (targetState) {
                                    items(count = 9) { _ ->
                                        Row(Modifier.animateItemPlacement()) {
                                            LabelSmallText(
                                                modifier = Modifier
                                                    .padding(PaddingTokens.Level4)
                                                    .treeShimmer(true),
                                                text = "ShimmerShimmerShimmer"
                                            )
                                        }
                                    }
                                } else {
                                    items(count = contentItems.size, key = { it }) { index ->
                                        Row(Modifier.animateItemPlacement()) {
                                            LabelSmallText(
                                                modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                                                text = contentItems[index],
                                                fontFamily = JetbrainsMonoFamily
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
