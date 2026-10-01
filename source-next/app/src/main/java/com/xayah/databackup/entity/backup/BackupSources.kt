package com.xayah.databackup.entity.backup

data class BackupStagedFile(
    val relativePath: String,
    val content: String,
)

data class BackupSources(
    val sourcePaths: Map<String, String>,
    val includedCount: Int,
    val skippedSources: List<BackupSkippedSource>,
)

