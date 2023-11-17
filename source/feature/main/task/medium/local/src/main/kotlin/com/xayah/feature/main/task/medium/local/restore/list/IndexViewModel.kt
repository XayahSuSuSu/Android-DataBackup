package com.xayah.feature.main.task.medium.local.restore.list

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.MediaRestoreRepository
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.DateUtil
import com.xayah.feature.main.task.medium.common.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class IndexUiState(
    val shimmerCount: Int = ConstantUtil.DefaultMediaList.size,
    val emphasizedState: Boolean = false,
    val batchSelection: List<String> = listOf(),
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Initialize : IndexUiIntent()
    object Update : IndexUiIntent()
    data class UpdateMedia(val entity: MediaRestoreEntity) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    object Emphasize : IndexUiIntent()
    object BatchingSelectAll : IndexUiIntent()
    data class BatchingSelect(val path: String) : IndexUiIntent()
    data class BatchSelectOp(val selected: Boolean, val pathList: List<String>) : IndexUiIntent()
    data class SelectTimestamp(val index: Int) : IndexUiIntent()
    data class UpdatePackageState(val entity: MediaRestoreEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val mediaRestoreRepository: MediaRestoreRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                _shimmeringState.value = false
                mediaRestoreRepository.loadLocalConfig(topBarState = _topBarState)
                emitIntentSuspend(IndexUiIntent.Update)
            }

            is IndexUiIntent.Update -> {
                // Inactivate all packages then activate displayed ones.
                mediaRestoreRepository.updateActive(active = false)
                mediaRestoreRepository.updateActive(
                    active = true,
                    timestamp = timestampState.value,
                    savePath = mediaRestoreRepository.restoreSavePath.first()
                )
            }

            is IndexUiIntent.UpdateMedia -> {
                mediaRestoreRepository.upsertRestore(intent.entity)
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.Emphasize -> {
                emitState(state.copy(emphasizedState = state.emphasizedState.not()))
            }

            is IndexUiIntent.BatchingSelectAll -> {
                var batchSelection: List<String> = listOf()
                if (state.batchSelection.isEmpty()) {
                    batchSelection = mediumState.first().filter { it.isExists }.map { it.path }
                }
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchingSelect -> {
                val batchSelection = state.batchSelection.toMutableList()
                if (intent.path in batchSelection)
                    batchSelection.remove(intent.path)
                else
                    batchSelection.add(intent.path)
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchSelectOp -> {
                mediaRestoreRepository.batchSelectOp(selected = intent.selected, timestamp = _timestampState.value, pathList = intent.pathList)
            }

            is IndexUiIntent.SelectTimestamp -> {
                _timestampState.value = _timestampListState.first().getOrNull(intent.index) ?: 0
                emitIntentSuspend(IndexUiIntent.Update)
            }

            is IndexUiIntent.UpdatePackageState -> {
                mediaRestoreRepository.updateMediaState(entity = intent.entity)
            }
        }
    }

    private var _timestampState: MutableStateFlow<Long> = MutableStateFlow(0)
    private val timestampState: StateFlow<Long> = _timestampState.asStateFlow()
    private var _timestampListState: Flow<List<Long>> =
        mediaRestoreRepository.observeTimestamps().onEach {
            if (timestampState.value == 0L) {
                _timestampState.value = it.lastOrNull() ?: 0
                emitIntentSuspend(IndexUiIntent.Update)
            }
        }.flowOnIO()
    val timestampIndexState: StateFlow<Int> =
        combine(_timestampState, _timestampListState) { timestamp, timestampList ->
            timestampList.indexOf(timestamp)
        }.flowOnIO().stateInScope(-1)
    val timestampListState: StateFlow<List<String>> =
        _timestampListState.map { it.map { timestamp -> DateUtil.formatTimestamp(timestamp) } }.flowOnIO().stateInScope(listOf())

    @OptIn(ExperimentalCoroutinesApi::class)
    private var _medium: Flow<List<MediaRestoreEntity>> = _timestampState.flatMapLatest {
        mediaRestoreRepository.observeMedium(it)
    }.flowOnIO()
    private val _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private val _mediumState: Flow<List<MediaRestoreEntity>> =
        combine(_medium, _keyState) { packages, key ->
            packages.filter(mediaRestoreRepository.getKeyPredicate(key = key))
        }.flowOnIO()
    val mediumState: StateFlow<List<MediaRestoreEntity>> = _mediumState.stateInScope(listOf())
    val mediumSelectedState: StateFlow<List<MediaRestoreEntity>> = _mediumState.map { packages ->
        packages.filter { it.selected }
    }.flowOnIO().stateInScope(listOf())
    val mediumNotSelectedState: StateFlow<List<MediaRestoreEntity>> = _mediumState.map { packages ->
        packages.filter { it.selected.not() }
    }.flowOnIO().stateInScope(listOf())

    private val _topBarState: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.restore_list)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()
    private val _shimmeringState: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shimmeringState: StateFlow<Boolean> = _shimmeringState.asStateFlow()
}
