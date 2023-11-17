package com.xayah.feature.main.log.page.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.feature.main.log.LogDetailRepository
import com.xayah.feature.main.log.LogRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class IndexUiState(
    val loading: Boolean = true,
    val name: String = "",
    val content: String = "",
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Load : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val logDetailRepository: LogDetailRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(IndexUiState(name = args.get<String>(LogRoutes.ArgFileName) ?: "")) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Load -> {
                emitState(state.copy(loading = true))
                val path = logDetailRepository.getFilePath(state.name)
                _contentItems.value = logDetailRepository.getContentList(path)
                emitState(state.copy(loading = false))
            }
        }
    }

    private val _contentItems: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val contentItems: StateFlow<List<String>> = _contentItems.asStateFlow()
}
