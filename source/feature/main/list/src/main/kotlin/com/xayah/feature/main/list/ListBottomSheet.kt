package com.xayah.feature.main.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.SortType
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStates.Companion.setSelected
import com.xayah.core.ui.component.BottomButton
import com.xayah.core.ui.component.CheckBox
import com.xayah.core.ui.component.DataChips
import com.xayah.core.ui.component.ModalBottomSheet
import com.xayah.core.ui.component.RadioButtons
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.TitleSort
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.token.SizeTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ListBottomSheet(
    viewModel: ListBottomSheetViewModel = hiltViewModel(),
) {
    val uiState: ListBottomSheetUiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is ListBottomSheetUiState.Success) {
        ListBottomSheet(
            uiState = uiState.castTo(),
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListBottomSheet(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    uiState: ListBottomSheetUiState.Success,
    viewModel: ListBottomSheetViewModel,
) {
    val sheetState = rememberModalBottomSheetState()
    val onDismissRequest: () -> Unit = {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                viewModel.setShowFilterSheet(false)
            }
        }
    }

    when (uiState) {
        is ListBottomSheetUiState.Success.Apps -> {
            AppsFilterSheet(
                isShow = uiState.showFilterSheet,
                sheetState = sheetState,
                isLoadSystemApps = uiState.showSystemApps,
                sortIndex = uiState.sortIndex,
                sortType = uiState.sortType,
                labels = uiState.labels,
                labelIds = uiState.labelIds,
                onClickLabel = viewModel::addOrRemoveLabelId,
                onLoadSystemAppsChanged = viewModel::setShowSystemApps,
                onSortByType = viewModel::setSortByType,
                onSortByIndex = viewModel::setSortByIndex,
                onDismissRequest = onDismissRequest,
            )

            val dataItemsSheetState = rememberModalBottomSheetState()
            AppsDataItemsSheet(
                isShow = uiState.showDataItemsSheet,
                sheetState = dataItemsSheetState,
                onDismissRequest = {
                    coroutineScope.launch { dataItemsSheetState.hide() }.invokeOnCompletion {
                        if (!dataItemsSheetState.isVisible) {
                            viewModel.setShowDataItemsSheet(false)
                        }
                    }
                },
                onSetDataItems = {
                    viewModel.setDataItems(it)
                }
            )
        }

        is ListBottomSheetUiState.Success.Files -> {
            FilesFilterSheet(
                isShow = uiState.showFilterSheet,
                sheetState = sheetState,
                sortIndex = uiState.sortIndex,
                sortType = uiState.sortType,
                labels = uiState.labels,
                labelIds = uiState.labelIds,
                onClickLabel = viewModel::addOrRemoveLabelId,
                onSortByType = viewModel::setSortByType,
                onSortByIndex = viewModel::setSortByIndex,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelsFlow(labels: List<LabelEntity>, labelIds: Set<Long>, onClick: (Long) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
        verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8)
    ) {
        labels.forEach { item ->
            val selected by remember(item.id, labelIds) { mutableStateOf(item.id in labelIds) }
            FilterChip(
                onClick = {
                    onClick(item.id)
                },
                label = { Text(item.label) },
                selected = selected,
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppsFilterSheet(
    isShow: Boolean,
    sheetState: SheetState,
    isLoadSystemApps: Boolean,
    sortIndex: Int,
    sortType: SortType,
    labels: List<LabelEntity>,
    labelIds: Set<Long>,
    onClickLabel: (Long) -> Unit,
    onLoadSystemAppsChanged: () -> Unit,
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            Title(text = stringResource(id = R.string.filters))
            CheckBox(checked = isLoadSystemApps, text = stringResource(id = R.string.load_system_apps), onValueChange = { onLoadSystemAppsChanged() })

            Title(text = stringResource(id = R.string.labels))
            LabelsFlow(labels = labels, labelIds = labelIds, onClick = onClickLabel)

            TitleSort(text = stringResource(id = R.string.sort), sortType = sortType, onSort = onSortByType)
            RadioButtons(selected = sortIndex, items = stringArrayResource(id = R.array.backup_sort_type_items_apps).toList(), onSelect = onSortByIndex)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilesFilterSheet(
    isShow: Boolean,
    sheetState: SheetState,
    sortIndex: Int,
    sortType: SortType,
    labels: List<LabelEntity>,
    labelIds: Set<Long>,
    onClickLabel: (Long) -> Unit,
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            Title(text = stringResource(id = R.string.labels))
            LabelsFlow(labels = labels, labelIds = labelIds, onClick = onClickLabel)

            TitleSort(text = stringResource(id = R.string.sort), sortType = sortType, onSort = onSortByType)
            RadioButtons(
                selected = sortIndex,
                items = stringArrayResource(id = R.array.backup_sort_type_items_files).toList(),
                onSelect = onSortByIndex
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppsDataItemsSheet(
    isShow: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSetDataItems: (PackageDataStates) -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            Title(text = stringResource(id = R.string.data_items))

            var selections by remember { mutableStateOf(PackageDataStates()) }
            DataChips(selections) { type, selected ->
                selections = type.setSelected(selections, selected.not())
            }

            BottomButton(text = stringResource(id = R.string.confirm)) {
                onDismissRequest()
                onSetDataItems(selections)
            }
        }
    }
}
