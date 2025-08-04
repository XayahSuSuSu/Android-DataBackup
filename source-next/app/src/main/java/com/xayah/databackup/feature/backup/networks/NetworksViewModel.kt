package com.xayah.databackup.feature.backup.networks

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.database.entity.unmarshall
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class UiState(
    val showPassword: Boolean = false,
)

open class NetworksViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val networks = combine(
        DatabaseHelper.networkDao.loadFlowNetworks().unmarshall(),
        _searchText,
    ) { networks, searchText ->
        networks.filter(searchText)
    }.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selected =
        networks.map { list -> list.count { it.selected } }.stateIn(
            scope = viewModelScope,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }

    fun showOrHidePassword() {
        withLock(Dispatchers.Default) {
            _uiState.emit(uiState.value.copy(showPassword = uiState.value.showPassword.not()))
        }
    }
}
