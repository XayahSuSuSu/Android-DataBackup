package com.xayah.databackup.util

import com.xayah.databackup.App
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.BackupInfo
import com.xayah.databackup.data.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class Loader {
    companion object {

        // 应用备份列表
        suspend fun loadAppInfoBackupList(): MutableList<AppInfoBackup> {
            return withContext(Dispatchers.IO) {
                Command.getAppInfoBackupList(App.globalContext).apply {
                    sortWith { appInfo1, appInfo2 ->
                        val collator = Collator.getInstance(Locale.CHINA)
                        collator.getCollationKey((appInfo1 as AppInfoBackup).infoBase.appName)
                            .compareTo(collator.getCollationKey((appInfo2 as AppInfoBackup).infoBase.appName))
                    }
                }
            }
        }

        // 应用恢复列表
        suspend fun loadAppInfoRestoreList(): MutableList<AppInfoRestore> {
            return withContext(Dispatchers.IO) {
                Command.getCachedAppInfoRestoreList().apply {
                    sortWith { appInfo1, appInfo2 ->
                        val collator = Collator.getInstance(Locale.CHINA)
                        collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                            .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                    }
                }
            }
        }

        // 媒体备份列表
        suspend fun loadMediaInfoBackupList(): MutableList<MediaInfo> {
            return withContext(Dispatchers.IO) {
                Command.getCachedMediaInfoBackupList()
            }
        }

        // 媒体恢复列表
        suspend fun loadMediaInfoRestoreList(): MutableList<MediaInfo> {
            return withContext(Dispatchers.IO) {
                Command.getCachedMediaInfoRestoreList()
            }
        }

        // 备份历史列表
        suspend fun loadBackupInfoList(): MutableList<BackupInfo> {
            return withContext(Dispatchers.IO) {
                Command.getCachedBackupInfoList()
            }
        }
    }
}