package com.xayah.databackup

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DataBackupApplication : Application() {
    companion object {
        lateinit var application: Application
    }

    override fun onCreate() {
        super.onCreate()
        application = this
    }
}
