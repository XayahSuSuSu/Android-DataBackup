package com.xayah.databackup.feature.backup.rustic

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.data.rustic.RusticBackupCoordinator
import com.xayah.databackup.data.rustic.RusticBackupEvent
import com.xayah.databackup.data.rustic.RusticBackupStage
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class RusticBackupProcessViewModel(
    private val mBackupConfigRepo: BackupConfigRepository,
    private val mBackupCoordinator: RusticBackupCoordinator,
    mAppRepo: AppRepository,
    mFileRepo: FileRepository,
    mNetworkRepo: NetworkRepository,
    mContactRepo: ContactRepository,
    mCallLogRepo: CallLogRepository,
    mMessageRepo: MessageRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "RusticBackupProcessViewModel"
    }

    private val _uiState: MutableStateFlow<RusticBackupProcessUiState> = MutableStateFlow(RusticBackupProcessUiState())
    val uiState: StateFlow<RusticBackupProcessUiState> = _uiState.asStateFlow()

    val appsItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mAppRepo.isBackupAppsSelected,
        mAppRepo.appsFilteredAndSelected,
        mAppRepo.appsFiltered,
    ) { selected, selectedApps, apps ->
        sourceItem(
            titleRes = R.string.apps,
            iconRes = R.drawable.ic_layout_grid,
            selected = selected,
            selectedCount = selectedApps.size,
            totalCount = apps.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.apps, R.drawable.ic_layout_grid),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val filesItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mFileRepo.isBackupFilesSelected,
        mFileRepo.filesSelected,
        mFileRepo.files,
    ) { selected, selectedFiles, files ->
        sourceItem(
            titleRes = R.string.files,
            iconRes = R.drawable.ic_folder,
            selected = selected,
            selectedCount = selectedFiles.size,
            totalCount = files.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.files, R.drawable.ic_folder),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val networksItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mNetworkRepo.isBackupNetworksSelected,
        mNetworkRepo.networksSelected,
        mNetworkRepo.networks,
    ) { selected, selectedNetworks, networks ->
        sourceItem(
            titleRes = R.string.networks,
            iconRes = R.drawable.ic_wifi,
            selected = selected,
            selectedCount = selectedNetworks.size,
            totalCount = networks.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.networks, R.drawable.ic_wifi),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val contactsItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mContactRepo.isBackupMessagesSelected,
        mContactRepo.contactsSelected,
        mContactRepo.contacts,
    ) { selected, selectedContacts, contacts ->
        sourceItem(
            titleRes = R.string.contacts,
            iconRes = R.drawable.ic_user_round,
            selected = selected,
            selectedCount = selectedContacts.size,
            totalCount = contacts.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.contacts, R.drawable.ic_user_round),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val callLogsItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mCallLogRepo.isBackupCallLogsSelected,
        mCallLogRepo.callLogsSelected,
        mCallLogRepo.callLogs,
    ) { selected, selectedCallLogs, callLogs ->
        sourceItem(
            titleRes = R.string.call_logs,
            iconRes = R.drawable.ic_phone,
            selected = selected,
            selectedCount = selectedCallLogs.size,
            totalCount = callLogs.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.call_logs, R.drawable.ic_phone),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val messagesItem: StateFlow<RusticBackupSourceUiItem> = combine(
        mMessageRepo.isBackupContactsSelected,
        mMessageRepo.smsList,
        mMessageRepo.mmsList,
    ) { selected, smsList, mmsList ->
        sourceItem(
            titleRes = R.string.messages,
            iconRes = R.drawable.ic_message_circle,
            selected = selected,
            selectedCount = smsList.count { it.selected } + mmsList.count { it.selected },
            totalCount = smsList.size + mmsList.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = initialSourceItem(R.string.messages, R.drawable.ic_message_circle),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val sourceItems: StateFlow<List<RusticBackupSourceUiItem>> = combine(
        appsItem,
        filesItem,
        networksItem,
        contactsItem,
        callLogsItem,
        messagesItem,
    ) { appsItem, filesItem, networksItem, contactsItem, callLogsItem, messagesItem ->
        listOf(appsItem, filesItem, networksItem, contactsItem, callLogsItem, messagesItem)
    }.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val overallProgress: StateFlow<String> = uiState
        .map { it.progress.progressPercent }
        .stateIn(
            scope = viewModelScope,
            initialValue = "0",
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun updateUiState(onUpdate: RusticBackupProcessUiState.() -> RusticBackupProcessUiState) {
        _uiState.update {
            it.onUpdate()
        }
    }

    fun loadProcessItems() {
        withLock(Dispatchers.IO) {
            if (uiState.value.isLoaded) return@withLock

            val config = mBackupConfigRepo.getCurrentConfig()
            val backend = config.backupBackend as? BackupBackend.Rustic
            val repositoryPath = PathHelper.getBackupRepoDir(config.path)
            updateUiState {
                copy {
                    RusticBackupProcessUiState.isLoaded set true
                    RusticBackupProcessUiState.status set RusticBackupProcessStatus.Processing
                    RusticBackupProcessUiState.backupName set config.displayName
                    RusticBackupProcessUiState.repositoryPath set repositoryPath
                    RusticBackupProcessUiState.isPasswordProtected set (backend?.password.isNullOrBlank().not()
                            && backend.password != BackupBackend.DEFAULT_PASSWORD)
                }
            }
            try {
                val result = mBackupCoordinator.start { event ->
                    _uiState.update { state -> reduceRusticBackupState(state, event) }
                }
                _uiState.update {
                    it.copy(
                        status = RusticBackupProcessStatus.Finished,
                        snapshotId = result.snapshotId,
                        progress = it.progress.copy(progress = 1f),
                    )
                }
            } catch (error: CancellationException) {
                // Cancellation should not be reported as a backup failure.
                throw error
            } catch (error: Throwable) {
                LogHelper.e(TAG, "loadProcessItems", "Rustic backup failed.", error)
                _uiState.update { state ->
                    state.copy(
                        status = RusticBackupProcessStatus.Failed,
                        errorMessage = App.application.getString(R.string.rustic_backup_failed),
                    )
                }
            }
        }
    }

    fun refreshRepositoryStorage() {
        _uiState.update { state ->
            state.copy(repositoryStorage = state.repositoryStorage.copy(isLoading = state.repositoryStorage.isAvailable.not()))
        }
        viewModelScope.launch(Dispatchers.IO) {
            val repositoryPath = uiState.value.repositoryPath
            if (repositoryPath.isBlank()) {
                _uiState.update { state ->
                    state.copy(
                        repositoryStorage = state.repositoryStorage.takeIf { it.isAvailable } ?: RusticRepositoryStorageUiState(isLoading = false),
                    )
                }
                return@launch
            }

            val storagePath = PathHelper.getParentPath(repositoryPath).ifBlank { repositoryPath }
            val stat = runCatching { RemoteRootService.readStatFs(storagePath) }.onFailure { error ->
                LogHelper.e(TAG, "refreshRepositoryStorage", "Failed to read storage statistics.", error)
            }.getOrNull()
            if (stat == null || stat.totalBytes <= 0L) {
                _uiState.update { state ->
                    state.copy(
                        repositoryStorage = state.repositoryStorage.takeIf { it.isAvailable } ?: RusticRepositoryStorageUiState(isLoading = false),
                    )
                }
                return@launch
            }

            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes.coerceIn(0L, totalBytes)
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val repositoryBytes = runCatching { RemoteRootService.calculateTreeSize(repositoryPath) }
                .onFailure { error ->
                    LogHelper.e(TAG, "refreshRepositoryStorage", "Failed to calculate repository size.", error)
                }
                .getOrDefault(0L)
                .coerceIn(0L, usedBytes)
            val otherBytes = (usedBytes - repositoryBytes).coerceAtLeast(0L)

            _uiState.update { state ->
                state.copy(
                    repositoryStorage = RusticRepositoryStorageUiState(
                        isLoading = false,
                        repositoryBytes = repositoryBytes,
                        otherBytes = otherBytes,
                        freeBytes = freeBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
        }
    }

    private fun initialSourceItem(
        titleRes: Int,
        iconRes: Int,
    ): RusticBackupSourceUiItem {
        return sourceItem(
            titleRes = titleRes,
            iconRes = iconRes,
            selected = false,
            selectedCount = 0,
            totalCount = 0,
        )
    }

    private fun sourceItem(
        titleRes: Int,
        iconRes: Int,
        selected: Boolean,
        selectedCount: Int,
        totalCount: Int,
    ): RusticBackupSourceUiItem {
        return RusticBackupSourceUiItem(
            titleRes = titleRes,
            iconRes = iconRes,
            selectedCount = if (selected) selectedCount else 0,
            totalCount = totalCount,
        )
    }

    private fun reduceRusticBackupState(
        state: RusticBackupProcessUiState,
        event: RusticBackupEvent,
    ): RusticBackupProcessUiState {
        return when (event) {
            is RusticBackupEvent.Progress -> state.copy(
                progress = RusticBackupProgressUiState(
                    bytesDone = event.bytesDone.coerceAtLeast(0),
                    speed = event.speed.coerceAtLeast(0),
                    progress = event.progress.coerceIn(0f, 1f),
                ),
            )

            is RusticBackupEvent.StageChanged -> {
                val activeIndex = when (event.stage) {
                    RusticBackupStage.PrepareRepository -> 0
                    RusticBackupStage.CollectSources -> 1
                    RusticBackupStage.CreateSnapshot -> 2
                    RusticBackupStage.FinalizeSnapshot -> 3
                }
                state.copy(
                    steps = state.steps.mapIndexed { index, step ->
                        step.copy(status = if (index == activeIndex) RusticBackupStepStatus.Active else RusticBackupStepStatus.Pending)
                    },
                )
            }
        }
    }
}
