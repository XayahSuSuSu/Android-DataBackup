package com.xayah.feature.main.medium.backup.processing

import android.content.Context
import android.view.SurfaceControlHidden
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.readScreenOffCountDown
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.StorageMode
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.network.client.getCloud
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.medium.backup.ProcessingServiceCloudImpl
import com.xayah.core.service.medium.backup.ProcessingServiceLocalImpl
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ProcessingMediaCardItem
import com.xayah.core.ui.model.ReportFileItemInfo
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.addInfo
import com.xayah.core.ui.util.toProcessingCardItem
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.DateUtil
import com.xayah.core.util.navigateSingle
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
    val medium: List<MediaEntity>,
    val mediumSize: String,
    val storageIndex: Int,
    val storageType: StorageMode,
    val cloudEntity: CloudEntity?,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateFiles : IndexUiIntent()
    data class SetCloudEntity(val name: String) : IndexUiIntent()
    data class FinishSetup(val navController: NavController) : IndexUiIntent()
    data object Backup : IndexUiIntent()
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
    private val localBackupService: ProcessingServiceLocalImpl,
    private val cloudBackupService: ProcessingServiceCloudImpl,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        state = OperationState.IDLE,
        isTesting = false,
        medium = listOf(),
        mediumSize = "",
        storageIndex = 0,
        storageType = StorageMode.Local,
        cloudEntity = null
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
                val medium = mediaRepo.queryActivated(OpType.BACKUP)
                var bytes = 0.0
                medium.forEach {
                    bytes += it.displayStatsBytes
                }
                emitState(state.copy(medium = medium, mediumSize = bytes.formatSize()))
            }

            is IndexUiIntent.SetCloudEntity -> {
                context.saveCloudActivatedAccountName(intent.name)
                emitState(state.copy(cloudEntity = cloudRepo.queryByName(intent.name)))
            }

            is IndexUiIntent.FinishSetup -> {
                if (state.storageType == StorageMode.Cloud) {
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
                        val client = state.cloudEntity!!.getCloud()
                        client.testConnection()
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        withMainContext {
                            intent.navController.popBackStack()
                            intent.navController.navigateSingle(MainRoutes.MediumBackupProcessing.route)
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
                        intent.navController.navigateSingle(MainRoutes.MediumBackupProcessing.route)
                    }
                }
            }

            is IndexUiIntent.Initialize -> {
                _taskId.value = if (state.cloudEntity != null) cloudBackupService.initialize()
                else localBackupService.initialize()
            }

            is IndexUiIntent.Backup -> {
                emitState(state.copy(state = OperationState.PROCESSING))
                if (state.cloudEntity != null) {
                    // Cloud
                    cloudBackupService.preprocessing()
                    cloudBackupService.processing()
                    cloudBackupService.postProcessing()
                    cloudBackupService.destroyService()
                } else {
                    // Local
                    localBackupService.preprocessing()
                    localBackupService.processing()
                    localBackupService.postProcessing()
                    localBackupService.destroyService()
                }
                emitState(state.copy(state = OperationState.DONE))
            }

            is IndexUiIntent.DestroyService -> {
                if (state.cloudEntity != null) {
                    // Cloud
                    cloudBackupService.destroyService(true)
                } else {
                    // Local
                    localBackupService.destroyService(true)
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
                            title = it.mediaEntity.name,
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
    private val _accounts: Flow<List<DialogRadioItem<Any>>> = cloudRepo.clouds.map { entities ->
        entities.map {
            DialogRadioItem(
                enum = Any(),
                title = it.name,
                desc = it.user,
            )
        }

    }.flowOnIO()
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
    val accounts: StateFlow<List<DialogRadioItem<Any>>> = _accounts.stateInScope(listOf())
    val timer: StateFlow<String> = _timer.stateInScope(DateUtil.getShortRelativeTimeSpanString(context, 0, 0))
    val screenOffCountDown: StateFlow<Int> = _screenOffCountDown.stateInScope(0)
}
