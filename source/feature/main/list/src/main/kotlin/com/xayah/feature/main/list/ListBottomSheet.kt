package com.xayah.feature.main.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.DataType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStates.Companion.getSelected
import com.xayah.core.model.database.PackageDataStates.Companion.setSelected
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.ModalBottomSheet
import com.xayah.core.ui.component.PackageDataChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
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
                onSortByType = viewModel::setSortByType,
                onSortByIndex = viewModel::setSortByIndex,
                onDismissRequest = onDismissRequest,
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
    onLoadSystemAppsChanged: () -> Unit,
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            Title(text = stringResource(id = R.string.filters))
            CheckBox(checked = isLoadSystemApps, text = stringResource(id = R.string.load_system_apps), onValueChange = { onLoadSystemAppsChanged() })

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
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
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

@Composable
private fun BottomButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .paddingTop(SizeTokens.Level12)
            .paddingHorizontal(SizeTokens.Level24),
        enabled = true,
        onClick = onClick
    ) {
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DataChips(selections: PackageDataStates, onItemClick: (DataType, Boolean) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
        verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
        maxItemsInEachRow = 2
    ) {
        val items = remember {
            listOf(
                DataType.PACKAGE_APK,
                DataType.PACKAGE_USER,
                DataType.PACKAGE_USER_DE,
                DataType.PACKAGE_DATA,
                DataType.PACKAGE_OBB,
                DataType.PACKAGE_MEDIA
            )
        }

        items.forEach {
            val selected = it.getSelected(selections)
            PackageDataChip(
                modifier = Modifier.weight(1f),
                dataType = it,
                selected = selected
            ) {
                onItemClick(it, selected)
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    TitleLargeText(
        modifier = Modifier
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        text = text
    )
}

@Composable
private fun TitleSort(text: String, sortType: SortType, onSort: () -> Unit) {
    Row(
        modifier = Modifier
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleLargeText(text = text)
        IconButton(
            icon = when (sortType) {
                SortType.ASCENDING -> Icons.Outlined.ArrowDropUp
                SortType.DESCENDING -> Icons.Outlined.ArrowDropDown
            },
            onClick = onSort
        )
    }
}

@Composable
private fun CheckBox(
    checked: Boolean,
    text: String,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onValueChange,
                role = Role.Checkbox
            )
            .paddingHorizontal(SizeTokens.Level24)
            .paddingVertical(SizeTokens.Level12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = text)
    }
}

@Composable
private fun RadioButtons(selected: Int, items: List<String>, onSelect: (Int) -> Unit) {
    Column(Modifier.selectableGroup()) {
        items.forEachIndexed { index, text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (index == selected),
                        onClick = {
                            onSelect(index)
                        },
                        role = Role.RadioButton
                    )
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (index == selected), onClick = null)
                BodyLargeText(modifier = Modifier.paddingStart(SizeTokens.Level16), text = text)
            }
        }
    }
}
