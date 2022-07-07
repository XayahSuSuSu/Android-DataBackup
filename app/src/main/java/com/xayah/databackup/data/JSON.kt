package com.xayah.databackup.data

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose

data class Release(
    val html_url: String, val name: String, val assets: List<Asset>, val body: String
)

data class Asset(
    val browser_download_url: String
)

data class Issue(
    val html_url: String, val title: String, val body: String
)

data class AppInfoBase(
    @Expose var appName: String,
    @Expose var packageName: String,
    @Expose var versionName: String,
    @Expose var versionCode: Long,
    @Expose var app: Boolean,
    @Expose var data: Boolean
)

data class AppInfoBaseNum(
    var appNum: Int, var dataNum: Int
)

data class AppInfoBackup(
    var appIcon: Drawable? = null,
    @Expose var appSize: String,
    @Expose var userSize: String,
    @Expose var dataSize: String,
    @Expose var obbSize: String,
    @Expose val infoBase: AppInfoBase,
)

data class AppInfoRestore(
    var appIcon: Drawable? = null,
    @Expose val infoBase: AppInfoBase,
)

data class MediaInfo(
    @Expose var name: String,
    @Expose var path: String,
    @Expose var data: Boolean,
    @Expose var size: String
)

data class BackupInfo(
    @Expose var version: String,
    @Expose var startTime: String,
    @Expose var endTime: String,
    @Expose var startSize: String,
    @Expose var endSize: String,
    @Expose var type: String,
    @Expose var backupUser: String
)