package com.xayah.feature.main.log.page.logcat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.ScrollBar
import com.xayah.core.ui.component.SecondaryMediumTopBar
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.theme.JetbrainsMonoFamily
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.log.R

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageLogcat(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val contentItems by viewModel.contentItems.collectAsState()
    val isRunning by IndexViewModel.isRunning.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryMediumTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.logcat),
                onBackClick = {
                    navController.popBackStack()
                },
                actions = {
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Rounded.Delete)) {
                        viewModel.emitIntentOnIO(IndexUiIntent.Clear)
                    }
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Rounded.Share)) {
                        viewModel.emitIntentOnIO(IndexUiIntent.Share)
                    }
                    IconButton(icon = ImageVectorToken.fromVector(if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow)) {
                        if (isRunning) viewModel.emitIntentOnIO(IndexUiIntent.Stop) else viewModel.emitIntentOnIO(IndexUiIntent.Start)
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                val scrollState = rememberLazyListState()
                Box {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                        }

                        items(count = contentItems.size, key = { it }) { index ->
                            Row(Modifier.animateItemPlacement()) {
                                OutlinedCard(modifier = Modifier.paddingHorizontal(PaddingTokens.Level2)) {
                                    Column(modifier = Modifier.padding(PaddingTokens.Level4)) {
                                        LabelLargeText(
                                            text = contentItems[index].tag,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JetbrainsMonoFamily
                                        )
                                        LabelSmallText(
                                            text = contentItems[index].msg,
                                            fontFamily = JetbrainsMonoFamily
                                        )
                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .paddingTop(PaddingTokens.Level2),
                                            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                                            verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                                            content = {
                                                RoundChip(text = contentItems[index].level)
                                                RoundChip(text = contentItems[index].date)
                                                RoundChip(text = contentItems[index].time)
                                                RoundChip(text = contentItems[index].pid)
                                                RoundChip(text = contentItems[index].tid)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                        }
                    }
                    ScrollBar(modifier = Modifier.align(Alignment.TopEnd), state = scrollState)
                }
            }
        }
    }
}
