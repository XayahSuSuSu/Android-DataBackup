package com.xayah.feature.main.packages.restore.processing

import android.content.Context
import android.view.SurfaceControlHidden
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.readScreenOffCountDown
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.packages.restore.ProcessingServiceCloudImpl
import com.xayah.core.service.packages.restore.ProcessingServiceLocalImpl
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ProcessingPackageCardItem
import com.xayah.core.ui.model.ReportAppItemInfo
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
import com.xayah.core.util.localBackupSaveDir
import com.xayah.feature.main.packages.R
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
    val packages: List<PackageEntity>,
    val packagesSize: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateApps : IndexUiIntent()
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
    private val pkgRepo: PackageRepository,
    private val cloudRepo: CloudRepository,
    private val localRestoreService: ProcessingServiceLocalImpl,
    private val cloudRestoreService: ProcessingServiceCloudImpl,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        state = OperationState.IDLE,
        isTesting = false,
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.trim() ?: "",
        packages = listOf(),
        packagesSize = "",
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
            is IndexUiIntent.UpdateApps -> {
                val packages = pkgRepo.filterRestore(
                    if (state.cloudName.isEmpty())
                        pkgRepo.queryActivated(OpType.RESTORE, "", context.localBackupSaveDir())
                    else
                        pkgRepo.queryActivated(OpType.RESTORE, state.cloudName, state.cloudRemote)
                )
                var bytes = 0.0
                packages.forEach {
                    bytes += it.storageStatsBytes
                }
                emitState(state.copy(packages = packages, packagesSize = bytes.formatSize()))
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
                                intent.navController.navigate(MainRoutes.PackagesRestoreProcessing.route)
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
                        intent.navController.navigate(MainRoutes.PackagesRestoreProcessing.route)
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
    private val _packageItems: Flow<List<ProcessingPackageCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackageFlow(id)
            .map { packages ->
                val items = mutableListOf<ProcessingPackageCardItem>()
                packages.map {
                    items.add(
                        ProcessingPackageCardItem(
                            title = StringResourceToken.fromString(it.packageEntity.packageInfo.label),
                            packageName = it.packageEntity.packageName,
                            items = listOf(
                                it.apkInfo.toProcessingCardItem,
                                it.userInfo.toProcessingCardItem,
                                it.userDeInfo.toProcessingCardItem,
                                it.dataInfo.toProcessingCardItem,
                                it.obbInfo.toProcessingCardItem,
                                it.mediaInfo.toProcessingCardItem,
                            )
                        )
                    )
                }
                items
            }
            .flowOnIO()
    }
    private val _packageSize: Flow<String> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackageFlow(id)
            .map { packages ->
                var bytes = 0.0
                packages.forEach {
                    bytes += it.packageEntity.storageStatsBytes
                }
                bytes.formatSize()
            }
            .flowOnIO()
    }
    private val _packageSucceed: Flow<List<ReportAppItemInfo>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackageFlow(id)
            .map { packages ->
                val firstIndex = packages.firstOrNull()?.id ?: 0
                packages.filter { it.state == OperationState.DONE }
                    .map {
                        ReportAppItemInfo(
                            packageName = it.packageEntity.packageName,
                            index = (it.id - firstIndex + 1).toInt(),
                            label = it.packageEntity.packageInfo.label.ifEmpty { context.getString(R.string.unknown) },
                            user = "${context.getString(R.string.user)}: ${it.packageEntity.userId}"
                        )
                    }
            }
            .flowOnIO()
    }
    private val _packageFailed: Flow<List<ReportAppItemInfo>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackageFlow(id)
            .map { packages ->
                val firstIndex = packages.firstOrNull()?.id ?: 0
                packages.filter { it.state == OperationState.ERROR }
                    .map {
                        ReportAppItemInfo(
                            packageName = it.packageEntity.packageName,
                            index = (it.id - firstIndex + 1).toInt(),
                            label = it.packageEntity.packageInfo.label.ifEmpty { context.getString(R.string.unknown) },
                            user = "${context.getString(R.string.user)}: ${it.packageEntity.userId}"
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
    val packageItems: StateFlow<List<ProcessingPackageCardItem>> = _packageItems.stateInScope(listOf())
    val packageSize: StateFlow<String> = _packageSize.stateInScope("")
    val packageSucceed: StateFlow<List<ReportAppItemInfo>> = _packageSucceed.stateInScope(listOf())
    val packageFailed: StateFlow<List<ReportAppItemInfo>> = _packageFailed.stateInScope(listOf())
    val timer: StateFlow<String> = _timer.stateInScope(DateUtil.getShortRelativeTimeSpanString(context, 0, 0))
    val screenOffCountDown: StateFlow<Int> = _screenOffCountDown.stateInScope(0)
}
