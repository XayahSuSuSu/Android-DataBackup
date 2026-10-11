package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreItemStatus
import com.xayah.databackup.entity.restore.RestoreProcessState
import com.xayah.databackup.entity.restore.RestoreProcessStatus
import com.xayah.databackup.entity.restore.RestoreRecordResult
import com.xayah.databackup.entity.restore.RestoreRecordStatus
import com.xayah.databackup.service.RestoreService
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

internal data class RestoreCurrentApp(
    val title: String,
    val status: RestoreRecordStatus,
    val currentPart: BackupSourceCategory?,
    val progress: Float,
)

internal data class RestoreProcessItemUiState(
    val category: RestoreCategory,
    val totalCount: Int,
    val completedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val status: RestoreItemStatus,
    val progress: Float,
    val currentApp: RestoreCurrentApp? = null,
)

internal data class RestoreProcessUiState(
    val backendType: BackupBackendType,
    val status: RestoreProcessStatus,
    val overallProgress: String,
    val items: List<RestoreProcessItemUiState>,
    val isExpired: Boolean = false,
) {
    val isProcessing: Boolean get() = status == RestoreProcessStatus.Pending || status == RestoreProcessStatus.Processing
    val isCanceling: Boolean get() = status == RestoreProcessStatus.Canceling
    val isFailed: Boolean get() = status == RestoreProcessStatus.Failed || status == RestoreProcessStatus.Interrupted
    val isTerminal: Boolean get() = !isProcessing && !isCanceling
    val canCancel: Boolean get() = backendType == BackupBackendType.Archive && status == RestoreProcessStatus.Processing
    val canLeave: Boolean get() = backendType == BackupBackendType.Rustic && isProcessing
    val isBackEnabled: Boolean get() = isTerminal || canCancel || canLeave
}

internal class RestoreProcessViewModel(private val mProcessRepo: RestoreProcessRepository, private val mTaskId: String) : BaseViewModel() {
    private val mState = mProcessRepo.getState(mTaskId)
    val uiState: StateFlow<RestoreProcessUiState> = mState.map { it.toRestoreProcessUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mState.value.toRestoreProcessUiState())

    fun cancel() {
        // A dialog can outlive the displayed running state; validate against the task itself.
        val state = mState.value
        if (state.backendType == BackupBackendType.Archive && state.status == RestoreProcessStatus.Processing) {
            RestoreService.cancel(App.application, mTaskId)
        }
    }
}

internal fun RestoreProcessState.toRestoreProcessUiState(): RestoreProcessUiState = RestoreProcessUiState(
    status = status,
    backendType = backendType,
    overallProgress = (calculateRestoreProgress(
        items.sumOf { it.completedCount.toLong() },
        items.sumOf { it.totalCount.toLong() },
    ) * 100).roundToInt().toString(),
    isExpired = isExpired,
    items = items.filter { it.totalCount > 0 }.map { item ->
        val currentApp = if (item.category == RestoreCategory.Apps && isActive) {
            item.records.firstOrNull { it.status == RestoreRecordStatus.Processing }
                ?: item.records.lastOrNull { it.status != RestoreRecordStatus.Pending && it.status != RestoreRecordStatus.NotProcessed }
                ?: item.records.firstOrNull()
        } else {
            null
        }
        RestoreProcessItemUiState(
            category = item.category,
            totalCount = item.totalCount,
            completedCount = item.completedCount,
            skippedCount = item.skippedCount,
            failedCount = item.failedCount,
            status = item.status,
            progress = calculateRestoreProgress(item.completedCount.toLong(), item.totalCount.toLong()).toFloat(),
            currentApp = currentApp?.toCurrentApp(),
        )
    },
)

private fun RestoreRecordResult.toCurrentApp(): RestoreCurrentApp = RestoreCurrentApp(
    title = title,
    status = status,
    currentPart = (parts.entries.firstOrNull { it.value == RestoreRecordStatus.Processing }
        ?: parts.entries.lastOrNull { it.value != RestoreRecordStatus.Pending && it.value != RestoreRecordStatus.NotProcessed }
        ?: parts.entries.firstOrNull())?.key,
    progress = when {
        status == RestoreRecordStatus.Restored || status == RestoreRecordStatus.Skipped -> 1f
        parts.isEmpty() -> 0f
        else -> parts.values.count {
            it == RestoreRecordStatus.Restored || it == RestoreRecordStatus.Skipped ||
                it == RestoreRecordStatus.Failed || it == RestoreRecordStatus.Interrupted
        }.toFloat() / parts.size
    },
)

private fun calculateRestoreProgress(completedCount: Long, totalCount: Long): Double = if (totalCount > 0) {
    (completedCount.toDouble() / totalCount).coerceIn(0.0, 1.0)
} else {
    0.0
}
