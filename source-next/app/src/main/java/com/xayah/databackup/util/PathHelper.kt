package com.xayah.databackup.util

import android.annotation.SuppressLint
import com.xayah.databackup.App
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@SuppressLint("SdCardPath")
object PathHelper {
    const val TMP_PARCEL_PREFIX = "databackup-parcel-"
    const val TMP_FIFO_PREFIX = "databackup-fifo-"
    const val TMP_SUFFIX = ".tmp"

    const val DEFAULT_BACKUP_PATH = "/storage/emulated/0/DataBackup"
    private const val SUBDIR_BACKUPS = "backups"
    private const val SUBDIR_APPS = "apps"
    private const val SUBDIR_APK = "apk"
    private const val SUBDIR_INT_DATA = "int_data"
    private const val SUBDIR_EXT_DATA = "ext_data"
    private const val SUBDIR_ADDL_DATA = "addl_data"
    private const val BACKUP_CONFIG_FILE = ".config"

    private const val APK_FILE_NAME = "apk.tar.zst"
    private const val USER_FILE_NAME = "user.tar.zst"
    private const val USER_DE_FILE_NAME = "user_de.tar.zst"
    private const val DATA_FILE_NAME = "data.tar.zst"
    private const val OBB_FILE_NAME = "obb.tar.zst"
    private const val MEDIA_FILE_NAME = "media.tar.zst"

    /**
     * Returns the parent path, or empty string if this path does not have a parent.
     */
    fun getParentPath(path: String): String {
        if (path.contains('/').not() || path == "/") return ""
        return path.substring(0, path.lastIndexOf('/'))
    }

    /**
     * Returns the child path, or empty string if this path does not have a child.
     */
    fun getChildPath(path: String): String {
        if (path.contains('/').not() || path == "/") return ""
        return path.substring(path.lastIndexOf('/') + 1)
    }

    fun getAppUserDir(userId: Int, packageName: String): String = "/data/user/$userId/$packageName"
    fun getAppUserDeDir(userId: Int, packageName: String): String = "/data/user_de/$userId/$packageName"
    fun getAppDataDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/data/$packageName"
    fun getAppObbDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/obb/$packageName"
    fun getAppMediaDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/media/$packageName"

    fun getBackupConfigFile(parent: String): String = "$parent/$BACKUP_CONFIG_FILE"
    fun getBackupAppsApkDir(parent: String, packageName: String): String = "$parent/$SUBDIR_APPS/$packageName/$SUBDIR_APK"
    fun getBackupAppsIntDataDir(parent: String, packageName: String): String = "$parent/$SUBDIR_APPS/$packageName/$SUBDIR_INT_DATA"
    fun getBackupAppsExtDataDir(parent: String, packageName: String): String = "$parent/$SUBDIR_APPS/$packageName/$SUBDIR_EXT_DATA"
    fun getBackupAppsAddlDataDir(parent: String, packageName: String): String = "$parent/$SUBDIR_APPS/$packageName/$SUBDIR_ADDL_DATA"

    fun getBackupAppsApkFilePath(parent: String, packageName: String): String =
        "${getBackupAppsApkDir(parent, packageName)}/$APK_FILE_NAME"

    fun getBackupAppsUserFilePath(parent: String, packageName: String): String =
        "${getBackupAppsIntDataDir(parent, packageName)}/$USER_FILE_NAME"

    fun getBackupAppsUserDeFilePath(parent: String, packageName: String): String =
        "${getBackupAppsIntDataDir(parent, packageName)}/$USER_DE_FILE_NAME"

    fun getBackupAppsDataFilePath(parent: String, packageName: String): String =
        "${getBackupAppsExtDataDir(parent, packageName)}/$DATA_FILE_NAME"

    fun getBackupAppsObbFilePath(parent: String, packageName: String): String =
        "${getBackupAppsAddlDataDir(parent, packageName)}/$OBB_FILE_NAME"

    fun getBackupAppsMediaFilePath(parent: String, packageName: String): String =
        "${getBackupAppsAddlDataDir(parent, packageName)}/$MEDIA_FILE_NAME"

    fun getBackupPath(): Flow<String> = App.application.readString(BackupPath)
    fun getBackupPathBackups(): Flow<String> = getBackupPath().map { "${it.trimEnd('/')}/$SUBDIR_BACKUPS" }
}
