package com.xayah.databackup.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.xayah.databackup.App
import com.xayah.databackup.adapter.UuidJsonAdapter
import com.xayah.databackup.data.rustic.RusticBackupGateway
import com.xayah.databackup.entity.BackupBackend
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class BackupConfigRepository(
    private val rusticBackupGateway: RusticBackupGateway,
) {
    companion object {
        private const val TAG = "BackupConfigRepository"

        const val NEW_CONFIG_INDEX = -1
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(BackupBackend::class.java, "type")
                .withSubtype(BackupBackend.Rustic::class.java, "rustic")
                .withSubtype(BackupBackend.Archive::class.java, "archive")
        )
        .add(UuidJsonAdapter())
        .build()

    private var _selectedIndex: MutableStateFlow<Int> = MutableStateFlow(NEW_CONFIG_INDEX)
    private val _configs: MutableStateFlow<List<BackupConfig>> = MutableStateFlow(listOf())
    private val _newConfig: MutableStateFlow<BackupConfig> = MutableStateFlow(BackupConfig())

    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()
    val configs: StateFlow<List<BackupConfig>> = _configs.asStateFlow()
    val newConfig: StateFlow<BackupConfig> = _newConfig.asStateFlow()

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
        _newConfig.value = createNewBackupDraft(path)
        LogHelper.i(TAG, "createNewBackup", "newConfig: ${_newConfig.value}")
    }

    private suspend fun createNewBackupDraft(path: String): BackupConfig {
        val newConfigTimestamp = System.currentTimeMillis()
        val pathPrefix = "$path/${TimeHelper.formatTimestampInDetail(newConfigTimestamp)}"
        var newConfigPath = pathPrefix
        var suffix = 2
        while (RemoteRootService.exists(newConfigPath)) {
            newConfigPath = "${pathPrefix}_${suffix++}"
        }
        return BackupConfig(
            source = Source.LOCAL,
            path = newConfigPath,
            createdAt = newConfigTimestamp,
        )
    }

    suspend fun saveNewBackup(): Int = withContext(Dispatchers.IO) {
        val config = _newConfig.value.copy(updatedAt = System.currentTimeMillis())
        check(config.path.isNotBlank()) { "Backup path is empty." }
        check(RemoteRootService.exists(config.path).not()) { "Backup directory already exists." }

        var directoryCreated = false
        try {
            check(RemoteRootService.mkdirs(config.path)) { "Failed to create backup directory." }
            directoryCreated = true
            check(saveBackupConfig(config)) { "Failed to write backup config." }

            (config.backupBackend as? BackupBackend.Rustic)?.let { backend ->
                rusticBackupGateway.prepareRepository(
                    repositoryPath = PathHelper.getBackupRepoDir(config.path),
                    password = backend.password,
                )
            }

            val configs = (_configs.value + config).sortedByDescending { it.updatedAt }
            val savedIndex = configs.indexOfFirst { it.uuid == config.uuid }
            check(savedIndex >= 0) { "Saved backup is missing from the config list." }

            App.application.saveString(BackupConfigSelectedUuid.first, config.uuidString)
            _configs.emit(configs)
            _selectedIndex.emit(savedIndex)
            savedIndex
        } catch (throwable: Throwable) {
            if (directoryCreated && RemoteRootService.deleteRecursively(config.path).not()) {
                LogHelper.w(TAG, "saveNewBackup", "Failed to clean incomplete backup directory: ${config.path}")
            }
            throw throwable
        }
    }

    suspend fun resetNewBackup() {
        createNewBackup(PathHelper.getParentPath(_newConfig.value.path))
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

    suspend fun saveBackupConfig(config: BackupConfig): Boolean {
        val configPath = PathHelper.getBackupConfigFile(config.path)
        val configParentPath = PathHelper.getParentPath(configPath)
        if (RemoteRootService.mkdirs(configParentPath).not()) {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to mkdirs: $configParentPath.")
            return false
        }

        val json = runCatching {
            moshi.adapter<BackupConfig>().toJson(config)
        }.onFailure {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to serialize to json.", it)
        }.getOrNull()
        if (json == null) {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to save backup config, json is null")
            return false
        }
        return runCatching {
            RemoteRootService.writeText(configPath, json)
            RemoteRootService.exists(configPath) && RemoteRootService.readText(configPath) == json
        }.onFailure {
            LogHelper.e(TAG, "saveBackupConfig", "Failed to write backup config.", it)
        }.getOrDefault(false)
    }

    suspend fun setupBackupConfig() {
        val currentConfig = getCurrentConfig()
        currentConfig.updatedAt = System.currentTimeMillis()
        check(saveBackupConfig(currentConfig)) { "Failed to save backup config." }
        if (_selectedIndex.value == NEW_CONFIG_INDEX) {
            App.application.saveString(BackupConfigSelectedUuid.first, currentConfig.uuidString)
        }

        // We don't need to update any flow here, 'cause loadBackupConfigsFromLocal() will be called once user return to setup page.
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

    fun updateNewConfig(onUpdate: BackupConfig.() -> BackupConfig) {
        _newConfig.update { onUpdate(it) }
    }

    suspend fun deleteConfig(uuid: String) {
        val config = _configs.value.firstOrNull { it.uuidString == uuid } ?: return
        if (RemoteRootService.deleteRecursively(config.path)) {
            _configs.update { list -> list.filterNot { it.uuidString == uuid } }
        }
    }
}
