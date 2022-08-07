package com.xayah.databackup.util

import android.content.Context
import android.os.Environment
import com.xayah.databackup.App

class Path {
    companion object {
        fun getFilesDir(): String {
            return App.globalContext.filesDir.path
        }

        fun getExternalFilesDir(context: Context): String? {
            return context.getExternalFilesDir(null)?.path
        }

        fun getExternalStorageDirectory(): String {
            return Environment.getExternalStorageDirectory().path
        }

        fun getExternalStorageDataBackupDirectory(): String {
            return "${getExternalStorageDirectory()}/DataBackup"
        }

        fun getCustomDataBackupDirectory(): String {
            return App.globalContext.readCustomBackupSavePath()
        }

        fun getUserPath(): String {
            return "/data/user/${App.globalContext.readBackupUser()}"
        }

        fun getUserPath(userId: String): String {
            return "/data/user/${userId}"
        }

        fun getDataPath(): String {
            return "/data/media/${App.globalContext.readBackupUser()}/Android/data"
        }

        fun getDataPath(userId: String): String {
            return "/data/media/${userId}/Android/data"
        }

        fun getObbPath(): String {
            return "/data/media/${App.globalContext.readBackupUser()}/Android/obb"
        }

        fun getObbPath(userId: String): String {
            return "/data/media/${userId}/Android/obb"
        }

        fun getAppInfoBackupListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/${App.globalContext.readBackupUser()}/backup/appList"
        }

        fun getAppInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/${App.globalContext.readBackupUser()}/restore/appList"
        }

        fun getMediaInfoBackupListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/${App.globalContext.readBackupUser()}/backup/mediaList"
        }

        fun getMediaInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/${App.globalContext.readBackupUser()}/restore/mediaList"
        }

        fun getBackupDataSavePath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/data"
        }

        fun getBackupMediaSavePath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/media"
        }

        fun getBackupUserPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup"
        }

        fun getBackupInfoListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/backupList"
        }

        fun getShellLogPath(): String {
            return "${App.globalContext.readBackupSavePath()}/log"
        }
    }
}