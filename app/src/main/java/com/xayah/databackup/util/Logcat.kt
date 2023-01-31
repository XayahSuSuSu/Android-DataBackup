package com.xayah.databackup.util

import android.annotation.SuppressLint
import java.io.File

class Logcat {
    object Instance {
        val instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance

        @SuppressLint("SetWorldWritable")
        fun appendToFile(file: File?, content: String) {
            file?.apply {
                try {
                    if (exists().not()) {
                        createNewFile()
                    }
                    appendText(content)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val shellLogPath =
        "${Path.getShellLogPath()}/log_${GlobalObject.getInstance().timeStampOnStart}"
    private val actionLogPath =
        "${Path.getActionLogPath()}/log_${GlobalObject.getInstance().timeStampOnStart}"

    val shellLogFile: File? = SafeFile.create(shellLogPath)
    val actionLogFile: File? = SafeFile.create(actionLogPath)


    fun shellLogAddLine(line: String) {
        if (line.isNotEmpty()) {
            appendToFile(shellLogFile, "${line}\n")
        }
    }

    fun actionLogAddLine(line: String) {
        if (line.isNotEmpty()) {
            appendToFile(actionLogFile, "${line}\n")
        }
    }
}