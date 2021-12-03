package com.xayah.databackup.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.text.SimpleDateFormat
import java.util.*

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

        fun getFormatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr: String = simpleDateFormat.format(date)
            return dateStr
        }
    }
}