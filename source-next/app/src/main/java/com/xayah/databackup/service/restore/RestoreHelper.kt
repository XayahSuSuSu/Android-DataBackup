package com.xayah.databackup.service.restore

import com.xayah.databackup.entity.restore.RestoreEvent
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreTask

internal fun interface RestoreHelper {
    /**
     * Reports record and app-part outcomes as they complete, and returns the skipped IDs from [RestoreTask.ids].
     */
    suspend fun restore(request: RestoreRequest, task: RestoreTask, onEvent: (RestoreEvent) -> Unit): List<String>
}
