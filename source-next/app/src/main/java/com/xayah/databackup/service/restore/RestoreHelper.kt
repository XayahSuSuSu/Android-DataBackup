package com.xayah.databackup.service.restore

import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRecordEvent
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreTask

internal fun interface RestoreHelper {
    /**
     * Reports record and app-part outcomes as they complete, and returns the skipped IDs from [RestoreTask.ids].
     */
    suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String>
}

internal fun RestoreTask.Records.toRestoreProgressCallback(onEvent: (RestoreEvent) -> Unit): RestoreProgressCallback =
    RestoreProgressCallback { event ->
        onEvent(when (event) {
            is RestoreRecordEvent.Started -> RestoreEvent.RecordStarted(this, event.id)
            is RestoreRecordEvent.Restored -> RestoreEvent.RecordCompleted(this, event.id, isSkipped = false)
            is RestoreRecordEvent.Skipped -> RestoreEvent.RecordCompleted(this, event.id, isSkipped = true)
            is RestoreRecordEvent.Failed -> RestoreEvent.RecordFailed(this, event.id)
        })
    }
