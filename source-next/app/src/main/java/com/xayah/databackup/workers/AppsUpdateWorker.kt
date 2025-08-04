package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.ParcelableHelper.marshall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsUpdateWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private var mNotificationBuilder = NotificationHelper.getNotificationBuilder(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationHelper.generateNotificationId(), mNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationHelper.generateNotificationId(), mNotificationBuilder.build())
        }
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        withContext(Dispatchers.Default) {
            // TMP
            val networks = mutableMapOf<Int, Network>()
            RemoteRootService.getPrivilegedConfiguredNetworks().forEach {
                if (networks.contains(it.networkId).not()) {
                    networks[it.networkId] = Network(
                        id = it.networkId,
                        ssid = it.SSID,
                        preSharedKey = it.preSharedKey,
                        selected = true,
                        config1 = it.marshall(),
                        config2 = null
                    )
                } else {
                    networks[it.networkId]?.config2 = it.marshall()
                }
            }
            runCatching {
                DatabaseHelper.networkDao.upsert(networks.values.toList())
            }.onFailure {
                LogHelper.e(TAG, "Failed to update networks.", it)
            }




            val appInfos = RemoteRootService.getInstalledAppInfos()
            runCatching {
                DatabaseHelper.appDao.upsertInfo(appInfos)
            }.onFailure {
                LogHelper.e(TAG, "Failed to update app infos.", it)
            }
            val appStorages = RemoteRootService.getInstalledAppStorages()
            runCatching {
                DatabaseHelper.appDao.upsertStorage(appStorages)
            }.onFailure {
                LogHelper.e(TAG, "Failed to update app storages.", it)
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
