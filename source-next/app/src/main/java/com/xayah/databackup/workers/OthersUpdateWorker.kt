package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.wifi.WifiConfiguration
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.R
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.NotificationHelper.NOTIFICATION_ID_OTHERS_UPDATE_WORKER
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OthersUpdateWorker(private val appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private var mNotificationBuilder = NotificationHelper.getNotificationBuilder(appContext)
    private val mMoshi: Moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_OTHERS_UPDATE_WORKER, mNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_OTHERS_UPDATE_WORKER, mNotificationBuilder.build())
        }
    }

    override suspend fun doWork(): Result {
        var hasFailures = false
        withContext(Dispatchers.Default) {
            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.networks))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Networks
            runCatching {
                val networks = mutableMapOf<Int, Network>()
                RemoteRootService.getPrivilegedConfiguredNetworks().forEach {
                    if (networks.contains(it.networkId).not()) {
                        networks[it.networkId] = Network(
                            id = it.networkId,
                            ssid = it.SSID,
                            preSharedKey = it.preSharedKey,
                            selected = true,
                            config1 = mMoshi.adapter<WifiConfiguration>().toJson(it),
                            config2 = null
                        )
                    } else {
                        networks[it.networkId]?.config2 = mMoshi.adapter<WifiConfiguration>().toJson(it)
                    }
                }
                DatabaseHelper.networkDao.upsert(networks.values.toList())
            }.onFailure {
                if (it is CancellationException) {
                    throw it
                }
                hasFailures = true
                LogHelper.e(TAG, "doWork", "Failed to update networks.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.contacts))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Contacts
            runCatching {
                val contacts = RemoteRootService.readContacts()
                DatabaseHelper.contactDao.upsert(contacts)
            }.onFailure {
                if (it is CancellationException) {
                    throw it
                }
                hasFailures = true
                LogHelper.e(TAG, "doWork", "Failed to update contacts.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.call_logs))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Call logs
            runCatching {
                val callLogs = RemoteRootService.readCallLogs()
                DatabaseHelper.callLogDao.upsert(callLogs)
            }.onFailure {
                if (it is CancellationException) {
                    throw it
                }
                hasFailures = true
                LogHelper.e(TAG, "doWork", "Failed to update call logs.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.messages))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Messages(SMS)
            runCatching {
                val smsList = RemoteRootService.readSms()
                DatabaseHelper.messageDao.upsertSms(smsList)
            }.onFailure {
                if (it is CancellationException) {
                    throw it
                }
                hasFailures = true
                LogHelper.e(TAG, "doWork", "Failed to update sms.", it)
            }

            // Messages(MMS)
            runCatching {
                val mmsList = RemoteRootService.readMms()
                DatabaseHelper.messageDao.upsertMms(mmsList)
            }.onFailure {
                if (it is CancellationException) {
                    throw it
                }
                hasFailures = true
                LogHelper.e(TAG, "doWork", "Failed to update mms.", it)
            }
        }
        return if (hasFailures) {
            Result.failure()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val TAG = "OthersUpdateWorker"

        fun buildRequest() = OneTimeWorkRequestBuilder<OthersUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
