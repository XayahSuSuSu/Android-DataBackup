package com.xayah.feature.main.reload.model

import com.google.gson.annotations.Expose

typealias MediaInfoRestoreMap = HashMap<String, MediaInfoRestore>
typealias AppInfoRestoreMap = HashMap<String, AppInfoRestore>

data class MediaInfoDetailBase(
    @Expose var data: Boolean = false,
    @Expose var size: String = "",
    @Expose var date: String = "",
)

data class MediaInfoRestore(
    @Expose var name: String = "",
    @Expose var path: String = "",
    @Expose var detailRestoreList: List<MediaInfoDetailBase> = mutableListOf(),
)

data class AppInfoDetailBase(
    @Expose var isSystemApp: Boolean = false,
    @Expose var appName: String = "",
    @Expose var packageName: String = "",
)

data class AppInfoDetailRestore(
    @Expose var versionName: String = "",
    @Expose var versionCode: Long = 0,
    @Expose var appSize: String = "",
    @Expose var userSize: String = "",
    @Expose var userDeSize: String = "",
    @Expose var dataSize: String = "",
    @Expose var obbSize: String = "",
    @Expose var mediaSize: String = "",
    @Expose var date: String = "",
    @Expose var selectApp: Boolean = false,
    @Expose var selectData: Boolean = false,
    @Expose var hasApp: Boolean = true,
    @Expose var hasData: Boolean = true,
)

data class AppInfoRestore(
    @Expose var detailBase: AppInfoDetailBase = AppInfoDetailBase(),
    @Expose var firstInstallTime: Long = 0,
    @Expose var detailRestoreList: MutableList<AppInfoDetailRestore> = mutableListOf(),
)
