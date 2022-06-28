package com.xayah.databackup.data

import android.graphics.drawable.Drawable

data class Release(
    val html_url: String, val name: String, val assets: List<Asset>, val body: String
)

data class Asset(
    val browser_download_url: String
)

data class Issue(
    val html_url: String, val title: String, val body: String
)

data class AppInfo2(
    val appName: String,
    val packageName: String,
    val version: String,
    val versionCode: String,
    val apkSize: String,
    val userSize: String,
    val dataSize: String,
    val obbSize: String
)

data class AppInfoBase(
    var appName: String, var packageName: String, var app: Boolean, var data: Boolean
)

data class AppInfoBaseNum(
    var appNum: Int, var dataNum: Int
)

data class AppInfo(
    val appIcon: Drawable, val infoBase: AppInfoBase
)

data class MediaInfo(
    val name: String, val path: String, val size: String
)