package com.xayah.feature.main.medium.restore.processing

import android.content.Context
import android.view.SurfaceControlHidden
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.readScreenOffCountDown
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.medium.restore.ProcessingServiceCloudImpl
import com.xayah.core.service.medium.restore.ProcessingServiceLocalImpl
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ProcessingMediaCardItem
import com.xayah.core.ui.model.ReportFileItemInfo
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.addInfo
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.toProcessingCardItem
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.DateUtil
import com.xayah.core.util.decodeURL
import com.xayah.core.util.localBackupSaveDir
import com.xayah.feature.main.medium.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val state: OperationState,
    val isTesting: Boolean,
    val cloudName: String,
    val cloudRemote: String,
    val medium: List<MediaEntity>,
    val mediumSize: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateFiles : IndexUiIntent()
    data class FinishSetup(val navController: NavController) : IndexUiIntent()
    data object Restore : IndexUiIntent()
    data object Initialize : IndexUiIntent()
    data object DestroyService : IndexUiIntent()
    data object TurnOffScreen : IndexUiIntent()
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val taskRepo: TaskRepository,
    private val mediaRepo: MediaRepository,
    private val cloudRepo: CloudRepository,
    private val localRestoreService: ProcessingServiceLocalImpl,
    private val cloudRestoreService: ProcessingServiceCloudImpl,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        state = OperationState.IDLE,
        isTesting = false,
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim() ?: "",
        medium = listOf(),
        mediumSize = "",
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateFiles -> {
                val medium = if (state.cloudName.isEmpty())
                    mediaRepo.queryActivated(OpType.RESTORE, "", context.localBackupSaveDir())
                else
                    mediaRepo.queryActivated(OpType.RESTORE, state.cloudName, state.cloudRemote)
                var bytes = 0.0
                medium.forEach {
                    bytes += it.displayStatsBytes
                }
                emitState(state.copy(medium = medium, mediumSize = bytes.formatSize()))
            }

            is IndexUiIntent.FinishSetup -> {
                if (state.cloudName.isNotEmpty()) {
                    emitState(state.copy(isTesting = true))
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffectOnIO(
                        IndexUiEffect.ShowSnackbar(
                            type = SnackbarType.Loading,
                            message = cloudRepo.getString(R.string.processing),
                            duration = SnackbarDuration.Indefinite,
                        )
                    )
                    runCatching {
                        cloudRepo.withClient(state.cloudName) { client, _ ->
                            client.testConnection()
                            emitEffect(IndexUiEffect.DismissSnackbar)
                            withMainContext {
                                intent.navController.popBackStack()
                                intent.navController.navigate(MainRoutes.MediumRestoreProcessing.route)
                            }
                        }
                    }.onFailure {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        if (it.localizedMessage != null)
                            emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                    }
                    emitState(state.copy(isTesting = false))
                } else {
                    withMainContext {
                        intent.navController.popBackStack()
                        intent.navController.navigate(MainRoutes.MediumRestoreProcessing.route)
                    }
                }
            }

            is IndexUiIntent.Initialize -> {
                _taskId.value = localRestoreService.initialize(state.cloudName, state.cloudRemote)
                _taskId.value = if (state.cloudName.isNotEmpty()) cloudRestoreService.initialize(state.cloudName, state.cloudRemote)
                else localRestoreService.initialize(state.cloudName, state.cloudRemote)
            }

            is IndexUiIntent.Restore -> {
                emitState(state.copy(state = OperationState.PROCESSING))
                if (state.cloudName.isNotEmpty()) {
                    // Cloud
                    cloudRestoreService.preprocessing()
                    cloudRestoreService.processing()
                    cloudRestoreService.postProcessing()
                    cloudRestoreService.destroyService()
                } else {
                    // Local
                    localRestoreService.preprocessing()
                    localRestoreService.processing()
                    localRestoreService.postProcessing()
                    localRestoreService.destroyService()
                }
                emitState(state.copy(state = OperationState.DONE))
            }

            is IndexUiIntent.DestroyService -> {
                if (state.cloudName.isNotEmpty()) {
                    // Cloud
                    cloudRestoreService.destroyService()
                } else {
                    // Local
                    localRestoreService.destroyService()
                }
            }

            is IndexUiIntent.TurnOffScreen -> {
                if (uiState.value.state == OperationState.PROCESSING) {
                    rootService.setScreenOffTimeout(Int.MAX_VALUE)
                    rootService.setDisplayPowerMode(SurfaceControlHidden.POWER_MODE_OFF)
                }
            }
        }
    }

    private val _taskId: MutableStateFlow<Long> = MutableStateFlow(-1)
    private var _task: Flow<TaskEntity?> = _taskId.flatMapLatest { id ->
        taskRepo.queryTaskFlow(id).flowOnIO()
    }
    private var _preItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryProcessingInfoFlow(id, ProcessingType.PREPROCESSING)
            .map { infoList ->
                val items = mutableListOf<ProcessingCardItem>()
                infoList.forEach {
                    items.addInfo(it)
                }
                items
            }
            .flowOnIO()
    }
    private val _postItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryProcessingInfoFlow(id, ProcessingType.POST_PROCESSING)
            .map { infoList ->
                val items = mutableListOf<ProcessingCardItem>()
                infoList.forEach {
                    items.addInfo(it)
                }
                items
            }
            .flowOnIO()
    }
    private val _mediaItems: Flow<List<ProcessingMediaCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryMediaFlow(id)
            .map { medium ->
                val items = mutableListOf<ProcessingMediaCardItem>()
                medium.map {
                    items.add(
                        ProcessingMediaCardItem(
                            title = StringResourceToken.fromString(it.mediaEntity.name),
                            name = it.mediaEntity.name,
                            items = listOf(it.mediaInfo.toProcessingCardItem)
                        )
                    )
                }
                items
            }
            .flowOnIO()
    }
    private val _mediaSize: Flow<String> = _taskId.flatMapLatest { id ->
        taskRepo.queryMediaFlow(id)
            .map { medium ->
                var bytes = 0.0
                medium.forEach {
                    bytes += it.mediaEntity.displayStatsBytes
                }
                bytes.formatSize()
            }
            .flowOnIO()
    }
    private val _mediaSucceed: Flow<List<ReportFileItemInfo>> = _taskId.flatMapLatest { id ->
        taskRepo.queryMediaFlow(id)
            .map { medium ->
                val firstIndex = medium.firstOrNull()?.id ?: 0
                medium.filter { it.state == OperationState.DONE }
                    .map {
                        ReportFileItemInfo(
                            name = it.mediaEntity.name,
                            index = (it.id - firstIndex + 1).toInt(),
                        )
                    }
            }
            .flowOnIO()
    }
    private val _mediaFailed: Flow<List<ReportFileItemInfo>> = _taskId.flatMapLatest { id ->
        taskRepo.queryMediaFlow(id)
            .map { medium ->
                val firstIndex = medium.firstOrNull()?.id ?: 0
                medium.filter { it.state == OperationState.ERROR }
                    .map {
                        ReportFileItemInfo(
                            name = it.mediaEntity.name,
                            index = (it.id - firstIndex + 1).toInt(),
                        )
                    }
            }
            .flowOnIO()
    }
    private val _timer: Flow<String> = _task.map { cur ->
        if (cur != null && cur.startTimestamp != 0L && cur.endTimestamp != 0L)
            DateUtil.getShortRelativeTimeSpanString(context, cur.startTimestamp, cur.endTimestamp)
        else
            DateUtil.getShortRelativeTimeSpanString(context, 0, 0)
    }.flowOnIO()
    private val _screenOffCountDown = context.readScreenOffCountDown().flowOnIO()

    var task: StateFlow<TaskEntity?> = _task.stateInScope(null)
    var preItems: StateFlow<List<ProcessingCardItem>> = _preItems.stateInScope(listOf())
    val postItems: StateFlow<List<ProcessingCardItem>> = _postItems.stateInScope(listOf())
    val mediaItems: StateFlow<List<ProcessingMediaCardItem>> = _mediaItems.stateInScope(listOf())
    val mediaSize: StateFlow<String> = _mediaSize.stateInScope("")
    val mediaSucceed: StateFlow<List<ReportFileItemInfo>> = _mediaSucceed.stateInScope(listOf())
    val mediaFailed: StateFlow<List<ReportFileItemInfo>> = _mediaFailed.stateInScope(listOf())
    val timer: StateFlow<String> = _timer.stateInScope(DateUtil.getShortRelativeTimeSpanString(context, 0, 0))
    val screenOffCountDown: StateFlow<Int> = _screenOffCountDown.stateInScope(0)
}
