package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyBackupFilterFlagIndex = intPreferencesKey("backup_filter_flag_index")
val KeyRestoreFilterFlagIndex = intPreferencesKey("restore_filter_flag_index")


// -----------------------------------------Read-----------------------------------------
fun Context.readBackupFilterFlagIndex() = readStoreInt(key = KeyBackupFilterFlagIndex, defValue = 1)
fun Context.readRestoreFilterFlagIndex() = readStoreInt(key = KeyRestoreFilterFlagIndex, defValue = 1)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveBackupFilterFlagIndex(value: Int) = saveStoreInt(key = KeyBackupFilterFlagIndex, value = value)
suspend fun Context.saveRestoreFilterFlagIndex(value: Int) = saveStoreInt(key = KeyRestoreFilterFlagIndex, value = value)
