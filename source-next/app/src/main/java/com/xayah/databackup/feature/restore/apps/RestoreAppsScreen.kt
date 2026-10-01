package com.xayah.databackup.feature.restore.apps

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.toAppOptions
import com.xayah.databackup.feature.restore.component.RestoreListScaffold
import com.xayah.databackup.ui.component.selection.AppFilterSheetContent
import com.xayah.databackup.ui.component.selection.AppFilterUserOption
import com.xayah.databackup.ui.component.selection.AppListItem
import com.xayah.databackup.ui.component.selection.AppSelectionMenu
import com.xayah.databackup.util.DefStorageSize
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely

@Composable
fun RestoreAppsScreen(navigator: Navigator, viewModel: AppsViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val apps by viewModel.items.collectAsStateWithLifecycle()
    val search by viewModel.searchText.collectAsStateWithLifecycle()
    val selectedBytes by viewModel.selectedBytes.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    RestoreListScaffold(
        title = stringResource(R.string.select_apps),
        items = apps,
        itemKey = { it.key },
        selectedCount = apps.count { it.key in state.selected },
        totalCount = apps.size,
        searchText = search,
        loading = state.loading,
        onSearchTextChange = viewModel::changeSearchText,
        onBack = navigator::popBackStackSafely,
        itemSpacing = 0.dp,
        separateSearchScroll = true,
        selectedSize = selectedBytes.takeUnless { it == DefStorageSize },
        actions = {
            IconButton(onClick = { showFilters = true }) {
                Icon(ImageVector.vectorResource(R.drawable.ic_funnel), stringResource(R.string.filters))
            }
            val apk = setOf(BackupSourceCategory.Apk)
            val internal = setOf(BackupSourceCategory.InternalData)
            val external = setOf(BackupSourceCategory.ExternalData)
            val additional = setOf(BackupSourceCategory.AdditionalData)
            val data = internal + external + additional
            AppSelectionMenu(
                apkAllSelected = viewModel.getAllPartsSelected(apk),
                dataAllSelected = viewModel.getAllPartsSelected(data),
                intDataAllSelected = viewModel.getAllPartsSelected(internal),
                extDataAllSelected = viewModel.getAllPartsSelected(external),
                addlDataAllSelected = viewModel.getAllPartsSelected(additional),
                onSelectAllApk = { viewModel.selectAllParts(apk) },
                onSelectAllData = { viewModel.selectAllParts(data) },
                onSelectAllIntData = { viewModel.selectAllParts(internal) },
                onSelectAllExtData = { viewModel.selectAllParts(external) },
                onSelectAllAddlData = { viewModel.selectAllParts(additional) },
            )
        },
    ) { modifier, item ->
        val parts = state.appParts[item.key].orEmpty()
        val availableParts = state.inventory?.availableAppParts?.get(item.key).orEmpty()
        AppListItem(
            modifier = modifier, context = context, app = item.value,
            availableOptions = availableParts.toAppOptions(),
            selectionState = when {
                parts.isEmpty() -> ToggleableState.Off
                parts == availableParts -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            },
            onSelectAll = { viewModel.selectItem(item.key, parts != availableParts) },
            onSelectApk = { viewModel.selectAppPart(item.key, BackupSourceCategory.Apk, it) },
            onSelectInternalData = { viewModel.selectAppPart(item.key, BackupSourceCategory.InternalData, it) },
            onSelectExternalData = { viewModel.selectAppPart(item.key, BackupSourceCategory.ExternalData, it) },
            onSelectAdditionalData = { viewModel.selectAppPart(item.key, BackupSourceCategory.AdditionalData, it) },
        )
    }
    if (showFilters) {
        val users by viewModel.users.collectAsStateWithLifecycle()
        val filters by viewModel.uiState.collectAsStateWithLifecycle()
        ModalBottomSheet(onDismissRequest = { showFilters = false }, sheetState = rememberModalBottomSheetState()) {
            AppFilterSheetContent(
                users = users.map { AppFilterUserOption(it, state.inventory?.usersMap?.get(it) ?: it.toString()) },
                selectedUserId = filters.userId ?: users.firstOrNull() ?: 0,
                sortsType = filters.sortsType,
                sortsSequence = filters.sortsSequence,
                selectedFirst = filters.selectedFirst,
                filtersUserApps = filters.filtersUserApps,
                filtersSystemApps = filters.filtersSystemApps,
                onUserClick = { viewModel.selectUser(it.id) },
                onSequenceClick = viewModel::toggleSequence,
                onSelectedFirstClick = viewModel::toggleSelectedFirst,
                onSortTypeClick = viewModel::selectSortType,
                onFilterUserAppsClick = viewModel::toggleUserApps,
                onFilterSystemAppsClick = viewModel::toggleSystemApps,
            )
        }
    }
}
