package com.xayah.databackup.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class DataUtil {
    companion object {
        fun getAppInfo(mContext: Context, packageName: String): Triple<Drawable, String, String> {
            val packageManager: PackageManager = mContext.packageManager
            val appInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val appName = packageManager.getApplicationLabel(appInfo)
            val appIcon = packageManager.getApplicationIcon(appInfo)
            return Triple(appIcon, appName.toString(), packageName)
        }
    }
}