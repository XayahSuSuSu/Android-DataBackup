package com.xayah.databackup.ui.activity.blacklist

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.BlackListItem
import kotlinx.coroutines.flow.MutableStateFlow

class BlackListViewModel : ViewModel() {
    val blackList by lazy {
        MutableStateFlow(SnapshotStateList<BlackListItem>())
    }
}
