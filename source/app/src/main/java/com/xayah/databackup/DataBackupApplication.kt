package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.core.util.LogUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.logDir
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DataBackupApplication : Application() {
    override fun attachBaseContext(context: Context) {
        val base: Context = if (context is Application) context.baseContext else context
        super.attachBaseContext(base)
        Shell.enableVerboseLogging = BuildConfig.ENABLE_VERBOSE
        Shell.setDefaultBuilder(
            BaseUtil.getShellBuilder(base)
        )
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.initialize(logDir())
    }
}
