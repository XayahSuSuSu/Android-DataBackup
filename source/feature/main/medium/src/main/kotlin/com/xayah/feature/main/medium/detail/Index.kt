package com.xayah.feature.main.medium.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.CompressionType
import com.xayah.core.model.util.of
import com.xayah.core.ui.component.IconTextButton
import com.xayah.core.ui.component.InfoItem
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleSmallText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.pullrefresh.PullRefreshIndicator
import com.xayah.core.ui.material3.pullrefresh.pullRefresh
import com.xayah.core.ui.material3.pullrefresh.rememberPullRefreshState
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.getActionMenuConfirmItem
import com.xayah.core.ui.model.getActionMenuReturnItem
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.medium.OpItem
import com.xayah.feature.main.medium.R

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediaDetail() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val navController = LocalNavController.current!!
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activatedState by viewModel.activatedState.collectAsStateWithLifecycle()
    val itemPairState by viewModel.itemPairState.collectAsStateWithLifecycle()
    val backupItemState = itemPairState?.backupEntity
    val restoreItemsState = itemPairState?.entities ?: listOf()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.emitIntent(IndexUiIntent.OnRefresh) })

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.OnRefresh)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(R.string.details)
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
                        .pullRefresh(pullRefreshState)
                        .paddingHorizontal(PaddingTokens.Level4),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)) {
                            Column {
                                TitleLargeText(
                                    text = uiState.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorSchemeKeyTokens.Primary.toColor()
                                )
                                TitleSmallText(
                                    text = backupItemState?.path ?: "",
                                    fontWeight = FontWeight.Bold,
                                    color = ColorSchemeKeyTokens.Secondary.toColor()
                                )
                            }
                        }
                    }

                    if (backupItemState != null) {
                        item {
                            OpItem(
                                title = StringResourceToken.fromStringId(R.string.backup),
                                btnText = StringResourceToken.fromStringId(R.string.back_up_to_local),
                                btnIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                                isRefreshing = isRefreshing,
                                activatedState = activatedState,
                                itemState = backupItemState,
                                onBtnClick = {
                                    viewModel.emitIntent(IndexUiIntent.BackupToLocal(backupItemState, navController))
                                },
                                infoContent = {
                                    val ct = backupItemState.indexInfo.compressionType
                                    val ctList = remember { listOf(CompressionType.TAR, CompressionType.ZSTD, CompressionType.LZ4).map { it.type } }
                                    val ctIndex = ctList.indexOf(ct.type)
                                    InfoItem(
                                        title = StringResourceToken.fromStringId(R.string.compression_type),
                                        content = StringResourceToken.fromString(ct.type),
                                        selectedIndex = ctIndex,
                                        list = ctList,
                                    ) { _, selected ->
                                        viewModel.emitIntent(
                                            IndexUiIntent.UpdateMedia(
                                                backupItemState.copy(
                                                    indexInfo = backupItemState.indexInfo.copy(
                                                        compressionType = CompressionType.of(selected)
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }

                    for (restoreItemState in restoreItemsState) {
                        item {
                            OpItem(
                                title = StringResourceToken.fromStringId(R.string.restore),
                                btnText = StringResourceToken.fromStringId(R.string.restore_from_local),
                                btnIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_history),
                                isRefreshing = isRefreshing,
                                activatedState = activatedState,
                                itemState = restoreItemState,
                                onBtnClick = {
                                    viewModel.emitIntent(IndexUiIntent.RestoreFromLocal(restoreItemState, navController))
                                },
                                infoContent = {
                                    val ct = restoreItemState.indexInfo.compressionType
                                    InfoItem(
                                        title = StringResourceToken.fromStringId(R.string.compression_type),
                                        content = StringResourceToken.fromString(ct.type)
                                    )
                                },
                                btnContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconTextButton(
                                            modifier = Modifier.weight(1f),
                                            text = if (activatedState)
                                                StringResourceToken.fromStringId(R.string.task_is_in_progress)
                                            else
                                                StringResourceToken.fromStringId(R.string.preserve),
                                            leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_shield_locked),
                                            enabled = isRefreshing.not() && activatedState.not() && restoreItemState.preserveId == 0L,
                                            onClick = {
                                                viewModel.emitIntent(IndexUiIntent.Preserve(restoreItemState))
                                            }
                                        )

                                        var expanded by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .wrapContentSize(Alignment.Center)
                                        ) {
                                            IconTextButton(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = if (activatedState)
                                                    StringResourceToken.fromStringId(R.string.task_is_in_progress)
                                                else
                                                    StringResourceToken.fromStringId(R.string.delete),
                                                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                                                enabled = isRefreshing.not() && activatedState.not(),
                                                onClick = {
                                                    expanded = true
                                                }
                                            )

                                            ModalActionDropdownMenu(expanded = expanded, actionList = listOf(
                                                getActionMenuReturnItem { expanded = false },
                                                getActionMenuConfirmItem {
                                                    viewModel.emitIntent(IndexUiIntent.Delete(restoreItemState))
                                                    expanded = false
                                                }
                                            ), onDismissRequest = { expanded = false })
                                        }
                                    }
                                }
                            )
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
