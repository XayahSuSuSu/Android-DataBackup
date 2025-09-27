package com.xayah.databackup.feature.backup

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.BackupApps
import com.xayah.databackup.feature.BackupCallLogs
import com.xayah.databackup.feature.BackupContacts
import com.xayah.databackup.feature.BackupMessages
import com.xayah.databackup.feature.BackupNetworks
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.combine
import com.xayah.databackup.util.filterApp
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readInt
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SetupUiState(
    val isLoadingConfigs: Boolean = true,
    val selectedConfigIndex: Int = -1,
    val configs: List<BackupConfig> = listOf()
)

data class TargetItem(
    var selected: Boolean,
    val title: String,
    var subtitle: String,
    val icon: ImageVector,
    val onClickSettings: (NavHostController) -> Unit,
    val onSelectedChanged: suspend (Boolean) -> Unit,
)

const val MaxSelectedItems = 6
val DefSelectedItems = 0 to MaxSelectedItems

open class BackupSetupViewModel(
    private val backupConfigRepo: BackupConfigRepository
) : BaseViewModel() {
    private val _uiState: MutableStateFlow<SetupUiState> = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val targetSourceItems: Flow<List<TargetItem>> = flowOf(
        listOf(
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.apps),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_layout_grid),
                onClickSettings = {
                    it.navigateSafely(BackupApps)
                },
                onSelectedChanged = {
                    App.application.saveBoolean(AppsOptionSelectedBackup.first, it)
                }
            ),
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.files),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_folder),
                onClickSettings = {},
                onSelectedChanged = {}
            ),
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.networks),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_wifi),
                onClickSettings = {
                    it.navigateSafely(BackupNetworks)
                },
                onSelectedChanged = {
                    App.application.saveBoolean(NetworksOptionSelectedBackup.first, it)
                }
            ),
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.contacts),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_user_round),
                onClickSettings = {
                    it.navigateSafely(BackupContacts)
                },
                onSelectedChanged = {
                    App.application.saveBoolean(ContactsOptionSelectedBackup.first, it)
                }
            ),
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.call_logs),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_phone),
                onClickSettings = {
                    it.navigateSafely(BackupCallLogs)
                },
                onSelectedChanged = {
                    App.application.saveBoolean(CallLogsOptionSelectedBackup.first, it)
                }
            ),
            TargetItem(
                selected = false,
                title = App.application.getString(R.string.messages),
                subtitle = App.application.getString(R.string.items_selected, "0", "0"),
                icon = ImageVector.vectorResource(null, App.application.resources, R.drawable.ic_message_circle),
                onClickSettings = {
                    it.navigateSafely(BackupMessages)
                },
                onSelectedChanged = {
                    App.application.saveBoolean(MessagesOptionSelectedBackup.first, it)
                }
            ),
        )
    )

    private val appsSelections = combine(
        DatabaseHelper.appDao.loadFlowApps(),
        App.application.readInt(FilterBackupUser),
        App.application.readBoolean(FiltersUserAppsBackup),
        App.application.readBoolean(FiltersSystemAppsBackup),
    ) { apps, userId, filterUserApps, filterSystemApps ->
        val list = apps.filterApp(userId, filterUserApps, filterSystemApps)
        list.count { it.option.apk || it.option.internalData || it.option.externalData || it.option.obbAndMedia } to list.size
    }
    private val networksSelections = DatabaseHelper.networkDao.loadFlowNetworks().map { list ->
        list.count { it.selected } to list.size
    }
    private val contactsSelections = DatabaseHelper.contactDao.loadFlowContacts().map { list ->
        list.count { it.selected } to list.size
    }
    private val callLogsSelections = DatabaseHelper.callLogDao.loadFlowCallLogs().map { list ->
        list.count { it.selected } to list.size
    }
    private val messagesSelections = combine(DatabaseHelper.messageDao.loadFlowSms(), DatabaseHelper.messageDao.loadFlowMms()) { smsList, mmsList ->
        smsList.count { it.selected } + mmsList.count { it.selected } to smsList.size + mmsList.size
    }
    private val _targetItems = combine(
        targetSourceItems,
        App.application.readBoolean(AppsOptionSelectedBackup),
        flowOf(false),
        App.application.readBoolean(NetworksOptionSelectedBackup),
        App.application.readBoolean(ContactsOptionSelectedBackup),
        App.application.readBoolean(CallLogsOptionSelectedBackup),
        App.application.readBoolean(MessagesOptionSelectedBackup),
        appsSelections,
        flowOf(0 to 0),
        networksSelections,
        contactsSelections,
        callLogsSelections,
        messagesSelections,
    ) { items,
        appsSelected, filesSelected, networksSelected, contactsSelected, callLogsSelected, messagesSelected,
        apps, files, networks, contacts, callLogs, messages ->
        val newItems = mutableListOf<TargetItem>()
        newItems.add(
            items[0].copy(
                selected = appsSelected,
                subtitle = App.application.getString(R.string.items_selected, "${apps.first}", "${apps.second}")
            )
        )
        newItems.add(
            items[1].copy(
                selected = filesSelected,
                subtitle = App.application.getString(R.string.items_selected, "${files.first}", "${files.second}")
            )
        )
        newItems.add(
            items[2].copy(
                selected = networksSelected,
                subtitle = App.application.getString(R.string.items_selected, "${networks.first}", "${networks.second}")
            )
        )
        newItems.add(
            items[3].copy(
                selected = contactsSelected,
                subtitle = App.application.getString(R.string.items_selected, "${contacts.first}", "${contacts.second}")
            )
        )
        newItems.add(
            items[4].copy(
                selected = callLogsSelected,
                subtitle = App.application.getString(R.string.items_selected, "${callLogs.first}", "${callLogs.second}")
            )
        )
        newItems.add(
            items[5].copy(
                selected = messagesSelected,
                subtitle = App.application.getString(R.string.items_selected, "${messages.first}", "${messages.second}")
            )
        )
        newItems
    }

    val targetItems: StateFlow<List<TargetItem>> = _targetItems.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    private val _selectedItems = _targetItems.map {
        var count = 0
        if (it[0].selected) count++
        if (it[1].selected) count++
        if (it[2].selected) count++
        if (it[3].selected) count++
        if (it[4].selected) count++
        if (it[5].selected) count++
        count to MaxSelectedItems
    }
    val selectedItems: StateFlow<Pair<Int, Int>> = _selectedItems.stateIn(
        scope = viewModelScope,
        initialValue = DefSelectedItems,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val nextBtnEnabled = combine(uiState, selectedItems) { uiState, selectedItems ->
        uiState.isLoadingConfigs.not() && selectedItems.first != 0
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    suspend fun getLocalStorage(): String {
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

    fun initialize() {
        withLock(Dispatchers.IO) {
            backupConfigRepo.loadBackupConfigsFromLocal()
            _uiState.emit(uiState.value.copy(isLoadingConfigs = false, configs = backupConfigRepo.getConfigs()))
        }
    }

    fun selectBackup(index: Int) {
        withLock(Dispatchers.Default) {
            _uiState.emit(uiState.value.copy(selectedConfigIndex = index))
        }
    }
}
