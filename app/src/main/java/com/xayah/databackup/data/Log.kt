package com.xayah.databackup.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

class Log {
    private val logs = MutableLiveData(mutableListOf<String>())

    fun add(line: String) {
        logs.postValue(logs.value?.apply {
            add(line)
        })
    }

    fun clear() {
        logs.postValue(logs.value?.apply {
            clear()
        })
    }

    fun onObserveLast(owner: LifecycleOwner, callback: (String) -> Unit) {
        logs.observe(owner) {
            if (it.isNotEmpty())
                callback(it.last())
        }
    }

    override fun toString(): String {
        return logs.value?.joinToString(separator = "\n") ?: ""
    }
}