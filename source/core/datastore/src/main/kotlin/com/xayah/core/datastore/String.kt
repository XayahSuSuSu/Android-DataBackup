package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xayah.core.model.CompressionType
import com.xayah.core.model.util.of
import com.xayah.core.util.ConstantUtil
import kotlinx.coroutines.flow.map

// -----------------------------------------Keys-----------------------------------------
val KeyLastBackupTime = stringPreferencesKey("last_backup_time")
val KeyLastRestoreTime = stringPreferencesKey("last_restore_time")
val KeyCompressionType = stringPreferencesKey("compression_type")
val KeyBackupSavePath = stringPreferencesKey("backup_save_path")
val KeyRestoreSavePath = stringPreferencesKey("restore_save_path")


// -----------------------------------------Read-----------------------------------------
fun Context.readLastBackupTime() = readStoreString(key = KeyLastBackupTime, defValue = "")
fun Context.readLastRestoreTime() = readStoreString(key = KeyLastRestoreTime, defValue = "")
fun Context.readCompressionType() = readStoreString(key = KeyCompressionType, defValue = "").map { CompressionType.of(it) }

/**
 * The final path for saving the backup.
 */
fun Context.readBackupSavePath() = readStoreString(key = KeyBackupSavePath, defValue = ConstantUtil.DefaultPath)

/**
 * It defines restore source path, user can set it at [PageRestore].
 * Databases, icons, archives and other stuffs need to be reloaded
 * each time since the path is changed.
 */
fun Context.readRestoreSavePath() = readStoreString(key = KeyRestoreSavePath, defValue = ConstantUtil.DefaultPath)

// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveLastBackupTime(value: String) = saveStoreString(key = KeyLastBackupTime, value = value.trim())
suspend fun Context.saveLastRestoreTime(value: String) = saveStoreString(key = KeyLastRestoreTime, value = value.trim())
suspend fun Context.saveCompressionType(value: CompressionType) = saveStoreString(key = KeyCompressionType, value = value.type.trim())

/**
 * @see [readBackupSavePath]
 */
suspend fun Context.saveBackupSavePath(value: String) = saveStoreString(key = KeyBackupSavePath, value = value.trim())

/**
 * @see [readRestoreSavePath]
 */
suspend fun Context.saveRestoreSavePath(value: String) = saveStoreString(key = KeyRestoreSavePath, value = value.trim())
