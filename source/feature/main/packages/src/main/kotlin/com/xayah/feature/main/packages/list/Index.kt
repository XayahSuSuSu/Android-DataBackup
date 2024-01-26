package com.xayah.feature.main.packages.list

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sort
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
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.MultipleSelectionFilterChip
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.pullrefresh.PullRefreshIndicator
import com.xayah.core.ui.material3.pullrefresh.pullRefresh
import com.xayah.core.ui.material3.pullrefresh.rememberPullRefreshState
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.packages.ChipRow
import com.xayah.feature.main.packages.PackageCard
import com.xayah.feature.main.packages.R

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackages() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val navController = LocalNavController.current!!
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarState by viewModel.topBarState.collectAsStateWithLifecycle()
    val flagIndexState by viewModel.flagIndexState.collectAsStateWithLifecycle()
    val userIdIndexListState by viewModel.userIdIndexListState.collectAsStateWithLifecycle()
    val sortIndexState by viewModel.sortIndexState.collectAsStateWithLifecycle()
    val sortTypeState by viewModel.sortTypeState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val snackbarHostState = viewModel.snackbarHostState
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.emitIntent(IndexUiIntent.OnRefresh) })
    val enabled = topBarState.progress == 1f
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { SecondaryTopBar(scrollBehavior = scrollBehavior, topBarState = topBarState) },
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
                            placeholder = StringResourceToken.fromStringId(R.string.search_bar_hint_packages),
                            onTextChange = {
                                viewModel.emitIntent(IndexUiIntent.FilterByKey(key = it))
                            }
                        )
                    }

                    item {
                        Column {
                            ChipRow {
                                SortChip(
                                    enabled = enabled,
                                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Sort),
                                    selectedIndex = sortIndexState,
                                    type = sortTypeState,
                                    list = stringArrayResource(id = R.array.backup_sort_type_items).toList(),
                                    onSelected = { index, _ ->
                                        viewModel.emitIntent(IndexUiIntent.Sort(index = index, type = sortTypeState))
                                    },
                                    onClick = {}
                                )

                                MultipleSelectionFilterChip(
                                    enabled = enabled,
                                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                                    label = StringResourceToken.fromStringId(R.string.user),
                                    selectedIndexList = userIdIndexListState,
                                    list = uiState.userIdList.map { it.toString() },
                                    onSelected = { indexList ->
                                        if (indexList.isNotEmpty()) {
                                            viewModel.emitIntent(IndexUiIntent.SetUserIdIndexList(indexList))
                                        }
                                    },
                                    onClick = {
                                        viewModel.emitIntent(IndexUiIntent.GetUserIds)
                                    }
                                )

                                FilterChip(
                                    enabled = enabled,
                                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                                    selectedIndex = flagIndexState,
                                    list = stringArrayResource(id = R.array.flag_type_items).toList(),
                                    onSelected = { index, _ ->
                                        viewModel.emitIntent(IndexUiIntent.FilterByFlag(index = index))
                                    },
                                    onClick = {}
                                )
                            }
                        }
                    }

                    items(items = packagesState, key = { "${it.packageName}: ${it.userId}" }) { item ->
                        AnimatedContent(
                            modifier = Modifier
                                .animateItemPlacement()
                                .paddingHorizontal(PaddingTokens.Level4),
                            targetState = item,
                            label = AnimationTokens.AnimatedContentLabel
                        ) { targetState ->
                            Row {
                                val userId = targetState.userId
                                val packageName = targetState.packageName
                                val versionName = targetState.packageInfo.versionName
                                val storageStatsFormat = targetState.storageStatsFormat
                                val hasKeystore = targetState.extraInfo.hasKeystore
                                val isSystemApp = targetState.isSystemApp
                                PackageCard(
                                    label = targetState.packageInfo.label,
                                    packageName = packageName,
                                    cardSelected = false,
                                    onCardClick = {
                                        viewModel.emitIntent(IndexUiIntent.ToPagePackageDetail(navController, targetState))
                                    },
                                    onCardLongClick = {},
                                ) {
                                    RoundChip(
                                        text = StringResourceToken.fromStringArgs(
                                            StringResourceToken.fromStringId(R.string.user),
                                            StringResourceToken.fromString(" $userId"),
                                        ).value,
                                        color = ColorSchemeKeyTokens.Primary.toColor(),
                                    ) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                    }
                                    if (hasKeystore) RoundChip(
                                        text = StringResourceToken.fromStringId(R.string.keystore).value,
                                        color = ColorSchemeKeyTokens.Error.toColor(),
                                    ) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                        viewModel.emitEffect(IndexUiEffect.ShowSnackbar(context.getString(R.string.keystore_desc)))
                                    }
                                    if (versionName.isNotEmpty()) RoundChip(text = versionName) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                        viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.version)}: $versionName"))
                                    }
                                    RoundChip(text = storageStatsFormat) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                        viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.data_size)}: $storageStatsFormat"))
                                    }
                                    RoundChip(
                                        text = if (isSystemApp) StringResourceToken.fromStringId(R.string.system).value
                                        else StringResourceToken.fromStringId(R.string.third_party).value
                                    ) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
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
