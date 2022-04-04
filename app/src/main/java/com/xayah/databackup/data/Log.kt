package com.xayah.databackup.data

class Log {
    private val logs = mutableListOf<String>()
    fun add(line: String) {
        logs.add(line)
    }
}