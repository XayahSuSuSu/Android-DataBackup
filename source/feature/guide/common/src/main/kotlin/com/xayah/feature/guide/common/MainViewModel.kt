package com.xayah.feature.guide.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromVector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class MainUiState(
    val isInitializing: Boolean = true,
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    val topBarTitle: StringResourceToken = StringResourceToken.fromString(""),
    val topBarIcon: ImageVectorToken = ImageVectorToken.fromVector(Icons.Rounded.Pending),
    val fabIcon: ImageVectorToken = ImageVectorToken.fromVector(Icons.Rounded.Pending),
    val onFabClick: () -> Unit = {},
) : UiState

sealed class MainUiIntent : UiIntent {
    data class SetUiState(val state: MainUiState) : MainUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class MainViewModel @Inject constructor(
) : BaseViewModel<MainUiState, MainUiIntent, IndexUiEffect>(MainUiState()) {
    override suspend fun onEvent(state: MainUiState, intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.SetUiState -> {
                emitStateSuspend(intent.state)
            }
        }
    }
}
