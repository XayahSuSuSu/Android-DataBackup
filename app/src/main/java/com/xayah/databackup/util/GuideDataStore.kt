package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.guideDataStore: DataStore<Preferences> by preferencesDataStore(name = "guide")

object GuideDataStore {
    private val backupGuide = booleanPreferencesKey("backupGuide")

    private val restoreGuide = booleanPreferencesKey("restoreGuide")

    suspend fun saveBackupGuide(context: Context, value: Boolean) {
        context.guideDataStore.edit { setting ->
            setting[backupGuide] = value
        }
    }

    suspend fun saveRestoreGuide(context: Context, value: Boolean) {
        context.guideDataStore.edit { setting ->
            setting[restoreGuide] = value
        }
    }

    fun getBackupGuide(context: Context): Flow<Boolean> {
        return context.guideDataStore.data.map { setting -> setting[backupGuide] ?: false }
    }

    fun getRestoreGuide(context: Context): Flow<Boolean> {
        return context.guideDataStore.data.map { setting -> setting[restoreGuide] ?: false }
    }

}