package com.xayah.feature.main.directory

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.model.OpType
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class IndexUiState(
    val updating: Boolean = true,
    val type: OpType = OpType.BACKUP,
    val directories: Flow<List<DirectoryEntity>> = flow {},
    val shimmerCount: Int = 1,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Update : IndexUiIntent()
    data class SelectDir(val type: OpType, val entity: DirectoryEntity) : IndexUiIntent()
    data class AddDir(val type: OpType, val context: ComponentActivity) : IndexUiIntent()
    data class DeleteDir(val type: OpType, val entity: DirectoryEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val directoryRepository: DirectoryRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, UiEffect>(
    IndexUiState(
        type = OpType.of(args.get<String>(MainRoutes.ArgOpType)),
        directories = directoryRepository.directories
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                emitState(state.copy(updating = true, shimmerCount = directoryRepository.countActiveDirectories()))
                directoryRepository.update(state.type)
                emitState(state.copy(updating = false))
            }

            is IndexUiIntent.SelectDir -> {
                directoryRepository.selectDir(type = state.type, entity = intent.entity)
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
                                directoryRepository.addDir(state.type, pathList)
                                emitIntent(IndexUiIntent.Update)
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.DeleteDir -> {
                directoryRepository.deleteDir(type = state.type, entity = intent.entity)
            }
        }
    }
}
