package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.filterApp
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data object UiState

open class BackupPreviewViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val appsSelected = combine(
        DatabaseHelper.appDao.loadFlowApps(),
        App.application.readInt(FilterBackupUser),
        App.application.readBoolean(FiltersUserAppsBackup),
        App.application.readBoolean(FiltersSystemAppsBackup),
    ) { apps, userId, filterUserApps, filterSystemApps ->
        val list = apps.filterApp(userId, filterUserApps, filterSystemApps)
        list.count { it.option.apk || it.option.internalData || it.option.externalData || it.option.obbAndMedia } to list.size
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0 to 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val networksSelected = DatabaseHelper.networkDao.loadFlowNetworks().map { list ->
        list.count { it.selected } to list.size
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0 to 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val contactsSelected = DatabaseHelper.contactDao.loadFlowContacts().map { list ->
        list.count { it.selected } to list.size
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0 to 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val callLogsSelected = DatabaseHelper.callLogDao.loadFlowCallLogs().map { list ->
        list.count { it.selected } to list.size
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0 to 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val messagesSelected = combine(DatabaseHelper.messageDao.loadFlowSms(), DatabaseHelper.messageDao.loadFlowMms()) { smsList, mmsList ->
        smsList.count { it.selected } + mmsList.count { it.selected } to smsList.size + mmsList.size
    }.stateIn(
        scope = viewModelScope,
        initialValue = 0 to 0,
        started = SharingStarted.WhileSubscribed(5_000),
    )
}
