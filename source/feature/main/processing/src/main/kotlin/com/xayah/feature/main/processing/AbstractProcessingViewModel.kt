package com.xayah.feature.main.processing

import android.content.Context
import android.view.SurfaceControlHidden
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.datastore.readScreenOffCountDown
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.StorageMode
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.AbstractProcessingServiceProxy
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ProcessingDataCardItem
import com.xayah.core.ui.util.addInfo
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class IndexUiState(
    val state: OperationState,
    val storageIndex: Int,
    val storageType: StorageMode,
    val cloudEntity: CloudEntity?,
) : UiState

open class ProcessingUiIntent : UiIntent {
    data object Process : ProcessingUiIntent()
    data object Initialize : ProcessingUiIntent()
    data object DestroyService : ProcessingUiIntent()
    data object TurnOffScreen : ProcessingUiIntent()
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
abstract class AbstractProcessingViewModel(
    @ApplicationContext private val mContext: Context,
    private val mRootService: RemoteRootService,
    private val mTaskRepo: TaskRepository,
    private val mLocalService: AbstractProcessingServiceProxy,
    private val mCloudService: AbstractProcessingServiceProxy,
) : BaseViewModel<IndexUiState, ProcessingUiIntent, IndexUiEffect>(
    IndexUiState(
        state = OperationState.IDLE,
        storageIndex = 0,
        storageType = StorageMode.Local,
        cloudEntity = null
    )
) {
    open suspend fun onOtherEvent(state: IndexUiState, intent: ProcessingUiIntent) {}

    init {
        mRootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: ProcessingUiIntent) {
        when (intent) {
            is ProcessingUiIntent.Initialize -> {
                _taskId.value = if (uiState.value.storageType == StorageMode.Cloud) mCloudService.initialize() else mLocalService.initialize()
            }

            is ProcessingUiIntent.Process -> {
                emitState(state.copy(state = OperationState.PROCESSING))
                if (state.storageType == StorageMode.Cloud) {
                    // Cloud
                    mCloudService.preprocessing()
                    mCloudService.processing()
                    mCloudService.postProcessing()
                    mCloudService.destroyService()
                } else {
                    // Local
                    mLocalService.preprocessing()
                    mLocalService.processing()
                    mLocalService.postProcessing()
                    mLocalService.destroyService()
                }
                emitState(state.copy(state = OperationState.DONE))
            }

            is ProcessingUiIntent.DestroyService -> {
                if (state.storageType == StorageMode.Cloud) {
                    // Cloud
                    mCloudService.destroyService(true)
                } else {
                    // Local
                    mLocalService.destroyService(true)
                }
            }

            is ProcessingUiIntent.TurnOffScreen -> {
                if (uiState.value.state == OperationState.PROCESSING) {
                    mRootService.setScreenOffTimeout(Int.MAX_VALUE)
                    mRootService.setDisplayPowerMode(SurfaceControlHidden.POWER_MODE_OFF)
                }
            }

            else -> {
                onOtherEvent(state, intent)
            }
        }
    }

    protected abstract val _dataItems: Flow<List<ProcessingDataCardItem>>

    protected val _taskId: MutableStateFlow<Long> = MutableStateFlow(-1)
    private var _task: Flow<TaskEntity?> = _taskId.flatMapLatest { id -> mTaskRepo.queryTaskFlow(id).flowOnIO() }
    private val _preItemsProgress: MutableStateFlow<Float> = MutableStateFlow(0F)
    private var _preItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        mTaskRepo.queryProcessingInfoFlow(id, ProcessingType.PREPROCESSING)
            .map { infoList ->
                _preItemsProgress.value = infoList.sumOf { it.progress.toDouble() }.toFloat() / infoList.size
                val items = mutableListOf<ProcessingCardItem>()
                infoList.forEach {
                    items.addInfo(it)
                }
                items
            }
            .flowOnIO()
    }
    private val _postItemsProgress: MutableStateFlow<Float> = MutableStateFlow(0F)
    private val _postItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        mTaskRepo.queryProcessingInfoFlow(id, ProcessingType.POST_PROCESSING)
            .map { infoList ->
                _postItemsProgress.value = infoList.sumOf { it.progress.toDouble() }.toFloat() / infoList.size
                val items = mutableListOf<ProcessingCardItem>()
                infoList.forEach {
                    items.addInfo(it)
                }
                items
            }
            .flowOnIO()
    }
    private val _screenOffCountDown = mContext.readScreenOffCountDown().flowOnIO()

    val task: StateFlow<TaskEntity?> = _task.stateInScope(null)
    val preItemsProgress: StateFlow<Float> = _preItemsProgress.stateInScope(0F)
    val preItems: StateFlow<List<ProcessingCardItem>> = _preItems.stateInScope(listOf())
    val dataItems: StateFlow<List<ProcessingDataCardItem>> by lazy { _dataItems.stateInScope(listOf()) }
    val postItemsProgress: StateFlow<Float> = _postItemsProgress.stateInScope(0F)
    val postItems: StateFlow<List<ProcessingCardItem>> = _postItems.stateInScope(listOf())
    val screenOffCountDown: StateFlow<Int> = _screenOffCountDown.stateInScope(0)
}
