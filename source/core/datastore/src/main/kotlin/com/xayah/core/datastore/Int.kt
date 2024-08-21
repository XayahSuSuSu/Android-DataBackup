package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import com.xayah.core.datastore.ConstantUtil.DEFAULT_IDLE_TIMEOUT

// -----------------------------------------Keys-----------------------------------------
val KeyBackupFilterFlagIndex = intPreferencesKey("backup_filter_flag_index")
val KeyRestoreFilterFlagIndex = intPreferencesKey("restore_filter_flag_index")
val KeyScreenOffCountDown = intPreferencesKey("screen_off_count_down")
val KeyScreenOffTimeout = intPreferencesKey("screen_off_timeout")
val KeyRestoreUser = intPreferencesKey("restore_user")


// -----------------------------------------Read-----------------------------------------
fun Context.readBackupFilterFlagIndex() = readStoreInt(key = KeyBackupFilterFlagIndex, defValue = 1)
fun Context.readRestoreFilterFlagIndex() = readStoreInt(key = KeyRestoreFilterFlagIndex, defValue = 1)
fun Context.readScreenOffCountDown() = readStoreInt(key = KeyScreenOffCountDown, defValue = 0)
fun Context.readScreenOffTimeout() = readStoreInt(key = KeyScreenOffTimeout, defValue = DEFAULT_IDLE_TIMEOUT)
fun Context.readRestoreUser() = readStoreInt(key = KeyRestoreUser, defValue = -1)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveBackupFilterFlagIndex(value: Int) = saveStoreInt(key = KeyBackupFilterFlagIndex, value = value)
suspend fun Context.saveRestoreFilterFlagIndex(value: Int) = saveStoreInt(key = KeyRestoreFilterFlagIndex, value = value)
suspend fun Context.saveScreenOffCountDown(value: Int) = saveStoreInt(key = KeyScreenOffCountDown, value = value)
suspend fun Context.saveScreenOffTimeout(value: Int) = saveStoreInt(key = KeyScreenOffTimeout, value = value)
suspend fun Context.saveRestoreUser(value: Int) = saveStoreInt(key = KeyRestoreUser, value = value)
