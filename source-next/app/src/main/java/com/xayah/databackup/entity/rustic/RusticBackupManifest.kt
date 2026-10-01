package com.xayah.databackup.entity.rustic

import com.squareup.moshi.JsonClass
import com.xayah.databackup.entity.backup.BackupSkippedSource
import com.xayah.databackup.entity.backup.BackupSourcePath

/** Describes an app and its included or skipped source paths in a Rustic snapshot. */
@JsonClass(generateAdapter = true)
data class RusticAppManifest(
    val packageName: String,
    val userId: Int,
    val userName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val flags: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkBytes: Long,
    val internalDataBytes: Long,
    val externalDataBytes: Long,
    val additionalDataBytes: Long,
    val apk: Boolean,
    val internalData: Boolean,
    val externalData: Boolean,
    val additionalData: Boolean,
    val included: List<BackupSourcePath>,
    val skipped: List<BackupSkippedSource>,
)

/** Describes the DataBackup metadata and source inventory stored in a Rustic snapshot. */
@JsonClass(generateAdapter = true)
data class RusticBackupManifest(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val configUuid: String,
    val createdAt: Long,
    val structuredFiles: List<String>,
    val apps: List<RusticAppManifest>,
    val included: List<BackupSourcePath>,
    val skipped: List<BackupSkippedSource>,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
