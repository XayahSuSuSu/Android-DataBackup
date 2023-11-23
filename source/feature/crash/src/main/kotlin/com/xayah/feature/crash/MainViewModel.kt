package com.xayah.feature.crash

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class IndexUiState(
    val text: String = "",
) : UiState

sealed class IndexUiIntent : UiIntent

sealed class IndexUiEffect : UiEffect

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor() : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {}
}
