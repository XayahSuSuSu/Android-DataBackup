package com.xayah.databackup.util

class Logcat {
    private val logs = mutableListOf<String>()

    fun add(line: String) {
        if (line.isNotEmpty()) logs.add(line)
    }

    fun clear() {
        logs.clear()
    }

    override fun toString(): String {
        return logs.joinToString(separator = "\n")
    }
}