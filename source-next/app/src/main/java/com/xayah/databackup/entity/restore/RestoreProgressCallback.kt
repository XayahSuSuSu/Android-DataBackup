package com.xayah.databackup.entity.restore

internal sealed interface RestoreRecordEvent {
    val id: String

    data class Started(override val id: String) : RestoreRecordEvent
    data class Restored(override val id: String) : RestoreRecordEvent
    data class Skipped(override val id: String) : RestoreRecordEvent
    data class Failed(override val id: String) : RestoreRecordEvent
}

internal fun interface RestoreProgressCallback {
    fun onEvent(event: RestoreRecordEvent)
}
