package com.xayah.databackup.entity.backup

enum class BackupCategory { Apps, Files, Networks, Contacts, CallLogs, Messages }
enum class BackupStage { PrepareDestination, CollectSources, WriteBackup, FinalizeBackup }
enum class BackupOutcome { Pending, Running, Completed, Skipped, Failed, Canceled, Unprocessed }
enum class BackupStatus { Pending, Running, Canceling, Completed, PartialFailure, Failed, Canceled, Interrupted }

sealed interface BackupEvent {
    data class StageChanged(val stage: BackupStage) : BackupEvent
    data class Progress(val bytes: Long, val speed: Long, val progress: Float) : BackupEvent
    data class CategoryStarted(val category: BackupCategory) : BackupEvent
    data class CategoryCompleted(val category: BackupCategory, val outcome: BackupOutcome) : BackupEvent
    data class AppStarted(val key: String) : BackupEvent
    data class PartChanged(
        val key: String,
        val category: BackupSourceCategory,
        val index: Int,
        val outcome: BackupOutcome,
        val bytes: Long = 0,
        val speed: Long = 0,
        val exitCode: Int = 0,
        val diagnostics: String = "",
    ) : BackupEvent
    data class AppCompleted(val key: String) : BackupEvent
    data class SnapshotCreated(val id: String) : BackupEvent
}

data class BackupResult(
    val snapshotId: String = "",
    val includedCount: Int = 0,
    val skippedSources: List<BackupSkippedSource> = emptyList(),
    val hasFailures: Boolean = false,
)
