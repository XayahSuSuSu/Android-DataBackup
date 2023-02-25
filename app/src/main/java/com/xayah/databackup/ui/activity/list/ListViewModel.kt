package com.xayah.databackup.ui.activity.list

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.*
import kotlinx.coroutines.flow.MutableStateFlow

class ListViewModel : ViewModel() {
    val isInitialized by lazy { MutableTransitionState(false) }
    val onManifest by lazy { MutableStateFlow(false) }

    // 搜索
    val searchText by lazy { MutableStateFlow("") }

    // 排序
    val activeSort by lazy { MutableStateFlow(AppListSort.Alphabet) }
    val ascending by lazy { MutableStateFlow(true) }

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
