package com.xayah.databackup.feature.backup

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.ui.component.CallLogPermissions
import com.xayah.databackup.ui.component.ContactPermissions
import com.xayah.databackup.ui.component.MessagePermissions
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.combine
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class BackupSetupUiState(
    val isLoadingConfigs: Boolean = true,
)

data class TargetItem(
    val selected: Boolean,
    val selections: Pair<Int, Int>,
)

const val MaxSelectedItems = 6

open class BackupSetupViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    appRepo: AppRepository,
    fileRepo: FileRepository,
    networkRepo: NetworkRepository,
    contactRepo: ContactRepository,
    callLogRepo: CallLogRepository,
    messageRepo: MessageRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "BackupSetupViewModel"
    }

    private val _uiState: MutableStateFlow<BackupSetupUiState> = MutableStateFlow(BackupSetupUiState())
    val uiState: StateFlow<BackupSetupUiState> = _uiState.asStateFlow()

    val selectedConfigIndex: StateFlow<Int> = backupConfigRepo.selectedIndex
    val backupConfigs: StateFlow<List<BackupConfig>> = backupConfigRepo.configs

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
        if (appsItem.selected) count++
        if (filesItem.selected) count++
        if (networksItem.selected) count++
        if (contactsItem.selected) count++
        if (callLogsItem.selected) count++
        if (messagesItem.selected) count++
        count to MaxSelectedItems
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val nextBtnEnabled = combine(uiState, selectedItems) { uiState, selectedItems ->
        uiState.isLoadingConfigs.not() && selectedItems?.first != 0
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    suspend fun getLocalStorage(): String {
        val backupPath = PathHelper.getBackupPath().first()
        if (RemoteRootService.mkdirs(backupPath).not()) {
            LogHelper.e(TAG, "getLocalStorage", "Failed to mkdirs: $backupPath.}")
        }
        val stat = RemoteRootService.readStatFs(PathHelper.getBackupPath().first())
        return if (stat == null) {
            App.application.getString(R.string.unknown)
        } else {
            "${stat.availableBytes.formatToStorageSize} / ${stat.totalBytes.formatToStorageSize}"
        }
    }

    suspend fun getBackupStorage(path: String): String {
        val size = RemoteRootService.calculateTreeSize(path)
        return if (size == 0L) {
            App.application.getString(R.string.unknown)
        } else {
            size.formatToStorageSize
        }
    }

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
            _uiState.emit(uiState.value.copy(isLoadingConfigs = true))
            backupConfigRepo.loadBackupConfigsFromLocal()
            _uiState.emit(uiState.value.copy(isLoadingConfigs = false))
        }
    }

    fun initialize() {
        withLock {
            checkPermissions()
            initBackupConfigs()
        }
    }

    fun selectBackup(index: Int) {
        withLock(Dispatchers.Default) {
            backupConfigRepo.selectBackup(index)
        }
    }
}
