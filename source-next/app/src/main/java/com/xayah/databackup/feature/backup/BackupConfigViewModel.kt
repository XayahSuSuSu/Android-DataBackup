package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.name
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

open class BackupConfigViewModel(
    private val route: BackupConfigRoute,
    private val backupConfigRepo: BackupConfigRepository,
) : BaseViewModel() {
    companion object {
        private val sharingStarted = SharingStarted.WhileSubscribed(5_000)
    }

    private val currentConfig: BackupConfig?
        get() = backupConfigRepo.configs.value.getOrNull(route.index)

    val backupConfig: StateFlow<BackupConfig?> =
        backupConfigRepo.configs.map { configs ->
            configs.getOrNull(route.index)
        }.stateIn(
            scope = viewModelScope,
            initialValue = currentConfig,
            started = sharingStarted,
        )

    fun changeName(name: String) {
        withLock(Dispatchers.Default) {
            currentConfig?.let { config ->
                backupConfigRepo.updateConfig(config.uuidString) {
                    copy {
                        BackupConfig.name set name
                    }
                }
            }
        }
    }

    fun deleteConfig(onDeleted: suspend () -> Unit) {
        withLock(Dispatchers.Default) {
            currentConfig?.let { config ->
                backupConfigRepo.deleteConfig(config.uuidString)
            }
            onDeleted()
        }
    }

    fun selectBackup(onSelected: () -> Unit) {
        withLock(Dispatchers.IO) {
            backupConfigRepo.selectBackup(route.index)
            withContext(Dispatchers.Main) {
                onSelected()
            }
        }
    }
}
