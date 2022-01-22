package com.xayah.databackup

import android.app.Application
import com.xayah.databackup.util.CrashHandler

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
    }
}