package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@optics
data class BackupProcessUiState(
    val isLoaded: Boolean = false,
    val isProcessing: Boolean = true,
) {
    companion object

    val canBeCanceled: Boolean = isLoaded && isProcessing
}

open class BackupProcessViewModel(private val backupProcessRepo: BackupProcessRepository) : BaseViewModel() {
    private val _uiState: MutableStateFlow<BackupProcessUiState> = MutableStateFlow(BackupProcessUiState())
    val uiState: StateFlow<BackupProcessUiState> = _uiState.asStateFlow()

    val appsItem: StateFlow<ProcessItem> = backupProcessRepo.getAppsItem().asStateFlow()
    val processingAppItem: StateFlow<ProcessAppItem?> = backupProcessRepo.getProcessAppItems().map { it.lastOrNull() }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val filesItem: StateFlow<ProcessItem> = backupProcessRepo.getFilesItem().asStateFlow()
    val networksItem: StateFlow<ProcessItem> = backupProcessRepo.getNetworksItem().asStateFlow()
    val contactsItem: StateFlow<ProcessItem> = backupProcessRepo.getContactsItem().asStateFlow()
    val callLogsItem: StateFlow<ProcessItem> = backupProcessRepo.getCallLogsItem().asStateFlow()
    val messagesItem: StateFlow<ProcessItem> = backupProcessRepo.getMessagesItem().asStateFlow()

    val overallProgress: StateFlow<String> = combine(
        appsItem,
        filesItem,
        networksItem,
        contactsItem,
        callLogsItem,
        messagesItem,
    ) { appsItem, filesItem, networksItem, contactsItem, callLogsItem, messagesItem ->
        var currentIndex = 0
        var totalCount = 0
        if (appsItem.isSelected) {
            currentIndex += appsItem.currentIndex
            totalCount += appsItem.totalCount
        }
        if (filesItem.isSelected) {
            currentIndex += filesItem.currentIndex
            totalCount += filesItem.totalCount
        }
        if (networksItem.isSelected) {
            currentIndex += networksItem.currentIndex
            totalCount += networksItem.totalCount
        }
        if (contactsItem.isSelected) {
            currentIndex += contactsItem.currentIndex
            totalCount += contactsItem.totalCount
        }
        if (callLogsItem.isSelected) {
            currentIndex += callLogsItem.currentIndex
            totalCount += callLogsItem.totalCount
        }
        if (messagesItem.isSelected) {
            currentIndex += messagesItem.currentIndex
            totalCount += messagesItem.totalCount
        }
        if (totalCount != 0) {
            ((currentIndex.toFloat() / totalCount) * 100).roundToInt().toString()
        } else {
            0.toString()
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = "0",
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun updateUiState(onUpdate: BackupProcessUiState.() -> BackupProcessUiState) {
        _uiState.update {
            it.onUpdate()
        }
    }

    fun cancel() {
        viewModelScope.launch(Dispatchers.Default) {
            backupProcessRepo.cancel()
        }
    }

    fun loadProcessItems() {
        withLock(Dispatchers.Default) {
            if (uiState.value.isLoaded.not()) {
                updateUiState {
                    copy {
                        BackupProcessUiState.isLoaded set true
                    }
                }
                backupProcessRepo.onStart()
                updateUiState {
                    copy {
                        BackupProcessUiState.isProcessing set false
                    }
                }
            }
        }
    }
}
