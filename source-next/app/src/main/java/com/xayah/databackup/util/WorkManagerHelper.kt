package com.xayah.databackup.util

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.xayah.databackup.App
import com.xayah.databackup.workers.AppsUpdateWorker
import com.xayah.databackup.workers.OthersUpdateWorker

object WorkManagerHelper {
    private const val APPS_UPDATE_WORK_NAME = "apps_update_work"
    private const val OTHERS_UPDATE_WORK_NAME = "others_update_work"

    fun enqueueAppsUpdateWork() {
        WorkManager.getInstance(App.application)
            .enqueueUniqueWork(APPS_UPDATE_WORK_NAME, ExistingWorkPolicy.KEEP, AppsUpdateWorker.buildRequest())
    }

    fun enqueueOthersUpdateWork() {
        WorkManager.getInstance(App.application)
            .enqueueUniqueWork(OTHERS_UPDATE_WORK_NAME, ExistingWorkPolicy.KEEP, OthersUpdateWorker.buildRequest())
    }
}
