package com.xayah.databackup.feature.backup.archive

import androidx.lifecycle.viewModelScope
import arrow.optics.optics
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.entity.backup.BackupAppProgress
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupOutcome
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.backup.BackupState
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.entity.backup.STATUS_CANCEL
import com.xayah.databackup.entity.backup.STATUS_ERROR
import com.xayah.databackup.entity.backup.STATUS_SKIP
import com.xayah.databackup.entity.backup.STATUS_SUCCESS
import com.xayah.databackup.service.BackupService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.formatToStorageSizePerSecond
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

enum class BackupProcessStatus { Processing, Canceling, Canceled, Finished, Failed, Interrupted, PartialFailure }

@optics
data class BackupProcessUiState(
    val isLoaded: Boolean = false,
    val status: BackupProcessStatus = BackupProcessStatus.Processing,
) {
    companion object
    val isProcessing: Boolean = status == BackupProcessStatus.Processing
    val isCanceling: Boolean = status == BackupProcessStatus.Canceling
    val isCanceled: Boolean = status == BackupProcessStatus.Canceled
    val canBeCanceled: Boolean = isLoaded && status == BackupProcessStatus.Processing
}

class BackupProcessViewModel(private val mProcessRepo: BackupProcessRepository, val taskId: String) : BaseViewModel() {
    private val mState = mProcessRepo.getState(taskId)
    val uiState: StateFlow<BackupProcessUiState> = mapState { state ->
        BackupProcessUiState(state.status != BackupStatus.Pending, when (state.status) {
            BackupStatus.Pending, BackupStatus.Running -> BackupProcessStatus.Processing
            BackupStatus.Canceling -> BackupProcessStatus.Canceling
            BackupStatus.Canceled -> BackupProcessStatus.Canceled
            BackupStatus.Completed -> BackupProcessStatus.Finished
            BackupStatus.PartialFailure -> BackupProcessStatus.PartialFailure
            BackupStatus.Failed -> BackupProcessStatus.Failed
            BackupStatus.Interrupted -> BackupProcessStatus.Interrupted
        })
    }
    val appsItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.Apps)
    val filesItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.Files)
    val networksItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.Networks)
    val contactsItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.Contacts)
    val callLogsItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.CallLogs)
    val messagesItem: StateFlow<ProcessItem> = categoryItem(BackupCategory.Messages)
    val allProcessedAppItems: StateFlow<List<ProcessAppItem>> = mapState { it.appsProgress.map { app -> app.toUiItem() } }
    val failedProcessedAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Failed)
    val canceledProcessedAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Canceled)
    val succeededProcessedAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Completed)
    val skippedAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Skipped)
    val unprocessedAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Pending, BackupOutcome.Unprocessed)
    val runningAppItems: StateFlow<List<ProcessAppItem>> = appItems(BackupOutcome.Running)
    val overallProgress: StateFlow<String> = mapState { state ->
        val total = state.categoryProgress.sumOf { it.totalCount }
        val completed = state.categoryProgress.sumOf { it.completedCount }
        if (total > 0) {
            (completed.toFloat() * 100 / total).roundToInt().coerceIn(0, 100).toString()
        } else {
            "0"
        }
    }

    fun cancel() {
        if (uiState.value.canBeCanceled) {
            BackupService.cancel(App.application, taskId)
        }
    }

    private fun <T> mapState(transform: (BackupState) -> T): StateFlow<T> = mState.map(transform)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), transform(mState.value))

    private fun appItems(vararg outcomes: BackupOutcome): StateFlow<List<ProcessAppItem>> = mapState { state ->
        state.appsProgress.filter { it.outcome in outcomes }.map { it.toUiItem() }
    }

    private fun categoryItem(category: BackupCategory): StateFlow<ProcessItem> = mapState { state ->
        val item = state.categoryProgress.firstOrNull { it.category == category }
        ProcessItem(
            isLoading = state.status == BackupStatus.Pending,
            isSelected = item?.isSelected == true,
            currentIndex = item?.completedCount ?: 0,
            totalCount = item?.totalCount ?: 0,
            msg = if (item?.outcome == BackupOutcome.Running && item.currentLabel.isNotBlank()) {
                item.currentLabel
            } else {
                getOutcomeLabel(item?.outcome ?: BackupOutcome.Unprocessed)
            },
            progress = if (item != null && item.totalCount > 0) {
                item.completedCount.toFloat() / item.totalCount
            } else {
                0f
            },
        )
    }
}

