package com.xayah.databackup.util

import android.annotation.SuppressLint

@SuppressLint("SdCardPath")
object PathHelper {
    fun getAppUserDir(userId: Int, packageName: String): String = "/data/user/$userId/$packageName"
    fun getAppUserDeDir(userId: Int, packageName: String): String = "/data/user_de/$userId/$packageName"
    fun getAppDataDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/data/$packageName"
    fun getAppObbDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/obb/$packageName"
    fun getAppMediaDir(userId: Int, packageName: String): String = "/data/media/${userId}/Android/media/$packageName"
}
