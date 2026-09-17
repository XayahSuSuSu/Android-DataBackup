package com.xayah.feature.main.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.ifEmptyEncodeURLWithSpace
import com.xayah.core.util.launchOnDefault
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.navigateSingle
import com.xayah.core.work.WorkManagerInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WifiRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filesRepo: FilesRepo,
) : ViewModel() {
    val uiState: StateFlow<WifiUiState> = filesRepo.getLocalRestoreWifi().map { entities ->
        WifiUiState.Success(
            items = entities.map { WifiItem(name = it.name, path = it.path) },
            selected = entities.filter { it.extraInfo.activated }.map { it.path }.toSet(),
            idByPath = entities.associate { it.path to it.id },
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = WifiUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun reload() {
        viewModelScope.launchOnDefault {
            WorkManagerInitializer.loadFileBackups(context, "", "")
        }
    }

    fun toggle(path: String) {
        viewModelScope.launchOnDefault {
            val state = uiState.value
            if (state is WifiUiState.Success) {
                val id = state.idByPath[path] ?: return@launchOnDefault
                filesRepo.selectFile(id, state.selected.contains(path).not())
            }
        }
    }

    fun selectAll() {
        viewModelScope.launchOnDefault {
            val state = uiState.value
            if (state is WifiUiState.Success) {
                filesRepo.selectAll(state.items.mapNotNull { state.idByPath[it.path] })
            }
        }
    }

    fun unselectAll() {
        viewModelScope.launchOnDefault {
            val state = uiState.value
            if (state is WifiUiState.Success) {
                filesRepo.unselectAll(state.items.mapNotNull { state.idByPath[it.path] })
            }
        }
    }

    fun restoreSelected(navController: NavController) {
        viewModelScope.launchOnDefault {
            val selected = (uiState.value as? WifiUiState.Success)?.selected.orEmpty()
            if (selected.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    navController.navigateSingle(
                        MainRoutes.MediumRestoreProcessingGraph.getRoute(
                            cloudName = "".ifEmptyEncodeURLWithSpace(),
                            backupDir = context.localBackupSaveDir().ifEmptyEncodeURLWithSpace(),
                        )
                    )
                }
            }
        }
    }
}
