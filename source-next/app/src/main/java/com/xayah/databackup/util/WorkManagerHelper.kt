package com.xayah.databackup.util

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.xayah.databackup.App
import com.xayah.databackup.workers.AppsUpdateWorker

object WorkManagerHelper {
    private const val APPS_UPDATE_WORK_NAME = "apps_update_work"

    fun enqueueAppsUpdateWork() {
        WorkManager.getInstance(App.application)
            .enqueueUniqueWork(APPS_UPDATE_WORK_NAME, ExistingWorkPolicy.KEEP, AppsUpdateWorker.buildRequest())
    }
}
