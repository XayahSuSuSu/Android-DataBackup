package com.xayah.databackup.ui.activity.list.telephony.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.makeActionToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.io.path.name

class Processor {
    companion object {
        suspend fun smsBackup(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
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
                        withContext(Dispatchers.Main) {
                            context.makeActionToast(this@apply)
                        }
                    }
            }
        }

        suspend fun smsRestore(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
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
        }

        suspend fun mmsBackup(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
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
                        withContext(Dispatchers.Main) {
                            context.makeActionToast(this@apply)
                        }
                    }
            }
        }

        suspend fun mmsRestore(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
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

        suspend fun contactsBackup(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
                val selectedList =
                    viewModel.contactsList.value.filter { it.isSelected.value || it.isInLocal.value }
                        .toMutableList()
                selectedList.forEach {
                    it.isInLocal.value = true
                    it.isSelected.value = false
                }
                GsonUtil.getInstance()
                    .saveContactListToFile(Path.getContactListPath(), selectedList)
                    .apply {
                        withContext(Dispatchers.Main) {
                            context.makeActionToast(this@apply)
                        }
                    }
            }
        }

        suspend fun contactsRestore(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
                for (i in viewModel.contactsList.value) {
                    if (i.isSelected.value) {
                        // Create raw_contacts first
                        val rawContactData = ContentValues().apply {
                            put(ContactsContract.RawContacts.AGGREGATION_MODE, i.rawContact.aggregationMode)
                            put(ContactsContract.RawContacts.DELETED, i.rawContact.deleted)
                            put(ContactsContract.RawContacts.CUSTOM_RINGTONE, i.rawContact.customRingtone)
                            put(ContactsContract.RawContacts.DISPLAY_NAME_ALTERNATIVE, i.rawContact.displayNameAlternative)
                            put(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY, i.rawContact.displayNamePrimary)
                            put(ContactsContract.RawContacts.DISPLAY_NAME_SOURCE, i.rawContact.displayNameSource)
                            put(ContactsContract.RawContacts.PHONETIC_NAME, i.rawContact.phoneticName)
                            put(ContactsContract.RawContacts.PHONETIC_NAME_STYLE, i.rawContact.phoneticNameStyle)
                            put(ContactsContract.RawContacts.SORT_KEY_ALTERNATIVE, i.rawContact.sortKeyAlternative)
                            put(ContactsContract.RawContacts.SORT_KEY_PRIMARY, i.rawContact.sortKeyPrimary)
                            put(ContactsContract.RawContacts.DIRTY, i.rawContact.dirty)
                            put(ContactsContract.RawContacts.VERSION, i.rawContact.version)
                        }
                        val rawContactSmsUri = context.contentResolver.insert(
                            ContactsContract.RawContacts.CONTENT_URI,
                            rawContactData
                        )

                        if (rawContactSmsUri != null) {
                            // Get the contact_id
                            context.contentResolver.query(
                                rawContactSmsUri,
                                null,
                                null,
                                null,
                                null
                            )?.apply {
                                while (moveToNext()) {
                                    try {
                                        val _id = getLong(getColumnIndexOrThrow(ContactsContract.RawContacts._ID))

                                        // Restore data table
                                        for (j in i.data) {
                                            val data = ContentValues().apply {
                                                put(ContactsContract.Contacts.Data.DATA1, j.data1)
                                                put(ContactsContract.Contacts.Data.DATA2, j.data2)
                                                put(ContactsContract.Contacts.Data.DATA3, j.data3)
                                                put(ContactsContract.Contacts.Data.DATA4, j.data4)
                                                put(ContactsContract.Contacts.Data.DATA5, j.data5)
                                                put(ContactsContract.Contacts.Data.DATA6, j.data6)
                                                put(ContactsContract.Contacts.Data.DATA7, j.data7)
                                                put(ContactsContract.Contacts.Data.DATA8, j.data8)
                                                put(ContactsContract.Contacts.Data.DATA9, j.data9)
                                                put(ContactsContract.Contacts.Data.DATA10, j.data10)
                                                put(ContactsContract.Contacts.Data.DATA11, j.data11)
                                                put(ContactsContract.Contacts.Data.DATA12, j.data12)
                                                put(ContactsContract.Contacts.Data.DATA13, j.data13)
                                                put(ContactsContract.Contacts.Data.DATA14, j.data14)
                                                put(ContactsContract.Contacts.Data.DATA15, j.data15)
                                                put(ContactsContract.Contacts.Data.DATA_VERSION, j.dataVersion)
                                                put(ContactsContract.Contacts.Data.IS_PRIMARY, j.isPrimary)
                                                put(ContactsContract.Contacts.Data.IS_SUPER_PRIMARY, j.isSuperPrimary)
                                                put(ContactsContract.Contacts.Data.MIMETYPE, j.mimetype)
                                                put(ContactsContract.Contacts.Data.PREFERRED_PHONE_ACCOUNT_COMPONENT_NAME, j.preferredPhoneAccountComponentName)
                                                put(ContactsContract.Contacts.Data.PREFERRED_PHONE_ACCOUNT_ID, j.preferredPhoneAccountId)
                                                put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, _id)
                                                put(ContactsContract.Contacts.Data.SYNC1, j.sync1)
                                                put(ContactsContract.Contacts.Data.SYNC2, j.sync2)
                                                put(ContactsContract.Contacts.Data.SYNC3, j.sync3)
                                                put(ContactsContract.Contacts.Data.SYNC4, j.sync4)
                                            }
                                            context.contentResolver.insert(
                                                ContactsContract.Data.CONTENT_URI,
                                                data
                                            )
                                        }
                                        i.isSelected.value = false
                                        i.isOnThisDevice.value = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                close()
                            }
                        }
                    }
                }
            }
        }

        suspend fun callLogBackup(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
                val selectedList =
                    viewModel.callLogList.value.filter { it.isSelected.value || it.isInLocal.value }
                        .toMutableList()
                selectedList.forEach {
                    it.isInLocal.value = true
                    it.isSelected.value = false
                }
                GsonUtil.getInstance()
                    .saveCallLogListToFile(Path.getCallLogListPath(), selectedList)
                    .apply {
                        withContext(Dispatchers.Main) {
                            context.makeActionToast(this@apply)
                        }
                    }
            }
        }

        suspend fun callLogRestore(viewModel: TelephonyViewModel, context: Context) {
            withContext(Dispatchers.IO) {
                for (i in viewModel.callLogList.value) {
                    if (i.isSelected.value) {
                        val data = ContentValues().apply {
                            put(CallLog.Calls.CACHED_FORMATTED_NUMBER, i.cachedFormattedNumber)
                            put(CallLog.Calls.CACHED_LOOKUP_URI, i.cachedLookupUri)
                            put(CallLog.Calls.CACHED_MATCHED_NUMBER, i.cachedMatchedNumber)
                            put(CallLog.Calls.CACHED_NAME, i.cachedName)
                            put(CallLog.Calls.CACHED_NORMALIZED_NUMBER, i.cachedNormalizedNumber)
                            put(CallLog.Calls.CACHED_NUMBER_LABEL, i.cachedNumberLabel)
                            put(CallLog.Calls.CACHED_NUMBER_TYPE, i.cachedNumberType)
                            put(CallLog.Calls.CACHED_PHOTO_ID, i.cachedPhotoId)
                            put(CallLog.Calls.CACHED_PHOTO_URI, i.cachedPhotoUri)
                            put(CallLog.Calls.COUNTRY_ISO, i.countryIso)
                            put(CallLog.Calls.DATA_USAGE, i.dataUsage)
                            put(CallLog.Calls.DATE, i.date)
                            put(CallLog.Calls.DURATION, i.duration)
                            put(CallLog.Calls.FEATURES, i.features)
                            put(CallLog.Calls.GEOCODED_LOCATION, i.geocodedLocation)
                            put(CallLog.Calls.IS_READ, i.isRead)
                            put(CallLog.Calls.LAST_MODIFIED, i.lastModified)
                            put(CallLog.Calls.NEW, i.new)
                            put(CallLog.Calls.NUMBER, i.number)
                            put(CallLog.Calls.NUMBER_PRESENTATION, i.numberPresentation)
                            put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, i.phoneAccountComponentName)
                            put(CallLog.Calls.PHONE_ACCOUNT_ID, i.phoneAccountId)
                            put(CallLog.Calls.POST_DIAL_DIGITS, i.postDialDigits)
                            put(CallLog.Calls.TRANSCRIPTION, i.transcription)
                            put(CallLog.Calls.TYPE, i.type)
                            put(CallLog.Calls.VIA_NUMBER, i.viaNumber)
                        }
                        context.contentResolver.insert(
                            CallLog.Calls.CONTENT_URI,
                            data
                        )
                        i.isSelected.value = false
                        i.isOnThisDevice.value = true
                    }
                }
            }
        }
    }
}