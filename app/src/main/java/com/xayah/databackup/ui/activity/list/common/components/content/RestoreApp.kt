package com.xayah.databackup.ui.activity.list.common.components.content

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.*
import com.xayah.databackup.ui.activity.list.common.CommonListViewModel
import com.xayah.databackup.ui.activity.list.common.components.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.common.components.SortItem
import com.xayah.databackup.ui.activity.list.common.components.item.AppRestoreItem
import com.xayah.databackup.ui.activity.list.common.components.item.deleteAppInfoRestoreItem
import com.xayah.databackup.ui.activity.list.common.components.manifest.contentManifest
import com.xayah.databackup.ui.activity.list.common.components.menu.ListBottomSheet
import com.xayah.databackup.ui.activity.list.common.components.menu.item.FilterItem
import com.xayah.databackup.ui.activity.list.common.components.menu.item.SortItem
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopActionButton
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopBackupUserButton
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopBatchDeleteButton
import com.xayah.databackup.ui.activity.list.common.components.menu.top.MenuTopRestoreUserButton
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.ui.components.LoadingDialog
import com.xayah.databackup.ui.components.SearchBar
import com.xayah.databackup.util.*
import com.xayah.databackup.util.command.Command
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentAppRestore(
    list: List<AppInfoRestore>,
    onSearch: (String) -> Unit,
    onItemUpdate: () -> Unit
) {
    item {
        SearchBar(onSearch)
    }
    items(items = list, key = { it.detailBase.packageName }) {
        AppRestoreItem(
            modifier = Modifier.animateItemPlacement(),
            appInfoRestore = it,
            onItemUpdate = onItemUpdate
        )
    }
}

suspend fun onAppRestoreInitialize(viewModel: CommonListViewModel) {
    if (GlobalObject.getInstance().appInfoRestoreMap.value.isEmpty()) {
        GlobalObject.getInstance().appInfoRestoreMap.emit(Command.getAppInfoRestoreMap {
            viewModel.progress.value = it
        })
    }
    if (viewModel.appRestoreList.value.isEmpty()) {
        refreshAppRestoreList(viewModel)
    }
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onAppRestoreManifest(viewModel: CommonListViewModel, context: Context) {
    // 重置列表, 否则Manifest可能和Processing有所出入
    viewModel.searchText.value = ""
    viewModel.filter.value = AppListFilter.Selected
    viewModel.type.value = AppListType.None
    refreshAppRestoreList(viewModel)

    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_app),
            subtitle = run {
                var size = 0
                for (i in viewModel.appRestoreList.value) {
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
                for (i in viewModel.appRestoreList.value) {
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
fun LazyListScope.onAppRestoreContent(viewModel: CommonListViewModel) {
    contentAppRestore(
        list = viewModel.appRestoreList.value,
        onSearch = { value ->
            viewModel.searchText.value = value
            refreshAppRestoreList(viewModel)
        },
        onItemUpdate = {
            refreshAppRestoreList(viewModel)
        }
    )
}

@ExperimentalMaterial3Api
fun toAppRestoreProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeRestoreApp)
    })
}

suspend fun onAppRestoreMapSave() {
    GsonUtil.saveAppInfoRestoreMapToFile(GlobalObject.getInstance().appInfoRestoreMap.value)
}

@ExperimentalMaterial3Api
@Composable
fun AppRestoreBottomSheet(
    isOpen: MutableState<Boolean>,
    viewModel: CommonListViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active = viewModel.activeSort.collectAsState()
    val ascending = viewModel.ascending.collectAsState()

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
                    viewModel.appRestoreList.collectAsState().value.filter { it.selectApp.value || it.selectData.value }
                MenuTopBatchDeleteButton(
                    isOpen = isConfirmDialogOpen,
                    selectedItems = selectedItems,
                    itemText = {
                        "${selectedItems[it].detailBase.appName} ${selectedItems[it].detailBase.packageName}"
                    }) {
                    scope.launch {
                        isLoadingDialogOpen.value = true
                        for (i in selectedItems) {
                            deleteAppInfoRestoreItem(i) {}
                        }
                        refreshAppRestoreList(viewModel)
                        isLoadingDialogOpen.value = false
                        isOpen.value = false
                    }
                }
            }
            item {
                var selectApp = remember { true }
                MenuTopActionButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_check),
                    title = stringResource(R.string.select_all)
                ) {
                    viewModel.appRestoreList.value.forEach {
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
                    viewModel.appRestoreList.value.forEach {
                        it.selectApp.value = selectAll
                        it.selectData.value = selectAll
                    }
                    selectAll = selectAll.not()
                }
            }
            item {
                MenuTopBackupUserButton(viewModel = viewModel) {
                    onAppRestoreInitialize(viewModel)
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
                refreshAppRestoreList(viewModel)
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
                ),
                FilterItem(
                    text = stringResource(R.string.installed),
                    AppListFilter.Installed
                ),
                FilterItem(
                    text = stringResource(R.string.not_installed),
                    AppListFilter.NotInstalled
                )
            )
            FilterItem(
                title = stringResource(id = R.string.filter),
                list = filterList,
                filter = viewModel.filter,
                onClick = {
                    refreshAppRestoreList(viewModel)
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
            FilterItem(
                title = stringResource(id = R.string.type),
                list = typeList,
                filter = viewModel.type,
                onClick = {
                    refreshAppRestoreList(viewModel)
                }
            )
        }
    )
}

