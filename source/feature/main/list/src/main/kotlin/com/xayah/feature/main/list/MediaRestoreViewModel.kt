package com.xayah.feature.main.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.model.MediaKind
import com.xayah.core.model.ScannedMediaFile
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
class MediaRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filesRepo: FilesRepo,
) : ViewModel() {
    val uiState: StateFlow<MediaUiState> = filesRepo.getLocalRestoreMedia().map { entities ->
        val items = entities.mapNotNull { e ->
            filesRepo.kindOfMedia(e.path)?.let { kind -> Triple(e.id, e, kind) }
        }
        MediaUiState.Success(
            images = items.filter { it.third == MediaKind.Images }
                .map { ScannedMediaFile(path = it.second.path, name = it.second.name, kind = it.third) },
            videos = items.filter { it.third == MediaKind.Videos }
                .map { ScannedMediaFile(path = it.second.path, name = it.second.name, kind = it.third) },
            audio = items.filter { it.third == MediaKind.Audio }
                .map { ScannedMediaFile(path = it.second.path, name = it.second.name, kind = it.third) },
            selected = items.filter { it.second.extraInfo.activated }.map { it.second.path }.toSet(),
            idByPath = items.associate { it.second.path to it.first },
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = MediaUiState.Loading,
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
            if (state is MediaUiState.Success) {
                val id = state.idByPath[path] ?: return@launchOnDefault
                filesRepo.selectFile(id, state.selected.contains(path).not())
            }
        }
    }

    fun selectAll(kind: MediaKind) {
        viewModelScope.launchOnDefault {
            val state = uiState.value
            if (state is MediaUiState.Success) {
                val paths = when (kind) {
                    MediaKind.Images -> state.images
                    MediaKind.Videos -> state.videos
                    MediaKind.Audio -> state.audio
                }.map { it.path }
                filesRepo.selectAll(paths.mapNotNull { state.idByPath[it] })
            }
        }
    }

    fun unselectAll(kind: MediaKind) {
        viewModelScope.launchOnDefault {
            val state = uiState.value
            if (state is MediaUiState.Success) {
                val paths = when (kind) {
                    MediaKind.Images -> state.images
                    MediaKind.Videos -> state.videos
                    MediaKind.Audio -> state.audio
                }.map { it.path }
                filesRepo.unselectAll(paths.mapNotNull { state.idByPath[it] })
            }
        }
    }

    fun restoreSelected(navController: NavController) {
        viewModelScope.launchOnDefault {
            val selected = (uiState.value as? MediaUiState.Success)?.selected.orEmpty()
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

    suspend fun loadThumbnail(path: String): String? =
        runCatching { filesRepo.getMediaThumbnail(path) }.getOrNull()
}
