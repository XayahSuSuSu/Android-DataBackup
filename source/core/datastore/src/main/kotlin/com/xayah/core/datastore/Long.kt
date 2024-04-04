package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyIconUpdateTime = longPreferencesKey("icon_update_time")
val KeyLastBackupTime = longPreferencesKey("last_backup_time")


// -----------------------------------------Read-----------------------------------------
fun Context.readIconUpdateTime() = readStoreLong(key = KeyIconUpdateTime, defValue = 0)
fun Context.readLastBackupTime() = readStoreLong(key = KeyLastBackupTime, defValue = 0)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveIconUpdateTime(value: Long) = saveStoreLong(key = KeyIconUpdateTime, value = value)
suspend fun Context.saveLastBackupTime(value: Long) = saveStoreLong(key = KeyLastBackupTime, value = value)
