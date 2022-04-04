package com.xayah.databackup.data

class Log {
    private val logs = mutableListOf<String>()

    fun add(line: String) {
        logs.add(line)
    }

    fun clear() {
        logs.clear()
    }

    override fun toString(): String {
        return logs.joinToString(separator = "\n")
    }
}