package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyBackupUserId = intPreferencesKey("backup_user_id")
val KeyRestoreUserId = intPreferencesKey("restore_user_id")


// -----------------------------------------Read-----------------------------------------
fun Context.readBackupUserId() = readStoreInt(key = KeyBackupUserId, defValue = 0)
fun Context.readRestoreUserId() = readStoreInt(key = KeyRestoreUserId, defValue = 0)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveBackupUserId(value: Int) = saveStoreInt(key = KeyBackupUserId, value = value)
suspend fun Context.saveRestoreUserId(value: Int) = saveStoreInt(key = KeyRestoreUserId, value = value)
