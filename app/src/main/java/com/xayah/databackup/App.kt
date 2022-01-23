package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.CrashHandler
import com.xayah.databackup.util.SettingsPreferencesDataStore

class App : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
                    .setInitializers(ScriptInitializer::class.java)
            )
        }
    }

    class ScriptInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            shell.newJob()
                .add("export APP_ENV=1")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
        SettingsPreferencesDataStore.initialize(this)
        val mShell = com.xayah.databackup.util.Shell(this)
        mShell.extractAssets()
    }
}