package com.xayah.databackup.adapter

import android.net.wifi.WifiConfiguration
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class WifiConfigurationAdapter {
    private val mType = object : TypeToken<WifiConfiguration>() {}.type
    private val mGson: Gson = GsonBuilder().addDeserializationExclusionStrategy(NetworkStrategy()).create()

    private class NetworkStrategy : ExclusionStrategy {
        private val mSkipFields: List<String> = listOf(
            "mNetworkSeclectionDisableCounter" // We need to skip this filed to make it work for restoring
        )

        override fun shouldSkipField(f: FieldAttributes): Boolean {
            return mSkipFields.contains(f.name)
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }
    }

    @ToJson
    fun toJson(wifiConfiguration: WifiConfiguration): String = mGson.toJson(wifiConfiguration)


    @FromJson
    fun fromJson(json: String): WifiConfiguration {
        return mGson.fromJson(json, mType)
    }
}
