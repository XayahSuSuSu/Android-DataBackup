package com.xayah.feature.main.medium.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.ContextRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.ModeState
import com.xayah.core.model.OpType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaEntityWithCount
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.medium.R
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PermissionType
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.xayah.core.service.medium.backup.CloudProcessingImpl as CloudBackupService
import com.xayah.core.service.medium.backup.LocalProcessingImpl as LocalBackupService
import com.xayah.core.service.medium.restore.CloudProcessingImpl as CloudRestoreService
import com.xayah.core.service.medium.restore.LocalProcessingImpl as LocalRestoreService

data class IndexUiState(
    val isRefreshing: Boolean,
    val cloud: String,
    val allSelected: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class ToPageMediaDetail(
        val navController: NavHostController,
        val mediaEntity: MediaEntity
    ) : IndexUiIntent()

    data class FilterByKey(val key: String) : IndexUiIntent()
    data class FilterByLocation(val index: Int) : IndexUiIntent()

    data class AddMedia(val context: Context) : IndexUiIntent()
    data class SetMode(val index: Int, val mode: ModeState) : IndexUiIntent()
    data class Select(val entity: MediaEntity) : IndexUiIntent()
    data class Process(val navController: NavHostController) : IndexUiIntent()

    data object SelectAll : IndexUiIntent()
    data object DeleteSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    rootService: RemoteRootService,
    taskRepo: TaskRepository,
    cloudRepo: CloudRepository,
    private val localBackupService: LocalBackupService,
    private val cloudBackupService: CloudBackupService,
    private val localRestoreService: LocalRestoreService,
    private val cloudRestoreService: CloudRestoreService,
    private val contextRepository: ContextRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isRefreshing = false, cloud = "", allSelected = false)) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onSuspendEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.DeleteSelected -> {
                mediumState.value.filter { it.entity.extraInfo.activated }.forEach {
                    if (state.cloud.isEmpty()) {
                        mediaRepo.deleteLocalArchive(it.entity)
                    } else {
                        mediaRepo.deleteRemoteArchive(state.cloud, it.entity)
                    }
                }
            }

            else -> {}
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitStateSuspend(state.copy(isRefreshing = true))
                mediaRepo.refresh(topBarState = _topBarState, modeState = modeState.value, cloud = state.cloud)
                emitStateSuspend(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.ToPageMediaDetail -> {
                val entity = intent.mediaEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.MediumDetail.getRoute(entity.name))
                }
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.FilterByLocation -> {
                mediaRepo.clearActivated()
                if (intent.index == 0) emitState(state.copy(cloud = "", allSelected = false))
                else emitState(state.copy(cloud = accountsState.value[intent.index - 1].name, allSelected = false))
                _locationIndexState.value = intent.index
            }

            is IndexUiIntent.AddMedia -> {
                withMainContext {
                    val context = intent.context
                    PickYouLauncher().apply {
                        setTitle(context.getString(R.string.select_target_directory))
                        setType(PickerType.DIRECTORY)
                        setLimitation(0)
                        setPermissionType(PermissionType.ROOT)
                        launch(context) { pathList ->
                            launchOnIO {
                                emitEffect(IndexUiEffect.ShowSnackbar(mediaRepo.addMedia(pathList)))
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.SetMode -> {
                mediaRepo.clearActivated()
                emitState(state.copy(allSelected = false))
                emitIntentSuspend(IndexUiIntent.FilterByLocation(0))
                _modeState.value = intent.mode
            }

            is IndexUiIntent.Select -> {
                mediaRepo.upsert(intent.entity.copy(extraInfo = intent.entity.extraInfo.copy(activated = intent.entity.extraInfo.activated.not())))
            }

            is IndexUiIntent.Process -> {
                launchOnGlobal {
                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
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
                                localBackupService.preprocessing()
                                localBackupService.processing()
                                localBackupService.postProcessing()
                                localBackupService.destroyService()
                            } else {
                                contextRepository.withContext {
                                    it.saveCloudActivatedAccountName(state.cloud)
                                    cloudBackupService.preprocessing()
                                    cloudBackupService.processing()
                                    cloudBackupService.postProcessing()
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

                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
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
                mediaRepo.upsert(mediumState.value.map { it.entity }.onEach { it.extraInfo.activated = state.allSelected.not() })
                emitState(state.copy(allSelected = state.allSelected.not()))
            }

            else -> {}
        }
    }

    private val _topBarState: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.media)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _mediumOverview: Flow<List<MediaEntityWithCount>> by lazy { mediaRepo.getMedium().flowOnIO() }
    private val _mediumBackup: Flow<List<MediaEntityWithCount>> by lazy { mediaRepo.getMedium(opType = OpType.BACKUP).flowOnIO() }
    private val _mediumRestore: Flow<List<MediaEntityWithCount>> by lazy { mediaRepo.getMedium(opType = OpType.RESTORE).flowOnIO() }
    private var _modeState: MutableStateFlow<ModeState> = MutableStateFlow(ModeState.OVERVIEW)
    val modeState: StateFlow<ModeState> = _modeState.stateInScope(ModeState.OVERVIEW)
    private var _medium: Flow<List<MediaEntityWithCount>> = combine(_modeState, _mediumOverview, _mediumBackup, _mediumRestore) { state, overview, backup, restore ->
        when (state) {
            ModeState.OVERVIEW -> overview
            ModeState.BATCH_BACKUP -> backup
            ModeState.BATCH_RESTORE -> restore
        }
    }

    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _locationIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    val locationIndexState: StateFlow<Int> = _locationIndexState.stateInScope(0)
    private val _mediumState: Flow<List<MediaEntityWithCount>> = combine(_medium, _keyState) { packages, key ->
        packages.filter(mediaRepo.getKeyPredicate(key = key))
    }.combine(_locationIndexState) { packages, locationIndex ->
        if (modeState.value == ModeState.BATCH_BACKUP) packages
        else packages.filter(mediaRepo.getLocationPredicate(index = locationIndex, accountList = accountsState.value))
    }.flowOnIO()
    val mediumState: StateFlow<List<MediaEntityWithCount>> = _mediumState.stateInScope(listOf())

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accountsState: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())

    private val _processingCount: Flow<Long> = taskRepo.processingCount.flowOnIO()
    val processingCountState: StateFlow<Boolean> = _processingCount.map { it != 0L }.stateInScope(false)

    private val _activatedCount: Flow<Long> = mediaRepo.activatedCount.flowOnIO()
    val activatedState: StateFlow<Boolean> = _activatedCount.map { it != 0L }.stateInScope(false)
}
