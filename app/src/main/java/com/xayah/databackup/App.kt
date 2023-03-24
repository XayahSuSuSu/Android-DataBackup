package com.xayah.databackup

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.*
import com.xayah.databackup.util.command.Command

class App : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.ENABLE_VERBOSE
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

        fun initShell(shell: Shell) {
            shell.newJob()
                .add("nsenter -t 1 -m su")
                .add("export PATH=${Path.getAppInternalFilesPath()}/bin:\$PATH")
                .add("export PATH=${Path.getAppInternalFilesPath()}/extend:\$PATH")
                .add("export HOME=${Path.getAppInternalFilesPath()}")
                .exec()
        }
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            initShell(shell)
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        globalContext = this
        versionName = Command.getVersion()
    }
}