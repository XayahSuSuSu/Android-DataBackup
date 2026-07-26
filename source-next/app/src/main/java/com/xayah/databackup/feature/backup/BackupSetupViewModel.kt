package com.xayah.databackup.feature.backup

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.ui.component.CallLogPermissions
import com.xayah.databackup.ui.component.ContactPermissions
import com.xayah.databackup.ui.component.MessagePermissions
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.combine
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class TargetItem(
    val selected: Boolean,
    val selections: Pair<Int, Int>,
)

const val MaxSelectedItems = 6

open class BackupSetupViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    private val backupProcessRepo: BackupProcessRepository,
    appRepo: AppRepository,
    fileRepo: FileRepository,
    networkRepo: NetworkRepository,
    contactRepo: ContactRepository,
    callLogRepo: CallLogRepository,
    messageRepo: MessageRepository,
) : BaseViewModel() {
    private val _isLoadingConfigs = MutableStateFlow(true)
    val isLoadingConfigs: StateFlow<Boolean> = _isLoadingConfigs.asStateFlow()

    val selectedBackup: StateFlow<BackupConfig?> = combine(
        backupConfigRepo.configs,
        backupConfigRepo.selectedIndex,
    ) { configs, selectedIndex ->
        configs.getOrNull(selectedIndex)
    }.stateIn(
        scope = viewModelScope,
        initialValue = backupConfigRepo.configs.value.getOrNull(backupConfigRepo.selectedIndex.value),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selectedBackupSize: StateFlow<Long?> = selectedBackup
        .map { backup ->
            backup?.let {
                withContext(Dispatchers.IO) {
                    runCatching { RemoteRootService.calculateTreeSize(it.path) }.getOrNull()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val appsItem: StateFlow<TargetItem?> = combine(
        appRepo.isBackupAppsSelected,
        appRepo.appsFilteredAndSelected,
        appRepo.appsFiltered,
    ) { selected, appsFilteredAndSelected, appsFiltered ->
        TargetItem(
            selected = selected,
            selections = appsFilteredAndSelected.size to appsFiltered.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val filesItem: StateFlow<TargetItem?> = combine(
        fileRepo.isBackupFilesSelected,
        fileRepo.files,
    ) { selected, files ->
        TargetItem(
            selected = selected,
            selections = files.count { false } to files.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val networksItem: StateFlow<TargetItem?> = combine(
        networkRepo.isBackupNetworksSelected,
        networkRepo.networks,
    ) { selected, networks ->
        TargetItem(
            selected = selected,
            selections = networks.count { it.selected } to networks.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val contactsItem: StateFlow<TargetItem?> = combine(
        contactRepo.isBackupMessagesSelected,
        contactRepo.contacts,
    ) { selected, contacts ->
        TargetItem(
            selected = selected,
            selections = contacts.count { it.selected } to contacts.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val callLogsItem: StateFlow<TargetItem?> = combine(
        callLogRepo.isBackupCallLogsSelected,
        callLogRepo.callLogs,
    ) { selected, callLogs ->
        TargetItem(
            selected = selected,
            selections = callLogs.count { it.selected } to callLogs.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val messagesItem: StateFlow<TargetItem?> = combine(
        messageRepo.isBackupContactsSelected,
        messageRepo.smsList,
        messageRepo.mmsList,
    ) { selected, smsList, mmsList ->
        TargetItem(
            selected = selected,
            selections = smsList.count { it.selected } + mmsList.count { it.selected } to smsList.size + mmsList.size
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selectedItems: StateFlow<Pair<Int, Int>?> = combine(
        appsItem,
        filesItem,
        networksItem,
        contactsItem,
        callLogsItem,
        messagesItem,
    ) { appsItem, filesItem, networksItem, contactsItem, callLogsItem, messagesItem ->
        if (appsItem == null || filesItem == null || networksItem == null || contactsItem == null || callLogsItem == null || messagesItem == null) {
            return@combine null
        }
        var count = 0
        if (appsItem.selected && appsItem.selections.first > 0) count++
        if (filesItem.selected && filesItem.selections.first > 0) count++
        if (networksItem.selected && networksItem.selections.first > 0) count++
        if (contactsItem.selected && contactsItem.selections.first > 0) count++
        if (callLogsItem.selected && callLogsItem.selections.first > 0) count++
        if (messagesItem.selected && messagesItem.selections.first > 0) count++
        count to MaxSelectedItems
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val nextBtnEnabled = combine(isLoadingConfigs, selectedItems, selectedBackup) { isLoading, selectedItems, selectedBackup ->
        isLoading.not() &&
                selectedItems?.first != 0 &&
                selectedBackup != null
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    private suspend fun checkPermissions() {
        withContext(Dispatchers.Default) {
            var isContactPermissionsGranted = true
            ContactPermissions.forEach {
                isContactPermissionsGranted = isContactPermissionsGranted &&
                        ContextCompat.checkSelfPermission(App.application, it) == PackageManager.PERMISSION_GRANTED
            }
            if (isContactPermissionsGranted.not()) {
                App.application.saveBoolean(ContactsOptionSelectedBackup.first, false)
            }

            var isCallLogPermissionsGranted = true
            CallLogPermissions.forEach {
                isCallLogPermissionsGranted = isCallLogPermissionsGranted &&
                        ContextCompat.checkSelfPermission(App.application, it) == PackageManager.PERMISSION_GRANTED
            }
            if (isCallLogPermissionsGranted.not()) {
                App.application.saveBoolean(CallLogsOptionSelectedBackup.first, false)
            }

            var isMessagePermissionsGranted = true
            MessagePermissions.forEach {
                isMessagePermissionsGranted = isMessagePermissionsGranted
                        && ContextCompat.checkSelfPermission(App.application, it) == PackageManager.PERMISSION_GRANTED
            }
            if (isMessagePermissionsGranted.not()) {
                App.application.saveBoolean(MessagesOptionSelectedBackup.first, false)
            }
        }
    }

    private suspend fun initBackupConfigs() {
        withContext(Dispatchers.IO) {
            _isLoadingConfigs.value = true
            backupConfigRepo.loadBackupConfigsFromLocal()
            _isLoadingConfigs.value = false
        }
    }

    fun initialize() {
        withLock {
            checkPermissions()
            initBackupConfigs()
        }
    }

    fun resetProcessRepo() {
        backupProcessRepo.reset()
    }

    fun isCurrentBackupRustic(): Boolean {
        return selectedBackup.value?.backupBackend is BackupBackend.Rustic
    }
}
