package com.xayah.databackup

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.xayah.crash.CrashHandler
import com.xayah.databackup.util.*
import java.io.InputStream

class App : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(8)
                    .setInitializers(EnvInitializer::class.java)
            )
        }

        @SuppressLint("StaticFieldLeak")
        lateinit var globalContext: Context
        lateinit var versionName: String

        fun getTimeStamp(): String {
            return System.currentTimeMillis().toString()
        }
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc: InputStream = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob()
                .add(bashrc)
                .add("export PATH=${Path.getAppInternalFilesPath()}/bin:\$PATH")
                .add("export PATH=${Path.getAppInternalFilesPath()}/extend:\$PATH")
                .add("export HOME=${Path.getAppInternalFilesPath()}")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
        globalContext = this
        versionName = Command.getVersion()
        if (globalContext.readIsDynamicColors()) DynamicColors.applyToActivitiesIfAvailable(this)
    }
}