package com.xayah.databackup.ui.activity.list.common.components.content

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.list.common.CommonListViewModel
import com.xayah.databackup.ui.activity.list.common.components.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.common.components.item.MediaBackupItem
import com.xayah.databackup.ui.activity.list.common.components.manifest.contentManifest
import com.xayah.databackup.ui.activity.list.common.components.menu.ListBottomSheet
import com.xayah.databackup.ui.activity.list.common.components.menu.item.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopActionButton
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopBackupUserButton
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopRestoreUserButton
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.ui.components.SearchBar
import com.xayah.databackup.util.*
import com.xayah.databackup.util.command.Command
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentRestoreBackup(
    list: List<MediaInfoBackup>,
    onSearch: (String) -> Unit,
    onItemUpdate: () -> Unit
) {
    item {
        SearchBar(onSearch)
    }
    items(items = list, key = { it.name }) {
        MediaBackupItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoBackup = it,
            onItemUpdate = onItemUpdate
        )
    }
}

suspend fun onMediaBackupInitialize(viewModel: CommonListViewModel) {
    if (GlobalObject.getInstance().mediaInfoBackupMap.value.isEmpty()) {
        GlobalObject.getInstance().mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap {
            viewModel.progress.value = it
        })
    }
    if (viewModel.mediaBackupList.value.isEmpty()) {
        refreshMediaBackupList(viewModel)
        // 当基于基类成员变量排序时, 会导致LazyColumn key重复使用的bug
    }
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onMediaBackupManifest(viewModel: CommonListViewModel, context: Context) {
    // 重置列表, 否则Manifest可能和Processing有所出入
    viewModel.searchText.value = ""
    viewModel.filter.value = AppListFilter.Selected
    refreshMediaBackupList(viewModel)

    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaBackupList.value) {
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
fun LazyListScope.onMediaBackupContent(viewModel: CommonListViewModel) {
    contentRestoreBackup(
        list = viewModel.mediaBackupList.value,
        onSearch = { value ->
            viewModel.searchText.value = value
            refreshMediaBackupList(viewModel)
        },
        onItemUpdate = {
            refreshMediaBackupList(viewModel)
        })
}

@ExperimentalMaterial3Api
fun toMediaBackupProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeBackupMedia)
    })
}

suspend fun onMediaBackupMapSave() {
    GsonUtil.saveMediaInfoBackupMapToFile(GlobalObject.getInstance().mediaInfoBackupMap.value)
}

fun onMediaBackupAdd(
    viewModel: CommonListViewModel,
    context: Context,
    explorer: MaterialYouFileExplorer
) {
    explorer.apply {
        isFile = false
        toExplorer(context) { path, _ ->
            viewModel.viewModelScope.launch {
                // 判断是否为备份目录
                if (path == context.readBackupSavePath()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.backup_dir_as_media_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                var name = path.split("/").last()
                for (i in viewModel.mediaBackupList.value) {
                    if (path == i.path) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.repeat_to_add),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    if (name == i.name) {
                        // 重名媒体资料
                        val nameList = name.split("_").toMutableList()
                        val index = nameList.last().toIntOrNull()
                        if (index == null) {
                            nameList.add("0")
                        } else {
                            nameList[nameList.lastIndex] = (index + 1).toString()
                        }
                        name = nameList.joinToString(separator = "_")
                    }
                }
                val mediaInfo = MediaInfoBackup().apply {
                    this.name = name
                    this.path = path
                    this.backupDetail.apply {
                        this.data.value = true
                        this.size = ""
                        this.date = ""
                    }
                    this.storageStats.dataBytes = RootService.getInstance().countSize(this.path)
                }
                viewModel.mediaBackupList.value.add(mediaInfo)
                GlobalObject.getInstance().mediaInfoBackupMap.value[mediaInfo.name] = mediaInfo
                GsonUtil.saveMediaInfoBackupMapToFile(GlobalObject.getInstance().mediaInfoBackupMap.value)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun MediaBackupBottomSheet(
    isOpen: MutableState<Boolean>,
    viewModel: CommonListViewModel,
    context: Context,
    explorer: MaterialYouFileExplorer
) {
    val scope = rememberCoroutineScope()
    ListBottomSheet(
        isOpen = isOpen,
        actions = {
            item {
                MenuTopActionButton(
                    icon = Icons.Rounded.Add,
                    title = stringResource(R.string.add)
                ) {
                    onMediaBackupAdd(
                        viewModel = viewModel,
                        context = context,
                        explorer = explorer
                    )
                    isOpen.value = false
                }
            }
            item {
                var selectAll = remember { true }
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_done_all),
                    title = stringResource(R.string.select_all)
                ) {
                    viewModel.mediaBackupList.value.forEach {
                        it.selectData.value = selectAll
                    }
                    selectAll = selectAll.not()
                }
            }
            item {
                MenuTopBackupUserButton(viewModel = viewModel) {
                    onMediaBackupInitialize(viewModel)
                }
            }
            item {
                MenuTopRestoreUserButton(viewModel = viewModel)
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
                    refreshMediaBackupList(viewModel)
                }
            )
        }
    )
}

fun filterMediaBackupNone(
    viewModel: CommonListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter(predicate)
    )
}

fun filterMediaBackupSelected(
    viewModel: CommonListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter { it.selectData.value }
            .filter(predicate)
    )
}

fun filterMediaBackupNotSelected(
    viewModel: CommonListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter { it.selectData.value.not() }
            .filter(predicate)
    )
}

fun filterMediaBackup(
    viewModel: CommonListViewModel,
    predicate: (MediaInfoBackup) -> Boolean = {
        val value = viewModel.searchText.value
        it.name.lowercase()
            .contains(value.lowercase()) ||
                it.path.lowercase()
                    .contains(value.lowercase())
    }
) {
    when (viewModel.filter.value) {
        AppListFilter.None -> {
            filterMediaBackupNone(viewModel, predicate)
        }
        AppListFilter.Selected -> {
            filterMediaBackupSelected(viewModel, predicate)
        }
        AppListFilter.NotSelected -> {
            filterMediaBackupNotSelected(viewModel, predicate)
        }
        else -> {
            viewModel.filter.value = AppListFilter.None
            filterMediaBackupNone(viewModel, predicate)
        }
    }
}

fun refreshMediaBackupList(viewModel: CommonListViewModel) {
    filterMediaBackup(viewModel)
}
