package com.xayah.databackup.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.Source
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupConfigRepository {
    companion object {
        private const val TAG = "BackupConfigRepository"
    }

    private val moshi: Moshi = Moshi.Builder().build()

    private val _selectedIndex: Int = -1
    private val _configs: MutableList<BackupConfig> = mutableListOf()

    fun getConfigs(): List<BackupConfig> {
        return _configs
    }

    suspend fun loadBackupConfigsFromLocal() {
        withContext(Dispatchers.IO) {
            _configs.clear()
            RemoteRootService.listFilePaths(path = PathHelper.getBackupPathBackups().first(), listFiles = false, listDirs = true).forEach {
                val config = RemoteRootService.readText(PathHelper.getBackupConfigFile(it.path))
                if (it.isDirectory) {
                    val backupConfig = moshi.adapter<BackupConfig>().fromJson(config)
                    backupConfig?.also { config ->
                        config.source = Source.LOCAL
                        config.path = it.path
                        _configs.add(config)
                    }
                }
            }
            LogHelper.i(TAG, "loadBackupConfigsFromLocal: $_configs")
        }
    }
}
