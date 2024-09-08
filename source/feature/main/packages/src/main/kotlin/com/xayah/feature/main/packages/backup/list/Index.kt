package com.xayah.feature.main.packages.backup.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.saveLoadSystemApps
import com.xayah.core.model.DataType
import com.xayah.core.model.SortType
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.ModalBottomSheet
import com.xayah.core.ui.component.PackageDataChip
import com.xayah.core.ui.component.PackageItem
import com.xayah.core.ui.component.SearchBar
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.packages.ListScaffold
import com.xayah.feature.main.packages.R
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesBackupList() {
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayPackagesState by viewModel.displayPackagesState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesSelectedState by viewModel.packagesSelectedState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(canScroll = { false })
    val scrollState = rememberLazyListState()
    val srcPackagesEmptyState by viewModel.srcPackagesEmptyState.collectAsStateWithLifecycle()
    var fabHeight: Float by remember { mutableFloatStateOf(0F) }
    val showFilterBottomSheet = remember { mutableStateOf(false) }
    val showDataItemsBottomSheet = remember { mutableStateOf(false) }

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.GetUsers)
    }
    SetOnResume {
        viewModel.emitIntentOnIO(IndexUiIntent.OnFastRefresh)
    }

    FilterBottomSheet(showBottomSheet = showFilterBottomSheet, scrollState = scrollState, viewModel = viewModel)
    DataItemsBottomSheet(showBottomSheet = showDataItemsBottomSheet, viewModel = viewModel)

    ListScaffold(
        scrollBehavior = scrollBehavior,
        progress = if (uiState.isLoading) -1F else null,
        title = stringResource(id = R.string.select_apps),
        subtitle = if (packagesSelectedState != 0) "(${packagesSelectedState}/${packagesState.size})" else null,
        actions = {
            if (srcPackagesEmptyState.not()) {
                IconButton(icon = Icons.Outlined.FilterList) {
                    showFilterBottomSheet.value = true
                }

                SelectIconButton(
                    viewModel = viewModel,
                    packagesSelectedState = packagesSelectedState,
                    showDataItemsBottomSheet = showDataItemsBottomSheet
                )

                MoreIconButton(viewModel = viewModel)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(visible = packagesSelectedState != 0, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.onSizeChanged { fabHeight = it.height * 1.5f },
                    onClick = {
                        navController.navigateSingle(MainRoutes.PackagesBackupProcessingGraph.route)
                    },
                    icon = { Icon(Icons.Rounded.ChevronRight, null) },
                    text = { Text(text = stringResource(id = R.string._continue)) },
                )
            }
        }
    ) {
        Column {
            val userListState by viewModel.userListState.collectAsStateWithLifecycle()
            val userIdIndexState by viewModel.userIdIndexState.collectAsStateWithLifecycle()
            val displayPackagesSelectedState by viewModel.displayPackagesSelectedState.collectAsStateWithLifecycle()
            SearchBar(
                modifier = Modifier
                    .paddingHorizontal(SizeTokens.Level16)
                    .paddingVertical(SizeTokens.Level8),
                enabled = true,
                placeholder = stringResource(id = R.string.search_bar_hint_packages),
                onTextChange = {
                    viewModel.emitIntentOnIO(IndexUiIntent.FilterByKey(key = it))
                }
            )

            AnimatedVisibility(visible = userListState.size > 1) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = userIdIndexState,
                    edgePadding = SizeTokens.Level0,
                    indicator = @Composable { tabPositions ->
                        if (userIdIndexState < tabPositions.size) {
                            val width by animateDpAsState(
                                targetValue = tabPositions[userIdIndexState].contentWidth,
                                label = AnimationTokens.AnimatedProgressLabel
                            )
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[userIdIndexState]),
                                width = width,
                                shape = CircleShape
                            )
                        }
                    },
                    divider = {
                        Divider(modifier = Modifier.fillMaxWidth())
                    }
                ) {
                    userListState.forEachIndexed { index, user ->
                        Tab(
                            selected = userIdIndexState == index,
                            onClick = {
                                viewModel.emitIntentOnIO(IndexUiIntent.SetUserId(index))
                            },
                            text = {
                                BadgedBox(
                                    modifier = Modifier.fillMaxSize(),
                                    badge = {
                                        val number = remember(displayPackagesSelectedState) {
                                            displayPackagesSelectedState[user.id]?.toString()
                                        }
                                        if (number != null) {
                                            Badge { Text(number) }
                                        }
                                    }
                                ) {
                                    Text(text = "${user.name} (${user.id})", maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = userListState.size <= 1) {
                Divider(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState,
            ) {
                item(key = "-1") {
                    Spacer(modifier = Modifier.size(SizeTokens.Level1))
                }

                items(items = displayPackagesState, key = { "${uiState.uuid}-${it.id}" }) { item ->
                    Row(modifier = Modifier.animateItemPlacement()) {
                        PackageItem(
                            item = item,
                            onCheckedChange = { viewModel.emitIntentOnIO(IndexUiIntent.Select(item)) },
                            onItemsIconClick = { flag ->
                                viewModel.emitIntentOnIO(IndexUiIntent.ChangeFlag(flag, item))
                            },
                            onClick = {
                                viewModel.emitIntentOnIO(IndexUiIntent.ToPageDetail(navController, item))
                            }
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
        }

        // TODO Issues of ScrollBar
        // ScrollBar(modifier = Modifier.align(Alignment.TopEnd), state = scrollState)
    }
}


@ExperimentalMaterial3Api
@Composable
fun FilterBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    scrollState: LazyListState,
    viewModel: IndexViewModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val loadSystemApps by viewModel.loadSystemApps.collectAsStateWithLifecycle()
    val sortIndexState by viewModel.sortIndexState.collectAsStateWithLifecycle()
    val sortTypeState by viewModel.sortTypeState.collectAsStateWithLifecycle()

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet.value = false
                }
            },
            sheetState = sheetState
        ) {
            TitleLargeText(
                modifier = Modifier
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                text = stringResource(id = R.string.filters)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = loadSystemApps,
                        onValueChange = {
                            scope.launch {
                                context.saveLoadSystemApps(loadSystemApps.not())
                            }
                        },
                        role = Role.Checkbox
                    )
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = loadSystemApps, onCheckedChange = null)
                BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = stringResource(id = R.string.load_system_apps))
            }

            Row(
                modifier = Modifier
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TitleLargeText(text = stringResource(id = R.string.sort))
                IconButton(
                    icon = when (sortTypeState) {
                        SortType.ASCENDING -> Icons.Outlined.ArrowDropUp
                        SortType.DESCENDING -> Icons.Outlined.ArrowDropDown
                    }
                ) {
                    scope.launch {
                        scrollState.scrollToItem(0)
                        viewModel.emitIntentOnIO(IndexUiIntent.SortByType(type = sortTypeState))
                    }
                }
            }
            val radioOptions = stringArrayResource(id = R.array.backup_sort_type_items_apps).toList()
            Column(Modifier.selectableGroup()) {
                radioOptions.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (index == sortIndexState),
                                onClick = {
                                    scope.launch {
                                        scrollState.scrollToItem(0)
                                        viewModel.emitIntentOnIO(IndexUiIntent.SortByIndex(index = index))
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .paddingHorizontal(SizeTokens.Level24)
                            .paddingVertical(SizeTokens.Level12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (index == sortIndexState), onClick = null)
                        BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = text)
                    }
                }
            }
        }
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DataItemsBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    viewModel: IndexViewModel,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet.value = false
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

            TitleLargeText(
                modifier = Modifier
                    .paddingHorizontal(SizeTokens.Level24),
                text = stringResource(id = R.string.data_items)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                maxItemsInEachRow = 2
            ) {
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.PACKAGE_APK,
                    selected = apkSelected
                ) {
                    apkSelected = apkSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.PACKAGE_USER,
                    selected = userSelected
                ) {
                    userSelected = userSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.PACKAGE_USER_DE,
                    selected = userDeSelected
                ) {
                    userDeSelected = userDeSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.PACKAGE_DATA,
                    selected = dataSelected
                ) {
                    dataSelected = dataSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.PACKAGE_OBB,
                    selected = obbSelected
                ) {
                    obbSelected = obbSelected.not()
                }
                PackageDataChip(
                    modifier = Modifier.weight(1f),
                    dataType = DataType.MEDIA_MEDIA,
                    selected = mediaSelected
                ) {
                    mediaSelected = mediaSelected.not()
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24),
                enabled = true,
                onClick = {
                    scope.launch {
                        viewModel.emitIntent(
                            IndexUiIntent.BatchSelectData(
                                apkSelected,
                                userSelected,
                                userDeSelected,
                                dataSelected,
                                obbSelected,
                                mediaSelected
                            )
                        )
                        sheetState.hide()
                        showBottomSheet.value = false
                    }
                }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun SelectIconButton(
    viewModel: IndexViewModel,
    packagesSelectedState: Int,
    showDataItemsBottomSheet: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot

    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(icon = Icons.Rounded.Checklist) {
            expanded = true
        }

        ModalActionDropdownMenu(expanded = expanded, actionList = listOf(
            ActionMenuItem(
                title = stringResource(id = R.string.select_all),
                icon = Icons.Rounded.CheckBox,
                enabled = true,
                secondaryMenu = listOf(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.SelectAll(true))
                    expanded = false
                }
            ),
            ActionMenuItem(
                title = stringResource(id = R.string.unselect_all),
                icon = Icons.Rounded.CheckBoxOutlineBlank,
                enabled = true,
                secondaryMenu = listOf(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.SelectAll(false))
                    expanded = false
                }
            ),
            ActionMenuItem(
                title = stringResource(id = R.string.reverse_selection),
                icon = Icons.Rounded.RestartAlt,
                enabled = true,
                secondaryMenu = listOf(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.Reverse)
                    expanded = false
                }
            ),
            ActionMenuItem(
                title = stringResource(id = R.string.for_selected),
                icon = Icons.Rounded.MoreVert,
                enabled = packagesSelectedState != 0,
                secondaryMenu = listOf(
                    ActionMenuItem(
                        title = stringResource(id = R.string.block),
                        icon = Icons.Rounded.Block,
                        enabled = packagesSelectedState != 0,
                        secondaryMenu = listOf(),
                        onClick = {
                            viewModel.launchOnIO {
                                if (dialogState.confirm(
                                        title = context.getString(R.string.prompt),
                                        text = context.getString(R.string.confirm_add_to_blacklist)
                                    )
                                ) {
                                    viewModel.emitIntentOnIO(IndexUiIntent.BlockSelected)
                                }
                            }
                            expanded = false
                        }
                    ),
                    ActionMenuItem(
                        title = stringResource(id = R.string.detailed_data_items),
                        icon = Icons.Rounded.Rule,
                        enabled = packagesSelectedState != 0,
                        secondaryMenu = listOf(),
                        onClick = {
                            showDataItemsBottomSheet.value = true
                            expanded = false
                        }
                    )
                ),
            )
        ), onDismissRequest = { expanded = false })
    }
}

@ExperimentalMaterial3Api
@Composable
fun MoreIconButton(
    viewModel: IndexViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(icon = Icons.Rounded.MoreVert) {
            expanded = true
        }

        ModalActionDropdownMenu(expanded = expanded, actionList = listOf(
            ActionMenuItem(
                title = stringResource(id = R.string.refresh),
                icon = Icons.Rounded.Refresh,
                enabled = true,
                secondaryMenu = listOf(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.OnRefresh)
                    expanded = false
                }
            ),
        ), onDismissRequest = { expanded = false })
    }
}