package com.xayah.feature.main.settings.blacklist

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val selectAll: Boolean,
    val appIds: Set<Long>,
    val fileIds: Set<Long>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data class SelectApp(val id: Long) : IndexUiIntent()
    data class SelectFile(val id: Long) : IndexUiIntent()
    data object SelectAll : IndexUiIntent()
    data object RemoveSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRepo: PackageRepository,
    private val mediaRepo: MediaRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        selectAll = false,
        appIds = setOf(),
        fileIds = setOf()
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.SelectApp -> {
                if (intent.id in state.appIds) {
                    emitState(state.copy(appIds = state.appIds.toMutableSet().apply { remove(intent.id) }))
                } else {
                    emitState(state.copy(appIds = state.appIds.toMutableSet().apply { add(intent.id) }))
                }
            }

            is IndexUiIntent.SelectFile -> {
                if (intent.id in state.fileIds) {
                    emitState(state.copy(fileIds = state.fileIds.toMutableSet().apply { remove(intent.id) }))
                } else {
                    emitState(state.copy(fileIds = state.fileIds.toMutableSet().apply { add(intent.id) }))
                }
            }

            is IndexUiIntent.SelectAll -> {
                emitState(
                    state.copy(
                        selectAll = state.selectAll.not(),
                        appIds = if (state.selectAll.not()) packagesState.value.map { it.id }.toSet() else setOf(),
                        fileIds = if (state.selectAll.not()) mediumState.value.map { it.id }.toSet() else setOf(),
                    )
                )
            }

            is IndexUiIntent.RemoveSelected -> {
                val appIds = state.appIds.toMutableSet()
                state.appIds.forEach {
                    packageRepo.setBlocked(it, false)
                    appIds.remove(it)
                }
                val fileIds = state.fileIds.toMutableSet()
                state.fileIds.forEach {
                    mediaRepo.setBlocked(it, false)
                    fileIds.remove(it)
                }
                emitState(state.copy(appIds = appIds, fileIds = fileIds))
            }
        }
    }

    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(opType = OpType.BACKUP, blocked = true).flowOnIO()
    val packagesState: StateFlow<List<PackageEntity>> = _packages.stateInScope(listOf())

    private val _medium: Flow<List<MediaEntity>> = mediaRepo.queryFlow(opType = OpType.BACKUP, blocked = true).flowOnIO()
    val mediumState: StateFlow<List<MediaEntity>> = _medium.stateInScope(listOf())
}
