package com.xayah.feature.main.task.packages.cloud.restore.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.database.model.OperationMask
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.readRestoreInstallationTypeIndex
import com.xayah.core.datastore.readRestoreSortType
import com.xayah.core.datastore.readRestoreSortTypeIndex
import com.xayah.core.model.SortType
import com.xayah.core.ui.component.ActionChip
import com.xayah.core.ui.component.AnimatedRoundChip
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.getActionMenuConfirmItem
import com.xayah.core.ui.model.getActionMenuReturnItem
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.task.packages.cloud.restore.TaskPackagesRestoreRoutes
import com.xayah.feature.main.task.packages.common.R
import com.xayah.feature.main.task.packages.common.component.ListScaffold
import com.xayah.feature.main.task.packages.common.component.PackageCard
import com.xayah.feature.main.task.packages.common.component.PackageCardShimmer

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageList(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarState by viewModel.topBarState.collectAsStateWithLifecycle()
    val selectedAPKsCount by viewModel.selectedAPKsCountState.collectAsStateWithLifecycle()
    val selectedDataCount by viewModel.selectedDataCountState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesSelected by viewModel.packagesSelectedState.collectAsStateWithLifecycle()
    val packagesNotSelected by viewModel.packagesNotSelectedState.collectAsStateWithLifecycle()
    val shimmering by viewModel.shimmeringState.collectAsStateWithLifecycle()
    val timestampIndexState by viewModel.timestampIndexState.collectAsStateWithLifecycle()
    val timestampListState by viewModel.timestampListState.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    ListScaffold(
        snackbarHostState = viewModel.snackbarHostState,
        topBarState = topBarState,
        fabVisible = uiState.activating.not(),
        fabEmphasizedState = uiState.emphasizedState,
        fabSelectedState = packagesSelected.isNotEmpty(),
        selectedAPKsCount = selectedAPKsCount,
        selectedDataCount = selectedDataCount,
        shimmering = shimmering,
        shimmerCount = uiState.shimmerCount,
        selectedItems = packagesSelected,
        notSelectedItems = packagesNotSelected,
        itemKey = { "${it.packageName} - ${it.operationCode}" },
        onFabClick = {
            if (packagesSelected.isEmpty()) viewModel.emitIntent(IndexUiIntent.Emphasize)
            else navController.navigate(TaskPackagesRestoreRoutes.Processing.route)
        },
        onSearchTextChange = { text ->
            viewModel.emitIntent(IndexUiIntent.FilterByKey(key = text))
        },
        mapChipGroup = { targetState ->
            FilterChip(
                enabled = targetState.not(),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_unfold_more),
                label = StringResourceToken.fromStringId(R.string.date),
                selectedIndex = timestampIndexState,
                list = timestampListState,
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.SelectTimestamp(index = index))
                },
                onClick = {}
            )

            val sortSelectedIndex by context.readRestoreSortTypeIndex().collectAsState(initial = 0)
            val sortType by context.readRestoreSortType().collectAsState(initial = SortType.ASCENDING)
            SortChip(
                enabled = targetState.not(),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_sort),
                selectedIndex = sortSelectedIndex,
                type = sortType,
                list = stringArrayResource(id = R.array.restore_sort_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.Sort(index = index, type = sortType))
                },
                onClick = {}
            )

            val filterSelectedIndex by context.readRestoreFilterFlagIndex().collectAsState(initial = 0)
            FilterChip(
                enabled = targetState.not(),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                selectedIndex = filterSelectedIndex,
                list = stringArrayResource(id = R.array.flag_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.FilterByFlag(index = index))
                },
                onClick = {}
            )

            val installationTypeIndex by context.readRestoreInstallationTypeIndex().collectAsState(initial = 0)
            FilterChip(
                enabled = targetState.not(),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_apk_install),
                selectedIndex = installationTypeIndex,
                list = stringArrayResource(id = R.array.restore_installation_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.FilterByInstallation(index = index))
                },
                onClick = {}
            )
        },
        actionChipGroup = { targetState ->
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.batching_select),
                    StringResourceToken.fromString("(${uiState.batchSelection.size})"),
                ),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Checklist),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelectAll)
                },
            )

            if (uiState.batchSelection.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                    ActionChip(
                        enabled = targetState.not(),
                        label = StringResourceToken.fromStringId(R.string.delete),
                        leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                        onClick = {
                            expanded = true
                        },
                    )

                    ModalActionDropdownMenu(expanded = expanded, actionList = listOf(
                        getActionMenuReturnItem {
                            expanded = false
                        },
                        getActionMenuConfirmItem {
                            viewModel.suspendEmitIntent(IndexUiIntent.Delete(items = uiState.batchSelection.map { packageName ->
                                packagesState.first { it.packageName == packageName }
                            }))
                        }
                    ), onDismissRequest = { expanded = false })
                }
            }

            var batchingApkSelection by remember { mutableStateOf(true) }
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.apk),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.TripOrigin),
                onClick = {
                    viewModel.launchOnIO {
                        val packageNames = uiState.batchSelection.ifEmpty { packagesState.map { it.packageName } }

                        if (batchingApkSelection)
                            viewModel.emitIntent(IndexUiIntent.BatchOrOp(mask = OperationMask.Apk, packageNames = packageNames))
                        else
                            viewModel.emitIntent(IndexUiIntent.BatchAndOp(mask = OperationMask.Apk.inv(), packageNames = packageNames))

                        batchingApkSelection = batchingApkSelection.not()
                    }
                },
            )
            var batchingDataSelection by remember { mutableStateOf(true) }
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.data),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.TripOrigin),
                onClick = {
                    viewModel.launchOnIO {
                        val packageNames = uiState.batchSelection.ifEmpty { packagesState.map { it.packageName } }

                        if (batchingDataSelection)
                            viewModel.emitIntent(IndexUiIntent.BatchOrOp(mask = OperationMask.Data, packageNames = packageNames))
                        else
                            viewModel.emitIntent(IndexUiIntent.BatchAndOp(mask = OperationMask.Data.inv(), packageNames = packageNames))

                        batchingDataSelection = batchingDataSelection.not()
                    }
                },
            )
        },
        shimmerItem = {
            PackageCardShimmer()
        },
        item = { item ->
            val enabled = remember(item.sizeBytes) { item.isExists }

            PackageCard(
                enabled = enabled,
                packageRestore = item,
                cardSelected = item.packageName in uiState.batchSelection,
                actions = listOf(
                    ActionMenuItem(
                        title = StringResourceToken.fromStringId(R.string.delete),
                        icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                        enabled = true,
                        secondaryMenu = listOf(
                            getActionMenuReturnItem(),
                            getActionMenuConfirmItem {
                                viewModel.suspendEmitIntent(IndexUiIntent.Delete(items = listOf(item)))
                            }
                        ),
                        onClick = {}
                    )
                ),
                onApkSelected = {
                    viewModel.emitIntent(
                        IndexUiIntent.UpdatePackage(
                            entity = item.copy(operationCode = item.operationCode xor OperationMask.Apk)
                        )
                    )
                },
                onDataSelected = {
                    viewModel.emitIntent(
                        IndexUiIntent.UpdatePackage(
                            entity = item.copy(operationCode = item.operationCode xor OperationMask.Data)
                        )
                    )
                },
                onCardClick = {
                    if (uiState.batchSelection.isNotEmpty()) {
                        viewModel.emitIntent(IndexUiIntent.BatchingSelect(packageName = item.packageName))
                    } else {
                        viewModel.emitIntent(
                            IndexUiIntent.UpdatePackage(
                                entity = item.copy(operationCode = if (item.operationCode == OperationMask.None) item.backupOpCode else OperationMask.None)
                            )
                        )
                    }
                },
                onCardLongClick = {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelect(packageName = item.packageName))
                },
                chipGroup = {
                    if (enabled) {
                        if (item.versionName.isNotEmpty()) RoundChip(text = item.versionName)
                        AnimatedRoundChip(text = item.sizeDisplay)
                        AnimatedRoundChip(text = StringResourceToken.fromStringId(if (item.installed) R.string.installed else R.string.not_installed).value)
                    } else {
                        RoundChip(text = StringResourceToken.fromStringId(R.string.not_exist).value, enabled = false)
                    }
                },
            )
        }
    )
}
