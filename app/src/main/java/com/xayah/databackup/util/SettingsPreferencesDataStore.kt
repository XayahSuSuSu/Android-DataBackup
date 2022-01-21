package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xayah.databackup.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsPreferencesDataStore {

    private val autoUpdate = booleanPreferencesKey("Auto_update")

    private val lo = booleanPreferencesKey("Lo")

    private val splist = booleanPreferencesKey("Splist")

    private val backupUserData = booleanPreferencesKey("Backup_user_data")

    private val backupObbData = booleanPreferencesKey("Backup_obb_data")

    private val backupMedia = booleanPreferencesKey("backup_media")

    private val usbDefault = booleanPreferencesKey("USBdefault")

    private val compressionMethod = stringPreferencesKey("Compression_method")

    private val info = stringPreferencesKey("info")

    private val outputPath = stringPreferencesKey("Output_path")

    private val customPath = stringPreferencesKey("Custom_path")

    fun initialize(mContext: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            saveAutoUpdate(mContext, false)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveLo(mContext, false)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveSplist(mContext, false)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveBackupUserData(mContext, true)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveBackupObbData(mContext, true)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveBackupMedia(mContext, false)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveUsbDefault(mContext, false)
        }
        CoroutineScope(Dispatchers.IO).launch {
            saveCompressionMethod(mContext, "zstd")
        }

        CoroutineScope(Dispatchers.IO).launch {
            saveInfo(mContext, mContext.getString(R.string.settings_sumarry_info))
        }

        CoroutineScope(Dispatchers.IO).launch {
            saveOutputPath(mContext, mContext.getString(R.string.settings_sumarry_output_path))
        }

        CoroutineScope(Dispatchers.IO).launch {
            saveCustomPath(mContext, mContext.getString(R.string.settings_summary_custom_path))
        }
    }

    suspend fun saveAutoUpdate(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[autoUpdate] = value
        }
    }

    suspend fun saveLo(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[lo] = value
        }
    }

    suspend fun saveSplist(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[splist] = value
        }
    }

    suspend fun saveBackupUserData(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[backupUserData] = value
        }
    }

    suspend fun saveBackupObbData(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[backupObbData] = value
        }
    }

    suspend fun saveBackupMedia(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[backupMedia] = value
        }
    }

    suspend fun saveUsbDefault(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[usbDefault] = value
        }
    }

    suspend fun saveCompressionMethod(context: Context, value: String) {
        context.settingsDataStore.edit { setting ->
            setting[compressionMethod] = value
        }
    }

    suspend fun saveInfo(context: Context, value: String) {
        context.settingsDataStore.edit { setting ->
            setting[info] = value
        }
    }

    suspend fun saveOutputPath(context: Context, value: String) {
        context.settingsDataStore.edit { setting ->
            setting[outputPath] = value
        }
    }

    suspend fun saveCustomPath(context: Context, value: String) {
        context.settingsDataStore.edit { setting ->
            setting[customPath] = value
        }
    }

    fun getAutoUpdate(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[autoUpdate] ?: false }
    }

    fun getLo(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[lo] ?: false }
    }

    fun getSplist(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[splist] ?: false }
    }

    fun getBackupUserData(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[backupUserData] ?: true }
    }

    fun getBackupObbData(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[backupObbData] ?: true }
    }

    fun getBackupMedia(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[backupMedia] ?: false }
    }

    fun getUsbDefault(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[usbDefault] ?: false }
    }

    fun getCompressionMethod(context: Context): Flow<String> {
        return context.settingsDataStore.data.map { setting ->
            setting[compressionMethod] ?: "zstd"
        }
    }

    fun getInfo(context: Context): Flow<String> {
        return context.settingsDataStore.data.map { setting ->
            setting[info] ?: context.getString(R.string.settings_sumarry_info)
        }
    }

    fun getOutputPath(context: Context): Flow<String> {
        return context.settingsDataStore.data.map { setting ->
            setting[outputPath] ?: context.getString(R.string.settings_sumarry_output_path)
        }
    }

    fun getCustomPath(context: Context): Flow<String> {
        return context.settingsDataStore.data.map { setting ->
            setting[customPath] ?: context.getString(R.string.settings_summary_custom_path)
        }
    }
}