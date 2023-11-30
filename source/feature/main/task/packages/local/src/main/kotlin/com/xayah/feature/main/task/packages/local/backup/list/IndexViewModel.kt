package com.xayah.feature.main.task.packages.local.backup.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageBackupRepository
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.model.SortType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.task.packages.common.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val activating: Boolean = true,
    val shimmerCount: Int = 9,
    val emphasizedState: Boolean = false,
    val batchSelection: List<String> = listOf(),
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Update : IndexUiIntent()
    data class UpdatePackage(val entity: PackageBackupEntire) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    object Emphasize : IndexUiIntent()
    object BatchingSelectAll : IndexUiIntent()
    data class BatchingSelect(val packageName: String) : IndexUiIntent()
    data class BatchAndOp(val mask: Int, val packageNames: List<String>) : IndexUiIntent()
    data class BatchOrOp(val mask: Int, val packageNames: List<String>) : IndexUiIntent()

}

sealed class IndexUiEffect : UiEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ) : IndexUiEffect()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val packageBackupRepository: PackageBackupRepository,
) :
    BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                packageBackupRepository.activate()
                emitState(uiState.value.copy(activating = false))
                packageBackupRepository.update(topBarState = _topBarState, endTitle = StringResourceToken.fromStringId(R.string.backup_list))
            }

            is IndexUiIntent.UpdatePackage -> {
                packageBackupRepository.updatePackage(intent.entity)
            }


            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.Sort -> {
                var type = intent.type
                val index = intent.index
                if (_sortIndexState.first() == index) {
                    type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                    packageBackupRepository.saveBackupSortType(type)
                }
                packageBackupRepository.saveBackupSortTypeIndex(index)
            }

            is IndexUiIntent.FilterByFlag -> {
                packageBackupRepository.saveBackupFilterFlagIndex(intent.index)
            }

            is IndexUiIntent.Emphasize -> {
                emitState(state.copy(emphasizedState = state.emphasizedState.not()))
            }

            is IndexUiIntent.BatchingSelectAll -> {
                var batchSelection: List<String> = listOf()
                if (state.batchSelection.isEmpty()) {
                    batchSelection = packagesState.first().map { it.packageName }
                }
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchingSelect -> {
                val batchSelection = state.batchSelection.toMutableList()
                if (intent.packageName in batchSelection)
                    batchSelection.remove(intent.packageName)
                else
                    batchSelection.add(intent.packageName)
                emitState(state.copy(batchSelection = batchSelection))
            }

            is IndexUiIntent.BatchAndOp -> {
                packageBackupRepository.andOpCodeByMask(mask = intent.mask, packageNames = intent.packageNames)
            }

            is IndexUiIntent.BatchOrOp -> {
                packageBackupRepository.orOpCodeByMask(mask = intent.mask, packageNames = intent.packageNames)
            }
        }
    }

    val snackbarHostState: SnackbarHostState = SnackbarHostState()
    override suspend fun onEffect(effect: IndexUiEffect) {
        when (effect) {
            is IndexUiEffect.ShowSnackbar -> {
                snackbarHostState.showSnackbar(effect.message, effect.actionLabel, effect.withDismissAction, effect.duration)
            }
        }
    }

    private var _packages: Flow<List<PackageBackupEntire>> = packageBackupRepository.packages.flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndexState: Flow<Int> = packageBackupRepository.backupFilterFlagIndex.flowOnIO()
    private var _sortIndexState: Flow<Int> = packageBackupRepository.backupSortTypeIndex.flowOnIO()
    private var _sortTypeState: Flow<SortType> = packageBackupRepository.backupSortType.flowOnIO()
    private val _packagesState: Flow<List<PackageBackupEntire>> =
        combine(_packages, _keyState, _flagIndexState, _sortIndexState, _sortTypeState) { packages, key, flagIndex, sortIndex, sortType ->
            packages.filter(packageBackupRepository.getKeyPredicate(key = key))
                .filter(packageBackupRepository.getFlagPredicate(index = flagIndex))
                .sortedWith(packageBackupRepository.getSortComparator(sortIndex = sortIndex, sortType = sortType))
        }.flowOnIO()
    val packagesState: StateFlow<List<PackageBackupEntire>> = _packagesState.stateInScope(listOf())
    val packagesSelectedState: StateFlow<List<PackageBackupEntire>> = _packagesState.map { packages ->
        packages.filter { it.operationCode != OperationMask.None }
    }.flowOnIO().stateInScope(listOf())
    val packagesNotSelectedState: StateFlow<List<PackageBackupEntire>> = _packagesState.map { packages ->
        packages.filter { it.operationCode == OperationMask.None }
    }.flowOnIO().stateInScope(listOf())

    private val _topBarState: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.backup_list)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()
    val shimmeringState: StateFlow<Boolean> = combine(_topBarState, _packages) { topBarState, packages ->
        topBarState.progress != 1f && packages.isEmpty()
    }.flowOnIO().stateInScope(initialValue = true)
    val selectedAPKsCountState: StateFlow<Int> = packageBackupRepository.selectedAPKsCount.flowOnIO().stateInScope(0)
    val selectedDataCountState: StateFlow<Int> = packageBackupRepository.selectedDataCount.flowOnIO().stateInScope(0)
}
