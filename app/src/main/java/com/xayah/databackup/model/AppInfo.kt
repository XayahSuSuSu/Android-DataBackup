package com.xayah.databackup.model

import android.graphics.drawable.Drawable

data class AppInfo(
    var appName: String,
    var appPackage: String,
    var appType: String,
    var isOnly: Boolean,
    var isSelected: Boolean,
    var appIcon: Drawable? = null
)

