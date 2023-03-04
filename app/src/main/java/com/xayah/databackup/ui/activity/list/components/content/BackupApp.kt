package com.xayah.databackup.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.blacklist.BlackListActivity
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.list.components.FilterItem
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.components.SortItem
import com.xayah.databackup.ui.activity.list.components.item.AppBackupItem
import com.xayah.databackup.ui.activity.list.components.manifest.contentManifest
import com.xayah.databackup.ui.activity.list.components.menu.ListBottomSheet
import com.xayah.databackup.ui.activity.list.components.menu.item.FilterItem
import com.xayah.databackup.ui.activity.list.components.menu.item.SortItem
import com.xayah.databackup.ui.activity.list.components.menu.top.MenuTopActionButton
import com.xayah.databackup.ui.activity.list.components.menu.top.MenuTopBackupUserButton
import com.xayah.databackup.ui.activity.list.components.menu.top.MenuTopRestoreUserButton
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.ui.components.ConfirmDialog
import com.xayah.databackup.ui.components.SearchBar
import com.xayah.databackup.util.*
import java.text.Collator
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentAppBackup(
    list: List<AppInfoBackup>,
    onSearch: (String) -> Unit,
    onItemUpdate: () -> Unit
) {
    item {
        SearchBar(onSearch)
    }
    items(items = list, key = { it.detailBase.packageName }) {
        AppBackupItem(
            modifier = Modifier.animateItemPlacement(),
            appInfoBackup = it,
            onItemUpdate = onItemUpdate
        )
    }
}

suspend fun onAppBackupInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().appInfoBackupMap.value.isEmpty()) {
        GlobalObject.getInstance().appInfoBackupMap.emit(Command.getAppInfoBackupMap())
    }
    if (viewModel.appBackupList.value.isEmpty()) {
        refreshAppBackupList(viewModel)
    }
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onAppBackupManifest(viewModel: ListViewModel, context: Context) {
    // 重置列表, 否则Manifest可能和Processing有所出入
    viewModel.searchText.value = ""
    viewModel.filter.value = AppListFilter.Selected
    viewModel.type.value = AppListType.None
    refreshAppBackupList(viewModel)

    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_app),
            subtitle = run {
                var size = 0
                for (i in viewModel.appBackupList.value) {
                    if (i.selectApp.value) size++
                }
                size.toString()
            },
            iconId = R.drawable.ic_round_apps
        ),
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.appBackupList.value) {
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
            subtitle = context.readCompressionType(),
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
fun LazyListScope.onAppBackupContent(viewModel: ListViewModel) {
    contentAppBackup(
        list = viewModel.appBackupList.value,
        onSearch = { value ->
            viewModel.searchText.value = value
            refreshAppBackupList(viewModel)
        },
        onItemUpdate = {
            refreshAppBackupList(viewModel)
        })
}

@ExperimentalMaterial3Api
fun toAppBackupProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeBackupApp)
    })
}

suspend fun onAppBackupMapSave() {
    GsonUtil.saveAppInfoBackupMapToFile(GlobalObject.getInstance().appInfoBackupMap.value)
}

@ExperimentalMaterial3Api
@Composable
fun AppBackupBottomSheet(
    isOpen: MutableState<Boolean>,
    viewModel: ListViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active = viewModel.activeSort.collectAsState()
    val ascending = viewModel.ascending.collectAsState()

    ListBottomSheet(
        isOpen = isOpen,
        actions = {
            item {
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_blacklist),
                    title = stringResource(R.string.blacklist)
                ) {
                    context.startActivity(Intent(context, BlackListActivity::class.java))
                    onFinish()
                }
            }
            item {
                var selectApp = remember { true }
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_check),
                    title = stringResource(R.string.select_all)
                ) {
                    viewModel.appBackupList.value.forEach {
                        it.selectApp.value = selectApp
                    }
                    selectApp = selectApp.not()
                }
            }
            item {
                var selectAll = remember { true }
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_done_all),
                    title = stringResource(R.string.select_all)
                ) {
                    viewModel.appBackupList.value.forEach {
                        it.selectApp.value = selectAll
                        it.selectData.value = selectAll
                    }
                    selectAll = selectAll.not()
                }
            }
            item {
                MenuTopBackupUserButton(viewModel = viewModel) {
                    onAppBackupInitialize(viewModel)
                }
            }
            item {
                MenuTopRestoreUserButton(viewModel = viewModel)
            }
        },
        content = {
            // 排序
            val sortList = listOf(
                SortItem(
                    text = stringResource(id = R.string.alphabet),
                    type = AppListSort.Alphabet,
                ),
                SortItem(
                    text = stringResource(id = R.string.install_time),
                    type = AppListSort.FirstInstallTime,
                ),
                SortItem(
                    text = stringResource(id = R.string.data_size),
                    type = AppListSort.DataSize,
                ),
            )
            SortItem(list = sortList, active = active, ascending = ascending, onClick = {
                viewModel.setActiveSort(context, it)
                viewModel.setAscending(context)
                refreshAppBackupList(viewModel)
            })

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
                    refreshAppBackupList(viewModel)
                }
            )

            // 类型
            val typeList = listOf(
                FilterItem(
                    text = stringResource(R.string.none),
                    AppListType.None
                ),
                FilterItem(
                    text = stringResource(R.string.installed_app),
                    AppListType.InstalledApp
                ),
                FilterItem(
                    text = stringResource(R.string.system_app),
                    AppListType.SystemApp
                ),
            )
            val isDialogOpen = remember {
                mutableStateOf(false)
            }
            ConfirmDialog(
                isOpen = isDialogOpen,
                icon = Icons.Rounded.Warning,
                title = stringResource(R.string.warning),
                content = {
                    Text(
                        text = stringResource(R.string.switch_to_system_app_warning)
                                + stringResource(id = R.string.symbol_exclamation)
                    )
                },
                cancelable = false
            ) {
                refreshAppBackupList(viewModel)
            }
            FilterItem(
                title = stringResource(id = R.string.type),
                list = typeList,
                filter = viewModel.type,
                onClick = {
                    when (it) {
                        AppListType.None, AppListType.SystemApp -> {
                            isDialogOpen.value = true
                        }
                        else -> {
                            refreshAppBackupList(viewModel)
                        }
                    }
                }
            )
        }
    )
}

