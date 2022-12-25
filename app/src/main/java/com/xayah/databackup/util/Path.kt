package com.xayah.databackup.util

import com.xayah.databackup.App

class Path {
    companion object {
        fun getFilesDir(): String {
            return App.globalContext.filesDir.path
        }

        fun getUserPath(): String {
            return "/data/user/${App.globalContext.readBackupUser()}"
        }

        fun getUserDePath(): String {
            return "/data/user_de/${App.globalContext.readBackupUser()}"
        }

        fun getUserPath(userId: String): String {
            return "/data/user/${userId}"
        }

        fun getUserDePath(userId: String): String {
            return "/data/user_de/${userId}"
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

        fun getAppInfoListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/appList"
        }

        fun getAppInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/restore/appList"
        }

        fun getMediaInfoBackupListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/backup/mediaList"
        }

        fun getMediaInfoRestoreListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/restore/mediaList"
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
            return "${App.globalContext.readBackupSavePath()}/backup/backupList"
        }

        fun getShellLogPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/log"
        }
    }
}