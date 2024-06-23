package com.xayah.feature.main.medium.backup.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.decodeURL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val name: String,
    val isCalculating: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class UpdateMedia(val mediaEntity: MediaEntity) : IndexUiIntent()
    data class Delete(val mediaEntity: MediaEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    args: SavedStateHandle,
    private val mediaRepo: MediaRepository,
    rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        name = args.get<String>(MainRoutes.ARG_MEDIA_NAME)?.decodeURL()?.trim() ?: "",
        isCalculating = false,
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitState(state.copy(isCalculating = true))
                mediaRepo.updateLocalMediaSize(name = state.name, opType = OpType.BACKUP, preserveId = 0)
                emitState(state.copy(isCalculating = false))
            }

            is IndexUiIntent.UpdateMedia -> {
                mediaRepo.upsert(intent.mediaEntity)
            }

            is IndexUiIntent.Delete -> {
                mediaRepo.delete(intent.mediaEntity.id)
            }
        }
    }

    private val _media: Flow<MediaEntity?> = mediaRepo.queryFlow(uiState.value.name, OpType.BACKUP, 0).flowOnIO()
    val mediaState: StateFlow<MediaEntity?> = _media.stateInScope(null)
}
