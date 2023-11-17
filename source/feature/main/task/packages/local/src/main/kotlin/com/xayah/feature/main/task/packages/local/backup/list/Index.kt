package com.xayah.feature.main.task.packages.local.backup.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.database.model.OperationMask
import com.xayah.core.datastore.readBackupFilterFlagIndex
import com.xayah.core.datastore.readBackupSortType
import com.xayah.core.datastore.readBackupSortTypeIndex
import com.xayah.core.model.SortType
import com.xayah.core.ui.component.ActionChip
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.task.packages.common.R
import com.xayah.feature.main.task.packages.common.component.ListScaffold
import com.xayah.feature.main.task.packages.common.component.PackageCard
import com.xayah.feature.main.task.packages.common.component.PackageCardShimmer
import com.xayah.feature.main.task.packages.local.backup.TaskPackagesBackupRoutes

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

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    ListScaffold(
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
            else navController.navigate(TaskPackagesBackupRoutes.Processing.route)
        },
        onSearchTextChange = { text ->
            viewModel.emitIntent(IndexUiIntent.FilterByKey(key = text))
        },
        mapChipGroup = { targetState ->
            val sortSelectedIndex by context.readBackupSortTypeIndex().collectAsState(initial = 0)
            val sortType by context.readBackupSortType().collectAsState(initial = SortType.ASCENDING)
            SortChip(
                enabled = targetState.not(),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_sort),
                selectedIndex = sortSelectedIndex,
                type = sortType,
                list = stringArrayResource(id = R.array.backup_sort_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.Sort(index = index, type = sortType))
                },
                onClick = {}
            )

            val filterSelectedIndex by context.readBackupFilterFlagIndex().collectAsState(initial = 0)
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
            PackageCard(
                packageBackup = item,
                cardSelected = item.packageName in uiState.batchSelection,
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
                                entity = item.copy(operationCode = if (item.operationCode == OperationMask.Both) OperationMask.None else OperationMask.Both)
                            )
                        )
                    }
                },
                onCardLongClick = {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelect(packageName = item.packageName))
                },
                chipGroup = {
                    if (item.versionName.isNotEmpty()) RoundChip(text = item.versionName)
                    RoundChip(text = item.sizeDisplay)
                },
            )
        }
    )
}
