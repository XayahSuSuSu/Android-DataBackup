package com.xayah.feature.main.medium.restore.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.decodeURL
import com.xayah.core.util.encodeURL
import com.xayah.core.util.navigateSingle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class IndexUiState(
    val cloudName: String,
    val cloudRemote: String,
    val selectAll: Boolean,
    val uuid: UUID,
    val isLoading: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data object ClearKey : IndexUiIntent()
    data class Select(val entity: MediaEntity) : IndexUiIntent()
    data class SelectAll(val selected: Boolean) : IndexUiIntent()
    data class ToPageDetail(val navController: NavHostController, val mediaEntity: MediaEntity) : IndexUiIntent()
    data class ToPageSetup(val navController: NavHostController) : IndexUiIntent()
    data object DeleteSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val rootService: RemoteRootService,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim() ?: "",
        selectAll = false,
        uuid = UUID.randomUUID(),
        isLoading = false
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitState(state.copy(isLoading = true))
                withIOContext {
                    if (state.cloudName.isEmpty()) {
                        // Local
                        mediaRepo.loadMediumFromLocal()
                    } else {
                        mediaRepo.loadMediumFromCloud(state.cloudName)
                    }
                }
                emitState(state.copy(isLoading = false, uuid = UUID.randomUUID()))
            }

            is IndexUiIntent.Sort -> {
                var type = intent.type
                val index = intent.index
                if (_sortIndexState.value == index) {
                    type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                    _sortTypeState.value = type
                }
                _sortIndexState.value = index
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.ClearKey -> {
                _keyState.value = ""
            }

            is IndexUiIntent.Select -> {
                mediaRepo.upsert(intent.entity.copy(extraInfo = intent.entity.extraInfo.copy(activated = intent.entity.extraInfo.activated.not())))
            }

            is IndexUiIntent.SelectAll -> {
                mediaRepo.upsert(mediumState.value.onEach { if (it.enabled) it.extraInfo.activated = intent.selected })
            }

            is IndexUiIntent.ToPageDetail -> {
                val entity = intent.mediaEntity
                withMainContext {
                    intent.navController.navigateSingle(MainRoutes.MediumRestoreDetail.getRoute(entity.name.encodeURL(), entity.preserveId))
                }
            }

            is IndexUiIntent.ToPageSetup -> {
                withMainContext {
                    intent.navController.navigateSingle(
                        MainRoutes.MediumRestoreProcessingGraph.getRoute(
                            state.cloudName.ifEmpty { " " }.encodeURL(),
                            state.cloudRemote.encodeURL()
                        )
                    )
                }
            }

            is IndexUiIntent.DeleteSelected -> {
                val packages = mediaRepo.queryActivated(OpType.RESTORE)
                packages.forEach {
                    mediaRepo.delete(it)
                }
                rootService.clearEmptyDirectoriesRecursively(mediaRepo.backupFilesDir)
            }
        }
    }

    private val _medium: Flow<List<MediaEntity>> = mediaRepo.queryFlow(OpType.RESTORE, uiState.value.cloudName, uiState.value.cloudRemote).flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    private val _mediumState: Flow<List<MediaEntity>> =
        combine(_medium, _keyState, _sortIndexState, _sortTypeState) { medium, key, sortIndex, sortType ->
            medium.filter(mediaRepo.getKeyPredicateNew(key = key))
                .sortedWith(mediaRepo.getSortComparatorNew(sortIndex = sortIndex, sortType = sortType))
                .sortedByDescending { it.extraInfo.activated }
        }
    private val _srcMediumEmptyState: Flow<Boolean> = _medium.map { medium -> medium.isEmpty() }.flowOnIO()
    private val _mediumSelectedState: Flow<Int> = _mediumState.map { medium -> medium.count { it.extraInfo.activated } }.flowOnIO()

    val mediumState: StateFlow<List<MediaEntity>> = _mediumState.stateInScope(listOf())
    val mediumSelectedState: StateFlow<Int> = _mediumSelectedState.stateInScope(0)
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcMediumEmptyState: StateFlow<Boolean> = _srcMediumEmptyState.stateInScope(true)
}
