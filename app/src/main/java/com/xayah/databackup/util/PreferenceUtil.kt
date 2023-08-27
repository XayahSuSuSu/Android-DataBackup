package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.util.command.EnvUtil.getCurrentAppVersionName
import com.xayah.librootservice.util.ExceptionUtil.tryOn

enum class CompressionType(val type: String, val suffix: String, val para: String) {
    TAR("tar", "tar", ""),
    ZSTD("zstd", "tar.zst", "zstd -r -T0 --ultra -1 -q --priority=rt"),
    LZ4("lz4", "tar.lz4", "zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4");

    companion object {
        fun of(name: String?): CompressionType {
            return tryOn(
                block = {
                    CompressionType.valueOf(name!!.uppercase())
                },
                onException = {
                    ZSTD
                })
        }
    }
}

enum class DataType(val type: String) {
    PACKAGE_APK("apk"),
    PACKAGE_USER("user"),
    PACKAGE_USER_DE("user_de"),
    PACKAGE_DATA("data"),
    PACKAGE_OBB("obb"),
    PACKAGE_MEDIA("media"),            // /data/media/$user_id/Android/media
    MEDIA_MEDIA("media");

    fun origin(userId: Int): String = when (this) {
        PACKAGE_USER -> PathUtil.getPackageUserPath(userId)
        PACKAGE_USER_DE -> PathUtil.getPackageUserDePath(userId)
        PACKAGE_DATA -> PathUtil.getPackageDataPath(userId)
        PACKAGE_OBB -> PathUtil.getPackageObbPath(userId)
        PACKAGE_MEDIA -> PathUtil.getPackageMediaPath(userId)
        else -> ""
    }

    fun updateEntityLog(entity: PackageBackupOperation, msg: String) {
        when (this) {
            PACKAGE_USER -> entity.userLog = msg
            PACKAGE_USER_DE -> entity.userDeLog = msg
            PACKAGE_DATA -> entity.dataLog = msg
            PACKAGE_OBB -> entity.obbLog = msg
            PACKAGE_MEDIA -> entity.mediaLog = msg
            else -> {}
        }
    }

    fun updateEntityState(entity: PackageBackupOperation, state: OperationState) {
        when (this) {
            PACKAGE_USER -> entity.userState = state
            PACKAGE_USER_DE -> entity.userDeState = state
            PACKAGE_DATA -> entity.dataState = state
            PACKAGE_OBB -> entity.obbState = state
            PACKAGE_MEDIA -> entity.mediaState = state
            else -> {}
        }
    }

    companion object {
        fun of(name: String): DataType {
            return tryOn(
                block = {
                    DataType.valueOf(name.uppercase())
                },
                onException = {
                    PACKAGE_USER
                })
        }
    }
}

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

fun Context.saveCompressionType(ct: CompressionType) {
    savePreferences("compression_type", ct.type)
}

fun Context.readCompressionType(): CompressionType {
    return CompressionType.of(readPreferencesString("compression_type"))
}

fun Context.saveCompatibleMode(value: Boolean) {
    savePreferences("compatible_mode", value)
}

fun Context.readCompatibleMode(): Boolean {
    return readPreferencesBoolean("compatible_mode", false)
}

fun Context.saveLastBackupTime(time: String) {
    savePreferences("last_backup_time", time)
}

fun Context.readLastBackupTime(): String {
    return readPreferencesString("last_backup_time", getString(R.string.none)) ?: getString(R.string.none)
}

fun Context.saveBackupUserId(userId: Int) {
    savePreferences("backup_user_id", userId)
}

fun Context.readBackupUserId(): Int {
    return readPreferencesInt("backup_user_id", ConstantUtil.DefaultBackupUserId)
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
