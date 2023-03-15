package com.xayah.databackup.util

import com.xayah.databackup.App

class Path {
    companion object {
        fun getAppInternalFilesPath(): String {
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

        fun getAppInfoBackupMapPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/appBackupMap"
        }

        fun getAppInfoRestoreMapPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/appRestoreMap"
        }

        fun getMediaInfoBackupMapPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/mediaBackupMap"
        }

        fun getMediaInfoRestoreMapPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/mediaRestoreMap"
        }

        fun getMediaInfoListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/config/mediaList"
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

        fun getLogPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/log"
        }

        fun getInternalLogPath(): String {
            return "${getAppInternalFilesPath()}/log"
        }

        fun getRcloneMountListPath(): String {
            return "${getAppInternalFilesPath()}/.config/rclone/mountList"
        }

        fun getDefaultBlackMapPath(): String {
            return "${App.globalContext.readBackupSavePath()}/app/config/blackListMap"
        }

        fun getSmsListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/telephony/smsList"
        }

        fun getMmsListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/telephony/mmsList"
        }

        fun getMmsDataPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/telephony/mmsData"
        }

        fun getContactListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/backup/${App.globalContext.readBackupUser()}/telephony/contactList"
        }
    }
}