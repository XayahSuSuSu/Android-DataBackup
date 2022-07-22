package com.xayah.databackup

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import com.xayah.crash.CrashHandler
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import java.io.InputStream
import java.text.Collator
import java.util.*

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

        @SuppressLint("StaticFieldLeak")
        lateinit var globalContext: Context
        lateinit var versionName: String
        lateinit var globalAppInfoBackupList: MutableList<AppInfoBackup>
        lateinit var globalAppInfoRestoreList: MutableList<AppInfoRestore>
        lateinit var globalMediaInfoBackupList: MutableList<MediaInfo>
        lateinit var globalMediaInfoRestoreList: MutableList<MediaInfo>
        lateinit var globalBackupInfoList: MutableList<BackupInfo>
        val logcat = Logcat()
        val openDate = Command.getDate()

        fun saveGlobalList() {
            // 保存AppInfoBackupList
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(globalAppInfoBackupList as MutableList<Any>),
                Path.getAppInfoBackupListPath()
            )
            // 保存AppInfoRestoreList
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(globalAppInfoRestoreList as MutableList<Any>),
                Path.getAppInfoRestoreListPath()
            )
            // 保存MediaInfoBackupList
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(globalMediaInfoBackupList as MutableList<Any>),
                Path.getMediaInfoBackupListPath()
            )
            // 保存MediaInfoRestoreList
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(globalMediaInfoRestoreList as MutableList<Any>),
                Path.getMediaInfoRestoreListPath()
            )
        }

        fun initializeGlobalList() {
            // 读取AppInfoBackupList (按照字母表排序)
            globalAppInfoBackupList = Command.getAppInfoBackupList(globalContext).apply {
                sortWith { appInfo1, appInfo2 ->
                    val collator = Collator.getInstance(Locale.CHINA)
                    collator.getCollationKey((appInfo1 as AppInfoBackup).infoBase.appName)
                        .compareTo(collator.getCollationKey((appInfo2 as AppInfoBackup).infoBase.appName))
                }
            }

            // 读取AppInfoRestoreList (按照字母表排序)
            globalAppInfoRestoreList = Command.getCachedAppInfoRestoreList().apply {
                sortWith { appInfo1, appInfo2 ->
                    val collator = Collator.getInstance(Locale.CHINA)
                    collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                        .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                }
            }

            // 读取MediaInfoBackupList
            globalMediaInfoBackupList = Command.getCachedMediaInfoBackupList()

            // 读取MediaInfoRestoreList
            globalMediaInfoRestoreList = Command.getCachedMediaInfoRestoreList()

            // 读取BackupInfoList
            globalBackupInfoList = Command.getCachedBackupInfoList()
        }
    }

    class EnvInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            val bashrc: InputStream = context.resources.openRawResource(R.raw.bashrc)
            shell.newJob().add(bashrc).add("export PATH=${Path.getFilesDir(context)}/bin:\$PATH")
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