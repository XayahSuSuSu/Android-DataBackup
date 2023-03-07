package com.xayah.databackup.ui.activity.list

import android.content.Context
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.data.*
import com.xayah.databackup.util.readListActiveSort
import com.xayah.databackup.util.readListSortAscending
import com.xayah.databackup.util.saveListActiveSort
import com.xayah.databackup.util.saveListSortAscending
import kotlinx.coroutines.flow.MutableStateFlow

class ListViewModel : ViewModel() {
    val isInitialized by lazy { MutableTransitionState(false) }
    val progress by lazy { mutableStateOf(0f) }
    val onManifest by lazy { MutableStateFlow(false) }

    // 搜索
    val searchText by lazy { MutableStateFlow("") }

    // 排序
    val activeSort by lazy { MutableStateFlow(App.globalContext.readListActiveSort()) }
    val ascending by lazy { MutableStateFlow(App.globalContext.readListSortAscending()) }

    fun setActiveSort(context: Context, value: AppListSort) {
        activeSort.value = value
        context.saveListActiveSort(value)
    }

    fun setAscending(context: Context) {
        ascending.value = ascending.value.not()
        context.saveListSortAscending(ascending.value)
    }

    // 过滤
    val filter by lazy { MutableStateFlow(AppListFilter.None) }

    // 类型
    val type by lazy { MutableStateFlow(AppListType.InstalledApp) }

    // 备份应用列表
    val appBackupList by lazy {
        MutableStateFlow(SnapshotStateList<AppInfoBackup>())
    }

    // 备份媒体列表
    val mediaBackupList by lazy {
        MutableStateFlow(SnapshotStateList<MediaInfoBackup>())
    }

    // 恢复应用列表
    val appRestoreList by lazy {
        MutableStateFlow(SnapshotStateList<AppInfoRestore>())
    }

    // 恢复媒体列表
    val mediaRestoreList by lazy {
        MutableStateFlow(SnapshotStateList<MediaInfoRestore>())
    }
}
