package com.xayah.databackup.ui.activity.list

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.*
import kotlinx.coroutines.flow.MutableStateFlow

class ListViewModel : ViewModel() {
    val isInitialized = MutableTransitionState(false)
    val onManifest = MutableStateFlow(false)

    // 搜索
    val searchText = MutableStateFlow("")

    // 排序
    val activeSort = MutableStateFlow(AppListSort.Alphabet)
    val ascending = MutableStateFlow(true)

    // 过滤
    val filter = MutableStateFlow(AppListFilter.None)

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
