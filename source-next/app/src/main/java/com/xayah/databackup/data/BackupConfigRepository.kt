package com.xayah.databackup.data

import arrow.optics.copy
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.adapter.UuidJsonAdapter
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.Source
import com.xayah.databackup.entity.appsBackupStrategy
import com.xayah.databackup.entity.createdAt
import com.xayah.databackup.entity.path
import com.xayah.databackup.entity.source
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BackupConfigSelectedUuid
import com.xayah.databackup.util.DefaultAppsBackupStrategy
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.TimeHelper
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readString
import com.xayah.databackup.util.saveString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class BackupConfigRepository {
    companion object {
        private const val TAG = "BackupConfigRepository"

        const val NEW_CONFIG_INDEX = -1
    }

    private val moshi: Moshi = Moshi.Builder().add(UuidJsonAdapter()).build()

    private var _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(NEW_CONFIG_INDEX)
    private val _configs: MutableStateFlow<List<BackupConfig>> = MutableStateFlow(listOf())
    private val _newConfig: MutableStateFlow<BackupConfig> = MutableStateFlow(BackupConfig())

    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    val configs: StateFlow<List<BackupConfig>> = _configs.asStateFlow()

    fun getCurrentConfig(): BackupConfig {
        return if (_selectedIndex.value == NEW_CONFIG_INDEX) {
            _newConfig.value
        } else {
            _configs.value[_selectedIndex.value]
        }
    }

    suspend fun selectBackup(index: Int) {
        _selectedIndex.emit(index)
        if (index == NEW_CONFIG_INDEX) {
            App.application.saveString(BackupConfigSelectedUuid.first, "")
        } else {
            App.application.saveString(BackupConfigSelectedUuid.first, _configs.value[_selectedIndex.value].uuidString)
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
        val strategy = App.application.readEnum(DefaultAppsBackupStrategy).first()
        _newConfig.update {
            it.copy {
                BackupConfig.source set Source.LOCAL
                BackupConfig.path set "$path/${TimeHelper.formatTimestampInDetail(newConfigTimestamp)}"
                BackupConfig.createdAt set newConfigTimestamp
                BackupConfig.appsBackupStrategy set strategy
            }
        }
        LogHelper.i(TAG, "createNewBackup", "newConfig: ${_newConfig.value}")
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
            _selectedIndex.emit(_configs.value.indexOfFirst { it.uuidString == selectedUuid })
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
        currentConfig.updatedAt = System.currentTimeMillis()
        if (_selectedIndex.value == NEW_CONFIG_INDEX) {
            App.application.saveString(BackupConfigSelectedUuid.first, currentConfig.uuidString)
        }
        saveBackupConfig(currentConfig)

        // We don't need update any flow here, 'cause loadBackupConfigsFromLocal() will be called once user return to setup page.
    }

    suspend fun updateConfig(uuid: String, onUpdate: BackupConfig.() -> BackupConfig) {
        _configs.update { currentConfigs ->
            currentConfigs.map { config ->
                if (uuid == config.uuidString) {
                    val newConfig = onUpdate(config)
                    saveBackupConfig(newConfig)
                    newConfig
                } else {
                    config
                }
            }
        }
    }

    suspend fun deleteConfig(uuid: String) {
        val config = _configs.value.firstOrNull { it.uuidString == uuid } ?: return
        if (RemoteRootService.deleteRecursively(config.path)) {
            _configs.update { list -> list.filterNot { it.uuidString == uuid } }
        }
    }
}
