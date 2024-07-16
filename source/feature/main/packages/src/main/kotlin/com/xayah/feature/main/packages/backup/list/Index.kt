package com.xayah.feature.main.packages.backup.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readLoadSystemApps
import com.xayah.core.model.DataType
import com.xayah.core.ui.component.ChipRow
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ModalBottomSheet
import com.xayah.core.ui.component.MultipleSelectionFilterChip
import com.xayah.core.ui.component.PackageDataChip
import com.xayah.core.ui.component.PackageItem
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.pullrefresh.PullRefreshIndicator
import com.xayah.core.ui.material3.pullrefresh.pullRefresh
import com.xayah.core.ui.material3.pullrefresh.rememberPullRefreshState
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.packages.DotLottieView
import com.xayah.feature.main.packages.ListScaffold
import com.xayah.feature.main.packages.R
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesBackupList() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesSelectedState by viewModel.packagesSelectedState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(canScroll = { false })
    val scrollState = rememberLazyListState()
    val srcPackagesEmptyState by viewModel.srcPackagesEmptyState.collectAsStateWithLifecycle()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { viewModel.emitIntentOnIO(IndexUiIntent.OnRefresh) })
    var fabHeight: Float by remember { mutableFloatStateOf(0F) }
    val loadSystemApps by context.readLoadSystemApps().collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.GetUserIds)
    }

    SetOnResume {
        viewModel.emitIntentOnIO(IndexUiIntent.OnFastRefresh)
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = sheetState
        ) {
            var apkSelected by remember { mutableStateOf(true) }
            var userSelected by remember { mutableStateOf(true) }
            var userDeSelected by remember { mutableStateOf(true) }
            var dataSelected by remember { mutableStateOf(true) }
            var obbSelected by remember { mutableStateOf(true) }
            var mediaSelected by remember { mutableStateOf(true) }

            TitleLargeText(modifier = Modifier.paddingHorizontal(SizeTokens.Level24), text = StringResourceToken.fromStringId(R.string.batch_select).value)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level16),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                maxItemsInEachRow = 2
            ) {

                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = apkSelected,
                    dataType = DataType.PACKAGE_APK,
                    subtitle = StringResourceToken.fromStringId(if (apkSelected) R.string.selected else R.string.not_selected)
                ) {
                    apkSelected = apkSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = userSelected,
                    dataType = DataType.PACKAGE_USER,
                    subtitle = StringResourceToken.fromStringId(if (userSelected) R.string.selected else R.string.not_selected)
                ) {
                    userSelected = userSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = userDeSelected,
                    dataType = DataType.PACKAGE_USER_DE,
                    subtitle = StringResourceToken.fromStringId(if (userDeSelected) R.string.selected else R.string.not_selected)
                ) {
                    userDeSelected = userDeSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = dataSelected,
                    dataType = DataType.PACKAGE_DATA,
                    subtitle = StringResourceToken.fromStringId(if (dataSelected) R.string.selected else R.string.not_selected)
                ) {
                    dataSelected = dataSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = obbSelected,
                    dataType = DataType.PACKAGE_OBB,
                    subtitle = StringResourceToken.fromStringId(if (obbSelected) R.string.selected else R.string.not_selected)
                ) {
                    obbSelected = obbSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    selected = mediaSelected,
                    dataType = DataType.MEDIA_MEDIA,
                    subtitle = StringResourceToken.fromStringId(if (mediaSelected) R.string.selected else R.string.not_selected)
                ) {
                    mediaSelected = mediaSelected.not()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingBottom(SizeTokens.Level16),
                horizontalArrangement = Arrangement.End
            ) {
                Button(enabled = true, onClick = {
                    scope.launch {
                        viewModel.emitIntent(IndexUiIntent.BatchSelectData(apkSelected, userSelected, userDeSelected, dataSelected, obbSelected, mediaSelected))
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }) {
                    Text(text = StringResourceToken.fromStringId(R.string.confirm).value)
                }
            }
        }
    }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        progress = if (uiState.isLoading) -1F else null,
        title = StringResourceToken.fromStringId(R.string.select_apps),
        subtitle = if (packagesSelectedState != 0 && isRefreshing.not()) StringResourceToken.fromString("(${packagesSelectedState}/${packagesState.size})") else null,
        actions = {
            if (isRefreshing.not() && srcPackagesEmptyState.not()) {
                AnimatedVisibility(visible = packagesSelectedState != 0) {
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.Block)) {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.prompt), text = StringResourceToken.fromStringId(R.string.confirm_add_to_blacklist))) {
                                viewModel.emitIntentOnIO(IndexUiIntent.BlockSelected)
                            }
                        }
                    }
                }
                IconButton(icon = ImageVectorToken.fromVector(if (uiState.filterMode) Icons.Filled.FilterAlt else Icons.Outlined.FilterAlt)) {
                    viewModel.emitStateOnMain(uiState.copy(filterMode = uiState.filterMode.not()))
                    viewModel.emitIntentOnIO(IndexUiIntent.ClearKey)
                }
                AnimatedVisibility(visible = packagesSelectedState != 0) {
                    IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.CheckBox)) {
                        showBottomSheet = true
                    }
                }
                IconButton(icon = ImageVectorToken.fromVector(Icons.Outlined.Checklist)) {
                    viewModel.emitIntentOnIO(IndexUiIntent.SelectAll(uiState.selectAll.not()))
                    viewModel.emitStateOnMain(uiState.copy(selectAll = uiState.selectAll.not()))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = packagesSelectedState != 0 && isRefreshing.not(), enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(
                    modifier = Modifier.onSizeChanged { fabHeight = it.height * 1.5f },
                    onClick = {
                        navController.navigate(MainRoutes.PackagesBackupProcessingGraph.route)
                    },
                ) {
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
        }
    ) {
        if (isRefreshing || srcPackagesEmptyState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState, uiState.isLoading.not()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), horizontalAlignment = Alignment.CenterHorizontally) {
                        DotLottieView(isRefreshing = isRefreshing, refreshState = refreshState)
                    }
                }
                InnerBottomSpacer(innerPadding = it)
            }
            PullRefreshIndicator(refreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
        } else {
            Column {
                val flagIndexState by viewModel.flagIndexState.collectAsStateWithLifecycle()
                val userIdListState by viewModel.userIdListState.collectAsStateWithLifecycle()
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
                                dismissOnSelected = true,
                                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Sort),
                                selectedIndex = sortIndexState,
                                type = sortTypeState,
                                list = stringArrayResource(id = R.array.backup_sort_type_items_apps).toList(),
                                onSelected = { index, _ ->
                                    scope.launch {
                                        scrollState.scrollToItem(0)
                                        viewModel.emitIntentOnIO(IndexUiIntent.Sort(index = index, type = sortTypeState))
                                    }
                                },
                                onClick = {}
                            )

                            if (userIdListState.size > 1)
                                MultipleSelectionFilterChip(
                                    enabled = true,
                                    dismissOnSelected = true,
                                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                                    label = StringResourceToken.fromStringId(R.string.user),
                                    selectedIndexList = userIdIndexListState,
                                    list = userIdListState.map { it.toString() },
                                    onSelected = { indexList ->
                                        if (indexList.isNotEmpty()) {
                                            viewModel.emitIntentOnIO(IndexUiIntent.SetUserIdIndexList(indexList))
                                        }
                                    },
                                    onClick = {
                                        viewModel.emitIntentOnIO(IndexUiIntent.GetUserIds)
                                    }
                                )

                            AnimatedVisibility(visible = loadSystemApps) {
                                FilterChip(
                                    enabled = true,
                                    dismissOnSelected = true,
                                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                                    selectedIndex = flagIndexState,
                                    list = stringArrayResource(id = R.array.flag_type_items).toList(),
                                    onSelected = { index, _ ->
                                        viewModel.emitIntentOnIO(IndexUiIntent.FilterByFlag(index = index))
                                    },
                                    onClick = {}
                                )
                            }
                        }

                        Divider(modifier = Modifier
                            .fillMaxWidth()
                            .paddingTop(SizeTokens.Level8))
                    }
                }

                Box {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState, uiState.isLoading.not()),
                        state = scrollState,
                    ) {
                        item(key = "-1") {
                            Spacer(modifier = Modifier.size(SizeTokens.Level1))
                        }

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
                                    }
                                }
                            }

                        item {
                            InnerBottomSpacer(innerPadding = it)
                        }
                    }
                    PullRefreshIndicator(refreshing = isRefreshing, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
        }

        // TODO Issues of ScrollBar
        // ScrollBar(modifier = Modifier.align(Alignment.TopEnd), state = scrollState)
    }
}
