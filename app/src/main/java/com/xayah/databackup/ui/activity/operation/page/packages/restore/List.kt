package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.ui.activity.operation.page.packages.backup.ListState
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.ChipDropdownMenu
import com.xayah.databackup.ui.component.ListItemPackageRestore
import com.xayah.databackup.ui.component.ListTopBar
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.SearchBar
import com.xayah.databackup.ui.component.SortState
import com.xayah.databackup.ui.component.SortStateChipDropdownMenu
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.emphasizedOffset
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.readRestoreFilterTypeIndex
import com.xayah.databackup.util.readRestoreFlagTypeIndex
import com.xayah.databackup.util.readRestoreSortState
import com.xayah.databackup.util.readRestoreSortTypeIndex
import com.xayah.databackup.util.saveRestoreFilterTypeIndex
import com.xayah.databackup.util.saveRestoreFlagTypeIndex
import com.xayah.databackup.util.saveRestoreSortState
import com.xayah.databackup.util.saveRestoreSortTypeIndex
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryService
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch
import java.text.Collator

private fun sortByAlphabet(state: SortState): Comparator<PackageRestoreEntire> = Comparator { entity1, entity2 ->
    if (entity1 == null && entity2 == null) {
        0
    } else if (entity1 == null) {
        -1
    } else if (entity2 == null) {
        1
    } else {
        when (state) {
            SortState.ASCENDING -> {
                Collator.getInstance().let { collator ->
                    collator.getCollationKey(entity1.label)
                        .compareTo(collator.getCollationKey(entity2.label))
                }
            }

            SortState.DESCENDING -> {
                Collator.getInstance().let { collator ->
                    collator.getCollationKey(entity2.label)
                        .compareTo(collator.getCollationKey(entity1.label))
                }
            }
        }
    }
}

private fun sort(index: Int, state: SortState): Comparator<PackageRestoreEntire> = when (index) {
    else -> sortByAlphabet(state)
}

