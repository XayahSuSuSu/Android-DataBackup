package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.os.Build
import android.provider.ContactsContract
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.FiledMap
import com.xayah.databackup.database.entity.FiledMutableMap
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

            // TMP
            fun getAllFields(cursor: Cursor): FiledMap {
                val map: FiledMutableMap = mutableMapOf()
                cursor.columnNames.forEach { column ->
                    runCatching {
                        val index = cursor.getColumnIndex(column)
                        if (index != -1) {
                            val type = cursor.getType(index)
                            when (type) {
                                Cursor.FIELD_TYPE_INTEGER -> {
                                    map.put(column, cursor.getLong(index))
                                }

                                Cursor.FIELD_TYPE_STRING -> {
                                    map.put(column, cursor.getString(index))
                                }

                                Cursor.FIELD_TYPE_NULL -> {
                                    // Do nothing
                                }

                                else -> {
                                    throw IllegalStateException("Unexpected type of the field: $column: $type")
                                }
                            }
                        }
                    }.onFailure { e -> e.printStackTrace() }
                }
                return map
            }

            val moshi: Moshi = Moshi.Builder().build()
            val contacts = mutableListOf<Contact>()
            App.application.contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, null, null, null)?.also { rawContactCursor ->
                while (rawContactCursor.moveToNext()) {
                    runCatching {
                        val contact = Contact(0, "", "", true)
                        val rawContact = getAllFields(cursor = rawContactCursor)
                        contact.rawContact = moshi.adapter<FiledMap>().toJson(rawContact)
                        contact.id = rawContact.getOrDefault(ContactsContract.RawContacts._ID, -1L) as Long
                        if (contact.id == -1L) {
                            throw IllegalStateException("Unexpected id: ${contact.id}")
                        }
                        App.application.contentResolver.query(
                            ContactsContract.Data.CONTENT_URI,
                            null,
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ?",
                            arrayOf(contact.id.toString()),
                            null
                        )?.also { dataCursor ->
                            val data = mutableListOf<FiledMap>()
                            while (dataCursor.moveToNext()) {
                                data.add(getAllFields(cursor = dataCursor))
                            }
                            contact.data = moshi.adapter<List<FiledMap>>().toJson(data)
                        }
                        contacts.add(contact)
                    }.onFailure { e -> e.printStackTrace() }
                }
            }
            runCatching {
                DatabaseHelper.contactDao.upsert(contacts)
            }.onFailure {
                LogHelper.e(TAG, "Failed to update contacts.", it)
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
