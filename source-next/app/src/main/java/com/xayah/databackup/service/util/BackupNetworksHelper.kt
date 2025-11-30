package com.xayah.databackup.service.util

import arrow.optics.copy
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class BackupNetworksHelper(private val mBackupProcessRepo: BackupProcessRepository) {
    companion object {
        private const val TAG = "BackupNetworksHelper"
    }

    suspend fun start() {
        val networks = mBackupProcessRepo.getNetworks()
        networks.forEachIndexed { index, network ->
            mBackupProcessRepo.updateNetworksItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set network.ssid
                    ProcessItem.progress set index.toFloat() / networks.size
                }
            }
        }
        val json = runCatching {
            val moshi: Moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()
            moshi.adapter<List<Network>>().toJson(networks)
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to serialize to json.", it)
        }.getOrNull()
        if (json != null) {
            val backupConfig = mBackupProcessRepo.getBackupConfig()
            val networksPath = PathHelper.getBackupNetworksDir(backupConfig.path)
            if (RemoteRootService.exists(networksPath).not() && RemoteRootService.mkdirs(networksPath)) {
                LogHelper.e(TAG, "start", "Failed to mkdirs: $networksPath.")
            }
            val configPath = PathHelper.getBackupNetworksConfigFilePath(backupConfig.path, System.currentTimeMillis())
            RemoteRootService.writeText(configPath, json)
        } else {
            LogHelper.e(TAG, "start", "Failed to save networks, json is null")
        }

        mBackupProcessRepo.updateNetworksItem {
            copy {
                ProcessItem.currentIndex set networks.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
