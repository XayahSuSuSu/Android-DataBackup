package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.Flow

class MessageRepository {
    companion object {
        private const val TAG = "MessageRepository"
    }

    val isBackupContactsSelected: Flow<Boolean> = application.readBoolean(MessagesOptionSelectedBackup)

    val smsList: Flow<List<Sms>> = DatabaseHelper.messageDao.loadFlowSms()

    val mmsList: Flow<List<Mms>> = DatabaseHelper.messageDao.loadFlowMms()
}
