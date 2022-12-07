package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.xayah.crash.CrashHandler
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoListSelectedNum
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.util.*
import kotlinx.coroutines.flow.MutableStateFlow
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
        lateinit var server: Server
        lateinit var logcat: Logcat

        // 应用备份列表
        val appInfoBackupList by lazy {
            MutableStateFlow(mutableListOf<AppInfoBackup>())
        }

        // 应用备份列表计数
        val appInfoBackupListNum
            get() = run {
                val num = AppInfoListSelectedNum(0, 0)
                for (i in appInfoBackupList.value) {
                    if (i.infoBase.app || i.infoBase.data) {
                        if (i.infoBase.isSystemApp) num.system++
                        else num.installed++
                    }
                }
                num
            }

        // 应用恢复列表
        val appInfoRestoreList by lazy {
            MutableStateFlow(mutableListOf<AppInfoRestore>())
        }

        // 应用恢复列表计数
        val appInfoRestoreListNum
            get() = run {
                val num = AppInfoListSelectedNum(0, 0)
                for (i in appInfoRestoreList.value) {
                    if (i.infoBase.app || i.infoBase.data) {
                        if (i.infoBase.isSystemApp) num.system++
                        else num.installed++
                    }
                }
                num
            }

        suspend fun loadList() {
            Command.retrieve(Command.getCachedAppInfoRestoreActualList())
            appInfoBackupList.emit(Command.getAppInfoBackupList(globalContext))
            appInfoRestoreList.emit(Command.getCachedAppInfoRestoreActualList())
        }
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
        logcat = Logcat()
        versionName = packageManager.getPackageInfo(packageName, 0).versionName
        server = Server()
        if (globalContext.readIsDynamicColors()) DynamicColors.applyToActivitiesIfAvailable(this)
    }
}