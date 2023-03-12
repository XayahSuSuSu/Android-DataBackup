package com.xayah.databackup.ui.activity.list.telephony.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.widget.Toast
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.Path
import com.xayah.librootservice.RootService
import java.nio.file.Paths
import kotlin.io.path.name

class Processor {
    companion object {
        suspend fun smsBackup(viewModel: TelephonyViewModel, context: Context) {
            val selectedList =
                viewModel.smsList.value.filter { it.isSelected.value || it.isInLocal.value }
                    .toMutableList()
            selectedList.forEach {
                it.isInLocal.value = true
                it.isSelected.value = false
            }
            GsonUtil.getInstance()
                .saveSmsListToFile(Path.getSmsListPath(), selectedList)
                .apply {
                    Toast.makeText(
                        context,
                        context.getString(if (this) R.string.succeed else R.string.failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        fun smsRestore(viewModel: TelephonyViewModel, context: Context) {
            for (i in viewModel.smsList.value) {
                if (i.isSelected.value) {
                    val data = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, i.address)
                        put(Telephony.Sms.BODY, i.body)
                        put(Telephony.Sms.CREATOR, i.creator)
                        put(Telephony.Sms.DATE, i.date)
                        put(Telephony.Sms.DATE_SENT, i.dateSent)
                        put(Telephony.Sms.ERROR_CODE, i.errorCode)
                        put(Telephony.Sms.LOCKED, i.locked)
                        put(Telephony.Sms.PERSON, i.person)
                        put(Telephony.Sms.PROTOCOL, i.protocol)
                        put(Telephony.Sms.READ, i.read)
                        put(
                            Telephony.Sms.REPLY_PATH_PRESENT,
                            i.replyPathPresent
                        )
                        put(Telephony.Sms.SEEN, i.seen)
                        put(Telephony.Sms.SERVICE_CENTER, i.serviceCenter)
                        put(Telephony.Sms.STATUS, i.status)
                        put(Telephony.Sms.SUBJECT, i.subject)
                        put(Telephony.Sms.SUBSCRIPTION_ID, i.subscriptionId)
                        put(Telephony.Sms.TYPE, i.type)
                    }
                    context.contentResolver.insert(
                        Telephony.Sms.CONTENT_URI,
                        data
                    )
                    i.isSelected.value = false
                    i.isOnThisDevice.value = true
                }
            }
        }

        suspend fun mmsBackup(viewModel: TelephonyViewModel, context: Context) {
            val selectedList =
                viewModel.mmsList.value.filter { it.isSelected.value || it.isInLocal.value }
                    .toMutableList()
            selectedList.forEach {
                it.isInLocal.value = true
                it.isSelected.value = false
            }
            for (i in selectedList) {
                for (j in i.part) {
                    if (j._data.isNotEmpty()) {
                        val targetPath = "${Path.getMmsDataPath()}/${Paths.get(j._data).name}"
                        RootService.getInstance().copyTo(j._data, targetPath, true)
                    }
                }
            }
            GsonUtil.getInstance()
                .saveMmsListToFile(Path.getMmsListPath(), selectedList)
                .apply {
                    Toast.makeText(
                        context,
                        context.getString(if (this) R.string.succeed else R.string.failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        fun mmsRestore(viewModel: TelephonyViewModel, context: Context) {
            for (i in viewModel.mmsList.value) {
                if (i.isSelected.value) {
                    // Create tmp sms to get thread_id
                    val tmpData = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, i.address)
                    }
                    val tmpSmsUri = context.contentResolver.insert(
                        Telephony.Sms.CONTENT_URI,
                        tmpData
                    )

                    if (tmpSmsUri != null) {
                        // Get the thread_id by tmp sms
                        context.contentResolver.query(
                            tmpSmsUri,
                            null,
                            null,
                            null,
                            Telephony.Sms.DEFAULT_SORT_ORDER
                        )?.apply {
                            while (moveToNext()) {
                                try {
                                    val threadId =
                                        getString(getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                                            ?: ""
                                    val pduData = ContentValues().apply {
                                        put(Telephony.Mms.CONTENT_CLASS, i.pdu.contentClass)
                                        put(Telephony.Mms.CONTENT_LOCATION, i.pdu.contentLocation)
                                        put(Telephony.Mms.CONTENT_TYPE, i.pdu.contentType)
                                        put(Telephony.Mms.DATE, i.pdu.date)
                                        put(Telephony.Mms.DATE_SENT, i.pdu.dateSent)
                                        put(Telephony.Mms.DELIVERY_REPORT, i.pdu.deliveryReport)
                                        put(Telephony.Mms.DELIVERY_TIME, i.pdu.deliveryTime)
                                        put(Telephony.Mms.EXPIRY, i.pdu.expiry)
                                        put(Telephony.Mms.LOCKED, i.pdu.locked)
                                        put(Telephony.Mms.MESSAGE_BOX, i.pdu.messageBox)
                                        put(Telephony.Mms.MESSAGE_CLASS, i.pdu.messageClass)
                                        put(Telephony.Mms.MESSAGE_ID, i.pdu.messageId)
                                        put(Telephony.Mms.MESSAGE_SIZE, i.pdu.messageSize)
                                        put(Telephony.Mms.MESSAGE_TYPE, i.pdu.messageType)
                                        put(Telephony.Mms.MMS_VERSION, i.pdu.mmsVersion)
                                        put(Telephony.Mms.PRIORITY, i.pdu.priority)
                                        put(Telephony.Mms.READ, i.pdu.read)
                                        put(Telephony.Mms.READ_REPORT, i.pdu.readReport)
                                        put(Telephony.Mms.READ_STATUS, i.pdu.readStatus)
                                        put(Telephony.Mms.REPORT_ALLOWED, i.pdu.reportAllowed)
                                        put(Telephony.Mms.RESPONSE_STATUS, i.pdu.responseStatus)
                                        put(Telephony.Mms.RESPONSE_TEXT, i.pdu.responseText)
                                        put(Telephony.Mms.RETRIEVE_STATUS, i.pdu.retrieveStatus)
                                        put(Telephony.Mms.RETRIEVE_TEXT, i.pdu.retrieveText)
                                        put(
                                            Telephony.Mms.RETRIEVE_TEXT_CHARSET,
                                            i.pdu.retrieveTextCharset
                                        )
                                        put(Telephony.Mms.SEEN, i.pdu.seen)
                                        put(Telephony.Mms.STATUS, i.pdu.status)
                                        put(Telephony.Mms.SUBJECT, i.pdu.subject)
                                        put(Telephony.Mms.SUBJECT_CHARSET, i.pdu.subjectCharset)
                                        put(Telephony.Mms.SUBSCRIPTION_ID, i.pdu.subscriptionId)
                                        put(Telephony.Mms.TEXT_ONLY, i.pdu.textOnly)
                                        put(Telephony.Mms.THREAD_ID, threadId)
                                        put(Telephony.Mms.TRANSACTION_ID, i.pdu.transactionId)
                                    }
                                    val pduUri = context.contentResolver.insert(
                                        Telephony.Mms.CONTENT_URI,
                                        pduData
                                    )

                                    if (pduUri != null) {
                                        context.contentResolver.query(
                                            pduUri,
                                            null,
                                            null,
                                            null,
                                            Telephony.Mms.DEFAULT_SORT_ORDER
                                        )?.apply {
                                            while (moveToNext()) {
                                                try {
                                                    val _id =
                                                        getString(getColumnIndexOrThrow(Telephony.Mms._ID))
                                                            ?: ""
                                                    // Restore addr table
                                                    for (j in i.addr) {
                                                        val addrData = ContentValues().apply {
                                                            put(
                                                                Telephony.Mms.Addr.ADDRESS,
                                                                j.address
                                                            )
                                                            put(
                                                                Telephony.Mms.Addr.CHARSET,
                                                                j.charset
                                                            )
                                                            put(
                                                                Telephony.Mms.Addr.CONTACT_ID,
                                                                j.contactId
                                                            )
                                                            put(Telephony.Mms.Addr.TYPE, j.type)
                                                        }
                                                        context.contentResolver.insert(
                                                            Uri.parse("content://mms/$_id/addr"),
                                                            addrData
                                                        )
                                                    }
                                                    // Restore part table
                                                    for (j in i.part) {
                                                        val partData = ContentValues().apply {
                                                            put(
                                                                Telephony.Mms.Part.CHARSET,
                                                                j.charset
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CONTENT_DISPOSITION,
                                                                j.contentDisposition
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CONTENT_ID,
                                                                j.contentId
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CONTENT_LOCATION,
                                                                j.contentLocation
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CONTENT_TYPE,
                                                                j.contentType
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CT_START,
                                                                j.ctStart
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.CT_TYPE,
                                                                j.ctType
                                                            )
                                                            put(
                                                                Telephony.Mms.Part.FILENAME,
                                                                j.filename
                                                            )
                                                            put(Telephony.Mms.Part.NAME, j.name)
                                                            put(Telephony.Mms.Part.SEQ, j.seq)
                                                            put(Telephony.Mms.Part.TEXT, j.text)
                                                            /**
                                                             * Here we can not put _DATA, since we couldn't pass the test
                                                             *
                                                             * https://cs.android.com/android/platform/superproject/+/master:packages/providers/TelephonyProvider/src/com/android/providers/telephony/MmsProvider.java;l=499;drc=c79808b14aa3e954e54261bc5b7a23420b6b3d27
                                                             *
                                                             * put(Telephony.Mms.Part._DATA, j._data)
                                                             */
                                                        }
                                                        val partUri =
                                                            context.contentResolver.insert(
                                                                Uri.parse("content://mms/$_id/part"),
                                                                partData
                                                            )
                                                        if (partUri != null) {
                                                            context.contentResolver.query(
                                                                partUri,
                                                                null,
                                                                null,
                                                                null,
                                                                null
                                                            )?.apply {
                                                                while (moveToNext()) {
                                                                    try {
                                                                        val _data = getString(
                                                                            getColumnIndexOrThrow(
                                                                                Telephony.Mms.Part._DATA
                                                                            )
                                                                        ) ?: ""
                                                                        val path =
                                                                            "${Path.getMmsDataPath()}/${
                                                                                Paths.get(
                                                                                    j._data
                                                                                ).name
                                                                            }"
                                                                        RootService.getInstance()
                                                                            .copyTo(
                                                                                path,
                                                                                _data,
                                                                                true
                                                                            )
                                                                    } catch (_: Exception) {

                                                                    }
                                                                }
                                                                close()
                                                            }
                                                        }
                                                    }
                                                    i.isSelected.value = false
                                                    i.isOnThisDevice.value = true
                                                } catch (_: Exception) {
                                                }
                                            }
                                            close()
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                            close()
                        }

                        // Delete the tmp sms
                        context.contentResolver.delete(
                            tmpSmsUri,
                            null,
                            null
                        )
                    }
                }
            }
        }
    }
}