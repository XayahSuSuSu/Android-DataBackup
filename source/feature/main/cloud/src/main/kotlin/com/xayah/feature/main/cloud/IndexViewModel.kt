package com.xayah.feature.main.cloud

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.network.client.getCloud
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val isProcessing: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data class TestConnection(val entity: CloudEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val cloudRepo: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isProcessing = false)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.TestConnection -> {
                emitState(state.copy(isProcessing = true))
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffectOnIO(
                    IndexUiEffect.ShowSnackbar(
                        type = SnackbarType.Loading,
                        message = cloudRepo.getString(R.string.processing),
                        duration = SnackbarDuration.Indefinite,
                    )
                )
                runCatching {
                    val client = intent.entity.getCloud()
                    client.testConnection()
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Success, message = cloudRepo.getString(R.string.connection_established)))
                }.onFailure {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    if (it.localizedMessage != null)
                        emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                }
                emitState(state.copy(isProcessing = false))
            }
        }
    }

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
