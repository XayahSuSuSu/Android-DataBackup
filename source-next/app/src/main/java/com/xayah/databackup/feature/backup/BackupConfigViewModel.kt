package com.xayah.databackup.feature.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import arrow.optics.copy
import com.xayah.databackup.App
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.AppsBackupStrategy
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.name
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DefaultAppsBackupStrategy
import com.xayah.databackup.util.KeyDefaultAppsBackupStrategy
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.saveEnum
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

    val appsBackupStrategySelectedIndex: StateFlow<Int> = combine(
        _backupConfig,
        App.application.readEnum(DefaultAppsBackupStrategy)
    ) { config, default ->
        (config?.appsBackupStrategy ?: default).let {
            when (it) {
                AppsBackupStrategy.Incremental -> 0
                AppsBackupStrategy.Standalone -> 1
            }
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun selectAppsBackupStrategy(index: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val strategy = when (index) {
                0 -> AppsBackupStrategy.Incremental
                1 -> AppsBackupStrategy.Standalone
                else -> AppsBackupStrategy.Standalone
            }
            App.application.saveEnum(KeyDefaultAppsBackupStrategy, strategy)
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
}
