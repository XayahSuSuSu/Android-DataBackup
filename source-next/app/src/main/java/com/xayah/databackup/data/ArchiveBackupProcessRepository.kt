package com.xayah.databackup.data

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.backup.ProcessAppItem
import com.xayah.databackup.entity.backup.ProcessItem
import com.xayah.databackup.service.BackupService
import com.xayah.databackup.util.ShellHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class ArchiveBackupProcessRepository(
    private val mBackupSelectionRepo: BackupSelectionRepository,
    private val mBackupConfigRepo: BackupConfigRepository,
) {
    companion object {
        private const val TAG = "ArchiveBackupProcessRepository"
    }

    var isCanceled: Boolean = false
        private set

    private var mBackupConfig: BackupConfig = BackupConfig()

    private val _appsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mApps: List<App> = listOf()
    private val _processAppItems: MutableStateFlow<List<ProcessAppItem>> = MutableStateFlow(listOf())

    private val _filesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mFiles: List<Any> = listOf()

    private val _networksItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mNetworks: List<Network> = listOf()

    private val _contactsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mContacts: List<Contact> = listOf()

    private val _callLogsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mCallLogs: List<CallLog> = listOf()

    private val _messagesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var mSmsList: List<Sms> = listOf()
    private var mMmsList: List<Mms> = listOf()

    suspend fun loadAppsProcessItems() {
        mApps = mBackupSelectionRepo.appsFilteredAndSelected.first()
        _appsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupAppsSelected.first(),
                currentIndex = 0,
                totalCount = mApps.size,
                progress = 0f
            )
        }
    }

    suspend fun loadFilesProcessItems() {
        mFiles = mBackupSelectionRepo.filesSelected.first()
        _filesItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupFilesSelected.first(),
                currentIndex = 0,
                totalCount = mFiles.size,
                progress = 0f
            )
        }
    }

    suspend fun loadNetworksProcessItems() {
        mNetworks = mBackupSelectionRepo.networksSelected.first()
        _networksItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupNetworksSelected.first(),
                currentIndex = 0,
                totalCount = mNetworks.size,
                progress = 0f
            )
        }
    }

    suspend fun loadContactsProcessItems() {
        mContacts = mBackupSelectionRepo.contactsSelected.first()
        _contactsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupContactsSelected.first(),
                currentIndex = 0,
                totalCount = mContacts.size,
                progress = 0f
            )
        }
    }

    suspend fun loadCallLogsProcessItems() {
        mCallLogs = mBackupSelectionRepo.callLogsSelected.first()
        _callLogsItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupCallLogsSelected.first(),
                currentIndex = 0,
                totalCount = mCallLogs.size,
                progress = 0f
            )
        }
    }

    suspend fun loadMessagesProcessItems() {
        mSmsList = mBackupSelectionRepo.smsListSelected.first()
        mMmsList = mBackupSelectionRepo.mmsListSelected.first()
        _messagesItem.update {
            it.copy(
                isLoading = false,
                isSelected = mBackupSelectionRepo.isBackupMessagesSelected.first(),
                currentIndex = 0,
                totalCount = mSmsList.size + mMmsList.size,
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
        mBackupConfig = mBackupConfigRepo.getCurrentConfig()
    }

    suspend fun cancel() {
        if (isCanceled.not()) {
            isCanceled = true
            ShellHelper.killRootService()
        }
    }

    suspend fun onStart() {
        isCanceled = false
        loadBackupPath()
        loadProcessItems()
        BackupService.start()
    }

    fun getBackupConfig(): BackupConfig {
        return mBackupConfig
    }

    fun getAppsItem(): StateFlow<ProcessItem> {
        return _appsItem.asStateFlow()
    }

    fun getFilesItem(): StateFlow<ProcessItem> {
        return _filesItem.asStateFlow()
    }

    fun getNetworksItem(): StateFlow<ProcessItem> {
        return _networksItem.asStateFlow()
    }

    fun getContactsItem(): StateFlow<ProcessItem> {
        return _contactsItem.asStateFlow()
    }

    fun getCallLogsItem(): StateFlow<ProcessItem> {
        return _callLogsItem.asStateFlow()
    }

    fun getMessagesItem(): StateFlow<ProcessItem> {
        return _messagesItem.asStateFlow()
    }

    fun getApps(): List<App> {
        return mApps
    }

    fun getNetworks(): List<Network> {
        return mNetworks
    }

    fun getContacts(): List<Contact> {
        return mContacts
    }

    fun getCallLogs(): List<CallLog> {
        return mCallLogs
    }

    fun getSmsList(): List<Sms> {
        return mSmsList
    }

    fun getMmsList(): List<Mms> {
        return mMmsList
    }

    fun reset() {
        updateAppsItem { ProcessItem() }
        updateFilesItem { ProcessItem() }
        updateNetworksItem { ProcessItem() }
        updateContactsItem { ProcessItem() }
        updateCallLogsItem { ProcessItem() }
        updateMessagesItem { ProcessItem() }
        clearProcessAppItems()
    }

    fun clearProcessAppItems() {
        _processAppItems.value = listOf()
    }

    fun getProcessAppItems(): StateFlow<List<ProcessAppItem>> {
        return _processAppItems.asStateFlow()
    }

    fun updateAppsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _appsItem.value = onUpdate(_appsItem.value)
    }

    fun updateFilesItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _filesItem.value = onUpdate(_filesItem.value)
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
