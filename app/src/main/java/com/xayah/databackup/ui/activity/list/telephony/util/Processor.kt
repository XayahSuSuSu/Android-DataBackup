package com.xayah.databackup.ui.activity.list.telephony.util

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import android.widget.Toast
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.Path

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
    }
}