package com.xayah.feature.main.task.detail.packages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val taskId: Long,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    args: SavedStateHandle,
    rootService: RemoteRootService,
    taskRepository: TaskRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(taskId = args.get<String>(MainRoutes.ArgTaskId)?.toLongOrNull() ?: 0)) {
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
            }
        }
    }

    private val _task: Flow<TaskEntity?> = taskRepository.queryTaskFlow(uiState.value.taskId).flowOnIO()
    val taskState: StateFlow<TaskEntity?> = _task.stateInScope(null)
    private val _internalTimer: Flow<String> = flow {
        while (true) {
            delay(1000)
            emit(taskRepository.getShortRelativeTimeSpanString(taskState.value?.startTimestamp ?: 0, DateUtil.getTimestamp()))
            if (taskState.value?.isProcessing?.not() == true) break
        }
    }.flowOnIO()
    val internalTimerState: StateFlow<String> = _internalTimer.stateInScope("")
    private val _taskTimer: Flow<String> = _task.map {
        taskRepository.getShortRelativeTimeSpanString(it?.startTimestamp ?: 0, it?.endTimestamp ?: 0)
    }.flowOnIO()
    val taskTimerState: StateFlow<String> = _taskTimer.stateInScope("")
    private val _taskProcessingDetails: Flow<List<TaskDetailPackageEntity>> =
        taskRepository.queryProcessingFlow<TaskDetailPackageEntity>(uiState.value.taskId, TaskType.PACKAGE).flowOnIO()
    private val _taskSuccessDetails: Flow<List<TaskDetailPackageEntity>> =
        taskRepository.querySuccessFlow<TaskDetailPackageEntity>(uiState.value.taskId, TaskType.PACKAGE).flowOnIO()
    private val _taskFailureDetails: Flow<List<TaskDetailPackageEntity>> =
        taskRepository.queryFailureFlow<TaskDetailPackageEntity>(uiState.value.taskId, TaskType.PACKAGE).flowOnIO()
    val taskProcessingDetailsState: StateFlow<List<TaskDetailPackageEntity>> = _taskProcessingDetails.stateInScope(listOf())
    val taskSuccessDetailsState: StateFlow<List<TaskDetailPackageEntity>> = _taskSuccessDetails.stateInScope(listOf())
    val taskFailureDetailsState: StateFlow<List<TaskDetailPackageEntity>> = _taskFailureDetails.stateInScope(listOf())
}
