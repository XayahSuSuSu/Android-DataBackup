package com.xayah.databackup.util

import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.App
import java.io.File
import java.io.IOException

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
            try {
                SuFile(logPath).apply {
                    appendText(line)
                    appendText("\n")
                }
            } catch (g: IOException) {
                g.printStackTrace()
            }
        }
    }
}