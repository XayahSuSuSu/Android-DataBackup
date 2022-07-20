package com.xayah.databackup.util

import androidx.lifecycle.MutableLiveData

class Logcat {
    private val logs = MutableLiveData(mutableListOf<String>())

    fun add(line: String) {
        logs.postValue(logs.value?.apply {
            if (line.isNotEmpty()) add(line)
        })
    }

    fun clear() {
        logs.postValue(logs.value?.apply {
            clear()
        })
    }

    override fun toString(): String {
        return logs.value?.joinToString(separator = "\n") ?: ""
    }
}