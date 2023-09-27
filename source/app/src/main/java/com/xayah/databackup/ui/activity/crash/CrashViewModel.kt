package com.xayah.databackup.ui.activity.crash

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class CrashUiState(
    val text: String,
)

@ExperimentalMaterial3Api
@HiltViewModel
class CrashViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf(CrashUiState(text = ""))
    val uiState: State<CrashUiState>
        get() = _uiState

    fun setText(text: String) {
        _uiState.value = uiState.value.copy(text = text)
    }
}
