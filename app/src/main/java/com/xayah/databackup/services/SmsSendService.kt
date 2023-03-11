package com.xayah.databackup.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SmsSendService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}