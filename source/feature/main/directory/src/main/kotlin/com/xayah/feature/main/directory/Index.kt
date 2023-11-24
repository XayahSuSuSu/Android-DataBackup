package com.xayah.feature.main.directory

import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageType
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.paddingBottom
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

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageDirectory() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val directories by uiState.directories.collectAsState(initial = listOf())

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringId(
                    when (uiState.type) {
                        OpType.BACKUP -> R.string.backup_dir
                        OpType.RESTORE -> R.string.restore_dir
                    }
                )
            )
        },
        floatingActionButton = {
            ExtendedFab(
                expanded = true,
                icon = ImageVectorToken.fromVector(Icons.Rounded.Add),
                text = StringResourceToken.fromStringId(R.string.add),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.AddDir(type = uiState.type, context = (context as ComponentActivity)))
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                    targetState = uiState.updating,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
                        }

                        if (targetState) {
                            items(count = uiState.shimmerCount) { _ ->
                                Row(Modifier.animateItemPlacement()) {
                                    DirectoryCardShimmer()
                                }
                            }
                        } else {
                            items(items = directories, key = { it.id }) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    DirectoryCard(
                                        entity = item,
                                        actions = listOf(
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.delete),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                                                enabled = item.storageType == StorageType.CUSTOM,
                                                secondaryMenu = listOf(
                                                    getActionMenuReturnItem(),
                                                    getActionMenuConfirmItem {
                                                        viewModel.emitIntent(IndexUiIntent.DeleteDir(type = uiState.type, entity = item))
                                                    }
                                                ),
                                                onClick = {}
                                            )
                                        ),
                                        onCardClick = {
                                            viewModel.emitIntent(IndexUiIntent.SelectDir(type = uiState.type, entity = item))
                                        },
                                        chipGroup = {
                                            for (tag in item.tags) {
                                                if (tag.isNotEmpty()) RoundChip(text = tag, enabled = item.enabled)
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level3))
                        }
                    }
                }
            }
        }
    }
}
