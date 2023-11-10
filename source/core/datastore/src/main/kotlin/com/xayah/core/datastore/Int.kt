package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyBackupUserId = intPreferencesKey("backup_user_id")
val KeyRestoreUserId = intPreferencesKey("restore_user_id")
val KeyBackupSortTypeIndex = intPreferencesKey("backup_sort_type_index")
val KeyBackupFilterFlagIndex = intPreferencesKey("backup_filter_flag_index")


// -----------------------------------------Read-----------------------------------------
fun Context.readBackupUserId() = readStoreInt(key = KeyBackupUserId, defValue = 0)
fun Context.readRestoreUserId() = readStoreInt(key = KeyRestoreUserId, defValue = 0)
fun Context.readBackupSortTypeIndex() = readStoreInt(key = KeyBackupSortTypeIndex, defValue = 0)
fun Context.readBackupFilterFlagIndex() = readStoreInt(key = KeyBackupFilterFlagIndex, defValue = 1)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveBackupUserId(value: Int) = saveStoreInt(key = KeyBackupUserId, value = value)
suspend fun Context.saveRestoreUserId(value: Int) = saveStoreInt(key = KeyRestoreUserId, value = value)
suspend fun Context.saveBackupSortTypeIndex(value: Int) = saveStoreInt(key = KeyBackupSortTypeIndex, value = value)
suspend fun Context.saveBackupFilterFlagIndex(value: Int) = saveStoreInt(key = KeyBackupFilterFlagIndex, value = value)
