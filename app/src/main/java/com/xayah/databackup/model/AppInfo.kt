package com.xayah.databackup.model

import android.graphics.drawable.Drawable

data class AppInfo(
    var appIcon: Drawable,
    var appName: String,
    var appPackage: String,
    var ban: Boolean = false,
    var onlyApp: Boolean = true
)

