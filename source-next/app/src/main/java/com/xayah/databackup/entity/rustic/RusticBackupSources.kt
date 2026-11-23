package com.xayah.databackup.entity.rustic

import com.xayah.databackup.entity.backup.BackupAppSources
import com.xayah.databackup.entity.backup.BackupSkippedSource

/**
 * Collected sources for a Rustic backup. [sourcePaths] maps each local source path to its destination
 * in the snapshot, including the staging directory containing generated metadata.
 */
data class RusticBackupSources(
    val sourcePaths: Map<String, String>,
    val includedCount: Int,
    val skippedSources: List<BackupSkippedSource>,
    val hasFailures: Boolean = false,
    val apps: List<BackupAppSources> = emptyList(),
    /** Physical subtrees below [sourcePaths] that are omitted from the snapshot, such as app caches. */
    val excludedPaths: List<String> = emptyList(),
)
