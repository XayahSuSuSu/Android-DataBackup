package com.xayah.databackup.data

import androidx.annotation.FloatRange
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.service.BackupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

data class ProcessItem(
    val isLoading: Boolean = true,
    val isSelected: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val msg: String = application.getString(R.string.idle),
    @FloatRange(0.0, 1.0) val progress: Float = 0f
)

class BackupProcessRepository(
    private val mAppRepo: AppRepository,
    private val mFileRepo: FileRepository,
    private val mNetworkRepo: NetworkRepository,
    private val mContactRepo: ContactRepository,
    private val mCallLogRepo: CallLogRepository,
    private val nMessageRepo: MessageRepository,
    private val mBackupConfigRepo: BackupConfigRepository,
) {
    companion object {
        private const val TAG = "BackupProcessRepository"
    }

    private var _backupConfig: BackupConfig = BackupConfig()

    private var _appsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _apps: List<App> = listOf()

    private var _filesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _files: List<Any> = listOf()

    private var _networksItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _networks: List<Network> = listOf()

    private var _contactsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _contacts: List<Contact> = listOf()

    private var _callLogsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _callLogs: List<CallLog> = listOf()

    private var _messagesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _smsList: List<Sms> = listOf()
    private var _mmsList: List<Mms> = listOf()

    suspend fun loadAppsProcessItems() {
        _apps = mAppRepo.appsFilteredAndSelected.first()
        _appsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mAppRepo.isBackupAppsSelected.first(),
                currentIndex = 0,
                totalCount = _apps.size,
                progress = 0f
            )
        }
    }

    suspend fun loadFilesProcessItems() {
        _files = mFileRepo.files.first()
        _filesItem.update {
            it.copy(
                isLoading = false,
                isSelected = mFileRepo.isBackupFilesSelected.first(),
                currentIndex = 0,
                totalCount = _files.size,
                progress = 0f
            )
        }
    }

    suspend fun loadNetworksProcessItems() {
        _networks = mNetworkRepo.networks.first()
        _networksItem.update {
            it.copy(
                isLoading = false,
                isSelected = mNetworkRepo.isBackupNetworksSelected.first(),
                currentIndex = 0,
                totalCount = _networks.size,
                progress = 0f
            )
        }
    }

    suspend fun loadContactsProcessItems() {
        _contacts = mContactRepo.contacts.first()
        _contactsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mContactRepo.isBackupMessagesSelected.first(),
                currentIndex = 0,
                totalCount = _contacts.size,
                progress = 0f
            )
        }
    }

    suspend fun loadCallLogsProcessItems() {
        _callLogs = mCallLogRepo.callLogs.first()
        _callLogsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mCallLogRepo.isBackupCallLogsSelected.first(),
                currentIndex = 0,
                totalCount = _callLogs.size,
                progress = 0f
            )
        }
    }

    suspend fun loadMessagesProcessItems() {
        _smsList = nMessageRepo.smsList.first()
        _mmsList = nMessageRepo.mmsList.first()
        _messagesItem.update {
            it.copy(
                isLoading = false,
                isSelected = nMessageRepo.isBackupContactsSelected.first(),
                currentIndex = 0,
                totalCount = _smsList.size + _mmsList.size,
                progress = 0f
            )
        }
    }

    private suspend fun loadProcessItems() {
        loadAppsProcessItems()
        loadFilesProcessItems()
        loadNetworksProcessItems()
        loadContactsProcessItems()
        loadCallLogsProcessItems()
        loadMessagesProcessItems()
    }

    private fun loadBackupPath() {
        _backupConfig = mBackupConfigRepo.getCurrentConfig()
    }

    suspend fun onStart() {
        loadBackupPath()
        loadProcessItems()
        BackupService.start()
    }

    fun getBackupConfig(): BackupConfig {
        return _backupConfig
    }

    fun getAppsItem(): MutableStateFlow<ProcessItem> {
        return _appsItem
    }

    fun getFilesItem(): MutableStateFlow<ProcessItem> {
        return _filesItem
    }

    fun getNetworksItem(): MutableStateFlow<ProcessItem> {
        return _networksItem
    }

    fun getContactsItem(): MutableStateFlow<ProcessItem> {
        return _contactsItem
    }

    fun getCallLogsItem(): MutableStateFlow<ProcessItem> {
        return _callLogsItem
    }

    fun getMessagesItem(): MutableStateFlow<ProcessItem> {
        return _messagesItem
    }

    fun getApps(): List<App> {
        return _apps
    }

    fun updateAppsItem(currentIndex: Int, msg: String) {
        _appsItem.update {
            it.copy(
                currentIndex = currentIndex,
                totalCount = _apps.size,
                msg = msg,
                progress = if (_apps.isNotEmpty()) currentIndex.toFloat() / _apps.size else 1f
            )
        }
    }
}
