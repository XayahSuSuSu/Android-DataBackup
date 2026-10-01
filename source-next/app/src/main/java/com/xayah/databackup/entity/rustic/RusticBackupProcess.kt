package com.xayah.databackup.entity.rustic

import com.xayah.databackup.entity.backup.BackupSkippedSource

enum class RusticBackupStage {
    PrepareRepository,
    CollectSources,
    CreateSnapshot,
    FinalizeSnapshot,
}

sealed interface RusticBackupEvent {
    data class StageChanged(val stage: RusticBackupStage) : RusticBackupEvent
    data class Progress(val bytesDone: Long, val speed: Long, val progress: Float) : RusticBackupEvent
}

data class RusticBackupResult(
    val snapshotId: String,
    val includedCount: Int,
    val skippedSources: List<BackupSkippedSource>,
)
