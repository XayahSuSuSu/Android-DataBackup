package com.xayah.databackup.feature.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import arrow.optics.copy
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.backupBackend
import com.xayah.databackup.entity.name
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data object BackupConfigUiState

open class BackupConfigViewModel(
    savedStateHandle: SavedStateHandle,
    private val backupConfigRepo: BackupConfigRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "BackupConfigViewModel"
    }

    private val route = savedStateHandle.toRoute<BackupConfigRoute>()

    private val _uiState: MutableStateFlow<BackupConfigUiState> = MutableStateFlow(BackupConfigUiState)
    val uiState: StateFlow<BackupConfigUiState> = _uiState.asStateFlow()

    private val _backupConfig: Flow<BackupConfig?> = backupConfigRepo.configs.map { configs ->
        configs.getOrNull(route.index)
    }
    val backupConfig: StateFlow<BackupConfig?> = _backupConfig.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val backupBackend: StateFlow<BackupBackend> = combine(backupConfigRepo.configs, backupConfigRepo.newConfig) { configs, newConfig ->
        if (route.index == BackupConfigRepository.NEW_CONFIG_INDEX) {
            newConfig.backupBackend
        } else {
            configs.getOrNull(route.index)?.backupBackend ?: newConfig.backupBackend
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = BackupBackend.Archive(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val backupBackendSelectedIndex: StateFlow<Int> = backupBackend.map {
        when (it) {
            is BackupBackend.Rustic -> 0
            is BackupBackend.Archive -> 1
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = 1,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun selectBackupBackend(index: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val currentPassword = (backupBackend.value as? BackupBackend.Rustic)?.password ?: BackupBackend.DEFAULT_PASSWORD
            val backend = when (index) {
                0 -> BackupBackend.Rustic(password = currentPassword)
                else -> BackupBackend.Archive()
            }
            updateBackupBackend(backend)
        }
    }

    fun changeRusticPassword(password: String) {
        viewModelScope.launch(Dispatchers.Default) {
            updateBackupBackend(BackupBackend.Rustic(password = password))
        }
    }

    fun changeName(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            backupConfig.value?.also {
                backupConfigRepo.updateConfig(it.uuidString) {
                    copy {
                        BackupConfig.name set name
                    }
                }
            }
        }
    }

    fun deleteConfig(onDeleted: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            backupConfig.value?.also {
                backupConfigRepo.deleteConfig(it.uuidString)
            }
            onDeleted.invoke()
        }
    }

    private suspend fun updateBackupBackend(backend: BackupBackend) {
        if (route.index == BackupConfigRepository.NEW_CONFIG_INDEX) {
            backupConfigRepo.updateNewConfig {
                copy {
                    BackupConfig.backupBackend set backend
                }
            }
        } else {
            backupConfig.value?.also {
                backupConfigRepo.updateConfig(it.uuidString) {
                    copy {
                        BackupConfig.backupBackend set backend
                    }
                }
            }
        }
    }
}
