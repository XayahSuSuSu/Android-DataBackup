package com.xayah.databackup.feature

import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.WorkManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val showErrorServiceDialog: Boolean = false,
    val showNoSpaceLeftDialog: Boolean = false,
)

class MainViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun checkRootService(onBind: (() -> Unit)? = null) {
        withLock(Dispatchers.Default) {
            _uiState.update { currentState ->
                currentState.copy(showErrorServiceDialog = false)
            }

            RemoteRootService.setOnErrorEvent {
                _uiState.update { currentState ->
                    currentState.copy(showErrorServiceDialog = true)
                }
            }
            RemoteRootService.setOnNoSpaceLeftEvent {
                _uiState.update { currentState ->
                    currentState.copy(showNoSpaceLeftDialog = true)
                }
            }
            if (RemoteRootService.checkService()) {
                _uiState.update { currentState ->
                    currentState.copy(showErrorServiceDialog = false)
                }
                onBind?.invoke()
            }
        }
    }

    fun dismissNoSpaceLeftDialog() {
        _uiState.update { currentState ->
            currentState.copy(showNoSpaceLeftDialog = false)
        }
    }

    fun initialize() {
        checkRootService {
            WorkManagerHelper.enqueueOthersUpdateWork()
            WorkManagerHelper.enqueueAppsUpdateWork()
        }
    }
}
