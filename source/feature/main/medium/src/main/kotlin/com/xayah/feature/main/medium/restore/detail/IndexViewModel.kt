package com.xayah.feature.main.medium.restore.detail

import android.content.Context
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
import com.xayah.feature.main.medium.R
import com.xayah.libpickyou.PickYouLauncher
import com.xayah.libpickyou.ui.model.PermissionType
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val name: String,
    val preserveId: Long,
    val isCalculating: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class UpdateMedia(val mediaEntity: MediaEntity) : IndexUiIntent()
    data class SetPath(val context: Context, val mediaEntity: MediaEntity) : IndexUiIntent()
    data class Delete(val mediaEntity: MediaEntity) : IndexUiIntent()
    data class Preserve(val mediaEntity: MediaEntity) : IndexUiIntent()
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
        preserveId = args.get<String>(MainRoutes.ARG_PRESERVE_ID)?.toLongOrNull() ?: 0,
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
                mediaRepo.updateLocalMediaArchivesSize(state.name, OpType.RESTORE)
                emitState(state.copy(isCalculating = false))
            }

            is IndexUiIntent.UpdateMedia -> {
                mediaRepo.upsert(intent.mediaEntity)
            }

            is IndexUiIntent.SetPath -> {
                val entity = intent.mediaEntity
                withMainContext {
                    val context = intent.context
                    PickYouLauncher(
                        checkPermission = true,
                        title = context.getString(R.string.select_target_directory),
                        pickerType = PickerType.DIRECTORY,
                        permissionType = PermissionType.ROOT
                    ).apply {
                        val pathString = awaitLaunch(context)
                        mediaRepo.upsert(entity.copy(mediaInfo = entity.mediaInfo.copy(path = pathString), extraInfo = entity.extraInfo.copy(existed = true)))
                    }
                }
            }

            is IndexUiIntent.Delete -> {
                mediaRepo.delete(intent.mediaEntity)
            }

            is IndexUiIntent.Preserve -> {
                mediaRepo.preserve(intent.mediaEntity)
            }
        }
    }

    private val _media: Flow<MediaEntity?> = mediaRepo.queryFlow(uiState.value.name, OpType.RESTORE, uiState.value.preserveId).flowOnIO()
    val mediaState: StateFlow<MediaEntity?> = _media.stateInScope(null)
}
