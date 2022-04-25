package com.xayah.databackup.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.xayah.databackup.R

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

fun Context.readPreferencesBoolean(key: String): Boolean {
    getSharedPreferences("settings", MODE_PRIVATE).apply {
        return getBoolean(key, false)
    }
}

fun Context.saveBackupSavePath(path: CharSequence?) {
    savePreferences("backup_save_path", path.toString().trim())
}

fun Context.readBackupSavePath(): String {
    return readPreferencesString("backup_save_path") ?: getString(R.string.default_backup_save_path)
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
        ?: getString(R.string.default_custom_directory_path)
}