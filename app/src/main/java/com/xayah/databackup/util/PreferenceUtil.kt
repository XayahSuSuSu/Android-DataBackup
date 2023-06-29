package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.R
import com.xayah.databackup.util.command.EnvUtil.getCurrentAppVersionName

const val PreferenceName = "settings"

private fun Context.savePreferences(key: String, value: String) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putString(key, value)
        apply()
    }
}

private fun Context.savePreferences(key: String, value: Boolean) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putBoolean(key, value)
        apply()
    }
}

private fun Context.savePreferences(key: String, value: Int) {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).edit().apply {
        putInt(key, value)
        apply()
    }
}

private fun Context.readPreferencesString(key: String, defValue: String = ""): String? {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getString(key, defValue)
    }
}

private fun Context.readPreferencesBoolean(key: String, defValue: Boolean): Boolean {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getBoolean(key, defValue)
    }
}

private fun Context.readPreferencesInt(key: String, defValue: Int): Int {
    getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).apply {
        return getInt(key, defValue)
    }
}

fun Context.saveAppVersionName() {
    savePreferences("app_version_name", getCurrentAppVersionName())
}

fun Context.readAppVersionName(): String {
    return readPreferencesString("app_version_name") ?: ""
}

fun Context.saveLastBackupTime(time: String) {
    savePreferences("last_backup_time", time)
}

fun Context.readLastBackupTime(): String {
    return readPreferencesString("last_backup_time", getString(R.string.none)) ?: getString(R.string.none)
}

fun Context.saveBackupSavePath(path: String) {
    savePreferences("backup_save_path", path.trim())
}

fun Context.readBackupSavePath(): String {
    return readPreferencesString("backup_save_path", ConstantUtil.DefaultBackupSavePath)
        ?: ConstantUtil.DefaultBackupSavePath
}

/**
 * The child of internal path.
 * e.g. "DataBackup" in "/storage/emulated/0/DataBackup".
 */
fun Context.saveInternalBackupSaveChild(child: String) {
    savePreferences("backup_save_child_internal", child.trim())
}

/**
 * @see [saveInternalBackupSaveChild]
 */
fun Context.readInternalBackupSaveChild(): String {
    return readPreferencesString("backup_save_child_internal", ConstantUtil.DefaultBackupChild)
        ?: ConstantUtil.DefaultBackupChild
}

/**
 * The child of external path.
 * Though there could be more than one external storage devices,
 * we save only one child of them.
 * e.g. "DataBackup" in "/mnt/media_rw/E7F9-FA61/DataBackup".
 */
fun Context.saveExternalBackupSaveChild(child: String) {
    savePreferences("backup_save_child_external", child.trim())
}

/**
 * @see [saveExternalBackupSaveChild]
 */
fun Context.readExternalBackupSaveChild(): String {
    return readPreferencesString("backup_save_child_external", ConstantUtil.DefaultBackupChild)
        ?: ConstantUtil.DefaultBackupChild
}
