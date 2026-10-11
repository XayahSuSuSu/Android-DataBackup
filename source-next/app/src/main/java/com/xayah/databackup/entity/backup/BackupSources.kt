package com.xayah.databackup.entity.backup

data class BackupStagedFile(
    val relativePath: String,
    val content: String,
)

