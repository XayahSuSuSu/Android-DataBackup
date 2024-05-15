package com.xayah.feature.crash

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class IndexUiState(
    val text: String = "",
) : UiState

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor() : BaseViewModel<IndexUiState, UiIntent, IndexUiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: UiIntent) {}
}
