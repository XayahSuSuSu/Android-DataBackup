package com.xayah.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationUtil {
    private const val ForegroundServiceChannelId = "ForegroundServiceChannel"
    private const val ForegroundServiceChannelName = "ForegroundService"
    private const val ForegroundServiceChannelDesc = "For foreground service"
    private var progressNotificationId = 0

    fun getForegroundNotification(context: Context) = run {
        val pendingIntent: PendingIntent = context.packageManager.getLaunchIntentForPackage(context.packageName).let { intent ->
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        val channel = NotificationChannel(ForegroundServiceChannelId, ForegroundServiceChannelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = ForegroundServiceChannelDesc
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        Notification.Builder(context, ForegroundServiceChannelId).setContentIntent(pendingIntent).build()
    }

    fun getProgressNotificationBuilder(context: Context) = NotificationCompat.Builder(context, ForegroundServiceChannelId).setSmallIcon(R.mipmap.ic_launcher)

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
