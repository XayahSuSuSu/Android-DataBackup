package com.xayah.databackup.service.backup

import com.xayah.databackup.entity.backup.BackupEvent
import com.xayah.databackup.entity.backup.BackupRequest
import com.xayah.databackup.entity.backup.BackupResult

interface BackupHelper {
    suspend fun backup(request: BackupRequest, onEvent: (BackupEvent) -> Unit): BackupResult
}
