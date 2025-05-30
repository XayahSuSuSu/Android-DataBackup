package com.xayah.databackup.util

import android.app.Activity
import android.os.Process
import kotlin.system.exitProcess

object ProcessHelper {
    fun killSelf(context: Activity) {
        context.finishAffinity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}
