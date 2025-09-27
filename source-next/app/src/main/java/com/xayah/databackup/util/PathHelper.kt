package com.xayah.databackup.util

import android.annotation.SuppressLint
import com.xayah.databackup.App
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@SuppressLint("SdCardPath")
object PathHelper {
    const val DEFAULT_BACKUP_PATH = "/storage/emulated/0/DataBackup"
    private const val SUBDIR_BACKUPS = "backups"
    private const val BACKUP_CONFIG_FILE = ".config"

    fun getAppUserDir(userId: Int, packageName: String): String = "/data/user/$userId/$packageName"
    fun getAppUserDeDir(userId: Int, packageName: String): String = "/data/user_de/$userId/$packageName"
    fun getAppDataDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/data/$packageName"
    fun getAppObbDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/obb/$packageName"
    fun getAppMediaDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/media/$packageName"

    fun getBackupConfigFile(parent: String): String = "$parent/$BACKUP_CONFIG_FILE"

    fun getBackupPath(): Flow<String> = App.application.readString(BackupPath)
    fun getBackupPathBackups(): Flow<String> = App.application.readString(BackupPath).map { "${it.trimEnd('/')}/$SUBDIR_BACKUPS" }
}
