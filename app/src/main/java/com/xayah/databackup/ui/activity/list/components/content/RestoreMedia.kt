package com.xayah.databackup.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.list.components.FilterItem
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.components.item.MediaRestoreItem
import com.xayah.databackup.ui.activity.list.components.item.deleteMediaInfoRestoreItem
import com.xayah.databackup.ui.activity.list.components.manifest.contentManifest
import com.xayah.databackup.ui.activity.list.components.menu.ListBottomSheet
import com.xayah.databackup.ui.activity.list.components.menu.item.FilterItem
import com.xayah.databackup.ui.activity.list.components.menu.top.MenuTopActionButton
import com.xayah.databackup.ui.activity.list.components.menu.top.MenuTopBatchDeleteButton
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.ui.components.LoadingDialog
import com.xayah.databackup.ui.components.SearchBar
import com.xayah.databackup.util.*
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentMediaRestore(
    list: List<MediaInfoRestore>,
    onSearch: (String) -> Unit,
    onItemUpdate: () -> Unit
) {
    item {
        SearchBar(onSearch)
    }
    items(items = list, key = { it.name }) {
        MediaRestoreItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoRestore = it,
            onItemUpdate = onItemUpdate
        )
    }
}

suspend fun onMediaRestoreInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().mediaInfoRestoreMap.value.isEmpty()) {
        GlobalObject.getInstance().mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
    }
    if (viewModel.mediaRestoreList.value.isEmpty()) {
        refreshMediaRestoreList(viewModel)
    }
    // 当基于基类成员变量排序时, 会导致LazyColumn key重复使用的bug
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onMediaRestoreManifest(viewModel: ListViewModel, context: Context) {
    // 重置列表, 否则Manifest可能和Processing有所出入
    viewModel.searchText.value = ""
    viewModel.filter.value = AppListFilter.Selected
    refreshMediaRestoreList(viewModel)

    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaRestoreList.value) {
                    if (i.selectData.value) size++
                }
                size.toString()
            },
            iconId = R.drawable.ic_round_database
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_user),
            subtitle = context.readBackupUser(),
            iconId = R.drawable.ic_round_person
        ),
        ManifestDescItem(
            title = context.getString(R.string.restore_user),
            subtitle = context.readRestoreUser(),
            iconId = R.drawable.ic_round_iphone
        ),
        ManifestDescItem(
            title = context.getString(R.string.compression_type),
            subtitle = "tar",
            iconId = R.drawable.ic_round_bolt
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_strategy),
            subtitle = ofBackupStrategy(context.readBackupStrategy()),
            iconId = -1,
            icon = Icons.Rounded.Place
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_dir),
            subtitle = context.readBackupSavePath(),
            iconId = R.drawable.ic_round_folder_open
        ),
    )
    contentManifest(list)
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.onMediaRestoreContent(viewModel: ListViewModel) {
    contentMediaRestore(list = viewModel.mediaRestoreList.value,
        onSearch = { value ->
            viewModel.searchText.value = value
            refreshMediaRestoreList(viewModel)
        },
        onItemUpdate = {
            refreshMediaRestoreList(viewModel)
        })
}

@ExperimentalMaterial3Api
fun toMediaRestoreProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeRestoreMedia)
    })
}

suspend fun onMediaRestoreMapSave() {
    GsonUtil.saveMediaInfoRestoreMapToFile(GlobalObject.getInstance().mediaInfoRestoreMap.value)
}

@ExperimentalMaterial3Api
@Composable
fun MediaRestoreBottomSheet(
    isOpen: MutableState<Boolean>,
    viewModel: ListViewModel
) {
    val scope = rememberCoroutineScope()

    ListBottomSheet(
        isOpen = isOpen,
        actions = {
            item {
                val isLoadingDialogOpen = remember {
                    mutableStateOf(false)
                }
                LoadingDialog(isOpen = isLoadingDialogOpen)

                val isConfirmDialogOpen = remember {
                    mutableStateOf(false)
                }
                val selectedItems =
                    viewModel.mediaRestoreList.collectAsState().value.filter { it.selectData.value }
                MenuTopBatchDeleteButton(
                    isOpen = isConfirmDialogOpen,
                    selectedItems = selectedItems,
                    itemText = {
                        selectedItems[it].name
                    }) {
                    scope.launch {
                        isLoadingDialogOpen.value = true
                        for (i in selectedItems) {
                            deleteMediaInfoRestoreItem(i) {}
                        }
                        refreshMediaRestoreList(viewModel)
                        isLoadingDialogOpen.value = false
                        isOpen.value = false
                    }
                }
            }
            item {
                var selectAll = remember { true }
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_done_all),
                    title = stringResource(R.string.select_all)
                ) {
                    viewModel.mediaRestoreList.value.forEach {
                        it.selectData.value = selectAll
                    }
                    selectAll = selectAll.not()
                }
            }
        },
        content = {
            // 过滤
            val filterList = listOf(
                FilterItem(
                    text = stringResource(R.string.none),
                    AppListFilter.None
                ),
                FilterItem(
                    text = stringResource(R.string.selected),
                    AppListFilter.Selected
                ),
                FilterItem(
                    text = stringResource(R.string.not_selected),
                    AppListFilter.NotSelected
                )
            )
            FilterItem(
                title = stringResource(id = R.string.filter),
                list = filterList,
                filter = viewModel.filter,
                onClick = {
                    refreshMediaRestoreList(viewModel)
                }
            )
        }
    )
}

fun filterMediaRestoreNone(
    viewModel: ListViewModel,
    predicate: (MediaInfoRestore) -> Boolean
) {
    viewModel.mediaRestoreList.value.clear()
    viewModel.mediaRestoreList.value.addAll(
        GlobalObject.getInstance().mediaInfoRestoreMap.value.values.toList()
            .filter(predicate)
    )
}

fun filterMediaRestoreSelected(
    viewModel: ListViewModel,
    predicate: (MediaInfoRestore) -> Boolean
) {
    viewModel.mediaRestoreList.value.clear()
    viewModel.mediaRestoreList.value.addAll(
        GlobalObject.getInstance().mediaInfoRestoreMap.value.values.toList()
            .filter { it.selectData.value }
            .filter(predicate)
    )
}

fun filterMediaRestoreNotSelected(
    viewModel: ListViewModel,
    predicate: (MediaInfoRestore) -> Boolean
) {
    viewModel.mediaRestoreList.value.clear()
    viewModel.mediaRestoreList.value.addAll(
        GlobalObject.getInstance().mediaInfoRestoreMap.value.values.toList()
            .filter { it.selectData.value.not() }
            .filter(predicate)
    )
}

fun filterMediaRestore(
    viewModel: ListViewModel,
    predicate: (MediaInfoRestore) -> Boolean = {
        val value = viewModel.searchText.value
        it.name.lowercase()
            .contains(value.lowercase()) ||
                it.path.lowercase()
                    .contains(value.lowercase())
    }
) {
    when (viewModel.filter.value) {
        AppListFilter.None -> {
            filterMediaRestoreNone(viewModel, predicate)
        }
        AppListFilter.Selected -> {
            filterMediaRestoreSelected(viewModel, predicate)
        }
        AppListFilter.NotSelected -> {
            filterMediaRestoreNotSelected(viewModel, predicate)
        }
        else -> {
            viewModel.filter.value = AppListFilter.None
            filterMediaRestoreNone(viewModel, predicate)
        }
    }
}

fun refreshMediaRestoreList(viewModel: ListViewModel) {
    filterMediaRestore(viewModel)
}