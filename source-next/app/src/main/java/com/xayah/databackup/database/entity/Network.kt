package com.xayah.databackup.database.entity

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiConfigurationHidden
import androidx.room.Entity
import com.xayah.databackup.util.ParcelableHelper.unmarshall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "networks", primaryKeys = ["id"])
data class Network(
    var id: Int,
    var ssid: String,
    var preSharedKey: String?,
    var selected: Boolean,
    var config1: ByteArray?,
    var config2: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Network

        if (id != other.id) return false
        if (selected != other.selected) return false
        if (ssid != other.ssid) return false
        if (preSharedKey != other.preSharedKey) return false
        if (!config1.contentEquals(other.config1)) return false
        if (!config2.contentEquals(other.config2)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + selected.hashCode()
        result = 31 * result + ssid.hashCode()
        result = 31 * result + (preSharedKey?.hashCode() ?: 0)
        result = 31 * result + (config1?.contentHashCode() ?: 0)
        result = 31 * result + (config2?.contentHashCode() ?: 0)
        return result
    }
}

data class NetworkUnmarshalled(
    var id: Int,
    var ssid: String,
    var preSharedKey: String?,
    var selected: Boolean,
    var config1: WifiConfiguration?,
    var config2: WifiConfiguration?,
)

private fun String.trimQuotes() = removeSurrounding("\"")

fun Flow<List<Network>>.unmarshall(): Flow<List<NetworkUnmarshalled>> = map { flow ->
    flow.map {
        val networkUnmarshalled = NetworkUnmarshalled(it.id, it.ssid.trimQuotes(), it.preSharedKey?.trimQuotes(), it.selected, null, null)
        it.config1?.unmarshall { parcel ->
            networkUnmarshalled.config1 = WifiConfigurationHidden.CREATOR.createFromParcel(parcel)
        }
        it.config2?.unmarshall { parcel ->
            networkUnmarshalled.config2 = WifiConfigurationHidden.CREATOR.createFromParcel(parcel)
        }
        networkUnmarshalled
    }
}
