package com.xayah.feature.main.task.packages.cloud.backup.processing

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageBackupOpRepository
import com.xayah.core.data.repository.PackageBackupRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.TaskEntity
import com.xayah.core.model.EmojiString
import com.xayah.core.model.OpType
import com.xayah.core.model.ProcessingState
import com.xayah.core.model.TaskType
import com.xayah.core.service.packages.backup.cloud.BackupService
import com.xayah.core.util.DateUtil
import com.xayah.core.util.command.Rclone
import com.xayah.feature.main.task.packages.cloud.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val timestampState: Long = DateUtil.getTimestamp(),
    val processingState: ProcessingState = ProcessingState.Idle,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Initialize : IndexUiIntent()
    object Process : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    packageBackupRepository: PackageBackupRepository,
    packageBackupOpRepository: PackageBackupOpRepository,
    private val taskRepository: TaskRepository,
    private val backupService: BackupService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                if (taskState.value == null) {
                    taskRepository.upsertTask(
                        TaskEntity(
                            timestamp = state.timestampState,
                            opType = OpType.BACKUP,
                            taskType = TaskType.PACKAGE,
                            startTimestamp = 0,
                            endTimestamp = 0,
                            path = taskRepository.getRcloneMainAccountRemote(),
                            rawBytes = taskRepository.getPackagesBackupRawBytes(),
                            availableBytes = 0.toDouble(),
                            totalBytes = 0.toDouble(),
                        )
                    )
                }
            }

            is IndexUiIntent.Process -> {
                if (uiState.value.processingState == ProcessingState.Idle) {
                    emitStateSuspend(uiState.value.copy(processingState = ProcessingState.Processing))

                    // Check server connection.
                    val mainAccountName = taskRepository.getRcloneMainAccountName()
                    val remote = taskRepository.getRcloneMainAccountRemote()
                    if (mainAccountName.isEmpty()) {
                        emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                        emitEffectSuspend(
                            IndexUiEffect.ShowSnackbar(
                                message = taskRepository.getString(R.string.main_account_not_set),
                                duration = SnackbarDuration.Long
                            )
                        )
                        emitStateSuspend(uiState.value.copy(processingState = ProcessingState.Idle))
                        return
                    } else {
                        Rclone.testIO(remote = remote).also { result ->
                            if (result.isSuccess.not()) {
                                emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                                emitEffectSuspend(IndexUiEffect.ShowSnackbar(message = result.outString, duration = SnackbarDuration.Long))
                                emitStateSuspend(uiState.value.copy(processingState = ProcessingState.Idle))
                                return
                            }
                        }
                    }


                    launchOnIO {
                        taskRepository.updateStartTimestamp(timestamp = state.timestampState, startTimestamp = DateUtil.getTimestamp())

                        while (uiState.value.processingState == ProcessingState.Processing) {
                            delay(1000)
                            taskRepository.updateEndTimestamp(timestamp = state.timestampState, endTimestamp = DateUtil.getTimestamp())
                        }
                    }

                    val backupPreprocessing = backupService.preprocessing()
                    backupService.processing(timestamp = state.timestampState)
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = taskRepository.getString(R.string.wait_for_remaining_data_processing),
                            duration = SnackbarDuration.Indefinite
                        )
                    )
                    backupService.postProcessing(backupPreprocessing = backupPreprocessing, timestamp = state.timestampState)
                    backupService.destroyService()
                    emitStateSuspend(uiState.value.copy(processingState = ProcessingState.DONE))
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = taskRepository.getString(R.string.backup_completed) + EmojiString.PARTY_POPPER.emoji
                        )
                    )
                }
            }
        }
    }

    val taskState: StateFlow<TaskEntity?> = taskRepository.getTaskOrNull(uiState.value.timestampState).stateInScope(initialValue = null)
    val taskTimerState: StateFlow<String> = taskState.map { cur ->
        if (cur != null && cur.startTimestamp != 0L && cur.endTimestamp != 0L)
            taskRepository.getShortRelativeTimeSpanString(cur.startTimestamp, cur.endTimestamp)
        else
            taskRepository.getShortRelativeTimeSpanString(0, 0)
    }.stateInScope(initialValue = taskRepository.getShortRelativeTimeSpanString(0, 0))

    val packagesState: StateFlow<List<PackageBackupEntire>> = packageBackupRepository.selectedPackages.stateInScope(initialValue = listOf())
    val packagesApkOnlyState: StateFlow<List<PackageBackupEntire>> = packageBackupRepository.packagesApkOnly.stateInScope(initialValue = listOf())
    val packagesDataOnlyState: StateFlow<List<PackageBackupEntire>> = packageBackupRepository.packagesDataOnly.stateInScope(initialValue = listOf())
    val packagesBothState: StateFlow<List<PackageBackupEntire>> = packageBackupRepository.packagesBoth.stateInScope(initialValue = listOf())

    val operationsProcessingState: StateFlow<List<PackageBackupOperation>> =
        packageBackupOpRepository.getOperationsProcessing(uiState.value.timestampState).stateInScope(initialValue = listOf())
    val operationsFailedState: StateFlow<List<PackageBackupOperation>> =
        packageBackupOpRepository.getOperationsFailed(uiState.value.timestampState).stateInScope(initialValue = listOf())
    val operationsSucceedState: StateFlow<List<PackageBackupOperation>> =
        packageBackupOpRepository.getOperationsSucceed(uiState.value.timestampState).stateInScope(initialValue = listOf())
}
