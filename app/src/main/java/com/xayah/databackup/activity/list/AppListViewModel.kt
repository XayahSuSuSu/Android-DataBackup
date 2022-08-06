package com.xayah.databackup.activity.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter

class AppListViewModel : ViewModel() {
    val _isInitialized by lazy {
        MutableLiveData(false)
    }
    var isInitialized
        get() = _isInitialized.value!!
        set(value) = _isInitialized.postValue(value)

    // 是否第一次进入Activity
    private val _isFirst by lazy {
        MutableLiveData(true)
    }
    var isFirst
        get() = _isFirst.value!!
        set(value) = _isFirst.postValue(value)

    val mAdapter = MultiTypeAdapter()
    lateinit var backup: Backup
    lateinit var restore: Restore
    var onPause = {}
    var onResume = {}

    fun initialize(isRestore: Boolean) {
        if (isRestore) {
            restore = Restore(this)
        } else {
            backup = Backup(this)
        }
    }
}