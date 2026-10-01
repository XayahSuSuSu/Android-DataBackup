package com.xayah.databackup.entity.backup

import com.squareup.moshi.JsonClass
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.database.entity.Storage

enum class BackupSourceCategory {
    Apk,
    InternalData,
    ExternalData,
    AdditionalData,
    File,
    MmsAttachment,
}

@JsonClass(generateAdapter = true)
data class BackupSourcePath(
    val path: String,
    val category: BackupSourceCategory,
)

@JsonClass(generateAdapter = true)
data class BackupSkippedSource(
    val path: String,
    val category: BackupSourceCategory,
    val reason: String,
) {
    companion object {
        const val REASON_NOT_FOUND = "not_found"
    }
}

data class BackupAppSources(
    val packageName: String,
    val userId: Int,
    val info: Info,
    val option: Option,
    val storage: Storage,
    val included: List<BackupSourcePath>,
    val skipped: List<BackupSkippedSource>,
)
