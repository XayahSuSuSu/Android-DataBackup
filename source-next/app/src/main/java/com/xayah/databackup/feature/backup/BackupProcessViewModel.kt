package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupState
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.service.BackupService
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

internal data class BackupCurrentApp(
    val packageName: String,
    val userId: Int,
    val label: String,
    val currentPart: BackupSourceCategory?,
    val progress: Float,
)

internal data class BackupProcessItem(
    val category: BackupCategory,
    val totalCount: Int,
    val availableCount: Int,
    val completedCount: Int,
    val outcome: BackupOutcome,
    val progress: Float,
    val currentApp: BackupCurrentApp? = null,
)

internal data class BackupProcessUiState(
    val backendType: BackupBackendType,
    val status: BackupStatus,
    val overallProgress: String,
    val items: List<BackupProcessItem>,
) {
    val isProcessing: Boolean get() = status == BackupStatus.Pending || status == BackupStatus.Running
    val isCanceling: Boolean get() = status == BackupStatus.Canceling
    val isTerminal: Boolean get() = !isProcessing && !isCanceling
    val canCancel: Boolean get() = backendType == BackupBackendType.Archive && status == BackupStatus.Running
    val isBackEnabled: Boolean
        get() = isTerminal || canCancel || (backendType == BackupBackendType.Rustic && isProcessing)
}

internal class BackupProcessViewModel(private val mProcessRepo: BackupProcessRepository, private val mTaskId: String) : BaseViewModel() {
    private val mState = mProcessRepo.getState(mTaskId)
    val uiState: StateFlow<BackupProcessUiState> = mState.map { it.toBackupProcessUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mState.value.toBackupProcessUiState())

    fun cancel() {
        // Read the task directly: a dialog or a stopped collector can outlive the displayed running state.
        val state = mState.value
        if (state.backendType == BackupBackendType.Archive && state.status == BackupStatus.Running) {
            BackupService.cancel(App.application, mTaskId)
        }
    }
}

internal fun BackupState.toBackupProcessUiState(): BackupProcessUiState {
    val isArchive = backendType == BackupBackendType.Archive
    val totalCount = categoryProgress.sumOf { it.totalCount.toLong() }
    val completedCount = categoryProgress.sumOf { it.completedCount.toLong() }
    val overallProgress = if (isArchive) {
        calculateBackupProgress(completedCount, totalCount)
    } else if (progress.isFinite()) {
        progress.coerceIn(0f, 1f)
    } else {
        0f
    }
    val currentApp = if (isArchive && status == BackupStatus.Running) {
        appsProgress.firstOrNull { it.outcome == BackupOutcome.Running }?.let { app ->
            BackupCurrentApp(
                packageName = app.packageName,
                userId = app.userId,
                label = app.label,
                currentPart = app.parts.firstOrNull { it.outcome == BackupOutcome.Running }?.category,
                progress = calculateBackupProgress(
                    app.parts.count {
                        it.outcome == BackupOutcome.Completed || it.outcome == BackupOutcome.Skipped || it.outcome == BackupOutcome.Failed
                    }.toLong(),
                    app.parts.size.toLong(),
                ),
            )
        }
    } else {
        null
    }
    return BackupProcessUiState(
        backendType = backendType,
        status = status,
        overallProgress = (overallProgress * 100).roundToInt().toString(),
        items = categoryProgress.filter { it.isSelected && it.totalCount > 0 }.map { item ->
            BackupProcessItem(
                category = item.category,
                totalCount = item.totalCount,
                availableCount = item.availableCount,
                completedCount = item.completedCount,
                outcome = item.outcome,
                progress = calculateBackupProgress(item.completedCount.toLong(), item.totalCount.toLong()),
                currentApp = currentApp.takeIf { item.category == BackupCategory.Apps },
            )
        },
    )
}

private fun calculateBackupProgress(completedCount: Long, totalCount: Long): Float = if (totalCount > 0) {
    (completedCount.toFloat() / totalCount).coerceIn(0f, 1f)
} else {
    0f
}
