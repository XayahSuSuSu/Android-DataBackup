package com.xayah.databackup.util

import android.annotation.SuppressLint

class Logcat {
    object Instance {
        val instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance

        @SuppressLint("SetWorldWritable")
        fun appendToFile(path: String, content: String) {
            SafeFile.create(path) {
                it.apply {
                    if (exists().not()) {
                        createNewFile()
                    }
                    appendText(content)
                }
            }
        }
    }

    val shellLogPath =
        "${Path.getShellLogPath()}/log_${GlobalObject.getInstance().timeStampOnStart}"
    val actionLogPath =
        "${Path.getActionLogPath()}/log_${GlobalObject.getInstance().timeStampOnStart}"

    fun shellLogAddLine(line: String) {
        if (line.isNotEmpty()) {
            appendToFile(shellLogPath, "${line}\n")
        }
    }

    fun actionLogAddLine(line: String) {
        if (line.isNotEmpty()) {
            appendToFile(actionLogPath, "${line}\n")
        }
    }
}