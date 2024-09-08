package com.xayah.core.data.repository

import com.xayah.core.datastore.DbPreferencesDataSource
import com.xayah.core.datastore.KeyAppsUpdateTime
import com.xayah.core.datastore.KeyCompressionType
import com.xayah.core.model.CompressionType
import com.xayah.core.model.SettingsData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsDataRepo @Inject constructor(
    private val dbPreferencesDataSource: DbPreferencesDataSource,
) {
    val settingsData: Flow<SettingsData> = dbPreferencesDataSource.settingsData

    suspend fun setCompressionType(value: CompressionType) {
        dbPreferencesDataSource.edit(KeyCompressionType, value.name)
    }

    suspend fun setAppsUpdateTime(value: Long) {
        dbPreferencesDataSource.edit(KeyAppsUpdateTime, value)
    }
}
