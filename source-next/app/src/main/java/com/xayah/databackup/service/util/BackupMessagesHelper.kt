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
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

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
        
        val moshi: Moshi = Moshi.Builder().build()
        val smsJson = runCatching {
            moshi.adapter<List<Sms>>().toJson(smsList)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize SMS to json.", it)
        }.getOrNull()
        
        val mmsJson = runCatching {
            moshi.adapter<List<Mms>>().toJson(mmsList)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize MMS to json.", it)
        }.getOrNull()
        
        // Save backup even if only one type succeeds, using empty arrays for failures
        val finalSmsJson = smsJson ?: "[]"
        val finalMmsJson = mmsJson ?: "[]"
        
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        val messagesPath = PathHelper.getBackupMessagesDir(backupConfig.path)
        if (RemoteRootService.exists(messagesPath).not() && RemoteRootService.mkdirs(messagesPath).not()) {
            LogHelper.e(TAG, "start", "Failed to mkdirs: $messagesPath.")
        }
        val timestamp = System.currentTimeMillis()
        val configPath = PathHelper.getBackupMessagesConfigFilePath(backupConfig.path, timestamp)
        
        // Combine SMS and MMS into a single JSON structure
        val combinedJson = """{"sms":$finalSmsJson,"mms":$finalMmsJson}"""
        RemoteRootService.writeText(configPath, combinedJson)

        mBackupProcessRepo.updateMessagesItem {
            copy {
                ProcessItem.currentIndex set totalCount
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
