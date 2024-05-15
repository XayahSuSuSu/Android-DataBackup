package com.xayah.feature.main.tree

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class IndexUiState(
    val refreshing: Boolean = true,
    val filterIndex: Int = 0,
    val filterList: List<String>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Refresh : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val treeRepository: TreeRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(filterList = treeRepository.filterList)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Refresh -> {
                emitState(state.copy(refreshing = true))
                _contentItems.value =
                    treeRepository.tree(src = treeRepository.getTargetPath(), exclusionList = treeRepository.getExclusionList(state.filterIndex))
                emitState(state.copy(refreshing = false))
            }
        }
    }

    private val _contentItems: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val contentItems: StateFlow<List<String>> = _contentItems.asStateFlow()
}
