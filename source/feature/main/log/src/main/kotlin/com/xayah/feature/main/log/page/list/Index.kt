package com.xayah.feature.main.log.page.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
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
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.getActionMenuConfirmItem
import com.xayah.core.ui.model.getActionMenuReturnItem
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.core.util.LogUtil
import com.xayah.feature.main.log.LogCard
import com.xayah.feature.main.log.LogCardShimmer
import com.xayah.feature.main.log.LogRoutes
import com.xayah.feature.main.log.R

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageList(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val logCardItems by viewModel.logCardItems.collectAsState()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.log),
                actions = {
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Rounded.Adb)) {
                        navController.navigate(LogRoutes.Logcat.route)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFab(
                expanded = true,
                icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                text = StringResourceToken.fromStringId(R.string.clear_all),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.DeleteAll)
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                    targetState = uiState.updating,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)) {
                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                        }

                        if (targetState) {
                            items(count = 9) { _ ->
                                Row(Modifier.animateItemPlacement()) {
                                    LogCardShimmer()
                                }
                            }
                        } else {
                            items(items = logCardItems, key = { it.path }) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    LogCard(item = item,
                                        selected = item.name == LogUtil.getLogFileName(),
                                        actions = listOf(
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.delete),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                                                enabled = item.name != LogUtil.getLogFileName(),
                                                secondaryMenu = listOf(
                                                    getActionMenuReturnItem(),
                                                    getActionMenuConfirmItem {
                                                        viewModel.emitIntent(IndexUiIntent.Delete(path = item.path))
                                                    }
                                                ),
                                                onClick = {}
                                            ),
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.share),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Share),
                                                enabled = true,
                                                secondaryMenu = listOf(),
                                                onClick = {
                                                    viewModel.emitIntent(IndexUiIntent.ShareLog(name = item.name))
                                                }
                                            )
                                        ),
                                        onCardClick = {
                                            navController.navigate(LogRoutes.Detail.getRoute(item.name))
                                        }
                                    ) {
                                        if (item.name == LogUtil.getLogFileName())
                                            RoundChip(text = StringResourceToken.fromStringId(R.string.in_use).value, enabled = true)
                                    }
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
