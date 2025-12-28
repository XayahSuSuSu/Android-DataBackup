package com.xayah.databackup.data

import androidx.annotation.FloatRange
import arrow.optics.optics
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
import com.xayah.databackup.util.ShellHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@optics
data class ProcessItem(
    val isLoading: Boolean = true,
    val isSelected: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val msg: String = application.getString(R.string.idle),
    @FloatRange(0.0, 1.0) val progress: Float = 0f
) {
    companion object
}

@optics
data class ProcessAppDataDetailItem(
    val bytes: Long = 0L,
    val speed: Long = 0L,
    val status: Int = 0,
    val info: String = "",
) {
    companion object
}

@optics
data class ProcessAppDataItem(
    val enabled: Boolean = true,
    val title: String = "",
    val subtitle: String = application.getString(R.string.idle),
    val msg: String = application.getString(R.string.idle),
    val details: List<ProcessAppDataDetailItem> = listOf(),
) {
    companion object
}

@optics
data class ProcessAppItem(
    val label: String = "",
    val packageName: String = "",
    val userId: Int = 0,
    val apkItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.apk),
        details = listOf(
            ProcessAppDataDetailItem(),
        )
    ),
    val intDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.internal_data),
        details = listOf(
            ProcessAppDataDetailItem(),
            ProcessAppDataDetailItem(),
        )
    ),
    val extDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.external_data),
        details = listOf(
            ProcessAppDataDetailItem(),
        )
    ),
    val addlDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.additional_data),
        details = listOf(
            ProcessAppDataDetailItem(),
            ProcessAppDataDetailItem(),
        )
    ),
    @FloatRange(0.0, 1.0) val progress: Float = 0f
) {
    companion object
}

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

    var mIsCanceled: Boolean = false
        private set

    private var _backupConfig: BackupConfig = BackupConfig()

    private var _appsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _apps: List<App> = listOf()
    private var _processAppItems: MutableStateFlow<List<ProcessAppItem>> = MutableStateFlow(listOf())

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
        _files = mFileRepo.filesSelected.first()
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
        _networks = mNetworkRepo.networksSelected.first()
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
        _contacts = mContactRepo.contactsSelected.first()
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
        _callLogs = mCallLogRepo.callLogsSelected.first()
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
        _smsList = nMessageRepo.smsListSelected.first()
        _mmsList = nMessageRepo.mmsListSelected.first()
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
        clearProcessAppItems()
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

    suspend fun cancel() {
        if (mIsCanceled.not()) {
            mIsCanceled = true
            ShellHelper.killRootService()
        }
    }

    suspend fun onStart() {
        mIsCanceled = false
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

    fun getNetworks(): List<Network> {
        return _networks
    }

    fun getContacts(): List<Contact> {
        return _contacts
    }

    fun getCallLogs(): List<CallLog> {
        return _callLogs
    }

    fun getSmsList(): List<Sms> {
        return _smsList
    }

    fun getMmsList(): List<Mms> {
        return _mmsList
    }

    fun clearProcessAppItems() {
        _processAppItems.value = listOf()
    }

    fun getProcessAppItems(): MutableStateFlow<List<ProcessAppItem>> {
        return _processAppItems
    }

    fun updateAppsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _appsItem.value = onUpdate(_appsItem.value)
    }

    fun updateNetworksItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _networksItem.value = onUpdate(_networksItem.value)
    }

    fun updateContactsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _contactsItem.value = onUpdate(_contactsItem.value)
    }

    fun updateCallLogsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _callLogsItem.value = onUpdate(_callLogsItem.value)
    }

    fun updateMessagesItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _messagesItem.value = onUpdate(_messagesItem.value)
    }

    fun addProcessAppItem(item: ProcessAppItem) {
        _processAppItems.update {
            val items = it.toMutableList()
            items.add(item)
            items
        }
    }

    fun updateProcessAppItem(onUpdate: ProcessAppItem.() -> ProcessAppItem) {
        val currentList = _processAppItems.value
        val newList = currentList.mapIndexed { index, item ->
            if (index == currentList.size - 1) {
                onUpdate(item)
            } else {
                item
            }
        }
        _processAppItems.value = newList
    }
}
