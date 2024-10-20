package com.xayah.feature.main.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.model.database.TaskEntity
import com.xayah.feature.main.history.HistoryUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    taskRepo: TaskRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = taskRepo.queryTasksFlow().map { HistoryUiState.Success(it) }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(
        val items: List<TaskEntity>
    ) : HistoryUiState

    data object Error : HistoryUiState
}
