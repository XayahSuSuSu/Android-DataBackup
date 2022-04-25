package com.xayah.databackup

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.xayah.crash.CrashHandler
import com.xayah.databackup.data.Log
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.readIsDynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

class App : Application() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
                    .setInitializers(EnvInitializer::class.java)
            )
        }

        val log = Log()

        @SuppressLint("StaticFieldLeak")
        lateinit var globalContext: Context
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc: InputStream = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob()
                .add(bashrc)
                .add("export PATH=${Path.getFilesDir(context)}/bin:\$PATH")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
        globalContext = this
        if (globalContext.readIsDynamicColors())
            DynamicColors.applyToActivitiesIfAvailable(this)

        val that = this
        CoroutineScope(Dispatchers.IO).launch {
            val versionName =
                that.packageManager.getPackageInfo(that.packageName, 0).versionName
            val oldVersionName =
                ShellUtils.fastCmd("cat ${Path.getFilesDir(that)}/version")
            if (versionName > oldVersionName) {
                ShellUtils.fastCmd("rm -rf ${Path.getFilesDir(that)}/bin")
                ShellUtils.fastCmd("rm -rf ${Path.getFilesDir(that)}/bin.zip")
            }

            if (!Command.ls("${Path.getFilesDir(that)}/bin")) {
                Command.extractAssets(that, "${Command.getABI()}/bin.zip", "bin.zip")
                Command.unzip(
                    "${Path.getFilesDir(that)}/bin.zip", "${Path.getFilesDir(that)}/bin"
                )
                ShellUtils.fastCmd("chmod 777 -R ${Path.getFilesDir(that)}")
                ShellUtils.fastCmd("echo \"${versionName}\" > ${Path.getFilesDir(that)}/version")
            }
        }
    }
}