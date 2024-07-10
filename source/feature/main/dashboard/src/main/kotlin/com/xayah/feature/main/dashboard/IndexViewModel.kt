package com.xayah.feature.main.dashboard

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.datastore.readLastBackupTime
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data object Update : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val directoryRepo: DirectoryRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                directoryRepo.updateSelected()
            }
        }
    }

    private val _lastBackupTime: Flow<Long> = context.readLastBackupTime().flowOnIO()
    val lastBackupTimeState: StateFlow<Long> = _lastBackupTime.stateInScope(0)

    private val _directory: Flow<DirectoryEntity?> = directoryRepo.querySelectedByDirectoryTypeFlow().flowOnIO()
    val directoryState: StateFlow<DirectoryEntity?> = _directory.stateInScope(null)
}
