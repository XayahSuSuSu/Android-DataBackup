package com.xayah.databackup.feature.restore.networks

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.database.entity.NetworkUnmarshalled
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.filterNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class UiState(val showPassword: Boolean = false)

class NetworksViewModel(
    private val mRestoreRepo: RestoreRepository,
) : BaseViewModel() {
    private val mSharingStarted = SharingStarted.WhileSubscribed(5_000)
    val state: StateFlow<RestoreState> = mRestoreRepo.state
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val items: StateFlow<Map<String, NetworkUnmarshalled>> = combine(state, searchText) { state, query ->
        state.inventory?.networks.orEmpty().filterNetwork(query)
    }.stateIn(viewModelScope, mSharingStarted, state.value.inventory?.networks.orEmpty())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val showPassword: StateFlow<Boolean> = uiState.map { it.showPassword }.stateIn(viewModelScope, mSharingStarted, false)

    fun showOrHidePassword() {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(showPassword = !it.showPassword) }
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }

    fun selectItem(id: String, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectItem(id, checked)
        }
    }

    fun selectAll() {
        withLock(Dispatchers.Default) {
            val visibleItems = items.value
            mRestoreRepo.selectItems(visibleItems.keys, visibleItems.keys.any { it !in state.value.selected })
        }
    }
}
