package com.xayah.databackup.util

import com.xayah.databackup.data.BackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Loader {
    companion object {
        // 备份历史列表
        suspend fun loadBackupInfoList(): MutableList<BackupInfo> {
            return withContext(Dispatchers.IO) {
                Command.getCachedBackupInfoList()
            }
        }
    }
}