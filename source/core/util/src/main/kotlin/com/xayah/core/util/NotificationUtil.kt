package com.xayah.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context

object NotificationUtil {
    private const val ForegroundServiceChannelId = "ForegroundServiceChannel"
    private const val ForegroundServiceChannelName = "ForegroundService"
    private const val ForegroundServiceChannelDesc = "For foreground service"

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
}
