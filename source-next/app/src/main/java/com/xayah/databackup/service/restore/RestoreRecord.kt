package com.xayah.databackup.service.restore

import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRecordEvent
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException

private const val TAG = "RestoreRecord"

/**
 * Restores a record and reports its progress and result.
 *
 * @return true if skipped, false if restored, or null if failed.
 */
internal fun restoreRecord(
    id: String,
    callback: RestoreProgressCallback,
    operation: () -> Boolean,
): Boolean? {
    callback.onEvent(RestoreRecordEvent.Started(id))
    val skipped = runCatching {
        operation()
    }.getOrElse { error ->
        if (error is CancellationException || error !is Exception) throw error
        LogHelper.e(TAG, "restoreRecord", "Record restore failed: id=$id", error)
        callback.onEvent(RestoreRecordEvent.Failed(id))
        return null
    }
    callback.onEvent(if (skipped) RestoreRecordEvent.Skipped(id) else RestoreRecordEvent.Restored(id))
    return skipped
}
