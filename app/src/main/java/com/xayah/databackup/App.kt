package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.xayah.crash.CrashHandler
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            shell.newJob()
                .add("export PATH=${Path.getExternalFilesDir(context)}/bin:\$PATH")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()

        val that = this
        CoroutineScope(Dispatchers.IO).launch {
            val versionName =
                that.packageManager.getPackageInfo(that.packageName, 0).versionName
            val oldVersionName =
                ShellUtils.fastCmd("cat ${Path.getExternalFilesDir(that)}/version")
            if (versionName > oldVersionName){
                ShellUtils.fastCmd("rm -rf ${Path.getExternalFilesDir(that)}/bin")
                ShellUtils.fastCmd("rm -rf ${Path.getExternalFilesDir(that)}/bin.zip")
            }

            if (!Command.ls("${Path.getExternalFilesDir(that)}/bin")) {
                Command.extractAssets(that, "${Command.getABI()}/bin.zip", "bin.zip")
                Command.unzip(
                    "${Path.getExternalFilesDir(that)}/bin.zip",
                    "${Path.getExternalFilesDir(that)}/bin"
                )
                ShellUtils.fastCmd("echo \"${versionName}\" > ${Path.getExternalFilesDir(that)}/version")
            }
        }
    }
}