fun sortAppBackupByAlphabet(
    viewModel: ListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appBackupList.value.sortWith { appInfo1, appInfo2 ->
            if (appInfo1 == null && appInfo2 == null) {
                0
            } else if (appInfo1 == null) {
                -1
            } else if (appInfo2 == null) {
                1
            } else {
                val collator = Collator.getInstance(Locale.CHINA)
                collator.getCollationKey(appInfo1.detailBase.appName)
                    .compareTo(collator.getCollationKey(appInfo2.detailBase.appName))
            }
        }
    else
        viewModel.appBackupList.value.sortWith { appInfo1, appInfo2 ->
            if (appInfo1 == null && appInfo2 == null) {
                0
            } else if (appInfo1 == null) {
                -1
            } else if (appInfo2 == null) {
                1
            } else {
                val collator = Collator.getInstance(Locale.CHINA)
                collator.getCollationKey(appInfo2.detailBase.appName)
                    .compareTo(collator.getCollationKey(appInfo1.detailBase.appName))
            }
        }
}

fun sortAppBackupByInstallTime(
    viewModel: ListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appBackupList.value.sortBy { it.firstInstallTime }
    else
        viewModel.appBackupList.value.sortByDescending { it.firstInstallTime }
}

fun sortAppBackupByDataSize(
    viewModel: ListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appBackupList.value.sortBy { it.sizeBytes }
    else
        viewModel.appBackupList.value.sortByDescending { it.sizeBytes }
}

fun sortAppBackup(viewModel: ListViewModel) {
    when (viewModel.activeSort.value) {
        AppListSort.Alphabet -> {
            sortAppBackupByAlphabet(viewModel, viewModel.ascending.value)
        }
        AppListSort.FirstInstallTime -> {
            sortAppBackupByInstallTime(viewModel, viewModel.ascending.value)
        }
        AppListSort.DataSize -> {
            sortAppBackupByDataSize(viewModel, viewModel.ascending.value)
        }
    }
}

fun filterAppBackupNone(
    viewModel: ListViewModel,
    predicate: (AppInfoBackup) -> Boolean
) {
    viewModel.appBackupList.value.clear()
    viewModel.appBackupList.value.addAll(
        GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
            .filter { it.isOnThisDevice && filterTypePredicateAppBackup(viewModel.type.value, it) }
            .filter(predicate)
    )
}

fun filterAppBackupSelected(
    viewModel: ListViewModel,
    predicate: (AppInfoBackup) -> Boolean
) {
    viewModel.appBackupList.value.clear()
    viewModel.appBackupList.value.addAll(
        GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
            .filter {
                it.isOnThisDevice
                        && filterTypePredicateAppBackup(viewModel.type.value, it)
                        && (it.selectApp.value || it.selectData.value)
            }
            .filter(predicate)
    )
}

fun filterAppBackupNotSelected(
    viewModel: ListViewModel,
    predicate: (AppInfoBackup) -> Boolean
) {
    viewModel.appBackupList.value.clear()
    viewModel.appBackupList.value.addAll(
        GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
            .filter {
                it.isOnThisDevice
                        && filterTypePredicateAppBackup(viewModel.type.value, it)
                        && (it.selectApp.value .not() && it.selectData.value.not())
            }
            .filter(predicate)
    )
}

fun filterAppBackup(
    viewModel: ListViewModel,
    predicate: (AppInfoBackup) -> Boolean = {
        val value = viewModel.searchText.value
        it.detailBase.appName.lowercase()
            .contains(value.lowercase()) ||
                it.detailBase.packageName.lowercase()
                    .contains(value.lowercase())
    }
) {
    when (viewModel.filter.value) {
        AppListFilter.None -> {
            filterAppBackupNone(viewModel, predicate)
        }
        AppListFilter.Selected -> {
            filterAppBackupSelected(viewModel, predicate)
        }
        AppListFilter.NotSelected -> {
            filterAppBackupNotSelected(viewModel, predicate)
        }
        else -> {
            viewModel.filter.value = AppListFilter.None
            filterAppBackupNone(viewModel, predicate)
        }
    }
}

fun filterTypePredicateAppBackup(type: AppListType, appInfoBackup: AppInfoBackup): Boolean {
    return when (type) {
        AppListType.InstalledApp -> {
            appInfoBackup.detailBase.isSystemApp.not()
        }
        AppListType.SystemApp -> {
            appInfoBackup.detailBase.isSystemApp
        }
        AppListType.None -> {
            true
        }
    }
}

fun refreshAppBackupList(viewModel: ListViewModel) {
    filterAppBackup(viewModel)
    sortAppBackup(viewModel)
}