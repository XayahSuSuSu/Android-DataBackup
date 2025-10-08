package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CallLog.Calls
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.FiledMap
import com.xayah.databackup.database.entity.FiledMutableMap
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.NotificationHelper.NOTIFICATION_ID_OTHERS_UPDATE_WORKER
import com.xayah.databackup.util.ParcelableHelper.marshall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OthersUpdateWorker(private val appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private var mNotificationBuilder = NotificationHelper.getNotificationBuilder(appContext)
    private val mMoshi: Moshi = Moshi.Builder().build()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_OTHERS_UPDATE_WORKER, mNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_OTHERS_UPDATE_WORKER, mNotificationBuilder.build())
        }
    }

    private fun getAllFields(cursor: Cursor): FiledMap {
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

    override suspend fun doWork(): Result {
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
                            config1 = it.marshall(),
                            config2 = null
                        )
                    } else {
                        networks[it.networkId]?.config2 = it.marshall()
                    }
                }
                DatabaseHelper.networkDao.upsert(networks.values.toList())
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update networks.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.contacts))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Contacts
            runCatching {
                val contacts = mutableListOf<Contact>()
                App.application.contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, null, null, null)?.also { rawContactCursor ->
                    while (rawContactCursor.moveToNext()) {
                        runCatching {
                            val contact = Contact(0, "", "", true)
                            val rawContact = getAllFields(cursor = rawContactCursor)
                            contact.rawContact = mMoshi.adapter<FiledMap>().toJson(rawContact)
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
                                contact.data = mMoshi.adapter<List<FiledMap>>().toJson(data)
                            }
                            contacts.add(contact)
                        }.onFailure { e -> e.printStackTrace() }
                    }
                }
                DatabaseHelper.contactDao.upsert(contacts)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update contacts.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.call_logs))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Call logs
            runCatching {
                val callLogs = mutableListOf<CallLog>()
                App.application.contentResolver.query(Calls.CONTENT_URI, null, null, null, Calls.DEFAULT_SORT_ORDER)?.also { callLogCursor ->
                    while (callLogCursor.moveToNext()) {
                        runCatching {
                            val callLog = CallLog(0, "", true)
                            val call = getAllFields(cursor = callLogCursor)
                            callLog.call = mMoshi.adapter<FiledMap>().toJson(call)
                            callLog.id = call.getOrDefault(Calls._ID, -1L) as Long
                            if (callLog.id == -1L) {
                                throw IllegalStateException("Unexpected id: ${callLog.id}")
                            }
                            callLogs.add(callLog)
                        }.onFailure { e -> e.printStackTrace() }
                    }
                }
                DatabaseHelper.callLogDao.upsert(callLogs)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update call logs.", it)
            }

            mNotificationBuilder.setContentTitle(appContext.getString(R.string.worker_update_other_info))
                .setSubText(appContext.getString(R.string.messages))
                .setProgress(0, 0, true)
                .setOngoing(true)
            setForeground(getForegroundInfo())

            // Messages(SMS)
            runCatching {
                val smsList = mutableListOf<Sms>()
                App.application.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)
                    ?.also { smsCursor ->
                        while (smsCursor.moveToNext()) {
                            runCatching {
                                val sms = Sms(0, "", true)
                                val config = getAllFields(cursor = smsCursor)
                                sms.config = mMoshi.adapter<FiledMap>().toJson(config)
                                sms.id = config.getOrDefault(Telephony.Sms._ID, -1L) as Long
                                if (sms.id == -1L) {
                                    throw IllegalStateException("Unexpected id: ${sms.id}")
                                }
                                smsList.add(sms)
                            }.onFailure { e -> e.printStackTrace() }
                        }
                    }
                DatabaseHelper.messageDao.upsertSms(smsList)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update sms.", it)
            }

            // Messages(MMS)
            runCatching {
                val mmsList = mutableListOf<Mms>()
                App.application.contentResolver.query(Telephony.Mms.CONTENT_URI, null, null, null, Telephony.Mms.DEFAULT_SORT_ORDER)
                    ?.also { pduCursor ->
                        // (part)mid -> (pdu)_id <- (addr)msg_id
                        while (pduCursor.moveToNext()) {
                            runCatching {
                                val mms = Mms(0, "", "", "", true)
                                val pdu = getAllFields(cursor = pduCursor)
                                mms.pdu = mMoshi.adapter<FiledMap>().toJson(pdu)
                                mms.id = pdu.getOrDefault(Telephony.Mms._ID, -1L) as Long
                                if (mms.id == -1L) {
                                    throw IllegalStateException("Unexpected id: ${mms.id}")
                                }

                                val ADDR_CONTENT_URI = Telephony.Mms.CONTENT_URI.buildUpon().appendPath(mms.id.toString()).appendPath("addr").build()
                                App.application.contentResolver.query(
                                    ADDR_CONTENT_URI,
                                    null,
                                    null,
                                    null,
                                    null
                                )?.also { addrCursor ->
                                    val addr = mutableListOf<FiledMap>()
                                    while (addrCursor.moveToNext()) {
                                        addr.add(getAllFields(cursor = addrCursor))
                                    }
                                    mms.addr = mMoshi.adapter<List<FiledMap>>().toJson(addr)
                                }

                                val TABLE_PART = "part"
                                val CONTENT_URI = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, TABLE_PART)
                                App.application.contentResolver.query(
                                    CONTENT_URI,
                                    null,
                                    "${Telephony.Mms.Part.MSG_ID} = ?",
                                    arrayOf(mms.id.toString()),
                                    null
                                )?.also { partCursor ->
                                    val part = mutableListOf<FiledMap>()
                                    while (partCursor.moveToNext()) {
                                        part.add(getAllFields(cursor = partCursor))
                                    }
                                    mms.part = mMoshi.adapter<List<FiledMap>>().toJson(part)
                                }

                                mmsList.add(mms)
                            }.onFailure { e -> e.printStackTrace() }
                        }
                    }
                DatabaseHelper.messageDao.upsertMms(mmsList)
            }.onFailure {
                LogHelper.e(TAG, "doWork", "Failed to update mms.", it)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "OthersUpdateWorker"

        fun buildRequest() = OneTimeWorkRequestBuilder<OthersUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
