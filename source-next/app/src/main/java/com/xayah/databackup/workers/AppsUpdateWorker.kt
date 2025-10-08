package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.xayah.databackup.R
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.NotificationHelper.NOTIFICATION_ID_APPS_UPDATE_WORKER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsUpdateWorker(private val appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private val mNotificationBuilder = NotificationHelper.getNotificationBuilder(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_APPS_UPDATE_WORKER, mNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_APPS_UPDATE_WORKER, mNotificationBuilder.build())
        }
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.Default) {
            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_apps_basic_info))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Apps basic info
            runCatching {
                val appInfos = RemoteRootService.getInstalledAppInfos()
                DatabaseHelper.appDao.upsertInfo(appInfos)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update app infos.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_apps_storage_info))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Apps storage info
            runCatching {
                val appStorages = RemoteRootService.getInstalledAppStorages()
                DatabaseHelper.appDao.upsertStorage(appStorages)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update app storages.", it)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "AppsUpdateWorker"

        fun buildRequest() = OneTimeWorkRequestBuilder<AppsUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
