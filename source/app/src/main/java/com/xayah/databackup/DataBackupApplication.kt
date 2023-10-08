package com.xayah.databackup

import android.app.Application
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.binPath
import com.xayah.databackup.util.extensionPath
import com.xayah.databackup.util.filesPath
import com.xayah.databackup.util.readMonetEnabled
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DataBackupApplication : Application() {
    companion object {
        lateinit var application: Application
        lateinit var monetEnabled: MutableState<Boolean>

        class EnvInitializer : Shell.Initializer() {
            companion object {
                fun initShell(shell: Shell, context: Context) {
                    shell.newJob()
                        .add("nsenter -t 1 -m su") // Switch to global namespace
                        .add("export PATH=${context.binPath()}:${SymbolUtil.USD}PATH")
                        .add("export PATH=${context.extensionPath()}:${SymbolUtil.USD}PATH")
                        .add("export HOME=${context.filesPath()}")
                        .add("set -o pipefail") // Ensure that the exit code of each command is correct.
                        .exec()
                }
            }

            override fun onInit(context: Context, shell: Shell): Boolean {
                initShell(shell, context)
                return true
            }
        }
    }

    override fun attachBaseContext(context: Context) {
        val base: Context = if (context is Application) context.baseContext else context
        super.attachBaseContext(base)
        Shell.enableVerboseLogging = BuildConfig.ENABLE_VERBOSE
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setInitializers(EnvInitializer::class.java)
                .setContext(base)
                .setTimeout(3)
        )
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        monetEnabled = mutableStateOf(readMonetEnabled())
    }
}
