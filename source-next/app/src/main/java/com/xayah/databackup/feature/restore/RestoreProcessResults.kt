package com.xayah.databackup.feature.restore

import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreInventory
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreTask

internal fun RestoreRequest.toInitialProcessUiState(inventory: RestoreInventory?): RestoreProcessUiState = RestoreProcessUiState(
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
                val subtitle = when (task) {
                    is RestoreTask.App -> task.source.let { "${it.packageName} · ${it.userId}" }
                    is RestoreTask.Records -> when (category) {
                        RestoreCategory.CallLogs -> inventory?.callLogs?.get(id)?.description.orEmpty()
                        RestoreCategory.Messages -> inventory?.sms?.get(id)?.body ?: inventory?.mms?.get(id)?.body?.text.orEmpty()
                        else -> ""
                    }
                }
                RestoreRecordResult(
                    id = id,
                    title = title,
                    subtitle = subtitle.take(160),
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

internal fun RestoreProcessUiState.reduceRestoreProcessState(event: RestoreEvent): RestoreProcessUiState {
    val task = event.task
    val ids = task.ids.toSet()
    return copy(items = items.map { item ->
        if (item.category != task.category) {
            return@map item
        }
        val records = item.records.map { record ->
            if (record.id !in ids) {
                return@map record
            }
            when (event) {
                is RestoreEvent.Started -> if (task is RestoreTask.App) {
                    record.copy(status = RestoreRecordStatus.Processing)
                } else {
                    record
                }

                is RestoreEvent.Completed -> if (task is RestoreTask.App) {
                    record.copy(status = RestoreRecordStatus.Restored)
                } else {
                    record
                }

                is RestoreEvent.Failed -> record.copy(
                    status = when (record.status) {
                        RestoreRecordStatus.Pending, RestoreRecordStatus.Processing -> RestoreRecordStatus.Failed
                        else -> record.status
                    },
                    parts = record.parts.mapValues { (_, status) ->
                        when (status) {
                            RestoreRecordStatus.Processing -> RestoreRecordStatus.Failed
                            RestoreRecordStatus.Pending -> RestoreRecordStatus.NotProcessed
                            else -> status
                        }
                    },
                )

                is RestoreEvent.RecordFailed -> if (record.id == event.id) {
                    record.copy(status = RestoreRecordStatus.Failed)
                } else {
                    record
                }

                is RestoreEvent.RecordStarted -> if (record.id == event.id) {
                    record.copy(status = RestoreRecordStatus.Processing)
                } else {
                    record
                }

                is RestoreEvent.RecordCompleted -> if (record.id == event.id) {
                    record.copy(status = if (event.skipped) RestoreRecordStatus.Skipped else RestoreRecordStatus.Restored)
                } else {
                    record
                }

                is RestoreEvent.AppPartStarted -> record.copy(parts = record.parts.mapValues { (part, status) ->
                    if (part in event.parts) RestoreRecordStatus.Processing else status
                })

                is RestoreEvent.AppPartCompleted -> record.copy(parts = record.parts.mapValues { (part, status) ->
                    if (part in event.parts) RestoreRecordStatus.Restored else status
                })
            }
        }
        val failed = records.count { it.status == RestoreRecordStatus.Failed }
        val count = records.count { it.status == RestoreRecordStatus.Restored || it.status == RestoreRecordStatus.Skipped } + failed
        item.copy(
            completedCount = count,
            skippedCount = records.count { it.status == RestoreRecordStatus.Skipped },
            failedCount = failed,
            progress = if (item.totalCount == 0) 0f else count.toFloat() / item.totalCount,
            status = when {
                event is RestoreEvent.Failed -> RestoreItemStatus.Failed
                count < item.totalCount -> RestoreItemStatus.Processing
                failed > 0 || item.status == RestoreItemStatus.Failed -> RestoreItemStatus.Failed
                else -> RestoreItemStatus.Finished
            },
            records = records,
        )
    })
}

internal fun RestoreProcessUiState.toTerminalState(finished: Boolean): RestoreProcessUiState = copy(
    status = when {
        !finished -> RestoreProcessStatus.Canceled
        items.any { it.status == RestoreItemStatus.Failed || it.failedCount > 0 } -> RestoreProcessStatus.FinishedWithErrors
        else -> RestoreProcessStatus.Finished
    },
    items = items.map { item ->
        item.copy(
            status = if (!finished && item.status !in setOf(RestoreItemStatus.Finished, RestoreItemStatus.Failed)) {
                RestoreItemStatus.Canceled
            } else {
                item.status
            },
            records = item.records.map { record ->
                record.copy(
                    status = if (record.status == RestoreRecordStatus.Pending) RestoreRecordStatus.NotProcessed else record.status,
                    parts = record.parts.mapValues { (_, status) ->
                        if (status == RestoreRecordStatus.Pending) RestoreRecordStatus.NotProcessed else status
                    },
                )
            },
        )
    },
)

internal fun RestoreProcessUiState.toFailedState(message: String): RestoreProcessUiState = copy(
    status = RestoreProcessStatus.Failed,
    errorMessage = message,
    items = items.map { item ->
        item.copy(
            status = if (item.status == RestoreItemStatus.Processing) RestoreItemStatus.Failed else item.status,
            records = item.records.map { record ->
                record.copy(
                    status = record.status.afterFailure(),
                    parts = record.parts.mapValues { (_, status) -> status.afterFailure() },
                )
            },
        )
    },
)

private fun RestoreRecordStatus.afterFailure(): RestoreRecordStatus = when (this) {
    RestoreRecordStatus.Processing -> RestoreRecordStatus.Failed
    RestoreRecordStatus.Pending -> RestoreRecordStatus.NotProcessed
    else -> this
}
