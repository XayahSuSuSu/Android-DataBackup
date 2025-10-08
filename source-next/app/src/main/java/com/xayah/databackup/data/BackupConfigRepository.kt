package com.xayah.databackup.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.adapter.UuidJsonAdapter
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.Source
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BackupConfigSelectedUuid
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.TimeHelper
import com.xayah.databackup.util.readString
import com.xayah.databackup.util.saveString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class BackupConfigRepository {
    companion object {
        private const val TAG = "BackupConfigRepository"
    }

    private val moshi: Moshi = Moshi.Builder().add(UuidJsonAdapter()).build()

    private var _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(-1) // -1: Create a new backup
    private val _configs: MutableStateFlow<List<BackupConfig>> = MutableStateFlow(listOf())
    private val _newConfig: MutableStateFlow<BackupConfig> = MutableStateFlow(BackupConfig())

    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    val configs: StateFlow<List<BackupConfig>> = _configs.asStateFlow()

    fun getCurrentConfig(): BackupConfig {
        return if (_selectedIndex.value == -1) {
            _newConfig.value
        } else {
            _configs.value[_selectedIndex.value]
        }
    }

    suspend fun selectBackup(index: Int) {
        _selectedIndex.emit(index)
        if (index == -1) {
            App.application.saveString(BackupConfigSelectedUuid.first, "")
        } else {
            App.application.saveString(BackupConfigSelectedUuid.first, _configs.value[_selectedIndex.value].uuid.toString())
        }

    }

    private suspend fun deduplicateConfigs(configs: List<BackupConfig>) {
        val duplicatedConfigs = configs.groupBy { it.uuid }.filter { it.value.size > 1 }
        if (duplicatedConfigs.isNotEmpty()) {
            duplicatedConfigs.forEach { (_, configs) ->
                configs.forEach {
                    it.uuid = Uuid.random()
                    saveBackupConfig(it)
                }
            }
        }
    }

    suspend fun createNewBackup(path: String) {
        val newConfigTimestamp = System.currentTimeMillis()
        _newConfig.emit(
            BackupConfig(
                source = Source.LOCAL,
                path = "$path/${TimeHelper.formatTimestampInDetail(newConfigTimestamp)}",
                createdAt = newConfigTimestamp
            )
        )

        LogHelper.i(TAG, "loadBackupConfigsFromLocal", "newConfig: ${_newConfig.value}")
    }

    suspend fun loadBackupConfigsFromLocal() {
        withContext(Dispatchers.IO) {
            val localBackupPath = PathHelper.getBackupPathBackups().first()
            createNewBackup(localBackupPath)
            val localConfigs = mutableListOf<BackupConfig>()
            RemoteRootService.listFilePaths(path = localBackupPath, listFiles = false, listDirs = true).forEach {
                val config = RemoteRootService.readText(PathHelper.getBackupConfigFile(it.path))
                if (it.isDirectory) {
                    val backupConfig = runCatching { moshi.adapter<BackupConfig>().fromJson(config) }.getOrNull()
                    (backupConfig ?: BackupConfig()).also { config ->
                        config.source = Source.LOCAL
                        config.path = it.path
                        localConfigs.add(config)
                    }
                }
            }
            deduplicateConfigs(localConfigs)
            localConfigs.sortByDescending { it.updatedAt }
            _configs.emit(localConfigs)
            val selectedUuid = App.application.readString(BackupConfigSelectedUuid).first()
            _selectedIndex.emit(_configs.value.indexOfFirst { it.uuid.toString() == selectedUuid })
            LogHelper.i(TAG, "loadBackupConfigsFromLocal", "configs: ${_configs.value}")
        }
    }

    suspend fun saveBackupConfig(config: BackupConfig) {
        val configPath = PathHelper.getBackupConfigFile(config.path)
        val configParentPath = PathHelper.getParentPath(configPath)
        if (RemoteRootService.mkdirs(configParentPath).not()) {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to mkdirs: $configParentPath.")
        }

        val json = runCatching {
            moshi.adapter<BackupConfig>().toJson(config)
        }.onFailure {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to serialize to json.", it)
        }.getOrNull()
        if (json != null) {
            RemoteRootService.writeText(configPath, json)
        } else {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to save backup config, json is null")
        }
    }

    suspend fun setupBackupConfig() {
        val currentConfig = getCurrentConfig()
        currentConfig.createdAt = System.currentTimeMillis()
        if (_selectedIndex.value == -1) {
            App.application.saveString(BackupConfigSelectedUuid.first, currentConfig.uuid.toString())
        }
        saveBackupConfig(currentConfig)

        // We don't need update any flow here, 'cause loadBackupConfigsFromLocal() will be called once user return to setup page.
    }
}
