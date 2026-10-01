package com.xayah.databackup.entity.restore

import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.entity.backup.BackupSourceCategory
import kotlinx.serialization.Serializable

@Serializable
enum class RestoreCategory { Apps, Networks, Contacts, CallLogs, Messages }

val AppRestoreParts = setOf(
    BackupSourceCategory.Apk,
    BackupSourceCategory.InternalData,
    BackupSourceCategory.ExternalData,
    BackupSourceCategory.AdditionalData,
)

internal fun Set<BackupSourceCategory>.toAppOptions() = Option(
    apk = BackupSourceCategory.Apk in this,
    internalData = BackupSourceCategory.InternalData in this,
    externalData = BackupSourceCategory.ExternalData in this,
    additionalData = BackupSourceCategory.AdditionalData in this,
)
