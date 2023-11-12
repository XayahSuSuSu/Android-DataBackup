package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyBackupUserId = intPreferencesKey("backup_user_id")
val KeyRestoreUserId = intPreferencesKey("restore_user_id")
val KeyBackupSortTypeIndex = intPreferencesKey("backup_sort_type_index")
val KeyRestoreSortTypeIndex = intPreferencesKey("restore_sort_type_index")
val KeyBackupFilterFlagIndex = intPreferencesKey("backup_filter_flag_index")
val KeyRestoreFilterFlagIndex = intPreferencesKey("restore_filter_flag_index")
val KeyRestoreInstallationTypeIndex = intPreferencesKey("restore_installation_type_index")


// -----------------------------------------Read-----------------------------------------
fun Context.readBackupUserId() = readStoreInt(key = KeyBackupUserId, defValue = 0)
fun Context.readRestoreUserId() = readStoreInt(key = KeyRestoreUserId, defValue = 0)
fun Context.readBackupSortTypeIndex() = readStoreInt(key = KeyBackupSortTypeIndex, defValue = 0)
fun Context.readRestoreSortTypeIndex() = readStoreInt(key = KeyRestoreSortTypeIndex, defValue = 0)
fun Context.readBackupFilterFlagIndex() = readStoreInt(key = KeyBackupFilterFlagIndex, defValue = 1)
fun Context.readRestoreFilterFlagIndex() = readStoreInt(key = KeyRestoreFilterFlagIndex, defValue = 1)
fun Context.readRestoreInstallationTypeIndex() = readStoreInt(key = KeyRestoreInstallationTypeIndex, defValue = 0)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveBackupUserId(value: Int) = saveStoreInt(key = KeyBackupUserId, value = value)
suspend fun Context.saveRestoreUserId(value: Int) = saveStoreInt(key = KeyRestoreUserId, value = value)
suspend fun Context.saveBackupSortTypeIndex(value: Int) = saveStoreInt(key = KeyBackupSortTypeIndex, value = value)
suspend fun Context.saveRestoreSortTypeIndex(value: Int) = saveStoreInt(key = KeyRestoreSortTypeIndex, value = value)
suspend fun Context.saveBackupFilterFlagIndex(value: Int) = saveStoreInt(key = KeyBackupFilterFlagIndex, value = value)
suspend fun Context.saveRestoreFilterFlagIndex(value: Int) = saveStoreInt(key = KeyRestoreFilterFlagIndex, value = value)
suspend fun Context.saveRestoreInstallationTypeIndex(value: Int) = saveStoreInt(key = KeyRestoreInstallationTypeIndex, value = value)
