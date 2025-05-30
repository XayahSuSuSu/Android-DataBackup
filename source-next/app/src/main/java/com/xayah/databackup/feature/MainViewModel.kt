package com.xayah.databackup.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.WorkManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class UiState(
    val showErrorServiceDialog: Boolean = false
)

class MainViewModel : ViewModel() {
    private val mMutex = Mutex()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun checkRootService(onBind: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                mMutex.withLock {
                    _uiState.update { currentState ->
                        currentState.copy(showErrorServiceDialog = false)
                    }

                    RemoteRootService.setOnErrorEvent {
                        _uiState.update { currentState ->
                            currentState.copy(showErrorServiceDialog = true)
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
        }
    }

    fun initialize() {
        checkRootService {
            WorkManagerHelper.enqueueAppsUpdateWork()
        }
    }
}
