package com.xayah.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DEFAULT_APPS_UPDATE_TIME
import com.xayah.core.model.DEFAULT_COMPRESSION_TYPE
import com.xayah.core.model.SettingsData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val KeyCompressionType = stringPreferencesKey("compression_type")
val KeyAppsUpdateTime = longPreferencesKey("apps_update_time")

class DbPreferencesDataSource @Inject constructor(
    private val preferences: DataStore<Preferences>
) {
    val settingsData = preferences.data.map {
        SettingsData(
            compressionType = it[KeyCompressionType]?.let { v -> CompressionType.valueOf(v) } ?: DEFAULT_COMPRESSION_TYPE,
            appsUpdateTime = it[KeyAppsUpdateTime] ?: DEFAULT_APPS_UPDATE_TIME
        )
    }

    suspend fun <T> edit(key: Preferences.Key<T>, value: T) {
        preferences.edit { settings -> settings[key] = value }
    }
}
