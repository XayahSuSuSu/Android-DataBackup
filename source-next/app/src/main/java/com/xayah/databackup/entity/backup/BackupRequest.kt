package com.xayah.databackup.entity.backup

import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.BackupConfig
import kotlin.uuid.Uuid

/**
 * Captures the backup selection at creation time. Copies on input and output prevent changes to
 * the original selection or returned data from modifying this request.
 */
class BackupRequest(selection: BackupSelection, val id: String = Uuid.random().toString(), val createdAt: Long = System.currentTimeMillis()) {
    private val mSelection = selection.deepCopy()
    val backendType: BackupBackendType = when (mSelection.config.backupBackend) {
        is BackupBackend.Archive -> BackupBackendType.Archive
        is BackupBackend.Rustic -> BackupBackendType.Rustic
    }

    fun getSelection(): BackupSelection = mSelection.deepCopy()
    fun getConfig(): BackupConfig = mSelection.config.copy()
}

private fun BackupSelection.deepCopy(): BackupSelection = copy(
    config = config.copy(),
    apps = apps?.map { it.copy(info = it.info.copy(), option = it.option.copy(), storage = it.storage.copy()) },
    files = files?.map { it.copy() },
    networks = networks?.map { it.copy() },
    contacts = contacts?.map { it.copy() },
    callLogs = callLogs?.map { it.copy() },
    sms = sms?.map { it.copy() },
    mms = mms?.map { it.copy() },
    totalCounts = totalCounts.toMap(),
)
