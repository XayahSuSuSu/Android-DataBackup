package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class SortsType {
    A2Z,
    DATA_SIZE,
    INSTALL_TIME,
    UPDATE_TIME,
}

enum class SortsSequence {
    ASCENDING,
    DESCENDING
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
suspend fun Context.preloadingDataStore() = dataStore.data.first()

fun Context.readString(pair: Pair<Preferences.Key<String>, String>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
inline fun <reified T : Enum<T>> Context.readEnum(pair: Pair<Preferences.Key<String>, T>) =
    dataStore.data.map { preferences -> enumValueOf<T>(preferences[pair.first] ?: pair.second.name) }
fun Context.readBoolean(pair: Pair<Preferences.Key<Boolean>, Boolean>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readInt(pair: Pair<Preferences.Key<Int>, Int>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readLong(pair: Pair<Preferences.Key<Long>, Long>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }

suspend fun Context.saveString(key: Preferences.Key<String>, value: String) = dataStore.edit { settings -> settings[key] = value }
suspend inline fun <reified T : Enum<T>> Context.saveEnum(key: Preferences.Key<String>, value: T) =
    dataStore.edit { settings -> settings[key] = value.name }
suspend fun Context.saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveInt(key: Preferences.Key<Int>, value: Int) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveLong(key: Preferences.Key<Long>, value: Long) = dataStore.edit { settings -> settings[key] = value }

// ----------------------------------------------------------------------------------------------------------------------------Setup

// Key to defValue
val KeyBackupPath = stringPreferencesKey("backup_path")
val BackupPath = Pair(KeyBackupPath, PathHelper.DEFAULT_BACKUP_PATH)

val KeyFirstLaunch = booleanPreferencesKey("first_launch")
const val DefFirstLaunch = true
val FirstLaunch = Pair(KeyFirstLaunch, DefFirstLaunch)

val KeyCustomSuFile = stringPreferencesKey("custom_su_file")
const val DefCustomSuFile = "su"
val CustomSuFile = Pair(KeyCustomSuFile, DefCustomSuFile)

// ----------------------------------------------------------------------------------------------------------------------------List

val KeyFilterBackupUser = intPreferencesKey("filter_backup_user")
const val DefFilterBackupUser = 0
val FilterBackupUser = Pair(KeyFilterBackupUser, DefFilterBackupUser)

val KeySortsTypeBackup = stringPreferencesKey("sorts_type_backup")
val DefSortsTypeBackup = SortsType.A2Z
val SortsTypeBackup = Pair(KeySortsTypeBackup, DefSortsTypeBackup)

val KeySortsSequenceBackup = stringPreferencesKey("sorts_sequence_backup")
val DefSortsSequenceBackup = SortsSequence.ASCENDING
val SortsSequenceBackup = Pair(KeySortsSequenceBackup, DefSortsSequenceBackup)

val KeyFiltersUserAppsBackup = booleanPreferencesKey("filters_user_apps_backup")
const val DefFiltersUserAppsBackup = true
val FiltersUserAppsBackup = Pair(KeyFiltersUserAppsBackup, DefFiltersUserAppsBackup)

val KeyFiltersSystemAppsBackup = booleanPreferencesKey("filters_system_apps_backup")
const val DefFiltersSystemAppsBackup = false
val FiltersSystemAppsBackup = Pair(KeyFiltersSystemAppsBackup, DefFiltersSystemAppsBackup)

// ----------------------------------------------------------------------------------------------------------------------------Settings

val KeyIncrementalBackup = booleanPreferencesKey("incremental_backup")
const val DefIncrementalBackup = true
val IncrementalBackup = Pair(KeyIncrementalBackup, DefIncrementalBackup)

val KeyCleanBackup = booleanPreferencesKey("clean_backup")
const val DefCleanBackup = false
val CleanBackup = Pair(KeyCleanBackup, DefCleanBackup)

val KeyAutoScreenOff = booleanPreferencesKey("auto_screen_off")
const val DefAutoScreenOff = true
val AutoScreenOff = Pair(KeyAutoScreenOff, DefAutoScreenOff)

val KeyResetBackupList = booleanPreferencesKey("reset_backup_list")
const val DefResetBackupList = false
val ResetBackupList = Pair(KeyResetBackupList, DefResetBackupList)

// ----------------------------------------------------------------------------------------------------------------------------Backup

val KeyAppsOptionSelectedBackup = booleanPreferencesKey("apps_option_selected_backup")
const val DefAppsOptionSelectedBackup = true
val AppsOptionSelectedBackup = Pair(KeyAppsOptionSelectedBackup, DefAppsOptionSelectedBackup)

val KeyNetworksOptionSelectedBackup = booleanPreferencesKey("networks_option_selected_backup")
const val DefNetworksOptionSelectedBackup = true
val NetworksOptionSelectedBackup = Pair(KeyNetworksOptionSelectedBackup, DefNetworksOptionSelectedBackup)

val KeyContactsOptionSelectedBackup = booleanPreferencesKey("contacts_option_selected_backup")
const val DefContactsOptionSelectedBackup = true
val ContactsOptionSelectedBackup = Pair(KeyContactsOptionSelectedBackup, DefContactsOptionSelectedBackup)

val KeyCallLogsOptionSelectedBackup = booleanPreferencesKey("call_logs_option_selected_backup")
const val DefCallLogsOptionSelectedBackup = true
val CallLogsOptionSelectedBackup = Pair(KeyCallLogsOptionSelectedBackup, DefCallLogsOptionSelectedBackup)

val KeyMessagesOptionSelectedBackup = booleanPreferencesKey("messages_option_selected_backup")
const val DefMessagesOptionSelectedBackup = true
val MessagesOptionSelectedBackup = Pair(KeyMessagesOptionSelectedBackup, DefMessagesOptionSelectedBackup)

val KeyBackupConfigSelectedUuid = stringPreferencesKey("backup_config_selected_uuid")
const val DefBackupConfigSelectedUuid = ""
val BackupConfigSelectedUuid = Pair(KeyBackupConfigSelectedUuid, DefBackupConfigSelectedUuid)
