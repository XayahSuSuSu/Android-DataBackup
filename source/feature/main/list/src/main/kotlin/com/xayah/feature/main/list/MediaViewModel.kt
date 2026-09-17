package com.xayah.feature.main.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.model.MediaKind
import com.xayah.core.model.ScannedMediaFile
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

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val filesRepo: FilesRepo,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launchOnDefault {
            _uiState.value = MediaUiState.Loading
            val files = runCatching { filesRepo.scanMediaFiles() }.getOrDefault(listOf())
            _uiState.value = MediaUiState.Success(
                images = files.filter { it.kind == MediaKind.Images },
                videos = files.filter { it.kind == MediaKind.Videos },
                audio = files.filter { it.kind == MediaKind.Audio },
                selected = setOf(),
            )
        }
    }

    fun toggle(path: String) {
        val state = _uiState.value
        if (state is MediaUiState.Success) {
            val selected = state.selected.toMutableSet()
            if (selected.contains(path)) selected.remove(path) else selected.add(path)
            _uiState.value = state.copy(selected = selected)
        }
    }

    fun selectAll(kind: MediaKind) {
        val state = _uiState.value
        if (state is MediaUiState.Success) {
            val paths = when (kind) {
                MediaKind.Images -> state.images
                MediaKind.Videos -> state.videos
                MediaKind.Audio -> state.audio
            }.map { it.path }
            _uiState.value = state.copy(selected = state.selected + paths)
        }
    }

    fun unselectAll(kind: MediaKind) {
        val state = _uiState.value
        if (state is MediaUiState.Success) {
            val paths = when (kind) {
                MediaKind.Images -> state.images
                MediaKind.Videos -> state.videos
                MediaKind.Audio -> state.audio
            }.map { it.path }.toSet()
            _uiState.value = state.copy(selected = state.selected - paths)
        }
    }

    fun backupSelected(navController: NavController) {
        viewModelScope.launchOnDefault {
            val selected = (uiState.value as? MediaUiState.Success)?.selected?.toList().orEmpty()
            if (selected.isNotEmpty()) {
                filesRepo.prepareMediaBackup(selected)
                withContext(Dispatchers.Main) {
                    navController.navigateSingle(MainRoutes.MediumBackupProcessingGraph.route)
                }
            }
        }
    }

    suspend fun loadThumbnail(path: String): String? =
        runCatching { filesRepo.getMediaThumbnail(path) }.getOrNull()
}

sealed interface MediaUiState {
    data object Loading : MediaUiState
    data class Success(
        val images: List<ScannedMediaFile>,
        val videos: List<ScannedMediaFile>,
        val audio: List<ScannedMediaFile>,
        val selected: Set<String>,
        val idByPath: Map<String, Long> = mapOf(),
    ) : MediaUiState {
        val total: Int get() = images.size + videos.size + audio.size
    }
}
