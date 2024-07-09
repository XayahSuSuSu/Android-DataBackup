package com.xayah.feature.main.settings.language

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.ContextRepository
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.datastore.readAppLanguage
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

data class IndexUiState(
    val selectedLanguage: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateLanguage : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    val contextRepo: ContextRepository,
) :
    BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
        IndexUiState(
            selectedLanguage = runBlocking {
                contextRepo.withContext {
                    it.readAppLanguage().first()
                }
            },
        )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateLanguage -> {
                emitState(
                    state.copy(
                        selectedLanguage = contextRepo.withContext {
                            it.readAppLanguage().first()
                        }
                    )
                )
            }
        }
    }
}
