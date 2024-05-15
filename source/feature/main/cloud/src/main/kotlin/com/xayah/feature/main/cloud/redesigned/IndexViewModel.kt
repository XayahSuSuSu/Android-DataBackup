package com.xayah.feature.main.cloud.redesigned

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.model.database.CloudEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    cloudRepos: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {}

    private val _accounts: Flow<List<CloudEntity>> = cloudRepos.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
