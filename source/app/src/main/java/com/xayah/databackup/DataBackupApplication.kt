package com.xayah.databackup

import android.app.Application
import com.topjohnwu.superuser.Shell
import com.xayah.core.util.LogUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.logDir
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DataBackupApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.enableVerboseLogging = BuildConfig.ENABLE_VERBOSE
        Shell.setDefaultBuilder(
            BaseUtil.getShellBuilder()
        )
        LogUtil.initialize(logDir())
    }
}
