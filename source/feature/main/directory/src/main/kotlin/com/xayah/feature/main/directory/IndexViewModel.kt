package com.xayah.feature.main.directory

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.model.StorageType
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.libpickyou.PickYouLauncher
import com.xayah.libpickyou.ui.model.PermissionType
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val updating: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Update : IndexUiIntent()
    data class Select(val entity: DirectoryEntity) : IndexUiIntent()
    data class Add(val context: Activity) : IndexUiIntent()
    data class Delete(val entity: DirectoryEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val directoryRepo: DirectoryRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(updating = true)) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                emitState(uiState.value.copy(updating = true))
                directoryRepo.update()
                emitState(uiState.value.copy(updating = false))
            }

            is IndexUiIntent.Select -> {
                directoryRepo.selectDir(entity = intent.entity)
            }

            is IndexUiIntent.Add -> {
                withMainContext {
                    val context = intent.context
                    PickYouLauncher(
                        checkPermission = true,
                        title = context.getString(R.string.select_target_directory),
                        pickerType = PickerType.DIRECTORY,
                        permissionType = PermissionType.ROOT,
                    ).apply {
                        launch(context) { pathString ->
                            launchOnIO {
                                directoryRepo.addDir(listOf(pathString))
                                emitIntent(IndexUiIntent.Update)
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.Delete -> {
                directoryRepo.deleteDir(entity = intent.entity)
            }
        }
    }

    private val _internalDirectories: Flow<List<DirectoryEntity>> = directoryRepo.queryActiveDirectoriesFlow(StorageType.INTERNAL).flowOnIO()
    val internalDirectoriesState: StateFlow<List<DirectoryEntity>> = _internalDirectories.stateInScope(listOf())

    private val _externalDirectories: Flow<List<DirectoryEntity>> = directoryRepo.queryActiveDirectoriesFlow(StorageType.EXTERNAL).flowOnIO()
    val externalDirectoriesState: StateFlow<List<DirectoryEntity>> = _externalDirectories.stateInScope(listOf())

    private val _customDirectories: Flow<List<DirectoryEntity>> = directoryRepo.queryActiveDirectoriesFlow(StorageType.CUSTOM).flowOnIO()
    val customDirectoriesState: StateFlow<List<DirectoryEntity>> = _customDirectories.stateInScope(listOf())
}
