package com.xayah.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import com.xayah.core.datastore.ConstantUtil.DEFAULT_IDLE_TIMEOUT

// -----------------------------------------Keys-----------------------------------------
val KeyScreenOffCountDown = intPreferencesKey("screen_off_count_down")
val KeyScreenOffTimeout = intPreferencesKey("screen_off_timeout")
val KeyRestoreUser = intPreferencesKey("restore_user")
val KeyCompressionLevel = intPreferencesKey("compression_level")


// -----------------------------------------Read-----------------------------------------
fun Context.readScreenOffCountDown() = readStoreInt(key = KeyScreenOffCountDown, defValue = 0)
fun Context.readScreenOffTimeout() = readStoreInt(key = KeyScreenOffTimeout, defValue = DEFAULT_IDLE_TIMEOUT)
fun Context.readRestoreUser() = readStoreInt(key = KeyRestoreUser, defValue = -1)
fun Context.readCompressionLevel() = readStoreInt(key = KeyCompressionLevel, defValue = 1)


// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveScreenOffCountDown(value: Int) = saveStoreInt(key = KeyScreenOffCountDown, value = value)
suspend fun Context.saveScreenOffTimeout(value: Int) = saveStoreInt(key = KeyScreenOffTimeout, value = value)
suspend fun Context.saveRestoreUser(value: Int) = saveStoreInt(key = KeyRestoreUser, value = value)
suspend fun Context.saveCompressionLevel(value: Int) = saveStoreInt(key = KeyCompressionLevel, value = value)
