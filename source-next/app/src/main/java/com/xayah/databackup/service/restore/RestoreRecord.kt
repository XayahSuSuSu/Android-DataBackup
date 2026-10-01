package com.xayah.databackup.service.restore

import com.xayah.databackup.data.restore.RestoreProgressCallback
import com.xayah.databackup.data.restore.RestoreRecordEvent
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
    val skipped = try {
        operation()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        LogHelper.e(TAG, "restoreRecord", "Record restore failed: id=$id", error)
        callback.onEvent(RestoreRecordEvent.Failed(id))
        return null
    }
    callback.onEvent(if (skipped) RestoreRecordEvent.Skipped(id) else RestoreRecordEvent.Restored(id))
    return skipped
}
