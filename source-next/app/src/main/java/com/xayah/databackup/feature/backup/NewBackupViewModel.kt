package com.xayah.databackup.feature.backup

import arrow.optics.copy
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.backupBackend
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class NewBackupUiState(
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

class NewBackupViewModel(
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "NewBackupViewModel"
    }

    private val _uiState = MutableStateFlow(NewBackupUiState())
    val uiState: StateFlow<NewBackupUiState> = _uiState.asStateFlow()

    private val _backupBackend = MutableStateFlow<BackupBackend>(BackupBackend.Archive())
    val backupBackend: StateFlow<BackupBackend> = _backupBackend.asStateFlow()

    fun selectBackupBackend(index: Int) {
        val currentPassword =
            (backupBackend.value as? BackupBackend.Rustic)?.password ?: BackupBackend.DEFAULT_PASSWORD
        _backupBackend.value = when (index) {
            0 -> BackupBackend.Archive()
            else -> BackupBackend.Rustic(password = currentPassword)
        }
    }

    fun changeRusticPassword(password: String) {
        _backupBackend.value = BackupBackend.Rustic(password = password)
    }

    fun saveNewBackup(onSaved: () -> Unit) {
        withLock(Dispatchers.IO) {
            if (uiState.value.isSaving) return@withLock

            _uiState.value = NewBackupUiState(isSaving = true)
            try {
                backupConfigRepository.updateNewConfig {
                    copy {
                        BackupConfig.backupBackend set this@NewBackupViewModel.backupBackend.value
                    }
                }
                backupConfigRepository.saveNewBackup()
                try {
                    backupConfigRepository.resetNewBackup()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LogHelper.w(TAG, "saveNewBackup", "Failed to reset the new backup draft: ${error.message}")
                }
                _uiState.value = NewBackupUiState()
                _backupBackend.value = BackupBackend.Archive()
                withContext(Dispatchers.Main) {
                    onSaved()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogHelper.e(TAG, "saveNewBackup", "Failed to save new backup.", error)
                _uiState.value = NewBackupUiState(saveError = error.message.orEmpty())
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun discardChanges() {
        if (uiState.value.isSaving.not()) {
            _backupBackend.value = BackupBackend.Archive()
            _uiState.value = NewBackupUiState()
        }
    }
}
