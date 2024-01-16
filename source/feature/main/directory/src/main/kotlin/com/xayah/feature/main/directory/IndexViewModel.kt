package com.xayah.feature.main.directory

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class IndexUiState(
    val updating: Boolean = true,
    val directories: Flow<List<DirectoryEntity>> = flow {},
    val shimmerCount: Int = 1,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Update : IndexUiIntent()
    data class SelectDir(val entity: DirectoryEntity) : IndexUiIntent()
    data class AddDir(val context: ComponentActivity) : IndexUiIntent()
    data class DeleteDir(val entity: DirectoryEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val directoryRepository: DirectoryRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(directories = directoryRepository.directories)) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                emitState(state.copy(updating = true, shimmerCount = directoryRepository.countActiveDirectories()))
                directoryRepository.update()
                emitState(state.copy(updating = false))
            }

            is IndexUiIntent.SelectDir -> {
                directoryRepository.selectDir(entity = intent.entity)
            }

            is IndexUiIntent.AddDir -> {
                withMainContext {
                    val context = intent.context
                    PickYouLauncher().apply {
                        setTitle(context.getString(R.string.select_target_directory))
                        setType(PickerType.DIRECTORY)
                        setLimitation(0)
                        launch(context) { pathList ->
                            launchOnIO {
                                directoryRepository.addDir(pathList)
                                emitIntent(IndexUiIntent.Update)
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.DeleteDir -> {
                directoryRepository.deleteDir(entity = intent.entity)
            }
        }
    }
}
