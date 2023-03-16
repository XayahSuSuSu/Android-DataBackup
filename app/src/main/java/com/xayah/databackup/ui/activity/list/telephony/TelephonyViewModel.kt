package com.xayah.databackup.ui.activity.list.telephony

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.CallLogItem
import com.xayah.databackup.data.ContactItem
import com.xayah.databackup.data.MmsItem
import com.xayah.databackup.data.SmsItem
import kotlinx.coroutines.flow.MutableStateFlow

class TelephonyViewModel : ViewModel() {
    val tabRowState: MutableState<Int> = mutableStateOf(0)
    val isRoleHolderDialogOpen: MutableState<Boolean> = mutableStateOf(false)

    val smsList by lazy {
        MutableStateFlow(SnapshotStateList<SmsItem>())
    }
    val mmsList by lazy {
        MutableStateFlow(SnapshotStateList<MmsItem>())
    }
    val contactsList by lazy {
        MutableStateFlow(SnapshotStateList<ContactItem>())
    }
    val callLogList by lazy {
        MutableStateFlow(SnapshotStateList<CallLogItem>())
    }
}
