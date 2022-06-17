package com.xayah.databackup.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.xayah.databackup.R

class Notification(val id: String, val name: String) {
    private val notificationChannel =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    fun initialize(context: Context) {
        notificationBuilder =
            NotificationCompat.Builder(context, id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("DataBackup Notification")
                .setProgress(100, 0, false)
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.createNotificationChannel(notificationChannel)
    }

    fun update(clearable: Boolean, callback: (builder: NotificationCompat.Builder?) -> Unit) {
        callback(notificationBuilder)
        notificationManager?.notify(1, notificationBuilder?.build()?.apply {
            if (!clearable)
                flags = flags or Notification.FLAG_NO_CLEAR
        })
    }

}