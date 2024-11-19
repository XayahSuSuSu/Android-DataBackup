package com.xayah.core.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.EXTRA_CHANNEL_ID
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo


object NotificationUtil {
    private const val ForegroundServiceChannelId = "ForegroundServiceChannel"
    private const val ForegroundServiceChannelName = "ForegroundService"
    private const val ForegroundServiceChannelDesc = "For foreground service"
    private var progressNotificationId = 0

    fun checkPermission(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun requestPermissions(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context.getActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val intent = Intent()
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.grant_ntfy_perm_manually), Toast.LENGTH_SHORT).show()
            }
        } else {
            runCatching {
                val intent = Intent().apply {
                    setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setData(Uri.parse("package:${context.packageName}"))
                }
                context.startActivity(intent)
            }
        }
    }

    fun getForegroundNotification(context: Context) = run {
        val pendingIntent: PendingIntent = context.packageManager.getLaunchIntentForPackage(context.packageName).let { intent ->
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        createChannelIfNecessary(context)
        NotificationCompat.Builder(context, ForegroundServiceChannelId).setContentIntent(pendingIntent).build()
    }

    fun getProgressNotificationBuilder(context: Context) =
        NotificationCompat.Builder(context, ForegroundServiceChannelId).setSmallIcon(R.mipmap.ic_launcher)

    fun notify(
        context: Context,
        builder: NotificationCompat.Builder,
        title: String,
        content: String,
        max: Int = 0,
        progress: Int = 0,
        indeterminate: Boolean = false,
        ongoing: Boolean = true,
    ) {
        builder.setContentTitle(title).setContentText(content).setProgress(max, progress, indeterminate).setOngoing(ongoing)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(progressNotificationId, builder.build())
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(progressNotificationId)
    }


    private fun createChannelIfNecessary(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ForegroundServiceChannelId,
                ForegroundServiceChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ForegroundServiceChannelDesc
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundInfo(
        context: Context,
        builder: NotificationCompat.Builder,
        title: String,
        content: String,
        max: Int = 0,
        progress: Int = 0,
        indeterminate: Boolean = false,
        ongoing: Boolean = true,
    ): ForegroundInfo {
        createChannelIfNecessary(context)
        val notification = builder.setContentTitle(title).setContentText(content).setProgress(max, progress, indeterminate).setOngoing(ongoing)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(progressNotificationId, notification.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(progressNotificationId, notification.build())
        }
    }

    fun createForegroundInfo(
        context: Context,
        builder: NotificationCompat.Builder,
        title: String,
        content: String,
        ongoing: Boolean = true,
    ): ForegroundInfo {
        createChannelIfNecessary(context)
        val notification = builder.setContentTitle(title).setContentText(content).setOngoing(ongoing)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(progressNotificationId, notification.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(progressNotificationId, notification.build())
        }
    }
}
