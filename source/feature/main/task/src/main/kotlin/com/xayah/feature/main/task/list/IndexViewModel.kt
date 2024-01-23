package com.xayah.feature.main.task.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.ui.route.MainRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data class ToPageTaskDetail(val navController: NavHostController, val taskEntity: TaskEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    taskRepository: TaskRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.ToPageTaskDetail -> {
                val entity = intent.taskEntity
                withMainContext {
                    when (entity.taskType) {
                        TaskType.PACKAGE -> {
                            intent.navController.navigate(MainRoutes.TaskPackageDetail.getRoute(entity.id))
                        }

                        TaskType.MEDIA -> {
                            intent.navController.navigate(MainRoutes.TaskMediaDetail.getRoute(entity.id))
                        }
                    }
                }
            }
        }
    }

    private val _tasks: Flow<List<TaskEntity>> = taskRepository.tasks.map { it.reversed() }.flowOnIO()
    private val _processingTasks: Flow<List<TaskEntity>> = _tasks.map { task -> task.filter { it.isProcessing } }.flowOnIO()
    private val _finishedTasks: Flow<List<TaskEntity>> = _tasks.map { task -> task.filter { it.isProcessing.not() } }.flowOnIO()
    val tasksProcessingState: StateFlow<List<TaskEntity>> = _processingTasks.stateInScope(listOf())
    val tasksFinishedState: StateFlow<List<TaskEntity>> = _finishedTasks.stateInScope(listOf())
}
