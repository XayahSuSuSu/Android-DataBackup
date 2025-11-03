package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkRepository {
    companion object {
        private const val TAG = "NetworkRepository"
    }

    val isBackupNetworksSelected: Flow<Boolean> = application.readBoolean(NetworksOptionSelectedBackup)

    val networks: Flow<List<Network>> = DatabaseHelper.networkDao.loadFlowNetworks()
    val networksSelected: Flow<List<Network>> = networks.map { networks -> networks.filter { it.selected } }
}
