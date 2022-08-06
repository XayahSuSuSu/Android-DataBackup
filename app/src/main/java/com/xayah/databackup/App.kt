package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.xayah.crash.CrashHandler
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Logcat
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.readIsDynamicColors
import java.io.InputStream

class App : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR).setTimeout(10)
                    .setInitializers(EnvInitializer::class.java)
            )
        }

        lateinit var globalContext: Context
        lateinit var versionName: String
        val logcat = Logcat()
        val openDate = Command.getDate()
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc: InputStream = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob().add(bashrc).add("export PATH=${Path.getFilesDir()}/bin:\$PATH")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
        globalContext = this
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
        if (globalContext.readIsDynamicColors()) DynamicColors.applyToActivitiesIfAvailable(this)
    }
}