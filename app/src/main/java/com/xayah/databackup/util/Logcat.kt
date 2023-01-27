package com.xayah.databackup.util

import com.xayah.databackup.App
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class Logcat {
    object Instance {
        val instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    private val logDir = "${Path.getFilesDir()}/log"
    val logPath = "${logDir}/log_${App.getTimeStamp()}"

    init {
        val dir = File(logDir)
        dir.deleteRecursively()
        dir.mkdir()
    }

    fun addLine(line: String) {
        if (line.isNotEmpty()) {
            try {
                val file = File(logPath)
                val bufferedWriter = BufferedWriter(FileWriter(file, true))
                bufferedWriter.write(line)
                bufferedWriter.newLine()
                bufferedWriter.close()
            } catch (g: IOException) {
                g.printStackTrace()
            }
        }
    }
}