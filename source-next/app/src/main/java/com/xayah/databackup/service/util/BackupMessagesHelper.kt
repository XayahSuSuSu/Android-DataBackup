package com.xayah.databackup.service.util

import arrow.optics.copy
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

@JsonClass(generateAdapter = true)
data class MessagesBackup(
    val sms: List<Sms>,
    val mms: List<Mms>
)

class BackupMessagesHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupMessagesHelper"
    }

    suspend fun start() {
        val smsList = mBackupProcessRepo.getSmsList()
        val mmsList = mBackupProcessRepo.getMmsList()
        val totalCount = smsList.size + mmsList.size
        
        // Process SMS
        smsList.forEachIndexed { index, sms ->
            mBackupProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set "SMS: ${sms.id}"
                    ProcessItem.progress set index.toFloat() / totalCount
                }
            }
        }
        
        // Process MMS
        mmsList.forEachIndexed { index, mms ->
            val currentIndex = smsList.size + index
            mBackupProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set currentIndex
                    ProcessItem.msg set "MMS: ${mms.id}"
                    ProcessItem.progress set currentIndex.toFloat() / totalCount
                }
            }
        }
        
        val json = runCatching {
            val moshi: Moshi = Moshi.Builder().build()
            val messagesBackup = MessagesBackup(sms = smsList, mms = mmsList)
            moshi.adapter<MessagesBackup>().toJson(messagesBackup)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize messages to json.", it)
        }.getOrNull()
        
        if (json != null) {
            val backupConfig = mBackupProcessRepo.getBackupConfig()
            val messagesPath = PathHelper.getBackupMessagesDir(backupConfig.path)
            if (RemoteRootService.exists(messagesPath).not() && RemoteRootService.mkdirs(messagesPath).not()) {
                LogHelper.e(TAG, "start", "Failed to mkdirs: $messagesPath.")
            }
            val configPath = PathHelper.getBackupMessagesConfigFilePath(backupConfig.path, System.currentTimeMillis())
            RemoteRootService.writeText(configPath, json)
        } else {
            LogHelper.e(TAG, "start", "Failed to save messages, json is null")
        }

        mBackupProcessRepo.updateMessagesItem {
            copy {
                ProcessItem.currentIndex set totalCount
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
