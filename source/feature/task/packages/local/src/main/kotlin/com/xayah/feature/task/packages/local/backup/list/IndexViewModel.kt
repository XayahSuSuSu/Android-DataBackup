package com.xayah.feature.task.packages.local.backup.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageBackupRepository
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.util.of
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val activating: Boolean = true,
    val topBarState: Flow<TopBarState> = flow {},
    val type: OpType = OpType.BACKUP,
    val packages: Flow<List<PackageBackupEntire>> = flow {},
    val shimmering: Flow<Boolean> = flow {},
    val shimmerCount: Int = 9,
    val emphasizedState: Boolean = false,
    val selectedAPKsCount: Flow<Int> = flow {},
    val selectedDataCount: Flow<Int> = flow {},
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

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageBackupRepository: PackageBackupRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(
    IndexUiState(
        type = OpType.of(args.get<String>(MainRoutes.ArgOpType)),
        packages = packageBackupRepository.getMappedPackages(),
        selectedAPKsCount = packageBackupRepository.selectedAPKsCount,
        selectedDataCount = packageBackupRepository.selectedDataCount,
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                packageBackupRepository.activate()
                emitState(
                    uiState.value.copy(
                        activating = false,
                        shimmering = packageBackupRepository.packages.map { it.isEmpty() },
                        topBarState = packageBackupRepository.update()
                    )
                )
            }

            is IndexUiIntent.UpdatePackage -> {
                packageBackupRepository.updatePackage(intent.entity)
            }


            is IndexUiIntent.FilterByKey -> {
                emitState(state.copy(packages = packageBackupRepository.getMappedPackages(key = intent.key)))
            }

            is IndexUiIntent.Sort -> {
                emitState(state.copy(packages = packageBackupRepository.getMappedPackages(sortIndex = intent.index, sortType = intent.type)))
            }

            is IndexUiIntent.FilterByFlag -> {
                emitState(state.copy(packages = packageBackupRepository.getMappedPackages(flagIndex = intent.index)))
            }

            is IndexUiIntent.Emphasize -> {
                emitState(state.copy(emphasizedState = state.emphasizedState.not()))
            }

            is IndexUiIntent.BatchingSelectAll -> {
                var batchSelection: List<String> = listOf()
                if (state.batchSelection.isEmpty()) {
                    batchSelection = state.packages.first().map { it.packageName }
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
}