private fun getOutcomeLabel(outcome: BackupOutcome): String = App.application.getString(when (outcome) {
    BackupOutcome.Pending -> R.string.idle
    BackupOutcome.Running -> R.string.processing
    BackupOutcome.Completed -> R.string.succeed
    BackupOutcome.Skipped -> R.string.skip
    BackupOutcome.Failed -> R.string.failed
    BackupOutcome.Canceled -> R.string.canceled
    BackupOutcome.Unprocessed -> R.string.restore_result_not_processed
})

private fun BackupAppProgress.toUiItem(): ProcessAppItem {
    fun partItem(category: BackupSourceCategory, title: Int): ProcessAppDataItem {
        val selectedParts = parts.filter { it.category == category }
        val partOutcome = when {
            selectedParts.isEmpty() -> BackupOutcome.Skipped
            selectedParts.any { it.outcome == BackupOutcome.Running } -> BackupOutcome.Running
            selectedParts.any { it.outcome == BackupOutcome.Failed } -> BackupOutcome.Failed
            selectedParts.any { it.outcome == BackupOutcome.Canceled } -> BackupOutcome.Canceled
            selectedParts.any { it.outcome == BackupOutcome.Unprocessed } -> BackupOutcome.Unprocessed
            selectedParts.any { it.outcome == BackupOutcome.Pending } -> BackupOutcome.Pending
            selectedParts.all { it.outcome == BackupOutcome.Skipped } -> BackupOutcome.Skipped
            else -> BackupOutcome.Completed
        }
        return ProcessAppDataItem(
            enabled = selectedParts.isNotEmpty(),
            title = App.application.getString(title),
            subtitle = if (selectedParts.isEmpty()) {
                App.application.getString(R.string.not_selected)
            } else {
                selectedParts.sumOf { it.bytes }.formatToStorageSize
            },
            msg = if (partOutcome == BackupOutcome.Running) {
                selectedParts.sumOf { it.speed }.formatToStorageSizePerSecond
            } else {
                getOutcomeLabel(partOutcome)
            },
            details = selectedParts.map {
                ProcessAppDataDetailItem(it.bytes, it.speed, when (it.outcome) {
                    BackupOutcome.Failed -> if (it.exitCode != 0) {
                        it.exitCode
                    } else {
                        STATUS_ERROR
                    }
                    BackupOutcome.Canceled -> STATUS_CANCEL
                    BackupOutcome.Skipped -> STATUS_SKIP
                    else -> STATUS_SUCCESS
                }, it.diagnostics.ifBlank { getOutcomeLabel(it.outcome) })
            },
        )
    }
    return ProcessAppItem(
        label = label, packageName = packageName, userId = userId,
        apkItem = partItem(BackupSourceCategory.Apk, R.string.apk),
        intDataItem = partItem(BackupSourceCategory.InternalData, R.string.internal_data),
        extDataItem = partItem(BackupSourceCategory.ExternalData, R.string.external_data),
        addlDataItem = partItem(BackupSourceCategory.AdditionalData, R.string.additional_data),
        progress = if (parts.isEmpty()) {
            0f
        } else {
            parts.count { it.outcome in listOf(BackupOutcome.Completed, BackupOutcome.Skipped, BackupOutcome.Failed) }.toFloat() / parts.size
        },
    )
}
