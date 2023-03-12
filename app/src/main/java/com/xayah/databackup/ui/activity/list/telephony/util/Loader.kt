package com.xayah.databackup.ui.activity.list.telephony.util

import android.content.Context
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.util.Command

class Loader {
    companion object {
        suspend fun smsBackupList(viewModel: TelephonyViewModel, context: Context) {
            viewModel.smsList.value.addAll(Command.getSmsList(context = context, readOnly = false))
        }

        suspend fun smsRestoreList(viewModel: TelephonyViewModel, context: Context) {
            viewModel.smsList.value.addAll(Command.getSmsList(context = context, readOnly = true))
        }

        suspend fun mmsBackupList(viewModel: TelephonyViewModel, context: Context) {
            viewModel.mmsList.value.addAll(Command.getMmsList(context = context, readOnly = false))
        }

        suspend fun mmsRestoreList(viewModel: TelephonyViewModel, context: Context) {
            viewModel.mmsList.value.addAll(Command.getMmsList(context = context, readOnly = true))
        }
    }
}