package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.entity.BackupBackendType
import com.xayah.databackup.entity.restore.RestoreProcessItem
import com.xayah.databackup.entity.restore.RestoreProcessState
import com.xayah.databackup.entity.restore.RestoreProcessStatus
import com.xayah.databackup.service.RestoreService
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

internal data class RestoreProcessUiState(
    val status: RestoreProcessStatus = RestoreProcessStatus.Processing,
    val backendType: BackupBackendType = BackupBackendType.Archive,
    val items: List<RestoreProcessItem> = emptyList(),
    val errorMessage: String = "",
) {
    val isProcessing: Boolean get() = status == RestoreProcessStatus.Pending || status == RestoreProcessStatus.Processing
    val isCanceling: Boolean get() = status == RestoreProcessStatus.Canceling
    val isFailed: Boolean get() = status == RestoreProcessStatus.Failed || status == RestoreProcessStatus.Interrupted
    val isTerminal: Boolean get() = !isProcessing && !isCanceling
    val canCancel: Boolean get() = backendType == BackupBackendType.Archive && status == RestoreProcessStatus.Processing
    val canLeave: Boolean get() = backendType == BackupBackendType.Rustic && isProcessing
}

internal class RestoreProcessViewModel(private val mProcessRepo: RestoreProcessRepository, private val mTaskId: String) : BaseViewModel() {
    private val mState = mProcessRepo.getState(mTaskId)
    val uiState: StateFlow<RestoreProcessUiState> = mState.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mState.value.toUiState())
    val overallProgress: StateFlow<String> = mState.map { calculateRestoreProgress(it.items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), calculateRestoreProgress(mState.value.items))

    fun cancel() {
        val state = mState.value
        if (state.backendType == BackupBackendType.Archive && state.status == RestoreProcessStatus.Processing) {
            RestoreService.cancel(App.application, mTaskId)
        }
    }
}

private fun RestoreProcessState.toUiState(): RestoreProcessUiState = RestoreProcessUiState(
    status = status,
    backendType = backendType,
    items = items,
    errorMessage = when {
        isExpired -> App.application.getString(R.string.restore_session_expired)
        status == RestoreProcessStatus.Interrupted -> App.application.getString(R.string.restore_process_failed)
        status == RestoreProcessStatus.Failed -> App.application.getString(R.string.restore_process_failed)
        else -> ""
    },
)

internal fun calculateRestoreProgress(items: List<RestoreProcessItem>): String {
    val completedCount = items.sumOf { it.completedCount.toLong() }
    val totalCount = items.sumOf { it.totalCount.toLong() }
    return if (totalCount > 0) {
        ((completedCount.toDouble() / totalCount) * 100).roundToInt().toString()
    } else {
        "0"
    }
}
