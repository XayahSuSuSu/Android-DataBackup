package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.CompressionType
import com.xayah.core.model.SelectionType
import com.xayah.core.model.ThemeType
import com.xayah.core.model.util.of
import kotlinx.coroutines.flow.map

// -----------------------------------------Keys-----------------------------------------
val KeyCompressionType = stringPreferencesKey("compression_type")
val KeyBackupSavePath = stringPreferencesKey("backup_save_path")
val KeyAppVersionName = stringPreferencesKey("app_version_name")
val KeyCloudActivatedAccountName = stringPreferencesKey("cloud_activated_account_name")
val KeyLoadedIconMD5 = stringPreferencesKey("loaded_icon_md5")
val KeySelectionType = stringPreferencesKey("selection_type")
val KeyThemeType = stringPreferencesKey("theme_type")
val KeyBackupUserIdIndex = stringPreferencesKey("backup_user_id_index")
val KeyRestoreUserIdIndex = stringPreferencesKey("restore_user_id_index")


// -----------------------------------------Read-----------------------------------------
fun Context.readCompressionType() = readStoreString(key = KeyCompressionType, defValue = "").map { CompressionType.of(it) }
fun Context.readAppVersionName() = readStoreString(key = KeyAppVersionName, defValue = "")
fun Context.readCloudActivatedAccountName() = readStoreString(key = KeyCloudActivatedAccountName, defValue = "")
fun Context.readLoadedIconMD5() = readStoreString(key = KeyLoadedIconMD5, defValue = "")
fun Context.readSelectionType() = readStoreString(key = KeySelectionType, defValue = "").map { SelectionType.of(it) }
fun Context.readThemeType() = readStoreString(key = KeyThemeType, defValue = "").map { ThemeType.of(it) }
fun Context.readBackupUserIdIndex() = readStoreString(key = KeyBackupUserIdIndex, defValue = "[0]").map { Gson().fromJson<List<Int>>(it, object : TypeToken<List<Int>>() {}.type) }
fun Context.readRestoreUserIdIndex() = readStoreString(key = KeyRestoreUserIdIndex, defValue = "[0]").map { Gson().fromJson<List<Int>>(it, object : TypeToken<List<Int>>() {}.type) }

/**
 * The final path for saving the backup.
 */
fun Context.readBackupSavePathSaved() = readStoreString(key = KeyBackupSavePath, defValue = "").map { it.isNotEmpty() }
fun Context.readBackupSavePath() = readStoreString(key = KeyBackupSavePath, defValue = ConstantUtil.DEFAULT_PATH)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveCompressionType(value: CompressionType) = saveStoreString(key = KeyCompressionType, value = value.type.trim())
suspend fun Context.saveAppVersionName() = saveStoreString(key = KeyAppVersionName, value = getCurrentAppVersionName())
suspend fun Context.saveCloudActivatedAccountName(value: String) = saveStoreString(key = KeyCloudActivatedAccountName, value = value.trim())
suspend fun Context.saveLoadedIconMD5(value: String) = saveStoreString(key = KeyLoadedIconMD5, value = value.trim())
suspend fun Context.saveSelectionType(value: SelectionType) = saveStoreString(key = KeySelectionType, value = value.name.trim())
suspend fun Context.saveThemeType(value: ThemeType) = saveStoreString(key = KeyThemeType, value = value.name.trim())
suspend fun Context.saveBackupUserIdIndex(value: List<Int>) = saveStoreString(key = KeyBackupUserIdIndex, value = Gson().toJson(value))
suspend fun Context.saveRestoreUserIdIndex(value: List<Int>) = saveStoreString(key = KeyRestoreUserIdIndex, value = Gson().toJson(value))
suspend fun Context.saveBackupSavePath(value: String) = saveStoreString(key = KeyBackupSavePath, value = value.trim())
