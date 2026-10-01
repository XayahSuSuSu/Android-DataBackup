package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class RestoreProcessViewModel(
    restoreRepo: RestoreRepository,
    processRepo: RestoreProcessRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(RestoreProcessUiState())
    val uiState: StateFlow<RestoreProcessUiState> = _uiState.asStateFlow()
    val overallProgress: StateFlow<String> = uiState
        .map { calculateRestoreProgress(it.items) }
        .stateIn(
            scope = viewModelScope,
            initialValue = "0",
            started = SharingStarted.WhileSubscribed(5_000),
        )
    private var mIsCancelRequested = false

    init {
        // Consumed once: recreation after process death must not repeat destructive writes.
        val request = restoreRepo.consumeRestoreRequest()
        if (request == null) {
            _uiState.value = RestoreProcessUiState(
                status = RestoreProcessStatus.Failed,
                errorMessage = App.application.getString(R.string.restore_session_expired),
            )
        } else {
            _uiState.value = request.toInitialProcessUiState(restoreRepo.state.value.inventory)
            viewModelScope.launch {
                runCatching {
                    val finished = processRepo.restore(request, { mIsCancelRequested }) { event ->
                        _uiState.update { it.reduceRestoreProcessState(event) }
                    }
                    _uiState.update { it.toTerminalState(finished) }
                }.onFailure { error ->
                    if (error is CancellationException || error !is Exception) throw error
                    // Parser and Binder exceptions can contain private data. Show a localized summary.
                    _uiState.update { it.toFailedState(App.application.getString(R.string.restore_process_failed)) }
                }
            }
        }
    }

    fun cancel() {
        if (!uiState.value.isProcessing) return
        mIsCancelRequested = true
        _uiState.update { it.copy(status = RestoreProcessStatus.Canceling) }
    }
}

internal fun calculateRestoreProgress(items: List<RestoreProcessItem>): String {
    val currentIndex = items.sumOf { it.completedCount }
    val totalCount = items.sumOf { it.totalCount }
    return if (totalCount != 0) {
        ((currentIndex.toFloat() / totalCount) * 100).roundToInt().toString()
    } else {
        "0"
    }
}
