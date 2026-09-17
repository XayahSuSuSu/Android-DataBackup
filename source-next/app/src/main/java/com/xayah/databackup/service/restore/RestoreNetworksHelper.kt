package com.xayah.databackup.service.restore

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfigurationHidden
import android.net.wifi.WifiManagerHidden
import android.os.Build
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.util.PathHelper
import com.xayah.hiddenapi.castTo
import com.xayah.libnative.Rustic
import java.util.BitSet

/**
 * Restores Wi-Fi records from a Rustic snapshot through the system Wi-Fi service without rollback.
 * The caller must serialize restores and provide a privileged Wi-Fi manager.
 * Adds or updates networks, enables them without disabling other networks,
 * and restores auto-join preferences where supported.
 * Skips records without a compatible security configuration and returns their inventory keys.
 *
 * see [WifiServiceImpl.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/modules/Wifi/service/java/com/android/server/wifi/WifiServiceImpl.java)
 */
internal class RestoreNetworksHelper(private val mWifiManager: WifiManagerHidden) {
    @WorkerThread
    fun restore(repositoryPath: String, password: String, snapshotId: String, networkIds: List<String>): List<String> {
        require(snapshotId.matches(Regex("[0-9a-fA-F]{64}"))) { "A full snapshot ID is required" }
        require(networkIds.isNotEmpty()) { "No Wi-Fi records selected" }
        val path = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getBackupNetworksConfigFileRelativePath())
        val serialized = Rustic.readSnapshotTextFiles(repositoryPath, password, snapshotId, listOf(path))
        val moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()
        val files = requireNotNull(moshi.adapter<Map<String, String>>().fromJson(serialized))
        val content = requireNotNull(files[path]) { "Missing Wi-Fi backup file" }
        val adapter = moshi.adapter<WifiConfiguration>()
        val skippedNetworkIds = mutableListOf<String>()
        val configurations = getSelectedNetworkConfigs(content, networkIds).flatMap { (recordId, variants) ->
            val parsed = variants.map { json ->
                requireNotNull(adapter.fromJson(json)) { "Missing Wi-Fi configuration" }.apply {
                    require(!SSID.isNullOrBlank()) { "Missing Wi-Fi SSID" }
                    networkId = -1
                }
            }
            // Older frameworks may discard unknown key-management bits when writing to supplicant.
            // Skip unsupported variants and report records with no compatible configuration.
            val indices = getSupportedConfigIndices(parsed.map { it.allowedKeyManagement }, WifiConfiguration.KeyMgmt.strings.size)
            if (indices.isEmpty()) skippedNetworkIds.add(recordId)
            indices.map(parsed::get)
        }
        configurations.forEachIndexed { index, config ->
            val id = mWifiManager.addNetwork(config)
            check(id >= 0) { "Failed to add Wi-Fi configuration $index" }
            check(mWifiManager.enableNetwork(id, false)) { "Failed to enable Wi-Fi configuration $index" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mWifiManager.allowAutojoin(id, config.castTo<WifiConfigurationHidden>().allowAutojoin)
            }
            // Before Android O, addNetwork/enableNetwork require an explicit save to survive reboot.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                check(mWifiManager.saveConfiguration()) { "Failed to persist Wi-Fi configuration $index" }
            }
        }
        return skippedNetworkIds
    }

    companion object {
        internal fun getSupportedConfigIndices(keyManagement: List<BitSet>, knownKeyManagementCount: Int): List<Int> {
            require(knownKeyManagementCount > 0) { "No supported Wi-Fi key-management types" }
            return keyManagement.indices.filter { index ->
                val bits = keyManagement[index]
                !bits.isEmpty && bits.nextSetBit(knownKeyManagementCount) == -1
            }
        }

        /**
         * Selects records by snapshot index to match the restore inventory's network:<index> keys.
         */
        internal fun getSelectedNetworkConfigs(content: String, networkIds: List<String>): Map<String, List<String>> {
            require(networkIds.isNotEmpty()) { "No Wi-Fi records selected" }
            val records = requireNotNull(Moshi.Builder().build().adapter<List<Network>>().fromJson(content)) {
                "Missing Wi-Fi records"
            }
            val recordsById = records.mapIndexed { index, network -> "network:$index" to network }.toMap()
            return networkIds.distinct().associateWith { id ->
                val record = requireNotNull(recordsById[id]) { "Unknown Wi-Fi record" }
                listOfNotNull(record.config1, record.config2).also { configs ->
                    require(configs.isNotEmpty() && configs.none { it.isBlank() }) { "Missing Wi-Fi configuration" }
                }
            }
        }
    }
}
