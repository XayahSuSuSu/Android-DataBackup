package com.xayah.databackup.compose.ui.activity.list

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import kotlinx.coroutines.flow.MutableStateFlow

class ListViewModel : ViewModel() {
    val isInitialized = MutableStateFlow(false)
    val onManifest = MutableStateFlow(false)

    // 备份应用列表
    val appBackupList by lazy {
        MutableStateFlow(SnapshotStateList<AppInfoBackup>())
    }

    // 恢复应用列表
    val appRestoreList by lazy {
        MutableStateFlow(SnapshotStateList<AppInfoRestore>())
    }
}
