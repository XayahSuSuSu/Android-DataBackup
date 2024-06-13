package com.xayah.feature.main.settings.blacklist

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.model.OpType
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
) : UiState

sealed class IndexUiIntent : UiIntent {
    data class Select(val id: Long) : IndexUiIntent()
    data object SelectAll : IndexUiIntent()
    data object RemoveSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRepo: PackageRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        selectAll = false,
        appIds = setOf()
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Select -> {
                if (intent.id in state.appIds) {
                    emitState(state.copy(appIds = state.appIds.toMutableSet().apply { remove(intent.id) }))
                } else {
                    emitState(state.copy(appIds = state.appIds.toMutableSet().apply { add(intent.id) }))
                }
            }

            is IndexUiIntent.SelectAll -> {
                emitState(state.copy(selectAll = state.selectAll.not(), appIds = if (state.selectAll.not()) packagesState.value.map { it.id }.toSet() else setOf()))
            }

            is IndexUiIntent.RemoveSelected -> {
                val appIds = state.appIds.toMutableSet()
                state.appIds.forEach {
                    packageRepo.setBlocked(it, false)
                    appIds.remove(it)
                }
                emitState(state.copy(appIds = appIds))
            }
        }
    }

    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(opType = OpType.BACKUP, blocked = true).flowOnIO()
    val packagesState: StateFlow<List<PackageEntity>> = _packages.stateInScope(listOf())
}
