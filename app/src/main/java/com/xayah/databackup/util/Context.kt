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

fun Context.readPreferences(key: String): String? {
    getSharedPreferences("settings", MODE_PRIVATE).apply {
        return getString(key, null)
    }
}

fun Context.saveBackupSavePath(path: CharSequence?) {
    savePreferences("backup_save_path", path.toString().trim())
}

fun Context.saveCompressionType(type: CharSequence?) {
    savePreferences("compression_type", type.toString().trim())
}

fun Context.readBackupSavePath(): String {
    return readPreferences("backup_save_path") ?: getString(R.string.default_backup_save_path)
}

fun Context.readCompressionType(): String {
    return readPreferences("compression_type") ?: "zstd"
}