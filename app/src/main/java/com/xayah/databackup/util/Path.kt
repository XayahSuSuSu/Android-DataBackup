package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.App

class Path {
    companion object {
        fun getFilesDir(context: Context): String {
            return context.filesDir.path
        }

        fun getExternalFilesDir(context: Context): String? {
            return context.getExternalFilesDir(null)?.path
        }

        fun getUserPath(userId: String): String {
            return "/data/user/$userId"
        }

        fun getDataPath(userId: String): String {
            return "/data/media/$userId/Android/data"
        }

        fun getObbPath(userId: String): String {
            return "/data/media/$userId/Android/obb"
        }

        fun getAppInfoBackupListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/backup/appList"
        }

        fun getAppInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/restore/appList"
        }

        fun getMediaInfoBackupListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/backup/mediaList"
        }

        fun getMediaInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/restore/mediaList"
        }

        fun getBackupDataSavePath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/data"
        }

        fun getBackupMediaSavePath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/media"
        }
    }
}