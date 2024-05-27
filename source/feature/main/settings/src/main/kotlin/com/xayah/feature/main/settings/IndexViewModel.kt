package com.xayah.feature.main.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    directoryRepo: DirectoryRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {}

    private val _directory: Flow<DirectoryEntity?> = directoryRepo.querySelectedByDirectoryTypeFlow().flowOnIO()
    val directoryState: StateFlow<DirectoryEntity?> = _directory.stateInScope(null)
}
