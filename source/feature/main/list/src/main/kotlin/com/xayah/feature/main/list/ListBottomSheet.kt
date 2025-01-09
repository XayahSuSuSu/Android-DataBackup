package com.xayah.feature.main.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.data.repository.Filters
import com.xayah.core.datastore.saveLoadSystemApps
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.CloudEntity
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
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.work.WorkManagerInitializer
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
                opType = uiState.opType,
                clouds = uiState.clouds,
                filters = uiState.filters,
                sortIndex = uiState.sortIndex,
                sortType = uiState.sortType,
                labelEntities = uiState.labelEntities,
                labels = uiState.labels,
                onClickLabel = viewModel::addOrRemoveLabel,
                setFilters = viewModel::setFilters,
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
                labelEntities = uiState.labelEntities,
                labels = uiState.labels,
                onClickLabel = viewModel::addOrRemoveLabel,
                onSortByType = viewModel::setSortByType,
                onSortByIndex = viewModel::setSortByIndex,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}

@Composable
private fun SourceChips(clouds: List<CloudEntity>, onChanged: (cloud: String, backupDir: String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
    ) {
        val context = LocalContext.current
        var index by remember { mutableIntStateOf(0) }
        FilterChip(
            onClick = {
                index = 0
                onChanged("", context.localBackupSaveDir())
            },
            label = { Text(stringResource(R.string.local)) },
            selected = index == 0,
            leadingIcon = if (index == 0) {
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

        clouds.forEachIndexed { i, cloudEntity ->
            FilterChip(
                onClick = {
                    index = i + 1
                    onChanged(cloudEntity.name, cloudEntity.remote)
                },
                label = { Text(cloudEntity.name) },
                selected = index - 1 == i,
                leadingIcon = if (index - 1 == i) {
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
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelsFlow(labelEntities: List<LabelEntity>, labels: Set<String>, onClick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(SizeTokens.Level24),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
        verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8)
    ) {
        labelEntities.forEach { item ->
            val selected by remember(item.label, labels) { mutableStateOf(item.label in labels) }
            FilterChip(
                onClick = {
                    onClick(item.label)
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
    opType: OpType,
    clouds: List<CloudEntity>,
    filters: Filters,
    sortIndex: Int,
    sortType: SortType,
    labelEntities: List<LabelEntity>,
    labels: Set<String>,
    onClickLabel: (String) -> Unit,
    setFilters: (Filters) -> Unit,
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            Title(text = stringResource(id = R.string.filters))
            if (opType == OpType.BACKUP) {
                SourceChips(clouds) { cloud, backupDir ->
                    setFilters(filters.copy(cloud = cloud, backupDir = backupDir))
                }
            }
            CheckBox(checked = filters.showSystemApps, text = stringResource(id = R.string.load_system_apps), onValueChange = {
                scope.launch {
                    if (filters.showSystemApps.not()) {
                        WorkManagerInitializer.fastInitializeAndUpdateApps(context)
                    }
                    context.saveLoadSystemApps(filters.showSystemApps.not())
                    setFilters(filters.copy(showSystemApps = filters.showSystemApps.not()))
                }
            })
            when (opType) {
                OpType.BACKUP -> {
                    CheckBox(checked = filters.hasBackups, text = stringResource(R.string.apps_which_have_backups), onValueChange = { setFilters(filters.copy(hasBackups = filters.hasBackups.not())) })
                    CheckBox(checked = filters.hasNoBackups, text = stringResource(R.string.apps_which_have_no_backups), onValueChange = { setFilters(filters.copy(hasNoBackups = filters.hasNoBackups.not())) })
                }

                OpType.RESTORE -> {
                    CheckBox(checked = filters.installedApps, text = stringResource(R.string.installed), onValueChange = { setFilters(filters.copy(installedApps = filters.installedApps.not())) })
                    CheckBox(checked = filters.notInstalledApps, text = stringResource(R.string.not_installed), onValueChange = { setFilters(filters.copy(notInstalledApps = filters.notInstalledApps.not())) })
                }
            }

            if (labelEntities.isNotEmpty()) {
                Title(text = stringResource(id = R.string.labels))
                LabelsFlow(labelEntities = labelEntities, labels = labels, onClick = onClickLabel)
            }

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
    labelEntities: List<LabelEntity>,
    labels: Set<String>,
    onClickLabel: (String) -> Unit,
    onSortByType: () -> Unit,
    onSortByIndex: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isShow) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
            if (labelEntities.isNotEmpty()) {
                Title(text = stringResource(id = R.string.labels))
                LabelsFlow(labelEntities = labelEntities, labels = labels, onClick = onClickLabel)
            }

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
