package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.binPath
import com.xayah.databackup.util.extendPath
import com.xayah.databackup.util.filesPath
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DataBackupApplication : Application() {
    companion object {
        class EnvInitializer : Shell.Initializer() {
            override fun onInit(context: Context, shell: Shell): Boolean {
                initShell(shell, context)
                return true
            }

            private fun initShell(shell: Shell, context: Context) {
                shell.newJob()
                    .add("nsenter -t 1 -m su") // Switch to global namespace
                    .add("export PATH=${context.binPath()}:${SymbolUtil.USD}PATH")
                    .add("export PATH=${context.extendPath()}:${SymbolUtil.USD}PATH")
                    .add("export HOME=${context.filesPath()}")
                    .exec()
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
}
