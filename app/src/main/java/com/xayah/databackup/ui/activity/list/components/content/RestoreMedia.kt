package com.xayah.databackup.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
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
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.list.components.ListBottomSheet
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.components.MediaRestoreItem
import com.xayah.databackup.ui.activity.list.components.SearchBar
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentMediaRestore(list: List<MediaInfoRestore>, onSearch: (String) -> Unit) {
    item {
        SearchBar(onSearch)
    }
    items(
        count = list.size,
    ) { index ->
        MediaRestoreItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoRestore = list[index]
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
    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaRestoreList.value) {
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
fun LazyListScope.onMediaRestoreContent(viewModel: ListViewModel) {
    contentMediaRestore(list = viewModel.mediaRestoreList.value) { value ->
        viewModel.searchText.value = value
        refreshMediaRestoreList(viewModel)
    }
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
                var selectAll = remember { true }
                Column(modifier = Modifier
                    .clip(RoundedCornerShape(smallPadding))
                    .clickable {
                        viewModel.mediaRestoreList.value.forEach {
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
                            refreshMediaRestoreList(viewModel)
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
                            refreshMediaRestoreList(viewModel)
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
                            refreshMediaRestoreList(viewModel)
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
            .filter { it.selectData }
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
            .filter { it.selectData.not() }
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
    }
}

fun refreshMediaRestoreList(viewModel: ListViewModel) {
    filterMediaRestore(viewModel)
}