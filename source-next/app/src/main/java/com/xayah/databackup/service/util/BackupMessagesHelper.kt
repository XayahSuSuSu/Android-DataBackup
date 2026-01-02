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
import com.xayah.databackup.database.entity.FiledMap
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
        val backupConfig = mBackupProcessRepo.getBackupConfig()
        val messagesPath = PathHelper.getBackupMessagesDir(backupConfig.path)
        if (RemoteRootService.exists(messagesPath).not() && RemoteRootService.mkdirs(messagesPath).not()) {
            LogHelper.e(TAG, "start", "Failed to mkdirs: $messagesPath.")
        }
        val timestamp = System.currentTimeMillis()
        
        // Backup SMS
        val smsJson = runCatching {
            moshi.adapter<List<Sms>>().toJson(smsList)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize SMS to json.", it)
        }.getOrNull()
        
        if (smsJson != null) {
            val smsConfigPath = PathHelper.getBackupMessagesSmsConfigFilePath(backupConfig.path, timestamp)
            RemoteRootService.writeText(smsConfigPath, smsJson)
        } else {
            LogHelper.e(TAG, "start", "Failed to save SMS, json is null")
        }
        
        // Backup MMS
        val mmsJson = runCatching {
            moshi.adapter<List<Mms>>().toJson(mmsList)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize MMS to json.", it)
        }.getOrNull()
        
        if (mmsJson != null) {
            val mmsConfigPath = PathHelper.getBackupMessagesMmsConfigFilePath(backupConfig.path, timestamp)
            RemoteRootService.writeText(mmsConfigPath, mmsJson)
        } else {
            LogHelper.e(TAG, "start", "Failed to save MMS, json is null")
        }
        
        // Backup MMS part files
        backupMmsPartFiles(mmsList, messagesPath, moshi)

        mBackupProcessRepo.updateMessagesItem {
            copy {
                ProcessItem.currentIndex set totalCount
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
    
    private suspend fun backupMmsPartFiles(mmsList: List<Mms>, messagesPath: String, moshi: Moshi) {
        val appPartsPath = PathHelper.getBackupAppPartsDir(messagesPath)
        if (RemoteRootService.exists(appPartsPath).not() && RemoteRootService.mkdirs(appPartsPath).not()) {
            LogHelper.e(TAG, "backupMmsPartFiles", "Failed to mkdirs: $appPartsPath.")
            return
        }
        
        mmsList.forEach { mms ->
            runCatching {
                // Parse the part JSON
                val partList = mms.part?.let { json -> 
                    moshi.adapter<List<FiledMap>>().fromJson(json) 
                } ?: emptyList()
                
                // Process each part
                partList.forEach { part ->
                    val dataPath = part["_data"]?.toString()
                    if (dataPath != null && dataPath.isNotEmpty()) {
                        // Check if file exists
                        if (RemoteRootService.exists(dataPath)) {
                            val fileName = PathHelper.getChildPath(dataPath)
                            val destPath = "$appPartsPath/${fileName}"

                            if (RemoteRootService.copyRecursively(dataPath, destPath, true)) {
                                LogHelper.i(TAG, "backupMmsPartFiles", "Successfully backed up MMS part file: $dataPath")
                            } else {
                                LogHelper.e(TAG, "backupMmsPartFiles", "Failed to backup MMS part file: $dataPath")
                            }
                        } else {
                            LogHelper.w(TAG, "backupMmsPartFiles", "MMS part file does not exist: $dataPath")
                        }
                    }
                }
            }.onFailure { e ->
                LogHelper.e(TAG, "backupMmsPartFiles", "Failed to process MMS part files for MMS id: ${mms.id}", e)
            }
        }
    }
}
