package com.xayah.databackup.service.restore.rustic

import android.net.wifi.WifiManagerHidden
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.rustic.requireFullSnapshotId
import com.xayah.databackup.service.restore.RestoreNetworksHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.libnative.RusticWrapper

/**
 * Restores Wi-Fi records from a Rustic snapshot through the system Wi-Fi service without rollback.
 * The caller must serialize restores and provide a privileged Wi-Fi manager.
 * Adds or updates networks, enables them without disabling other networks,
 * and restores auto-join preferences where supported.
 * Skips records without a compatible security configuration and returns their inventory keys.
 *
 * see [WifiServiceImpl.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/modules/Wifi/service/java/com/android/server/wifi/WifiServiceImpl.java)
 */
internal class RusticRestoreNetworksHelper(private val mWifiManager: WifiManagerHidden) {
    @WorkerThread
    fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        networkIds: List<String>,
        callback: RestoreProgressCallback,
    ): List<String> {
        requireFullSnapshotId(snapshotId)
        require(networkIds.isNotEmpty()) { "No Wi-Fi records selected" }
        val path = PathHelper.getRusticSnapshotMetadataFilePath(PathHelper.getBackupNetworksConfigFileRelativePath())
        val serialized = RusticWrapper.readSnapshotTextFiles(repositoryPath, password, snapshotId, listOf(path))
        val moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()
        val files = requireNotNull(moshi.adapter<Map<String, String>>().fromJson(serialized))
        val content = requireNotNull(files[path]) { "Missing Wi-Fi backup file" }
        return RestoreNetworksHelper(mWifiManager).restore(content, networkIds, callback)
    }
}
