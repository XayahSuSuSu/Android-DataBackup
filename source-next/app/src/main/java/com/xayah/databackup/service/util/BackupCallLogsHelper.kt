package com.xayah.databackup.service.util

import arrow.optics.copy
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class BackupCallLogsHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupCallLogsHelper"
    }

    suspend fun start() {
        val callLogs = mBackupProcessRepo.getCallLogs()
        callLogs.forEachIndexed { index, callLog ->
            mBackupProcessRepo.updateCallLogsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set callLog.id.toString()
                    ProcessItem.progress set index.toFloat() / callLogs.size
                }
            }
        }
        val json = runCatching {
            val moshi: Moshi = Moshi.Builder().build()
            moshi.adapter<List<CallLog>>().toJson(callLogs)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize to json.", it)
        }.getOrNull()
        if (json != null) {
            val backupConfig = mBackupProcessRepo.getBackupConfig()
            val callLogsPath = PathHelper.getBackupCallLogsDir(backupConfig.path)
            if (RemoteRootService.exists(callLogsPath).not() && RemoteRootService.mkdirs(callLogsPath).not()) {
                LogHelper.e(TAG, "start", "Failed to mkdirs: $callLogsPath.")
            }
            val configPath = PathHelper.getBackupCallLogsConfigFilePath(backupConfig.path, System.currentTimeMillis())
            RemoteRootService.writeText(configPath, json)
        } else {
            LogHelper.e(TAG, "start", "Failed to save call logs, json is null")
        }

        mBackupProcessRepo.updateCallLogsItem {
            copy {
                ProcessItem.currentIndex set callLogs.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
