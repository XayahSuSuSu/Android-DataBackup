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

        fun getBackupAppListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/backup/appList"
        }

        fun getBackupMediaListPath(): String {
            return "${App.globalContext.readBackupSavePath()}/config/backup/mediaList"
        }
    }
}