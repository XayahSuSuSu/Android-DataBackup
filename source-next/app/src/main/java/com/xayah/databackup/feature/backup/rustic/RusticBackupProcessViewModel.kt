package com.xayah.databackup.feature.backup.rustic

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.entity.backup.BackupCategory
import com.xayah.databackup.entity.backup.BackupStatus
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RusticBackupProcessViewModel(private val mProcessRepo: BackupProcessRepository, val taskId: String) : BaseViewModel() {
    companion object {
        private const val TAG = "RusticBackupProcessViewModel"
    }
    private val mState = mProcessRepo.getState(taskId)
    private val _uiState = MutableStateFlow(RusticBackupProcessUiState())
    val uiState: StateFlow<RusticBackupProcessUiState> = _uiState.asStateFlow()
    val sourceItems: StateFlow<List<RusticBackupSourceUiItem>> = mState.map { state ->
        state.categoryProgress.map { category ->
            val (title, icon) = when (category.category) {
                BackupCategory.Apps -> R.string.apps to R.drawable.ic_layout_grid
                BackupCategory.Files -> R.string.files to R.drawable.ic_folder
                BackupCategory.Networks -> R.string.networks to R.drawable.ic_wifi
                BackupCategory.Contacts -> R.string.contacts to R.drawable.ic_user_round
                BackupCategory.CallLogs -> R.string.call_logs to R.drawable.ic_phone
                BackupCategory.Messages -> R.string.messages to R.drawable.ic_message_circle
            }
            RusticBackupSourceUiItem(title, icon, category.totalCount, category.availableCount)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val overallProgress: StateFlow<String> = uiState.map { it.progress.progressPercent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0")

    init {
        viewModelScope.launch {
            mState.collect { state ->
                _uiState.update { previous ->
                    previous.copy(
                        isLoaded = true,
                        status = when (state.status) {
                            BackupStatus.Pending, BackupStatus.Running, BackupStatus.Canceling -> RusticBackupProcessStatus.Processing
                            BackupStatus.Completed -> RusticBackupProcessStatus.Finished
                            else -> RusticBackupProcessStatus.Failed
                        },
                        backupName = state.backupName.ifBlank { App.application.getString(R.string.unnamed) },
                        repositoryPath = if (state.backupPath.isNotBlank()) {
                            PathHelper.getBackupRepoDir(state.backupPath)
                        } else {
                            ""
                        },
                        isPasswordProtected = state.isPasswordProtected,
                        snapshotId = state.result.snapshotId,
                        progress = RusticBackupProgressUiState(state.bytes, state.speed, state.progress),
                        steps = rusticBackupProcessSteps().mapIndexed { index, step ->
                            step.copy(status = if (index == state.stage.ordinal) {
                                RusticBackupStepStatus.Active
                            } else {
                                RusticBackupStepStatus.Pending
                            })
                        },
                        errorMessage = when (state.status) {
                            BackupStatus.Interrupted -> App.application.getString(R.string.backup_interrupted)
                            BackupStatus.PartialFailure -> App.application.getString(R.string.backup_partial_failure)
                            BackupStatus.Failed -> App.application.getString(R.string.rustic_backup_failed)
                            else -> ""
                        },
                    )
                }
            }
        }
    }

    fun refreshRepositoryStorage() {
        _uiState.update { state ->
            state.copy(repositoryStorage = state.repositoryStorage.copy(isLoading = state.repositoryStorage.isAvailable.not()))
        }
        viewModelScope.launch(Dispatchers.IO) {
            val repositoryPath = uiState.value.repositoryPath
            if (repositoryPath.isBlank()) {
                _uiState.update { state ->
                    state.copy(
                        repositoryStorage = state.repositoryStorage.takeIf { it.isAvailable } ?: RusticRepositoryStorageUiState(isLoading = false),
                    )
                }
                return@launch
            }

            val storagePath = PathHelper.getParentPath(repositoryPath).ifBlank { repositoryPath }
            val stat = runCatching { RemoteRootService.readStatFs(storagePath) }.onFailure { error ->
                if (error is CancellationException) throw error
                LogHelper.e(TAG, "refreshRepositoryStorage", "Failed to read storage statistics.", error)
            }.getOrNull()
            if (stat == null || stat.totalBytes <= 0L) {
                _uiState.update { state ->
                    state.copy(
                        repositoryStorage = state.repositoryStorage.takeIf { it.isAvailable } ?: RusticRepositoryStorageUiState(isLoading = false),
                    )
                }
                return@launch
            }

            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes.coerceIn(0L, totalBytes)
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val repositoryBytes = runCatching { RemoteRootService.calculateTreeSize(repositoryPath) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    LogHelper.e(TAG, "refreshRepositoryStorage", "Failed to calculate repository size.", error)
                }
                .getOrDefault(0L)
                .coerceIn(0L, usedBytes)
            val otherBytes = (usedBytes - repositoryBytes).coerceAtLeast(0L)

            _uiState.update { state ->
                state.copy(
                    repositoryStorage = RusticRepositoryStorageUiState(
                        isLoading = false,
                        repositoryBytes = repositoryBytes,
                        otherBytes = otherBytes,
                        freeBytes = freeBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
        }
    }

}
