package com.xayah.feature.main.home.restore

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.datastore.readLastRestoreTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class IndexUiState(
    val lastRestoreTime: Flow<String>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object ToList : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(
    IndexUiState(
        lastRestoreTime = context.readLastRestoreTime().distinctUntilChanged(),
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.ToList -> {}
        }
    }
}
