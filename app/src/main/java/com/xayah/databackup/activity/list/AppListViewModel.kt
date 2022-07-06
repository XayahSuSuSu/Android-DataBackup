package com.xayah.databackup.activity.list

import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter

class AppListViewModel : ViewModel() {
    val mAdapter = MultiTypeAdapter()
    lateinit var backup: Backup
    lateinit var restore: Restore
    var isRestore = false

    fun initialize(mIsRestore: Boolean, onInitialized: () -> Unit) {
        isRestore = mIsRestore
        if (isRestore)
            restore = Restore(mAdapter).apply {
                initialize(onInitialized)
            }
        else
            backup = Backup(mAdapter).apply {
                initialize(onInitialized)
            }
    }

    fun saveAppList() {
        if (isRestore)
            restore.saveAppList()
        else
            backup.saveAppList()
    }
}