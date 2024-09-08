package com.xayah.core.datastore

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey

// -----------------------------------------Keys-----------------------------------------
val KeyMonet = booleanPreferencesKey("monet")
val KeyKeepScreenOn = booleanPreferencesKey("keep_screen_on")
val KeyBackupItself = booleanPreferencesKey("backup_itself")
val KeyCompressionTest = booleanPreferencesKey("compression_test")
val KeyResetBackupList = booleanPreferencesKey("reset_backup_list")
val KeyCompatibleMode = booleanPreferencesKey("compatible_mode")
val KeyFollowSymlinks = booleanPreferencesKey("follow_symlinks")
val KeyCleanRestoring = booleanPreferencesKey("clean_restoring")
val KeyResetRestoreList = booleanPreferencesKey("reset_restore_list")
val KeyCheckKeystore = booleanPreferencesKey("check_keystore")
val KeyReloadDumpApk = booleanPreferencesKey("reload_dump_apk")
val KeyAutoScreenOff = booleanPreferencesKey("auto_screen_off")
val KeyLoadSystemApps = booleanPreferencesKey("load_system_apps")

// -----------------------------------------Read-----------------------------------------
fun Context.readMonet() = readStoreBoolean(key = KeyMonet, defValue = true)
fun Context.readKeepScreenOn() = readStoreBoolean(key = KeyKeepScreenOn, defValue = true)
fun Context.readBackupItself() = readStoreBoolean(key = KeyBackupItself, defValue = true)
fun Context.readCompressionTest() = readStoreBoolean(key = KeyCompressionTest, defValue = true)
fun Context.readResetBackupList() = readStoreBoolean(key = KeyResetBackupList, defValue = false)
fun Context.readCompatibleMode() = readStoreBoolean(key = KeyCompatibleMode, defValue = Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
fun Context.readFollowSymlinks() = readStoreBoolean(key = KeyFollowSymlinks, defValue = false)
fun Context.readCleanRestoring() = readStoreBoolean(key = KeyCleanRestoring, defValue = false)
fun Context.readResetRestoreList() = readStoreBoolean(key = KeyResetRestoreList, defValue = false)
fun Context.readCheckKeystore() = readStoreBoolean(key = KeyCheckKeystore, defValue = true)
fun Context.readLoadSystemApps() = readStoreBoolean(key = KeyLoadSystemApps, defValue = false)
fun Context.readReloadDumpApk() = readStoreBoolean(key = KeyReloadDumpApk, defValue = true)
fun Context.readAutoScreenOff() = readStoreBoolean(key = KeyAutoScreenOff, defValue = false)

// -----------------------------------------Write-----------------------------------------
suspend fun Context.saveMonet(value: Boolean) = saveStoreBoolean(key = KeyMonet, value = value)
suspend fun Context.saveKeepScreenOn(value: Boolean) = saveStoreBoolean(key = KeyKeepScreenOn, value = value)
suspend fun Context.saveBackupItself(value: Boolean) = saveStoreBoolean(key = KeyBackupItself, value = value)
suspend fun Context.saveCompressionTest(value: Boolean) = saveStoreBoolean(key = KeyCompressionTest, value = value)
suspend fun Context.saveResetBackupList(value: Boolean) = saveStoreBoolean(key = KeyResetBackupList, value = value)
suspend fun Context.saveCompatibleMode(value: Boolean) = saveStoreBoolean(key = KeyCompatibleMode, value = value)
suspend fun Context.saveFollowSymlinks(value: Boolean) = saveStoreBoolean(key = KeyFollowSymlinks, value = value)
suspend fun Context.saveCleanRestoring(value: Boolean) = saveStoreBoolean(key = KeyCleanRestoring, value = value)
suspend fun Context.saveResetRestoreList(value: Boolean) = saveStoreBoolean(key = KeyResetRestoreList, value = value)
suspend fun Context.saveCheckKeystore(value: Boolean) = saveStoreBoolean(key = KeyCheckKeystore, value = value)
suspend fun Context.saveLoadSystemApps(value: Boolean) = saveStoreBoolean(key = KeyLoadSystemApps, value = value)
suspend fun Context.saveReloadDumpApk(value: Boolean) = saveStoreBoolean(key = KeyReloadDumpApk, value = value)
suspend fun Context.saveAutoScreenOff(value: Boolean) = saveStoreBoolean(key = KeyAutoScreenOff, value = value)
