package com.xayah.databackup.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.xayah.databackup.R
import java.text.SimpleDateFormat
import java.util.*

class DataUtil {
    companion object {
        fun getAppInfo(mContext: Context, packageName: String): Triple<Drawable, String, String> {
            var appName = mContext.getString(R.string.get_error)
            var appIcon = AppCompatResources.getDrawable(mContext, R.drawable.ic_outline_android)!!
            try {
                val packageManager: PackageManager = mContext.packageManager
                val appInfo =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                appName = packageManager.getApplicationLabel(appInfo).toString()
                appIcon = packageManager.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return Triple(appIcon, appName, packageName)
        }

        fun getFormatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr: String = simpleDateFormat.format(date)
            return dateStr
        }
    }
}