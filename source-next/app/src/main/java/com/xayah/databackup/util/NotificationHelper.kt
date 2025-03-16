package com.xayah.databackup.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.EXTRA_CHANNEL_ID
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object NotificationHelper {
    const val REQUEST_CODE = 1

    fun checkPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun requestPermission(context: Context): String? {
        var msg: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val intent = Intent()
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }.onFailure {
                msg = "Please grant notification permission manually"
            }
        } else {
            runCatching {
                val intent = Intent().apply {
                    setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setData("package:${context.packageName}".toUri())
                }
                context.startActivity(intent)
            }.onFailure {
                msg = "Please grant notification permission manually"
            }
        }
        return msg
    }
}