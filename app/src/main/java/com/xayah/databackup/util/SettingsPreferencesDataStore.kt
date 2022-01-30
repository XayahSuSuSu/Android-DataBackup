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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsPreferencesDataStore {

    private val systemName = booleanPreferencesKey("System_name")

    private val toastInfo = booleanPreferencesKey("Toast_info")

    private val update = booleanPreferencesKey("Update")

    private val updateBehavior = booleanPreferencesKey("Update_behavior")

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
            getSystemName(mContext).collect {
                saveSystemName(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getToastInfo(mContext).collect {
                saveToastInfo(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUpdate(mContext).collect {
                saveUpdate(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUpdateBehavior(mContext).collect {
                saveUpdateBehavior(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getLo(mContext).collect {
                saveLo(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getSplist(mContext).collect {
                saveSplist(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupUserData(mContext).collect {
                saveBackupUserData(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupObbData(mContext).collect {
                saveBackupObbData(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupMedia(mContext).collect {
                saveBackupMedia(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUsbDefault(mContext).collect {
                saveUsbDefault(mContext, it)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            getCompressionMethod(mContext).collect {
                saveCompressionMethod(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getInfo(mContext).collect {
                saveInfo(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getOutputPath(mContext).collect {
                saveOutputPath(mContext, it)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getCustomPath(mContext).collect {
                saveCustomPath(mContext, it)
            }
        }
    }

    fun generateConfigFile(mContext: Context) {
        val content = mutableListOf<String>()
        val pathUtil = PathUtil(mContext)
        CoroutineScope(Dispatchers.IO).launch {
            getSystemName(mContext).collect {
                content.add(if (it) "system_name=1" else "system_name=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getToastInfo(mContext).collect {
                content.add(if (it) "toast_info=1" else "toast_info=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUpdate(mContext).collect {
                content.add(if (it) "update=1" else "update=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUpdateBehavior(mContext).collect {
                content.add(if (it) "update_behavior=1" else "update_behavior=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getLo(mContext).collect {
                content.add(if (it) "Lo=1" else "Lo=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getSplist(mContext).collect {
                content.add(if (it) "Splist=1" else "Splist=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupUserData(mContext).collect {
                content.add(if (it) "Backup_user_data=1" else "Backup_user_data=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupObbData(mContext).collect {
                content.add(if (it) "Backup_obb_data=1" else "Backup_obb_data=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getBackupMedia(mContext).collect {
                content.add(if (it) "backup_media=1" else "backup_media=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getUsbDefault(mContext).collect {
                content.add(if (it) "USBdefault=1" else "USBdefault=0")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            CoroutineScope(Dispatchers.IO).launch {
                getCompressionMethod(mContext).collect {
                    content.add("Compression_method=$it")
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getInfo(mContext).collect {
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getOutputPath(mContext).collect {
                content.add("Output_path=$it")
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getCustomPath(mContext).collect {
                content.add(
                    "Custom_path=\"\n" + it + "\n\""
                )
            }
        }
        ShellUtil.rm(pathUtil.BACKUP_SETTINGS_PATH)
        ShellUtil.writeFile(content.joinToString(separator = "\n"), pathUtil.BACKUP_SETTINGS_PATH)
    }

    suspend fun saveSystemName(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[systemName] = value
        }
    }

    suspend fun saveToastInfo(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[toastInfo] = value
        }
    }

    suspend fun saveUpdate(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[update] = value
        }
    }

    suspend fun saveUpdateBehavior(context: Context, value: Boolean) {
        context.settingsDataStore.edit { setting ->
            setting[updateBehavior] = value
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

    fun getSystemName(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[systemName] ?: false }
    }

    fun getToastInfo(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[toastInfo] ?: false }
    }

    fun getUpdate(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[update] ?: false }
    }

    fun getUpdateBehavior(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { setting -> setting[updateBehavior] ?: false }
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