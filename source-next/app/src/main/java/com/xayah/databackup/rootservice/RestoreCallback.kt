package com.xayah.databackup.rootservice

import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.restore.RestoreRecordEvent

internal fun IRestoreCallback.asProgressCallback(): RestoreProgressCallback = RestoreProgressCallback { event ->
    when (event) {
        is RestoreRecordEvent.Started -> onStarted(event.id)
        is RestoreRecordEvent.Restored -> onCompleted(event.id, false)
        is RestoreRecordEvent.Skipped -> onCompleted(event.id, true)
        is RestoreRecordEvent.Failed -> onFailed(event.id)
    }
}

internal fun RestoreProgressCallback.asBinderCallback(): IRestoreCallback = object : IRestoreCallback.Stub() {
    override fun onStarted(id: String) = onEvent(RestoreRecordEvent.Started(id))

    override fun onCompleted(id: String, skipped: Boolean) = onEvent(
        if (skipped) {
            RestoreRecordEvent.Skipped(id)
        } else {
            RestoreRecordEvent.Restored(id)
        }
    )

    override fun onFailed(id: String) = onEvent(RestoreRecordEvent.Failed(id))
}
