package com.xayah.feature.main.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class IndexUiState(
    val settingsInfoItems: List<SettingsInfoItem> = infoList,
) : UiState

sealed class IndexUiIntent : UiIntent

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {}
}
