package com.xayah.databackup.data.rustic

import com.squareup.moshi.JsonClass

/** Describes an app and its included or skipped source paths in a Rustic snapshot. */
@JsonClass(generateAdapter = true)
data class RusticAppManifest(
    val packageName: String,
    val userId: Int,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val apk: Boolean,
    val internalData: Boolean,
    val externalData: Boolean,
    val additionalData: Boolean,
    val included: List<RusticSourcePath>,
    val skipped: List<RusticSkippedSource>,
)

/** Describes the DataBackup metadata and source inventory stored in a Rustic snapshot. */
@JsonClass(generateAdapter = true)
data class RusticBackupManifest(
    val schemaVersion: Int = 1,
    val configUuid: String,
    val createdAt: Long,
    val structuredFiles: List<String>,
    val apps: List<RusticAppManifest>,
    val included: List<RusticSourcePath>,
    val skipped: List<RusticSkippedSource>,
)
