package com.xayah.databackup.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.list.components.ListBottomSheet
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.components.MediaBackupItem
import com.xayah.databackup.ui.activity.list.components.SearchBar
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.librootservice.RootService
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
    items(
        count = list.size,
        key = {
            list[it].name
        }) { index ->
        MediaBackupItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoBackup = list[index],
            onItemUpdate = onItemUpdate
        )
    }
}

suspend fun onMediaBackupInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().mediaInfoBackupMap.value.isEmpty()) {
        GlobalObject.getInstance().mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap())
    }
    if (viewModel.mediaBackupList.value.isEmpty()) {
        refreshMediaBackupList(viewModel)
        // 当基于基类成员变量排序时, 会导致LazyColumn key重复使用的bug
    }
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onMediaBackupManifest(viewModel: ListViewModel, context: Context) {
    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaBackupList.value) {
                    if (i.selectData) size++
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
fun LazyListScope.onMediaBackupContent(viewModel: ListViewModel) {
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
    viewModel: ListViewModel,
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
                        this.data = true
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
    viewModel: ListViewModel,
    context: Context,
    explorer: MaterialYouFileExplorer
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val filter = viewModel.filter.collectAsState()

    ListBottomSheet(
        isOpen = isOpen,
        actions = {
            item {
                Column(modifier = Modifier
                    .clip(RoundedCornerShape(smallPadding))
                    .clickable {
                        onMediaBackupAdd(
                            viewModel = viewModel,
                            context = context,
                            explorer = explorer
                        )
                        isOpen.value = false
                    }
                    .padding(smallPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tinyPadding)
                ) {
                    Icon(
                        modifier = Modifier.size(iconSmallSize),
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.add),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                var selectAll = remember { true }
                Column(modifier = Modifier
                    .clip(RoundedCornerShape(smallPadding))
                    .clickable {
                        viewModel.mediaBackupList.value.forEach {
                            it.selectData = selectAll
                        }
                        selectAll = selectAll.not()
                    }
                    .padding(smallPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tinyPadding)
                ) {
                    Icon(
                        modifier = Modifier.size(iconSmallSize),
                        imageVector = ImageVector.vectorResource(
                            id = R.drawable.ic_round_done_all
                        ),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.select_all),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        content = {
            // 过滤
            Text(
                modifier = Modifier.padding(nonePadding, mediumPadding, nonePadding, nonePadding),
                text = stringResource(id = R.string.filter),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                FilterChip(
                    selected = filter.value == AppListFilter.None,
                    onClick = {
                        if (viewModel.filter.value != AppListFilter.None) {
                            viewModel.filter.value = AppListFilter.None
                            refreshMediaBackupList(viewModel)
                        }
                    },
                    label = { Text(stringResource(R.string.none)) },
                    leadingIcon = if (filter.value == AppListFilter.None) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
                FilterChip(
                    selected = filter.value == AppListFilter.Selected,
                    onClick = {
                        if (viewModel.filter.value != AppListFilter.Selected) {
                            viewModel.filter.value = AppListFilter.Selected
                            refreshMediaBackupList(viewModel)
                        }
                    },
                    label = { Text(stringResource(R.string.selected)) },
                    leadingIcon = if (filter.value == AppListFilter.Selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
                FilterChip(
                    selected = filter.value == AppListFilter.NotSelected,
                    onClick = {
                        if (viewModel.filter.value != AppListFilter.NotSelected) {
                            viewModel.filter.value = AppListFilter.NotSelected
                            refreshMediaBackupList(viewModel)
                        }
                    },
                    label = { Text(stringResource(R.string.not_selected)) },
                    leadingIcon = if (filter.value == AppListFilter.NotSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    )
}

fun filterMediaBackupNone(
    viewModel: ListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter(predicate)
    )
}

fun filterMediaBackupSelected(
    viewModel: ListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter { it.selectData }
            .filter(predicate)
    )
}

fun filterMediaBackupNotSelected(
    viewModel: ListViewModel,
    predicate: (MediaInfoBackup) -> Boolean
) {
    viewModel.mediaBackupList.value.clear()
    viewModel.mediaBackupList.value.addAll(
        GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
            .filter { it.selectData.not() }
            .filter(predicate)
    )
}

fun filterMediaBackup(
    viewModel: ListViewModel,
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
    }
}

fun refreshMediaBackupList(viewModel: ListViewModel) {
    filterMediaBackup(viewModel)
}
