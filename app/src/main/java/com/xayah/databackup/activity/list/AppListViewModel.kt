package com.xayah.databackup.activity.list

import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter

class AppListViewModel : ViewModel() {
    val mAdapter = MultiTypeAdapter()
    val backup = Backup(mAdapter)

    fun initialize(onInitialized: () -> Unit) {
        backup.initialize(onInitialized)
    }

    fun saveAppList() {
        backup.saveAppList()
    }
}