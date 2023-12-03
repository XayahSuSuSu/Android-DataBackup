package com.xayah.feature.main.tree

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class IndexUiState(
    val refreshing: Boolean = true,
    val typeIndex: Int = 0,
    val typeList: List<String>,
    val filterIndex: Int = 0,
    val filterList: List<String>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Refresh : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val treeRepository: TreeRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(typeList = treeRepository.typeList, filterList = treeRepository.filterList)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Refresh -> {
                emitState(state.copy(refreshing = true))
                _contentItems.value =
                    treeRepository.tree(src = treeRepository.getTargetPath(state.typeIndex), exclusionList = treeRepository.getExclusionList(state.filterIndex))
                emitState(state.copy(refreshing = false))
            }
        }
    }

    private val _contentItems: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val contentItems: StateFlow<List<String>> = _contentItems.asStateFlow()
}
