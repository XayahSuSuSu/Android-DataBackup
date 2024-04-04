package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.SelectionType
import com.xayah.core.model.SortType
import com.xayah.core.model.ThemeType
import com.xayah.core.model.util.of
import kotlinx.coroutines.flow.map

// -----------------------------------------Keys-----------------------------------------
val KeyLastRestoreTime = stringPreferencesKey("last_restore_time")
val KeyCompressionType = stringPreferencesKey("compression_type")
val KeyBackupSavePath = stringPreferencesKey("backup_save_path")
val KeyBackupSaveParentPath = stringPreferencesKey("backup_save_parent_path")
val KeyRestoreSavePath = stringPreferencesKey("restore_save_path")
val KeyRestoreSaveParentPath = stringPreferencesKey("restore_save_parent_path")
val KeyBackupSortType = stringPreferencesKey("backup_sort_type")
val KeyRestoreSortType = stringPreferencesKey("restore_sort_type")
val KeyAppVersionName = stringPreferencesKey("app_version_name")
val KeyCloudActivatedAccountName = stringPreferencesKey("cloud_activated_account_name")
val KeyLoadedIconMD5 = stringPreferencesKey("loaded_icon_md5")
val KeySelectionType = stringPreferencesKey("selection_type")
val KeyThemeType = stringPreferencesKey("theme_type")


// -----------------------------------------Read-----------------------------------------
fun Context.readLastRestoreTime() = readStoreString(key = KeyLastRestoreTime, defValue = "")
fun Context.readCompressionType() = readStoreString(key = KeyCompressionType, defValue = "").map { CompressionType.of(it) }
fun Context.readBackupSortType() = readStoreString(key = KeyBackupSortType, defValue = "").map { SortType.of(it) }
fun Context.readRestoreSortType() = readStoreString(key = KeyRestoreSortType, defValue = "").map { SortType.of(it) }
fun Context.readAppVersionName() = readStoreString(key = KeyAppVersionName, defValue = "")
fun Context.readCloudActivatedAccountName() = readStoreString(key = KeyCloudActivatedAccountName, defValue = "")
fun Context.readLoadedIconMD5() = readStoreString(key = KeyLoadedIconMD5, defValue = "")
fun Context.readSelectionType() = readStoreString(key = KeySelectionType, defValue = "").map { SelectionType.of(it) }
fun Context.readThemeType() = readStoreString(key = KeyThemeType, defValue = "").map { ThemeType.of(it) }

/**
 * The final path for saving the backup.
 */
fun Context.readBackupSavePathSaved() = readStoreString(key = KeyBackupSavePath, defValue = "").map { it.isNotEmpty() }
fun Context.readBackupSavePath() = readStoreString(key = KeyBackupSavePath, defValue = ConstantUtil.DefaultPath)
fun Context.readBackupSaveParentPath() = readStoreString(key = KeyBackupSaveParentPath, defValue = ConstantUtil.DefaultPathParent)

/**
 * It defines restore source path, user can set it at [PageRestore].
 * Databases, icons, archives and other stuffs need to be reloaded
 * each time since the path is changed.
 */
fun Context.readRestoreSavePath() = readStoreString(key = KeyRestoreSavePath, defValue = ConstantUtil.DefaultPath)
fun Context.readRestoreSaveParentPath() = readStoreString(key = KeyRestoreSaveParentPath, defValue = ConstantUtil.DefaultPathParent)

// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveLastRestoreTime(value: String) = saveStoreString(key = KeyLastRestoreTime, value = value.trim())
suspend fun Context.saveCompressionType(value: CompressionType) = saveStoreString(key = KeyCompressionType, value = value.type.trim())
suspend fun Context.saveBackupSortType(value: SortType) = saveStoreString(key = KeyBackupSortType, value = value.name.trim())
suspend fun Context.saveRestoreSortType(value: SortType) = saveStoreString(key = KeyRestoreSortType, value = value.name.trim())
suspend fun Context.saveAppVersionName() = saveStoreString(key = KeyAppVersionName, value = getCurrentAppVersionName())
suspend fun Context.saveCloudActivatedAccountName(value: String) = saveStoreString(key = KeyCloudActivatedAccountName, value = value.trim())
suspend fun Context.saveLoadedIconMD5(value: String) = saveStoreString(key = KeyLoadedIconMD5, value = value.trim())
suspend fun Context.saveSelectionType(value: SelectionType) = saveStoreString(key = KeySelectionType, value = value.name.trim())
suspend fun Context.saveThemeType(value: ThemeType) = saveStoreString(key = KeyThemeType, value = value.name.trim())

/**
 * @see [readBackupSavePath]
 */
suspend fun Context.saveBackupSavePath(value: String) = saveStoreString(key = KeyBackupSavePath, value = value.trim())
suspend fun Context.saveBackupSaveParentPath(value: String) = saveStoreString(key = KeyBackupSaveParentPath, value = value.trim())

/**
 * @see [readRestoreSavePath]
 */
suspend fun Context.saveRestoreSavePath(value: String) = saveStoreString(key = KeyRestoreSavePath, value = value.trim())
suspend fun Context.saveRestoreSaveParentPath(value: String) = saveStoreString(key = KeyRestoreSaveParentPath, value = value.trim())
