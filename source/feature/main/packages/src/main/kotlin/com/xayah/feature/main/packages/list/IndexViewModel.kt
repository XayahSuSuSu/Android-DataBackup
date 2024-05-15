package com.xayah.feature.main.packages.list

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.material3.SnackbarDuration
import androidx.navigation.NavHostController
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.ContextRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.ModeState
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageEntityWithCount
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.packages.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.xayah.core.service.packages.backup.impl.CloudProcessingImpl as CloudBackupService
import com.xayah.core.service.packages.backup.impl.LocalProcessingImpl as LocalBackupService
import com.xayah.core.service.packages.restore.impl.CloudProcessingImpl as CloudRestoreService
import com.xayah.core.service.packages.restore.impl.LocalProcessingImpl as LocalRestoreService

data class IndexUiState(
    val isRefreshing: Boolean,
    val userIdList: List<Int>,
    val cloud: String,
    val allSelected: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class ToPagePackageDetail(val navController: NavHostController, val packageEntity: PackageEntity) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data class FilterByLocation(val index: Int) : IndexUiIntent()
    data object GetUserIds : IndexUiIntent()
    data class SetUserIdIndexList(val list: List<Int>) : IndexUiIntent()
    data class SetMode(val index: Int, val mode: ModeState) : IndexUiIntent()
    data class Select(val entity: PackageEntity) : IndexUiIntent()
    data class Process(val navController: NavHostController) : IndexUiIntent()
    data object SelectAll : IndexUiIntent()
    data object DeleteSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRepo: PackageRepository,
    taskRepo: TaskRepository,
    cloudRepo: CloudRepository,
    private val rootService: RemoteRootService,
    private val localBackupService: LocalBackupService,
    private val cloudBackupService: CloudBackupService,
    private val localRestoreService: LocalRestoreService,
    private val cloudRestoreService: CloudRestoreService,
    private val contextRepository: ContextRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isRefreshing = false, userIdList = listOf(), cloud = "", allSelected = false)) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitState(state.copy(isRefreshing = true))
                packageRepo.refresh(topBarState = _topBarState, modeState = modeState.value, cloud = state.cloud)
                emitState(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.ToPagePackageDetail -> {
                val entity = intent.packageEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.PackageDetail.getRoute(entity.packageName, entity.userId))
                }
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.Sort -> {
                var type = intent.type
                val index = intent.index
                if (_sortIndexState.value == index) {
                    type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                    _sortTypeState.value = type
                }
                _sortIndexState.value = index
            }

            is IndexUiIntent.FilterByFlag -> {
                _flagIndexState.value = intent.index
            }

            is IndexUiIntent.GetUserIds -> {
                emitState(state.copy(userIdList = rootService.getUsers().map { it.id }))
            }

            is IndexUiIntent.SetUserIdIndexList -> {
                _userIdIndexListState.value = intent.list
            }

            is IndexUiIntent.FilterByLocation -> {
                packageRepo.clearActivated()
                if (intent.index == 0) emitState(state.copy(cloud = "", allSelected = false))
                else emitState(state.copy(cloud = accountsState.value[intent.index - 1].name, allSelected = false))
                _locationIndexState.value = intent.index
            }

            is IndexUiIntent.SetMode -> {
                packageRepo.clearActivated()
                emitState(state.copy(allSelected = false))
                emitIntent(IndexUiIntent.FilterByLocation(0))
                _modeState.value = intent.mode
            }

            is IndexUiIntent.Select -> {
                packageRepo.upsert(intent.entity.copy(extraInfo = intent.entity.extraInfo.copy(activated = intent.entity.extraInfo.activated.not())))
            }

            is IndexUiIntent.Process -> {
                launchOnGlobal {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.task_is_in_progress),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Indefinite,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )

                    when (modeState.value) {
                        ModeState.BATCH_BACKUP -> {
                            if (state.cloud.isEmpty()) {
                                val backupPreprocessing = localBackupService.preprocessing()
                                localBackupService.processing()
                                localBackupService.postProcessing(backupPreprocessing = backupPreprocessing)
                                localBackupService.destroyService()
                            } else {
                                contextRepository.withContext {
                                    it.saveCloudActivatedAccountName(state.cloud)
                                    val backupPreprocessing = cloudBackupService.preprocessing()
                                    cloudBackupService.processing()
                                    cloudBackupService.postProcessing(backupPreprocessing = backupPreprocessing)
                                    cloudBackupService.destroyService()
                                }
                            }
                        }

                        ModeState.BATCH_RESTORE -> {
                            if (state.cloud.isEmpty()) {
                                localRestoreService.preprocessing()
                                localRestoreService.processing()
                                localRestoreService.postProcessing()
                                localRestoreService.destroyService()
                            } else {
                                contextRepository.withContext {
                                    it.saveCloudActivatedAccountName(state.cloud)
                                    cloudRestoreService.preprocessing()
                                    cloudRestoreService.processing()
                                    cloudRestoreService.postProcessing()
                                    cloudRestoreService.destroyService()
                                }
                            }
                        }

                        else -> {}
                    }

                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.backup_completed),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Long,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                }
            }

            is IndexUiIntent.SelectAll -> {
                packageRepo.upsert(packagesState.value.map { it.entity }.onEach { it.extraInfo.activated = state.allSelected.not() })
                emitState(state.copy(allSelected = state.allSelected.not()))
            }

            is IndexUiIntent.DeleteSelected -> {
                packagesState.value.filter { it.entity.extraInfo.activated }.forEach {
                    if (state.cloud.isEmpty()) {
                        packageRepo.deleteLocalArchive(it.entity)
                    } else {
                        packageRepo.deleteRemoteArchive(state.cloud, it.entity)
                    }
                }
            }
        }
    }

    private val _topBarState: MutableStateFlow<TopBarState> =
        MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.app_and_data)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _packagesOverview: Flow<List<PackageEntityWithCount>> by lazy { packageRepo.getPackages().flowOnIO() }
    private val _packagesBackup: Flow<List<PackageEntityWithCount>> by lazy { packageRepo.getPackages(opType = OpType.BACKUP).flowOnIO() }
    private val _packagesRestore: Flow<List<PackageEntityWithCount>> by lazy { packageRepo.getPackages(opType = OpType.RESTORE).flowOnIO() }
    private var _modeState: MutableStateFlow<ModeState> = MutableStateFlow(ModeState.OVERVIEW)
    val modeState: StateFlow<ModeState> = _modeState.stateInScope(ModeState.OVERVIEW)
    private var _packages: Flow<List<PackageEntityWithCount>> = combine(_modeState, _packagesOverview, _packagesBackup, _packagesRestore) { state, overview, backup, restore ->
        when (state) {
            ModeState.OVERVIEW -> overview
            ModeState.BATCH_BACKUP -> backup
            ModeState.BATCH_RESTORE -> restore
        }
    }
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndexState: MutableStateFlow<Int> = MutableStateFlow(1)
    val flagIndexState: StateFlow<Int> = _flagIndexState.stateInScope(1)
    private var _userIdIndexListState: MutableStateFlow<List<Int>> = MutableStateFlow(listOf(0))
    val userIdIndexListState: StateFlow<List<Int>> = _userIdIndexListState.stateInScope(listOf(0))
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    private var _locationIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    val locationIndexState: StateFlow<Int> = _locationIndexState.stateInScope(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntityWithCount>> =
        combine(_packages, _keyState, _flagIndexState, _sortIndexState, _sortTypeState) { packages, key, flagIndex, sortIndex, sortType ->
            packages.filter(packageRepo.getKeyPredicate(key = key))
                .filter(packageRepo.getFlagPredicate(index = flagIndex))
                .sortedWith(packageRepo.getSortComparator(sortIndex = sortIndex, sortType = sortType))
        }.combine(_userIdIndexListState) { packages, userIdIndexList ->
            packages.filter(packageRepo.getUserIdPredicate(indexList = userIdIndexList, userIdList = uiState.value.userIdList))
        }.combine(_locationIndexState) { packages, locationIndex ->
            if (modeState.value == ModeState.BATCH_BACKUP) packages
            else packages.filter(packageRepo.getLocationPredicate(index = locationIndex, accountList = accountsState.value))
        }.flowOnIO()
    val packagesState: StateFlow<List<PackageEntityWithCount>> = _packagesState.stateInScope(listOf())

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accountsState: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())

    private val _processingCount: Flow<Long> = taskRepo.processingCount.flowOnIO()
    val processingCountState: StateFlow<Boolean> = _processingCount.map { it != 0L }.stateInScope(false)

    private val _activatedCount: Flow<Long> = packageRepo.activatedCount.flowOnIO()
    val activatedState: StateFlow<Boolean> = _activatedCount.map { it != 0L }.stateInScope(false)
}
