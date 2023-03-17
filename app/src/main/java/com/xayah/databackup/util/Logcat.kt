package com.xayah.databackup.util

import com.xayah.databackup.librootservice.RootService

class Logcat {
    object Instance {
        var instance = Logcat()
    }

    companion object {
        fun getInstance() = Instance.instance
        fun refreshInstance() {
            Instance.instance = Logcat()
            Instance.instance.init()
        }
    }

    fun init(): Boolean {
        val actionLogPath =
            "${Path.getLogPath()}/action_log_${GlobalObject.getInstance().timeStampOnStart}"
        return RootService.getInstance().initActionLogFile(actionLogPath)
    }

    fun shellLogAddLine(line: String) {}

    fun actionLogAddLine(funName: String, line: String) {
        if (line.isNotEmpty()) {
            RootService.getInstance().appendActionLog("${funName}: ${line}\n")
        }
    }
}