package com.xayah.core.util

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.EXTRA_CHANNEL_ID
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat


object NotificationUtil {
    private const val ForegroundServiceChannelId = "ForegroundServiceChannel"
    private const val ForegroundServiceChannelName = "ForegroundService"
    private const val ForegroundServiceChannelDesc = "For foreground service"
    private var progressNotificationId = 0

    fun checkPermission(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun requestPermissions(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
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
        }
    }

    fun getForegroundNotification(context: Context) = run {
        val pendingIntent: PendingIntent = context.packageManager.getLaunchIntentForPackage(context.packageName).let { intent ->
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        val channel = NotificationChannel(ForegroundServiceChannelId, ForegroundServiceChannelName, NotificationManager.IMPORTANCE_LOW).apply {
            description = ForegroundServiceChannelDesc
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        Notification.Builder(context, ForegroundServiceChannelId).setContentIntent(pendingIntent).build()
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
}
