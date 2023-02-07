package com.xayah.databackup.util

import android.annotation.SuppressLint
import com.topjohnwu.superuser.io.SuFile

class Logcat {
    object Instance {
        var instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance
        fun refreshInstance() {
            Instance.instance = Logcat()
        }

        @SuppressLint("SetWorldWritable")
        fun appendToFile(file: SuFile?, content: String) {
            file?.apply {
                try {
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

    private val shellLogFile: SuFile? = SafeFile.create(shellLogPath).apply {
        this?.apply {
            if (exists().not()) SafeFile.createNewFile(this)
        }
    }
    private val actionLogFile: SuFile? = SafeFile.create(actionLogPath).apply {
        this?.apply {
            if (exists().not()) SafeFile.createNewFile(this)
        }
    }

    fun shellLogAddLine(line: String) {
        if (line.isNotEmpty()) {
            appendToFile(shellLogFile, "${line}\n")
        }
    }

    fun actionLogAddLine(funName: String, line: String) {
        if (line.isNotEmpty()) {
            appendToFile(actionLogFile, "${funName}: ${line}\n")
        }
    }
}