private fun filter(index: Int, isFlagType: Boolean): (PackageRestoreEntire) -> Boolean = { packageRestoreEntire ->
    if (isFlagType.not()) {
        when (index) {
            1 -> packageRestoreEntire.operationCode != OperationMask.None
            2 -> packageRestoreEntire.operationCode == OperationMask.None
            else -> true
        }
    } else {
        when (index) {
            1 -> packageRestoreEntire.isSystemApp.not()
            2 -> packageRestoreEntire.isSystemApp
            else -> true
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageRestoreList() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<ListViewModel>()
    val navController = LocalSlotScope.current!!.navController
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState
    val packagesState by uiState.packages.collectAsState(initial = listOf())
    var packageSearchPredicate: (PackageRestoreEntire) -> Boolean by remember { mutableStateOf({ true }) }
    var packageSelectionPredicate: (PackageRestoreEntire) -> Boolean by remember { mutableStateOf(filter(context.readRestoreFilterTypeIndex(), false)) }
    var packageFlagTypePredicate: (PackageRestoreEntire) -> Boolean by remember { mutableStateOf(filter(context.readRestoreFlagTypeIndex(), true)) }
    var packageSortComparator: Comparator<in PackageRestoreEntire> by remember {
        mutableStateOf(
            sort(
                index = context.readRestoreSortTypeIndex(),
                state = context.readRestoreSortState()
            )
        )
    }
    val packages = remember(packagesState, packageSearchPredicate, packageSelectionPredicate, packageFlagTypePredicate, packageSortComparator) {
        packagesState.filter(packageSearchPredicate).filter(packageSelectionPredicate).filter(packageFlagTypePredicate).sortedWith(packageSortComparator)
    }
    val selectedAPKs by uiState.selectedAPKs.collectAsState(initial = 0)
    val selectedData by uiState.selectedData.collectAsState(initial = 0)
    val selected = remember(selectedAPKs, selectedData) {
        selectedAPKs != 0 || selectedData != 0
    }
    var state by remember { mutableStateOf(ListState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    var emphasizedState by remember { mutableStateOf(false) }
    val emphasizedOffset by emphasizedOffset(targetState = emphasizedState)

    LaunchedEffect(null) {
        withIOContext {
            val remoteRootService = RemoteRootService(context)
            tryService(onFailed = { msg ->
                scope.launch {
                    state = state.setState(ListState.Error)
                    snackbarHostState.showSnackbar(
                        message = "$msg\n${context.getString(R.string.remote_service_err_info)}",
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }) {
                remoteRootService.testService()
            }
            viewModel.initializeUiState()
            state = state.setState(ListState.Done)
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ListTopBar(scrollBehavior = scrollBehavior, title = stringResource(id = R.string.restore_list))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(visible = state != ListState.Idle, enter = scaleIn(), exit = scaleOut()) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .padding(CommonTokens.PaddingMedium)
                        .offset(x = emphasizedOffset),
                    onClick = {
                        if (selected.not() || state == ListState.Error) emphasizedState = !emphasizedState
                        else navController.navigate(OperationRoutes.PackageRestoreManifest.route)
                    },
                    expanded = selected || state != ListState.Done,
                    icon = {
                        Icon(
                            imageVector = if (state != ListState.Done) Icons.Rounded.Refresh else if (selected) Icons.Rounded.ArrowForward else Icons.Rounded.Close,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = "$selectedAPKs ${stringResource(id = R.string.apk)}, $selectedData ${stringResource(id = R.string.data)}"
                        )
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = state, label = AnimationTokens.CrossFadeLabel) { state ->
                    if (state != ListState.Idle)
                        LazyColumn(
                            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                                SearchBar(onTextChange = { text ->
                                    packageSearchPredicate = { packageRestoreEntire ->
                                        packageRestoreEntire.label.lowercase().contains(text.lowercase())
                                                || packageRestoreEntire.packageName.lowercase().contains(text.lowercase())
                                    }
                                })
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                                        .horizontalScroll(rememberScrollState())
                                        .paddingHorizontal(CommonTokens.PaddingMedium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                                ) {
                                    val dateList = uiState.timestamps.map { timestamp -> DateUtil.formatTimestamp(timestamp) }
                                    ChipDropdownMenu(
                                        label = stringResource(R.string.date),
                                        trailingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_unfold_more),
                                        defaultSelectedIndex = uiState.selectedIndex,
                                        list = dateList,
                                    ) { index, _ ->
                                        scope.launch {
                                            viewModel.setSelectedIndex(index)
                                        }
                                    }

                                    SortStateChipDropdownMenu(
                                        icon = ImageVector.vectorResource(R.drawable.ic_rounded_sort),
                                        defaultSelectedIndex = remember { context.readRestoreSortTypeIndex() },
                                        defaultSortState = remember { context.readRestoreSortState() },
                                        list = stringArrayResource(id = R.array.restore_sort_type_items).toList(),
                                        onSelected = { index, _, state ->
                                            context.saveRestoreSortTypeIndex(index)
                                            context.saveRestoreSortState(state)
                                            packageSortComparator = sort(index = index, state = state)
                                        }
                                    )

                                    ChipDropdownMenu(
                                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_filter_list),
                                        defaultSelectedIndex = remember { context.readRestoreFilterTypeIndex() },
                                        list = stringArrayResource(id = R.array.filter_type_items).toList(),
                                    ) { index, _ ->
                                        context.saveRestoreFilterTypeIndex(index)
                                        packageSelectionPredicate = filter(index, false)
                                    }

                                    ChipDropdownMenu(
                                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_deployed_code),
                                        defaultSelectedIndex = remember { context.readRestoreFlagTypeIndex() },
                                        list = stringArrayResource(id = R.array.flag_type_items).toList(),
                                    ) { index, _ ->
                                        context.saveRestoreFlagTypeIndex(index)
                                        packageFlagTypePredicate = filter(index, true)
                                    }
                                }
                            }

                            items(
                                items = packages, key = { it.packageName }) { packageInfo ->
                                ListItemPackageRestore(packageInfo = packageInfo)
                            }
                            item {
                                Spacer(modifier = Modifier.height(CommonTokens.None))
                            }
                        }
                }
            }
        }
    }
}
