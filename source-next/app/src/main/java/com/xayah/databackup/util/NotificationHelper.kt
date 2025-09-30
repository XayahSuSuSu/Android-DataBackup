package com.xayah.databackup.util

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.EXTRA_CHANNEL_ID
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xayah.databackup.R

object NotificationHelper {
    private const val FOREGROUND_SERVICE_CHANNEL_ID = "ForegroundServiceChannel"
    private const val FOREGROUND_SERVICE_CHANNEL_NAME = "ForegroundService"
    private const val FOREGROUND_SERVICE_CHANNEL_DESC = "For foreground service"
    const val REQUEST_CODE = 1

    const val NOTIFICATION_ID_APPS_UPDATE_WORKER = 1
    const val NOTIFICATION_ID_OTHERS_UPDATE_WORKER = 2

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

    fun createChannelIfNecessary(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                FOREGROUND_SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = FOREGROUND_SERVICE_CHANNEL_DESC
            }
            val notificationManager: NotificationManager = getNotificationManager(context)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationBuilder(context: Context) =
        NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher).setOnlyAlertOnce(true).setSilent(true)

    fun getNotificationManager(context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}