fun sortAppRestoreByAlphabet(
    viewModel: CommonListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appRestoreList.value.sortWith { appInfo1, appInfo2 ->
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
        viewModel.appRestoreList.value.sortWith { appInfo1, appInfo2 ->
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

fun sortAppRestoreByInstallTime(
    viewModel: CommonListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appRestoreList.value.sortBy { it.firstInstallTime }
    else
        viewModel.appRestoreList.value.sortByDescending { it.firstInstallTime }
}

fun sortAppRestoreByDataSize(
    viewModel: CommonListViewModel,
    ascending: Boolean
) {
    if (ascending)
        viewModel.appRestoreList.value.sortBy { it.sizeBytes }
    else
        viewModel.appRestoreList.value.sortByDescending { it.sizeBytes }
}

fun sortAppRestore(viewModel: CommonListViewModel) {
    when (viewModel.activeSort.value) {
        AppListSort.Alphabet -> {
            sortAppRestoreByAlphabet(viewModel, viewModel.ascending.value)
        }
        AppListSort.FirstInstallTime -> {
            sortAppRestoreByInstallTime(viewModel, viewModel.ascending.value)
        }
        AppListSort.DataSize -> {
            sortAppRestoreByDataSize(viewModel, viewModel.ascending.value)
        }
    }
}

fun filterAppRestoreNone(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean
) {
    viewModel.appRestoreList.value.clear()
    viewModel.appRestoreList.value.addAll(
        GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter { filterTypePredicateAppRestore(viewModel.type.value, it) }
            .filter(predicate)
    )
}

fun filterAppRestoreSelected(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean
) {
    viewModel.appRestoreList.value.clear()
    viewModel.appRestoreList.value.addAll(
        GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter {
                filterTypePredicateAppRestore(viewModel.type.value, it)
                        && (it.selectApp.value || it.selectData.value)
            }
            .filter(predicate)
    )
}

fun filterAppRestoreNotSelected(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean
) {
    viewModel.appRestoreList.value.clear()
    viewModel.appRestoreList.value.addAll(
        GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter {
                filterTypePredicateAppRestore(viewModel.type.value, it)
                        && it.selectApp.value.not() && it.selectData.value.not()
            }
            .filter(predicate)
    )
}

fun filterAppRestoreInstalled(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean
) {
    viewModel.appRestoreList.value.clear()
    viewModel.appRestoreList.value.addAll(
        GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter {
                filterTypePredicateAppRestore(viewModel.type.value, it)
                        && it.isOnThisDevice.value
            }
            .filter(predicate)
    )
}

fun filterAppRestoreNotInstalled(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean
) {
    viewModel.appRestoreList.value.clear()
    viewModel.appRestoreList.value.addAll(
        GlobalObject.getInstance().appInfoRestoreMap.value.values.toList()
            .filter {
                filterTypePredicateAppRestore(viewModel.type.value, it)
                        && it.isOnThisDevice.value.not()
            }
            .filter(predicate)
    )
}

fun filterAppRestore(
    viewModel: CommonListViewModel,
    predicate: (AppInfoRestore) -> Boolean = {
        val value = viewModel.searchText.value
        it.detailBase.appName.lowercase()
            .contains(value.lowercase()) ||
                it.detailBase.packageName.lowercase()
                    .contains(value.lowercase())
    }
) {
    when (viewModel.filter.value) {
        AppListFilter.None -> {
            filterAppRestoreNone(viewModel, predicate)
        }
        AppListFilter.Selected -> {
            filterAppRestoreSelected(viewModel, predicate)
        }
        AppListFilter.NotSelected -> {
            filterAppRestoreNotSelected(viewModel, predicate)
        }
        AppListFilter.Installed -> {
            filterAppRestoreInstalled(viewModel, predicate)
        }
        AppListFilter.NotInstalled -> {
            filterAppRestoreNotInstalled(viewModel, predicate)
        }
    }
}

fun filterTypePredicateAppRestore(type: AppListType, appInfoRestore: AppInfoRestore): Boolean {
    return when (type) {
        AppListType.InstalledApp -> {
            appInfoRestore.detailBase.isSystemApp.not()
        }
        AppListType.SystemApp -> {
            appInfoRestore.detailBase.isSystemApp
        }
        AppListType.None -> {
            true
        }
    }
}

fun refreshAppRestoreList(viewModel: CommonListViewModel) {
    filterAppRestore(viewModel)
    sortAppRestore(viewModel)
}