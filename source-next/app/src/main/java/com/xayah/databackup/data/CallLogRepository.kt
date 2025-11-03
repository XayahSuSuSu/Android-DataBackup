package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CallLogRepository {
    companion object {
        private const val TAG = "CallLogRepository"
    }

    val isBackupCallLogsSelected: Flow<Boolean> = application.readBoolean(CallLogsOptionSelectedBackup)

    val callLogs: Flow<List<CallLog>> = DatabaseHelper.callLogDao.loadFlowCallLogs()
    val callLogsSelected: Flow<List<CallLog>> = callLogs.map { callLogs -> callLogs.filter { it.selected } }
}
