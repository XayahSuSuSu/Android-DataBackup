package com.xayah.databackup.feature.backup

import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data object BackupProcessUiState

open class BackupProcessViewModel(private val backupProcessRepo: BackupProcessRepository) : BaseViewModel() {
    private var isLoaded = false
    private val _uiState: MutableStateFlow<BackupProcessUiState> = MutableStateFlow(BackupProcessUiState)
    val uiState: StateFlow<BackupProcessUiState> = _uiState.asStateFlow()

    val appsItem: StateFlow<ProcessItem> = backupProcessRepo.getAppsItem().asStateFlow()
    val filesItem: StateFlow<ProcessItem> = backupProcessRepo.getFilesItem().asStateFlow()
    val networksItem: StateFlow<ProcessItem> = backupProcessRepo.getNetworksItem().asStateFlow()
    val contactsItem: StateFlow<ProcessItem> = backupProcessRepo.getContactsItem().asStateFlow()
    val callLogsItem: StateFlow<ProcessItem> = backupProcessRepo.getCallLogsItem().asStateFlow()
    val messagesItem: StateFlow<ProcessItem> = backupProcessRepo.getMessagesItem().asStateFlow()

    fun loadProcessItems() {
        withLock(Dispatchers.Default) {
            if (isLoaded.not()) {
                isLoaded = true
                backupProcessRepo.onStart()
            }
        }
    }
}
