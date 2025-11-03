package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository {
    companion object {
        private const val TAG = "MessageRepository"
    }

    val isBackupContactsSelected: Flow<Boolean> = application.readBoolean(MessagesOptionSelectedBackup)

    val smsList: Flow<List<Sms>> = DatabaseHelper.messageDao.loadFlowSms()
    val smsListSelected: Flow<List<Sms>> = smsList.map { smsList -> smsList.filter { it.selected } }

    val mmsList: Flow<List<Mms>> = DatabaseHelper.messageDao.loadFlowMms()
    val mmsListSelected: Flow<List<Mms>> = mmsList.map { mmsList -> mmsList.filter { it.selected } }
}
