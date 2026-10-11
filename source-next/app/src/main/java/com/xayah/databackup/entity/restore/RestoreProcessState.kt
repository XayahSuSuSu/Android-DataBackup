package com.xayah.databackup.entity.restore

import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupSourceCategory

internal enum class RestoreProcessStatus {
    Pending,
    Processing,
    Canceling,
    Canceled,
    Finished,
    FinishedWithErrors,
    Failed,
    Interrupted,
}

internal enum class RestoreItemStatus {
    Pending,
    Processing,
    Finished,
    Failed,
    Canceled,
}

internal enum class RestoreRecordStatus {
    Pending,
    Processing,
    Restored,
    Skipped,
    Failed,
    NotProcessed,
    Interrupted,
}

@optics
internal data class RestoreRecordResult(
    val id: String,
    val title: String,
    val status: RestoreRecordStatus = RestoreRecordStatus.Pending,
    val parts: Map<BackupSourceCategory, RestoreRecordStatus> = emptyMap(),
) {
    companion object
}

@optics
internal data class RestoreProcessItem(
    val category: RestoreCategory,
    val totalCount: Int,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val status: RestoreItemStatus = RestoreItemStatus.Pending,
    val records: List<RestoreRecordResult> = emptyList(),
) {
    companion object
}

/** Tracks a restore task's status, progress, and results for each category and record. */
@optics
internal data class RestoreProcessState(
    val id: String,
    val backendType: BackupBackendType,
    val backupName: String = "",
    val status: RestoreProcessStatus = RestoreProcessStatus.Pending,
    val items: List<RestoreProcessItem> = emptyList(),
    val isExpired: Boolean = false,
) {
    companion object

    val isActive: Boolean
        get() = status == RestoreProcessStatus.Pending || status == RestoreProcessStatus.Processing || status == RestoreProcessStatus.Canceling
}

internal fun RestoreRequest.toInitialState(inventory: RestoreInventory?, backupName: String): RestoreProcessState = RestoreProcessState(
    id = id,
    backendType = backendType,
    backupName = backupName,
    items = tasks.groupBy { it.category }.map { (category, tasks) ->
        val records = tasks.flatMap { task ->
            task.ids.map { id ->
                val title = when (category) {
                    RestoreCategory.Apps -> inventory?.apps?.get(id)?.info?.label
                    RestoreCategory.Networks -> inventory?.networks?.get(id)?.ssid
                    RestoreCategory.Contacts -> inventory?.contacts?.get(id)?.displayName
                    RestoreCategory.CallLogs -> inventory?.callLogs?.get(id)?.number
                    RestoreCategory.Messages -> inventory?.sms?.get(id)?.address ?: inventory?.mms?.get(id)?.address
                }.orEmpty()
                RestoreRecordResult(
                    id = id,
                    title = title,
                    parts = if (task is RestoreTask.App) {
                        task.source.paths.map { it.category }.distinct().associateWith { RestoreRecordStatus.Pending }
                    } else {
                        emptyMap()
                    },
                )
            }
        }
        RestoreProcessItem(category = category, totalCount = records.size, records = records)
    },
)

internal fun RestoreProcessState.reduce(event: RestoreEvent): RestoreProcessState = copy {
    RestoreProcessState.items transform { items ->
        items.map { item ->
            if (item.category != event.task.category) {
                item
            } else {
                val ids = event.task.ids.toSet()
                val records = item.records.map { record ->
                    if (record.id in ids) {
                        record.reduce(event)
                    } else {
                        record
                    }
                }
                val completedCount = records.count { it.status.isCompleted }
                val hasFailures = event is RestoreEvent.Failed || records.any { it.status == RestoreRecordStatus.Failed }
                item.copy(
                    status = when {
                        event is RestoreEvent.Failed -> RestoreItemStatus.Failed
                        completedCount < item.totalCount -> RestoreItemStatus.Processing
                        hasFailures || item.status == RestoreItemStatus.Failed -> RestoreItemStatus.Failed
                        else -> RestoreItemStatus.Finished
                    },
                ).updateRecords(records)
            }
        }
    }
}

private fun RestoreRecordResult.reduce(event: RestoreEvent): RestoreRecordResult = when (event) {
    is RestoreEvent.Started -> if (event.task is RestoreTask.App) {
        copy(status = RestoreRecordStatus.Processing)
    } else {
        this
    }
    is RestoreEvent.Completed -> if (event.task is RestoreTask.App) {
        copy(status = RestoreRecordStatus.Restored)
    } else {
        this
    }
    is RestoreEvent.Failed -> finish(RestoreProcessStatus.Failed)
    is RestoreEvent.RecordFailed -> if (id == event.id) {
        copy(status = RestoreRecordStatus.Failed)
    } else {
        this
    }
    is RestoreEvent.RecordStarted -> if (id == event.id) {
        copy(status = RestoreRecordStatus.Processing)
    } else {
        this
    }
    is RestoreEvent.RecordCompleted -> if (id == event.id) {
        copy(status = if (event.isSkipped) {
            RestoreRecordStatus.Skipped
        } else {
            RestoreRecordStatus.Restored
        })
    } else {
        this
    }
    is RestoreEvent.AppPartStarted -> copy {
        RestoreRecordResult.parts transform { parts ->
            parts.mapValues { (part, status) ->
                if (part in event.parts) {
                    RestoreRecordStatus.Processing
                } else {
                    status
                }
            }
        }
    }
    is RestoreEvent.AppPartCompleted -> copy {
        RestoreRecordResult.parts transform { parts ->
            parts.mapValues { (part, status) ->
                if (part in event.parts) {
                    RestoreRecordStatus.Restored
                } else {
                    status
                }
            }
        }
    }
}

internal fun RestoreProcessState.finish(status: RestoreProcessStatus): RestoreProcessState = copy {
    RestoreProcessState.status set status
    RestoreProcessState.items transform { items ->
        items.map { item ->
            item.copy(
                status = when {
                    item.status == RestoreItemStatus.Finished || item.status == RestoreItemStatus.Failed -> item.status
                    status == RestoreProcessStatus.Canceled -> RestoreItemStatus.Canceled
                    else -> RestoreItemStatus.Failed
                },
            ).updateRecords(item.records.map { it.finish(status) })
        }
    }
}

private fun RestoreProcessItem.updateRecords(records: List<RestoreRecordResult>): RestoreProcessItem {
    val completedCount = records.count { it.status.isCompleted }
    return copy(
        records = records,
        completedCount = completedCount,
        skippedCount = records.count { it.status == RestoreRecordStatus.Skipped },
        failedCount = records.count { it.status == RestoreRecordStatus.Failed },
    )
}

private fun RestoreRecordResult.finish(status: RestoreProcessStatus): RestoreRecordResult = copy {
    RestoreRecordResult.status transform { it.finish(status) }
    RestoreRecordResult.parts transform { parts -> parts.mapValues { (_, partStatus) -> partStatus.finish(status) } }
}

private val RestoreRecordStatus.isCompleted: Boolean
    get() = this == RestoreRecordStatus.Restored || this == RestoreRecordStatus.Skipped || this == RestoreRecordStatus.Failed

private fun RestoreRecordStatus.finish(status: RestoreProcessStatus): RestoreRecordStatus = when (this) {
    RestoreRecordStatus.Pending -> RestoreRecordStatus.NotProcessed
    RestoreRecordStatus.Processing -> when (status) {
        RestoreProcessStatus.Canceled, RestoreProcessStatus.Interrupted -> RestoreRecordStatus.Interrupted
        else -> RestoreRecordStatus.Failed
    }
    else -> this
}
