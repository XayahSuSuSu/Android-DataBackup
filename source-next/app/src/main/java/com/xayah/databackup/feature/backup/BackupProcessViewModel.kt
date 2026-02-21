package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.STATUS_CANCEL
import com.xayah.databackup.data.isFailedStatus
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

enum class BackupProcessStatus {
    Processing,
    Canceling,
    Canceled,
    Finished,
}

@optics
data class BackupProcessUiState(
    val isLoaded: Boolean = false,
    val status: BackupProcessStatus = BackupProcessStatus.Processing,
) {
    companion object

    val isProcessing: Boolean = status == BackupProcessStatus.Processing
    val isCanceling: Boolean = status == BackupProcessStatus.Canceling
    val isCanceled: Boolean = status == BackupProcessStatus.Canceled
    val canBeCanceled: Boolean = isLoaded && status == BackupProcessStatus.Processing
}

open class BackupProcessViewModel(private val backupProcessRepo: BackupProcessRepository) : BaseViewModel() {
    private val _uiState: MutableStateFlow<BackupProcessUiState> = MutableStateFlow(BackupProcessUiState())
    val uiState: StateFlow<BackupProcessUiState> = _uiState.asStateFlow()

    val appsItem: StateFlow<ProcessItem> = backupProcessRepo.getAppsItem().asStateFlow()
    val allProcessedAppItems = backupProcessRepo.getProcessAppItems().asStateFlow()
    val failedProcessedAppItems: StateFlow<List<ProcessAppItem>> = allProcessedAppItems
        .map { appItems -> appItems.filter { getAppProcessCategory(it) == AppProcessCategory.Failed } }
        .stateIn(
            scope = viewModelScope,
            initialValue = listOf(),
            started = SharingStarted.WhileSubscribed(5_000),
        )
    val canceledProcessedAppItems: StateFlow<List<ProcessAppItem>> = allProcessedAppItems
        .map { appItems -> appItems.filter { getAppProcessCategory(it) == AppProcessCategory.Canceled } }
        .stateIn(
            scope = viewModelScope,
            initialValue = listOf(),
            started = SharingStarted.WhileSubscribed(5_000),
        )
    val succeededProcessedAppItems: StateFlow<List<ProcessAppItem>> = allProcessedAppItems
        .map { appItems -> appItems.filter { getAppProcessCategory(it) == AppProcessCategory.Succeeded } }
        .stateIn(
            scope = viewModelScope,
            initialValue = listOf(),
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
        if (uiState.value.canBeCanceled.not()) return
        updateUiState {
            copy {
                BackupProcessUiState.status set BackupProcessStatus.Canceling
            }
        }
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
                        BackupProcessUiState.status set BackupProcessStatus.Processing
                    }
                }
                backupProcessRepo.onStart()
                updateUiState {
                    copy {
                        BackupProcessUiState.status set if (status == BackupProcessStatus.Canceling) {
                            BackupProcessStatus.Canceled
                        } else {
                            BackupProcessStatus.Finished
                        }
                    }
                }
            }
        }
    }
}

private enum class AppProcessCategory {
    Failed,
    Canceled,
    Succeeded,
}

private fun isAppFailed(appItem: ProcessAppItem): Boolean {
    return listOf(
        appItem.apkItem.details,
        appItem.intDataItem.details,
        appItem.extDataItem.details,
        appItem.addlDataItem.details,
    ).any { detailList ->
        detailList.any { isFailedStatus(it.status) }
    }
}

private fun isAppCanceled(appItem: ProcessAppItem): Boolean {
    return listOf(
        appItem.apkItem.details,
        appItem.intDataItem.details,
        appItem.extDataItem.details,
        appItem.addlDataItem.details,
    ).any { detailList ->
        detailList.any { it.status == STATUS_CANCEL }
    }
}

private fun getAppProcessCategory(appItem: ProcessAppItem): AppProcessCategory {
    return when {
        isAppFailed(appItem) -> AppProcessCategory.Failed
        isAppCanceled(appItem) -> AppProcessCategory.Canceled
        else -> AppProcessCategory.Succeeded
    }
}
