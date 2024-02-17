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
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.model.ModeState
import com.xayah.core.model.OpType
import com.xayah.core.model.getLabel
import com.xayah.core.ui.component.CheckIconButton
import com.xayah.core.ui.component.ChipRow
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
import com.xayah.feature.main.packages.PackageCard
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.countBackups

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
    val activatedState by viewModel.activatedState.collectAsStateWithLifecycle()
    val processingCountState by viewModel.processingCountState.collectAsStateWithLifecycle()
    val locationIndexState by viewModel.locationIndexState.collectAsStateWithLifecycle()
    val modeState by viewModel.modeState.collectAsStateWithLifecycle()
    val accountsState by viewModel.accountsState.collectAsStateWithLifecycle()
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
                    if (activatedState && modeState != ModeState.OVERVIEW) {
                        CheckIconButton {
                            viewModel.emitIntent(IndexUiIntent.Process(navController = navController))
                        }
                    }
                })
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
                            ChipRow {
                                val modes by remember { mutableStateOf(listOf(ModeState.OVERVIEW, ModeState.BATCH_BACKUP, ModeState.BATCH_RESTORE)) }
                                FilterChip(
                                    enabled = enabled,
                                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.FactCheck),
                                    selectedIndex = modes.indexOf(modeState),
                                    list = modes.map { it.getLabel(context) },
                                    onSelected = { index, _ ->
                                        viewModel.emitIntent(IndexUiIntent.SetMode(index = index, mode = modes[index]))
                                    },
                                    onClick = {}
                                )
                                if (modeState != ModeState.OVERVIEW)
                                    FilterChip(
                                        enabled = enabled,
                                        leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.LocationOn),
                                        selectedIndex = locationIndexState,
                                        list = accountsState.map { "${context.getString(R.string.cloud)}: ${it.name}" }.toMutableList().also { it.add(0, context.getString(R.string.local)) },
                                        onSelected = { index, _ ->
                                            viewModel.emitIntent(IndexUiIntent.FilterByLocation(index = index))
                                        },
                                        onClick = {}
                                    )
                            }
                        }
                    }

                    items(
                        items = packagesState,
                        key = { "${it.entity.packageName}: ${it.entity.userId}-${it.entity.preserveId}-${it.entity.indexInfo.cloud}-${it.entity.indexInfo.backupDir}" }) { item ->
                        AnimatedContent(
                            modifier = Modifier
                                .animateItemPlacement()
                                .paddingHorizontal(PaddingTokens.Level4),
                            targetState = item,
                            label = AnimationTokens.AnimatedContentLabel
                        ) { targetState ->
                            Row {
                                val userId = targetState.entity.userId
                                val preserveId = targetState.entity.preserveId
                                val packageName = targetState.entity.packageName
                                val versionName = targetState.entity.packageInfo.versionName
                                val storageStatsFormat = targetState.entity.storageStatsFormat
                                val hasKeystore = targetState.entity.extraInfo.hasKeystore
                                val ssaid = targetState.entity.extraInfo.ssaid
                                val isSystemApp = targetState.entity.isSystemApp
                                val backupsCount = when (targetState.entity.indexInfo.opType) {
                                    OpType.BACKUP -> targetState.count - 1
                                    OpType.RESTORE -> targetState.count
                                }
                                val itemEnabled = isRefreshing.not() && processingCountState.not()
                                PackageCard(
                                    label = targetState.entity.packageInfo.label,
                                    packageName = packageName,
                                    enabled = itemEnabled,
                                    cardSelected = if (modeState == ModeState.OVERVIEW) false else targetState.entity.extraInfo.activated,
                                    onCardClick = {
                                        when (modeState) {
                                            ModeState.OVERVIEW -> {
                                                viewModel.emitIntent(IndexUiIntent.ToPagePackageDetail(navController, targetState.entity))
                                            }

                                            else -> {
                                                viewModel.emitIntent(IndexUiIntent.Select(targetState.entity))
                                            }
                                        }

                                    },
                                    onCardLongClick = {},
                                ) {
                                    when (modeState) {
                                        ModeState.OVERVIEW, ModeState.BATCH_BACKUP -> {
                                            if (backupsCount > 0) RoundChip(
                                                enabled = itemEnabled,
                                                text = countBackups(context = context, count = backupsCount),
                                                color = ColorSchemeKeyTokens.Primary.toColor(),
                                            ) {
                                                viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            }
                                        }

                                        ModeState.BATCH_RESTORE -> {
                                            RoundChip(
                                                enabled = itemEnabled,
                                                text = "${StringResourceToken.fromStringId(R.string.id).value}: $preserveId",
                                                color = ColorSchemeKeyTokens.Primary.toColor(),
                                            ) {
                                                viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            }
                                        }
                                    }
                                    RoundChip(
                                        enabled = itemEnabled,
                                        text = StringResourceToken.fromStringArgs(
                                            StringResourceToken.fromStringId(R.string.user),
                                            StringResourceToken.fromString(": $userId"),
                                        ).value,
                                        color = ColorSchemeKeyTokens.Primary.toColor(),
                                    ) {
                                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                    }
                                    if (item.entity.extraInfo.existed) {
                                        if (ssaid.isNotEmpty()) RoundChip(
                                            enabled = itemEnabled,
                                            text = StringResourceToken.fromStringId(R.string.ssaid).value,
                                            color = ColorSchemeKeyTokens.Secondary.toColor(),
                                        ) {
                                            viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.ssaid)}: $ssaid"))
                                        }
                                        if (hasKeystore) RoundChip(
                                            enabled = itemEnabled,
                                            text = StringResourceToken.fromStringId(R.string.keystore).value,
                                            color = ColorSchemeKeyTokens.Error.toColor(),
                                        ) {
                                            viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            viewModel.emitEffect(IndexUiEffect.ShowSnackbar(context.getString(R.string.keystore_desc)))
                                        }
                                        if (versionName.isNotEmpty()) RoundChip(enabled = itemEnabled, text = versionName) {
                                            viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.version)}: $versionName"))
                                        }
                                        RoundChip(enabled = itemEnabled, text = storageStatsFormat) {
                                            viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                                            viewModel.emitEffect(IndexUiEffect.ShowSnackbar("${context.getString(R.string.data_size)}: $storageStatsFormat"))
                                        }
                                    }
                                    RoundChip(
                                        enabled = itemEnabled,
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
