package com.xayah.databackup.feature.restore

import androidx.annotation.FloatRange
import com.xayah.databackup.data.restore.RestoreCategory
import com.xayah.databackup.data.rustic.RusticSourceCategory

internal enum class RestoreProcessStatus {
    Processing,
    Canceling,
    Canceled,
    Finished,
    FinishedWithErrors,
    Failed,
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
}

internal data class RestoreRecordResult(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val status: RestoreRecordStatus = RestoreRecordStatus.Pending,
    val parts: Map<RusticSourceCategory, RestoreRecordStatus> = emptyMap(),
) {
    val currentPart: RusticSourceCategory? =
        (parts.entries.firstOrNull { it.value == RestoreRecordStatus.Processing }
            ?: parts.entries.lastOrNull { it.value != RestoreRecordStatus.Pending && it.value != RestoreRecordStatus.NotProcessed }
            ?: parts.entries.firstOrNull())?.key

    val progress: Float =
        when {
            status == RestoreRecordStatus.Restored || status == RestoreRecordStatus.Skipped -> 1f
            parts.isEmpty() -> 0f
            else -> parts.values.count { it == RestoreRecordStatus.Restored || it == RestoreRecordStatus.Skipped || it == RestoreRecordStatus.Failed }
                .toFloat() / parts.size
        }
}

internal data class RestoreProcessItem(
    val category: RestoreCategory,
    val totalCount: Int,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val status: RestoreItemStatus = RestoreItemStatus.Pending,
    val message: String = "",
    val records: List<RestoreRecordResult> = emptyList(),
    @FloatRange(0.0, 1.0) val progress: Float = 0f,
) {
    val currentApp: RestoreRecordResult? =
        if (category == RestoreCategory.Apps) {
            records.firstOrNull { it.status == RestoreRecordStatus.Processing }
                ?: records.lastOrNull { it.status != RestoreRecordStatus.Pending && it.status != RestoreRecordStatus.NotProcessed }
                ?: records.firstOrNull()
        } else {
            null
        }
}

/** Presentation state only; the restore coordinator supplies progress and terminal results. */
internal data class RestoreProcessUiState(
    val status: RestoreProcessStatus = RestoreProcessStatus.Processing,
    val items: List<RestoreProcessItem> = emptyList(),
    val errorMessage: String = "",
) {
    val isProcessing: Boolean get() = status == RestoreProcessStatus.Processing
    val isCanceling: Boolean get() = status == RestoreProcessStatus.Canceling
    val isFailed: Boolean get() = status == RestoreProcessStatus.Failed
    val isTerminal: Boolean
        get() = status == RestoreProcessStatus.Canceled || status == RestoreProcessStatus.Finished
                || status == RestoreProcessStatus.FinishedWithErrors || status == RestoreProcessStatus.Failed
}
