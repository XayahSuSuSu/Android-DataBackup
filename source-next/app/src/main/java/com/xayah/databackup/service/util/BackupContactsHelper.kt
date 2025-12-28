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
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class BackupContactsHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupContactsHelper"
    }

    suspend fun start() {
        val contacts = mBackupProcessRepo.getContacts()
        contacts.forEachIndexed { index, contact ->
            mBackupProcessRepo.updateContactsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set contact.id.toString()
                    ProcessItem.progress set index.toFloat() / contacts.size
                }
            }
        }
        val json = runCatching {
            val moshi: Moshi = Moshi.Builder().build()
            moshi.adapter<List<Contact>>().toJson(contacts)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize to json.", it)
        }.getOrNull()
        if (json != null) {
            val backupConfig = mBackupProcessRepo.getBackupConfig()
            val contactsPath = PathHelper.getBackupContactsDir(backupConfig.path)
            if (RemoteRootService.exists(contactsPath).not() && RemoteRootService.mkdirs(contactsPath)) {
                LogHelper.e(TAG, "start", "Failed to mkdirs: $contactsPath.")
            }
            val configPath = PathHelper.getBackupContactsConfigFilePath(backupConfig.path, System.currentTimeMillis())
            RemoteRootService.writeText(configPath, json)
        } else {
            LogHelper.e(TAG, "start", "Failed to save contacts, json is null")
        }

        mBackupProcessRepo.updateContactsItem {
            copy {
                ProcessItem.currentIndex set contacts.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
