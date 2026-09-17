package com.xayah.feature.main.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.launchOnDefault
import com.xayah.core.util.navigateSingle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WifiItem(
    val name: String,
    val path: String,
)

sealed interface WifiUiState {
    data object Loading : WifiUiState
    data class Success(
        val items: List<WifiItem>,
        val selected: Set<String>,
        val idByPath: Map<String, Long> = mapOf(),
    ) : WifiUiState
}

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val filesRepo: FilesRepo,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WifiUiState>(WifiUiState.Loading)
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launchOnDefault {
            _uiState.value = WifiUiState.Loading
            val found = runCatching { filesRepo.scanWifiConfigs() }.getOrDefault(listOf())
            _uiState.value = WifiUiState.Success(
                items = found.map { (name, path) -> WifiItem(name = name, path = path) },
                selected = setOf(),
            )
        }
    }

    fun toggle(path: String) {
        val state = _uiState.value
        if (state is WifiUiState.Success) {
            val selected = state.selected.toMutableSet()
            if (selected.contains(path)) selected.remove(path) else selected.add(path)
            _uiState.value = state.copy(selected = selected)
        }
    }

    fun selectAll() {
        val state = _uiState.value
        if (state is WifiUiState.Success) {
            _uiState.value = state.copy(selected = state.items.map { it.path }.toSet())
        }
    }

    fun unselectAll() {
        val state = _uiState.value
        if (state is WifiUiState.Success) {
            _uiState.value = state.copy(selected = setOf())
        }
    }

    fun backupSelected(navController: NavController) {
        viewModelScope.launchOnDefault {
            val selected = (_uiState.value as? WifiUiState.Success)?.selected?.toList().orEmpty()
            if (selected.isNotEmpty()) {
                filesRepo.prepareMediaBackup(selected)
                withContext(Dispatchers.Main) {
                    navController.navigateSingle(MainRoutes.MediumBackupProcessingGraph.route)
                }
            }
        }
    }
}
