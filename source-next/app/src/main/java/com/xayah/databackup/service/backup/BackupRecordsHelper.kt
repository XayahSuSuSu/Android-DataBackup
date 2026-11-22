package com.xayah.databackup.service.backup

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog.Calls
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.provider.Telephony
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.MessageConstant.MMS_ADDR_PATH
import com.xayah.databackup.database.entity.MessageConstant.MMS_PART_PATH
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms

internal class BackupRecordsHelper(private val mResolver: ContentResolver) {
    private val mMoshi = Moshi.Builder().build()
    private val mRowAdapter = mMoshi.adapter<FieldMap>()
    private val mRowsAdapter = mMoshi.adapter<List<FieldMap>>()

    private fun readRows(uri: Uri, sort: String? = null, selection: String? = null, args: Array<String>? = null): List<FieldMap> =
        checkNotNull(mResolver.query(uri, null, selection, args, sort)) { "Record query failed" }.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(buildMap {
                        cursor.columnNames.forEachIndexed { index, column ->
                            when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_INTEGER -> put(column, cursor.getLong(index))
                                Cursor.FIELD_TYPE_STRING -> put(column, cursor.getString(index))
                            }
                        }
                    })
                }
            }
        }

    fun readContacts(): List<Contact> = readRows(RawContacts.CONTENT_URI).map { row ->
        val id = row.getValue(RawContacts._ID) as Long
        val data = readRows(Data.CONTENT_URI, selection = "${Data.RAW_CONTACT_ID} = ?", args = arrayOf(id.toString()))
        Contact(id, mRowAdapter.toJson(row), mRowsAdapter.toJson(data), true)
    }

    fun readCallLogs(): List<CallLog> = readRows(Calls.CONTENT_URI, Calls.DEFAULT_SORT_ORDER).map { row ->
        CallLog(row.getValue(Calls._ID) as Long, mRowAdapter.toJson(row), true)
    }

    fun readSms(): List<Sms> = readRows(Telephony.Sms.CONTENT_URI, Telephony.Sms.DEFAULT_SORT_ORDER).map { row ->
        Sms(row.getValue(Telephony.Sms._ID) as Long, mRowAdapter.toJson(row), true)
    }

    fun readMms(): List<Mms> = readRows(Telephony.Mms.CONTENT_URI, Telephony.Mms.DEFAULT_SORT_ORDER).map { row ->
        val id = row.getValue(Telephony.Mms._ID) as Long
        val addresses = readRows(Telephony.Mms.CONTENT_URI.buildUpon().appendPath(id.toString()).appendPath(MMS_ADDR_PATH).build())
        val parts = readRows(
            Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, MMS_PART_PATH),
            selection = "${Telephony.Mms.Part.MSG_ID} = ?",
            args = arrayOf(id.toString()),
        )
        Mms(id, mRowAdapter.toJson(row), mRowsAdapter.toJson(addresses), mRowsAdapter.toJson(parts), true)
    }
}
