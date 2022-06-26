package com.xayah.databackup.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

fun Context.savePreferences(key: String, value: String) {
    getSharedPreferences("settings", MODE_PRIVATE).edit().apply {
        putString(key, value)
        apply()
    }
}

fun Context.savePreferences(key: String, value: Boolean) {
    getSharedPreferences("settings", MODE_PRIVATE).edit().apply {
        putBoolean(key, value)
        apply()
    }
}

fun Context.readPreferencesString(key: String): String? {
    getSharedPreferences("settings", MODE_PRIVATE).apply {
        return getString(key, null)
    }
}

fun Context.readPreferencesBoolean(key: String, defValue: Boolean = false): Boolean {
    getSharedPreferences("settings", MODE_PRIVATE).apply {
        return getBoolean(key, defValue)
    }
}

fun Context.saveBackupSavePath(path: CharSequence?) {
    savePreferences("backup_save_path", path.toString().trim())
}

fun Context.readBackupSavePath(): String {
    return readPreferencesString("backup_save_path") ?: GlobalString.defaultBackupSavePath
}

fun Context.saveCompressionType(type: CharSequence?) {
    savePreferences("compression_type", type.toString().trim())
}

fun Context.readCompressionType(): String {
    return readPreferencesString("compression_type") ?: "zstd"
}

fun Context.saveIsCustomDirectoryPath(value: Boolean) {
    savePreferences("is_custom_directory_path", value)
}

fun Context.readIsCustomDirectoryPath(): Boolean {
    return readPreferencesBoolean("is_custom_directory_path")
}

fun Context.saveIsDynamicColors(value: Boolean) {
    savePreferences("is_dynamic_colors", value)
}

fun Context.readIsDynamicColors(): Boolean {
    return readPreferencesBoolean("is_dynamic_colors")
}

fun Context.saveCustomDirectoryPath(path: CharSequence?) {
    savePreferences("custom_directory_path", path.toString().trim())
}

fun Context.readCustomDirectoryPath(): String {
    return readPreferencesString("custom_directory_path")
        ?: GlobalString.defaultCustomDirectoryPath
}

fun Context.saveIsBackupItself(value: Boolean) {
    savePreferences("is_backup_itself", value)
}

fun Context.readIsBackupItself(): Boolean {
    return readPreferencesBoolean("is_backup_itself", true)
}

fun Context.saveBackupUser(type: CharSequence?) {
    savePreferences("backup_user", type.toString().trim())
}

fun Context.readBackupUser(): String {
    return readPreferencesString("backup_user")
        ?: if (Bashrc.listUsers().first && Bashrc.listUsers().second.isNotEmpty()) Bashrc.listUsers().second[0] else "0"
}

fun Context.saveRestoreUser(type: CharSequence?) {
    savePreferences("restore_user", type.toString().trim())
}

fun Context.readRestoreUser(): String {
    return readPreferencesString("restore_user")
        ?: if (Bashrc.listUsers().first && Bashrc.listUsers().second.isNotEmpty()) Bashrc.listUsers().second[0] else "0"
}

fun Context.saveIsSupportSystemApp(value: Boolean) {
    savePreferences("support_system_app", value)
}

fun Context.readIsSupportSystemApp(): Boolean {
    return readPreferencesBoolean("support_system_app", false)
}

fun Context.saveFileExplorerPath(path: CharSequence?) {
    savePreferences("file_explorer_path", path.toString().trim())
}

fun Context.readFileExplorerPath(): String {
    return readPreferencesString("file_explorer_path") ?: GlobalString.defaultBackupSavePath
}

fun Context.saveInitializedVersionName(value: CharSequence?) {
    savePreferences("initialized_version_name", value.toString().trim())
}

fun Context.readInitializedVersionName(): String {
    return readPreferencesString("initialized_version_name") ?: ""
}