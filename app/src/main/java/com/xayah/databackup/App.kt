package com.xayah.databackup

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.xayah.crash.CrashHandler
import com.xayah.databackup.data.AppInfo
import com.xayah.databackup.data.AppInfoListSelectedNum
import com.xayah.databackup.data.MediaInfo
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

        // 应用列表
        val appInfoList by lazy {
            MutableStateFlow(mutableListOf<AppInfo>())
        }

        // 应用备份列表计数
        val appInfoBackupListNum
            get() = run {
                val num = AppInfoListSelectedNum(0, 0)
                for (i in appInfoList.value) {
                    if (i.backup.app || i.backup.data) {
                        if (i.isSystemApp) num.system++
                        else num.installed++
                    }
                }
                num
            }

        // 应用恢复列表计数
        val appInfoRestoreListNum
            get() = run {
                val num = AppInfoListSelectedNum(0, 0)
                for (i in appInfoList.value) {
                    if (i.restoreList.isNotEmpty()) {
                        if (i.restoreList[i.restoreIndex].app || i.restoreList[i.restoreIndex].data) {
                            if (i.isSystemApp) num.system++
                            else num.installed++
                        }
                    }
                }
                num
            }

        // 媒体列表
        val mediaInfoList by lazy {
            MutableStateFlow(mutableListOf<MediaInfo>())
        }

        suspend fun loadList() {
            appInfoList.emit(Command.getAppInfoList())
            mediaInfoList.emit(Command.getMediaInfoList())
        }

        suspend fun saveMediaInfoList() {
            JSON.saveMediaInfoList(mediaInfoList.value)
        }

        fun getTimeStamp(): String {
            return System.currentTimeMillis().toString()
        }
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc: InputStream = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob()
                .add(bashrc)
                .add("export PATH=${Path.getFilesDir()}/bin:\$PATH")
                .add("export PATH=${Path.getFilesDir()}/extend:\$PATH")
                .exec()
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this).initialize()
        globalContext = this
        logcat = Logcat()
        versionName = Command.getVersion()
        server = Server()
        if (globalContext.readIsDynamicColors()) DynamicColors.applyToActivitiesIfAvailable(this)
    }
}