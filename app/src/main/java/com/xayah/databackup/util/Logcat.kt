package com.xayah.databackup.util

import com.xayah.databackup.App

class Logcat {
    object Instance {
        val instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    private val logPath = "${Path.getShellLogPath()}/log_${App.getTimeStamp()}"

    fun addLine(line: String) {
        if (line.isNotEmpty()) {
            SafeFile.create(logPath) {
                it.apply {
                    appendText(line)
                    appendText("\n")
                }
            }
        }
    }
}