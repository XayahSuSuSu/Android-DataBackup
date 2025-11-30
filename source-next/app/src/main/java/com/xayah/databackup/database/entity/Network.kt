package com.xayah.databackup.database.entity

import android.net.wifi.WifiConfiguration
import androidx.room.Entity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@JsonClass(generateAdapter = true)
@Entity(tableName = "networks", primaryKeys = ["id"])
data class Network(
    var id: Int,
    var ssid: String,
    var preSharedKey: String?,
    var selected: Boolean,
    var config1: String?,
    var config2: String?,
)

data class NetworkUnmarshalled(
    var id: Int,
    var ssid: String,
    var preSharedKey: String?,
    var selected: Boolean,
    var config1: WifiConfiguration?,
    var config2: WifiConfiguration?,
)

private fun String.trimQuotes() = removeSurrounding("\"")

fun Flow<List<Network>>.deserialize(): Flow<List<NetworkUnmarshalled>> = map { flow ->
    flow.map {
        val moshi: Moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()
        val networkUnmarshalled = NetworkUnmarshalled(it.id, it.ssid.trimQuotes(), it.preSharedKey?.trimQuotes(), it.selected, null, null)
        it.config1?.also { json ->
            networkUnmarshalled.config1 = moshi.adapter<WifiConfiguration>().fromJson(json)
        }
        it.config2?.also { json ->
            networkUnmarshalled.config2 = moshi.adapter<WifiConfiguration>().fromJson(json)
        }
        networkUnmarshalled
    }
}
