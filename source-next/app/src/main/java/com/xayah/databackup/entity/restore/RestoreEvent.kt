package com.xayah.databackup.entity.restore

import com.xayah.databackup.entity.backup.BackupSourceCategory

internal sealed interface RestoreEvent {
    val task: RestoreTask

    data class Started(override val task: RestoreTask) : RestoreEvent
    data class Completed(override val task: RestoreTask, val skipped: Set<String>) : RestoreEvent
    data class Failed(override val task: RestoreTask) : RestoreEvent
    data class RecordFailed(override val task: RestoreTask.Records, val id: String) : RestoreEvent
    data class RecordStarted(override val task: RestoreTask.Records, val id: String) : RestoreEvent
    data class RecordCompleted(override val task: RestoreTask.Records, val id: String, val skipped: Boolean) : RestoreEvent
    data class AppPartStarted(override val task: RestoreTask.App, val parts: Set<BackupSourceCategory>) : RestoreEvent
    data class AppPartCompleted(override val task: RestoreTask.App, val parts: Set<BackupSourceCategory>) : RestoreEvent
}
