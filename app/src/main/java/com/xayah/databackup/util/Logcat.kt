package com.xayah.databackup.util

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class Logcat {
    val logPath = "${Path.getFilesDir()}/log_${System.currentTimeMillis()}"

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