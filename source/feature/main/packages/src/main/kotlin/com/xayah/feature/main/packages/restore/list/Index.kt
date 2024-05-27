package com.xayah.feature.main.packages.restore.list

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.ChipRow
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MultipleSelectionFilterChip
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.packages.DotLottieView
import com.xayah.feature.main.packages.ListScaffold
import com.xayah.feature.main.packages.PackageItem
import com.xayah.feature.main.packages.R

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesRestoreList() {
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesSelectedState by viewModel.packagesSelectedState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberLazyListState()
    val srcPackagesEmptyState by viewModel.srcPackagesEmptyState.collectAsStateWithLifecycle()
    var fabHeight: Float by remember { mutableFloatStateOf(0F) }

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.OnRefresh)
    }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringArgs(
            StringResourceToken.fromStringId(R.string.backed_up_apps),
            StringResourceToken.fromString(if (packagesSelectedState != 0) " (${packagesSelectedState}/${packagesState.size})" else ""),
        ),
        actions = {
            if (srcPackagesEmptyState.not()) {
                AnimatedVisibility(visible = packagesSelectedState != 0) {
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.Delete)) {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.prompt), text = StringResourceToken.fromStringId(R.string.confirm_delete))) {
                                viewModel.emitIntent(IndexUiIntent.DeleteSelected)
                            }
                        }
                    }
                }
                IconButton(icon = ImageVectorToken.fromVector(if (uiState.filterMode) Icons.Filled.FilterAlt else Icons.Outlined.FilterAlt)) {
                    viewModel.emitStateOnMain(uiState.copy(filterMode = uiState.filterMode.not()))
                    viewModel.emitIntentOnIO(IndexUiIntent.ClearKey)
                }
                IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.Checklist)) {
                    viewModel.emitIntentOnIO(IndexUiIntent.SelectAll(uiState.selectAll.not()))
                    viewModel.emitStateOnMain(uiState.copy(selectAll = uiState.selectAll.not()))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = packagesSelectedState != 0, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.onSizeChanged { fabHeight = it.height * 1.5f },
                    onClick = {
                        viewModel.emitIntentOnIO(IndexUiIntent.ToPageSetup(navController))
                    },
                ) {
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    ) {
        if (srcPackagesEmptyState) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), horizontalAlignment = Alignment.CenterHorizontally) {
                        DotLottieView()
                    }
                }
                InnerBottomSpacer(innerPadding = it)
            }
        } else {
            Column {
                val flagIndexState by viewModel.flagIndexState.collectAsStateWithLifecycle()
                val userIdIndexListState by viewModel.userIdIndexListState.collectAsStateWithLifecycle()
                val sortIndexState by viewModel.sortIndexState.collectAsStateWithLifecycle()
                val sortTypeState by viewModel.sortTypeState.collectAsStateWithLifecycle()

                AnimatedVisibility(visible = uiState.filterMode, enter = fadeIn() + slideInVertically(), exit = slideOutVertically() + fadeOut()) {
                    Column {
                        SearchBar(
                            modifier = Modifier
                                .paddingHorizontal(SizeTokens.Level16)
                                .paddingVertical(SizeTokens.Level8),
                            enabled = true,
                            placeholder = StringResourceToken.fromStringId(R.string.search_bar_hint_packages),
                            onTextChange = {
                                viewModel.emitIntentOnIO(IndexUiIntent.FilterByKey(key = it))
                            }
                        )

                        ChipRow(horizontalSpace = SizeTokens.Level16) {
                            SortChip(
                                enabled = true,
                                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Sort),
                                selectedIndex = sortIndexState,
                                type = sortTypeState,
                                list = stringArrayResource(id = R.array.backup_sort_type_items).toList(),
                                onSelected = { index, _ ->
                                    viewModel.emitIntentOnIO(IndexUiIntent.Sort(index = index, type = sortTypeState))
                                },
                                onClick = {}
                            )

                            MultipleSelectionFilterChip(
                                enabled = true,
                                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                                label = StringResourceToken.fromStringId(R.string.user),
                                selectedIndexList = userIdIndexListState,
                                list = uiState.userIdList.map { it.toString() },
                                onSelected = { indexList ->
                                    if (indexList.isNotEmpty()) {
                                        viewModel.emitIntentOnIO(IndexUiIntent.SetUserIdIndexList(indexList))
                                    }
                                },
                                onClick = {
                                    viewModel.emitIntentOnIO(IndexUiIntent.GetUserIds)
                                }
                            )

                            FilterChip(
                                enabled = true,
                                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                                selectedIndex = flagIndexState,
                                list = stringArrayResource(id = R.array.flag_type_items).toList(),
                                onSelected = { index, _ ->
                                    viewModel.emitIntentOnIO(IndexUiIntent.FilterByFlag(index = index))
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

                LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
                    items(items = packagesState, key = { "${uiState.uuid}-${it.id}" }) { item ->
                        Row(modifier = Modifier.animateItemPlacement()) {
                            PackageItem(
                                item = item,
                                onCheckedChange = { viewModel.emitIntentOnIO(IndexUiIntent.Select(item)) },
                                onClick = {
                                    if (uiState.filterMode) viewModel.emitIntentOnIO(IndexUiIntent.ToPageDetail(navController, item))
                                    else viewModel.emitIntentOnIO(IndexUiIntent.Select(item))
                                },
                                filterMode = uiState.filterMode
                            )
                        }
                    }

                    if (packagesSelectedState != 0)
                        item {
                            with(LocalDensity.current) {
                                Column {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(fabHeight.toDp())
                                    )
                                    InnerBottomSpacer(innerPadding = it)
                                }
                            }
                        }
                }
            }
        }
    }
}
