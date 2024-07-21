package com.xayah.feature.main.medium.backup.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.ChipRow
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MediaItem
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.medium.ListScaffold
import com.xayah.feature.main.medium.R
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediumBackupList() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediumState by viewModel.mediumState.collectAsStateWithLifecycle()
    val mediumSelectedState by viewModel.mediumSelectedState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(canScroll = { false })
    val scrollState = rememberLazyListState()
    val srcMediumEmptyState by viewModel.srcMediumEmptyState.collectAsStateWithLifecycle()
    var fabHeight: Float by remember { mutableFloatStateOf(0F) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.OnRefresh)
    }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.select_files),
        subtitle = if (mediumSelectedState != 0) "(${mediumSelectedState}/${mediumState.size})" else null,
        actions = {
            if (srcMediumEmptyState.not()) {
                AnimatedVisibility(visible = mediumSelectedState != 0) {
                    IconButton(icon = Icons.Outlined.Block) {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = context.getString(R.string.prompt), text = context.getString(R.string.confirm_add_to_blacklist))) {
                                viewModel.emitIntentOnIO(IndexUiIntent.BlockSelected)
                            }
                        }
                    }
                }
                IconButton(icon = if (uiState.filterMode) Icons.Filled.FilterAlt else Icons.Outlined.FilterAlt) {
                    viewModel.emitStateOnMain(uiState.copy(filterMode = uiState.filterMode.not()))
                    viewModel.emitIntentOnIO(IndexUiIntent.ClearKey)
                }
                IconButton(icon = Icons.Outlined.Checklist) {
                    viewModel.emitIntentOnIO(IndexUiIntent.SelectAll(uiState.selectAll.not()))
                    viewModel.emitStateOnMain(uiState.copy(selectAll = uiState.selectAll.not()))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = mediumSelectedState != 0, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.onSizeChanged { fabHeight = it.height * 1.5f },
                    onClick = {
                        navController.navigateSingle(MainRoutes.MediumBackupProcessingGraph.route)
                    },
                ) {
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    ) {
        Column {
            val sortIndexState by viewModel.sortIndexState.collectAsStateWithLifecycle()
            val sortTypeState by viewModel.sortTypeState.collectAsStateWithLifecycle()

            AnimatedVisibility(visible = uiState.filterMode, enter = fadeIn() + slideInVertically(), exit = slideOutVertically() + fadeOut()) {
                Column {
                    SearchBar(
                        modifier = Modifier
                            .paddingHorizontal(SizeTokens.Level16)
                            .paddingVertical(SizeTokens.Level8),
                        enabled = true,
                        placeholder = stringResource(id = R.string.search_bar_hint_medium),
                        onTextChange = {
                            viewModel.emitIntentOnIO(IndexUiIntent.FilterByKey(key = it))
                        }
                    )

                    ChipRow(horizontalSpace = SizeTokens.Level16) {
                        SortChip(
                            enabled = true,
                            dismissOnSelected = true,
                            leadingIcon = Icons.Rounded.Sort,
                            selectedIndex = sortIndexState,
                            type = sortTypeState,
                            list = stringArrayResource(id = R.array.backup_sort_type_items_files).toList(),
                            onSelected = { index, _ ->
                                scope.launch {
                                    scrollState.scrollToItem(0)
                                    viewModel.emitIntentOnIO(IndexUiIntent.Sort(index = index, type = sortTypeState))
                                }
                            },
                            onClick = {}
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingTop(SizeTokens.Level8)
                    )
                }
            }

            Box {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
                    item {
                        Clickable(
                            icon = Icons.Rounded.Add,
                            title = stringResource(id = R.string.add),
                            value = stringResource(id = R.string.choose_a_custom_path)
                        ) {
                            viewModel.emitIntentOnIO(IndexUiIntent.Add(context = context))
                        }
                    }

                    items(items = mediumState, key = { "${uiState.uuid}-${it.id}" }) { item ->
                        Row(modifier = Modifier.animateItemPlacement()) {
                            MediaItem(
                                item = item,
                                onCheckedChange = { viewModel.emitIntentOnIO(IndexUiIntent.Select(item)) },
                                filterMode = uiState.filterMode,
                                onClick = {
                                    if (uiState.filterMode) viewModel.emitIntentOnIO(IndexUiIntent.ToPageDetail(navController, item))
                                    else viewModel.emitIntentOnIO(IndexUiIntent.Select(item))
                                }
                            )
                        }
                    }

                    if (mediumSelectedState != 0)
                        item {
                            with(LocalDensity.current) {
                                Column {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(fabHeight.toDp())
                                    )
                                }
                            }
                        }

                    item {
                        InnerBottomSpacer(innerPadding = it)
                    }
                }
            }
        }

        // TODO Issues of ScrollBar
        // ScrollBar(modifier = Modifier.align(Alignment.TopEnd), state = scrollState)
    }
}
