package com.xayah.feature.main.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.data.repository.ListData
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.App
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.decodeURL
import com.xayah.core.util.launchOnDefault
import com.xayah.core.work.WorkManagerInitializer
import com.xayah.feature.main.list.ListActionsUiState.Loading
import com.xayah.feature.main.list.ListActionsUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListActionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val listDataRepo: ListDataRepo,
    private val appsRepo: AppsRepo,
    private val filesRepo: FilesRepo,
) : ViewModel() {
    private val target: Target = Target.valueOf(savedStateHandle.get<String>(MainRoutes.ARG_TARGET)!!.decodeURL().trim())
    private val opType: OpType = OpType.of(savedStateHandle.get<String>(MainRoutes.ARG_OP_TYPE)?.decodeURL()?.trim())
    private val cloudName: String = savedStateHandle.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: ""
    private val backupDir: String = savedStateHandle.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim() ?: ""

    val uiState: StateFlow<ListActionsUiState> = when (target) {
        Target.Apps -> combine(
            listDataRepo.getListData(),
            listDataRepo.getAppList()
        ) { lData, aList ->
            val listData = lData.castTo<ListData.Apps>()
            Success.Apps(
                opType = opType,
                selected = listData.selected,
                isUpdating = listData.isUpdating,
                appList = aList,
            )
        }

        Target.Files -> combine(
            listDataRepo.getListData(),
            listDataRepo.getFileList()
        ) { lData, fList ->
            val listData = lData.castTo<ListData.Files>()
            Success.Files(
                opType = opType,
                selected = listData.selected,
                isUpdating = listData.isUpdating,
                fileList = fList,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun refresh() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    when (opType) {
                        OpType.BACKUP -> {
                            WorkManagerInitializer.fullInitializeAndUpdateApps(context)
                        }

                        OpType.RESTORE -> {
                            WorkManagerInitializer.loadAppBackups(context, cloudName, backupDir)
                        }
                    }
                }

                is Success.Files -> {
                    when (opType) {
                        OpType.BACKUP -> {
                            WorkManagerInitializer.fastInitializeAndUpdateFiles(context)
                        }

                        OpType.RESTORE -> {
                            WorkManagerInitializer.loadFileBackups(context, cloudName, backupDir)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    fun showFilterSheet() {
        viewModelScope.launchOnDefault {
            listDataRepo.setShowFilterSheet(true)
        }
    }

    fun selectAll() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    val state = uiState.value.castTo<Success.Apps>()
                    appsRepo.selectAll(state.appList.map { it.id })
                }

                is Success.Files -> {
                    val state = uiState.value.castTo<Success.Files>()
                    filesRepo.selectAll(state.fileList.map { it.id })
                }

                else -> {}
            }

        }
    }

    fun unselectAll() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    val state = uiState.value.castTo<Success.Apps>()
                    appsRepo.unselectAll(state.appList.map { it.id })
                }

                is Success.Files -> {
                    val state = uiState.value.castTo<Success.Files>()
                    filesRepo.unselectAll(state.fileList.map { it.id })
                }

                else -> {}
            }

        }
    }

    fun reverseAll() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    val state = uiState.value.castTo<Success.Apps>()
                    appsRepo.reverseAll(state.appList.map { it.id })
                }

                is Success.Files -> {
                    val state = uiState.value.castTo<Success.Files>()
                    filesRepo.reverseAll(state.fileList.map { it.id })
                }

                else -> {}
            }

        }
    }

    fun blockSelected() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    val state = uiState.value.castTo<Success.Apps>()
                    appsRepo.blockSelected(state.appList.filter { it.selected }.map { it.id })
                }

                is Success.Files -> {
                    val state = uiState.value.castTo<Success.Files>()
                    filesRepo.blockSelected(state.fileList.filter { it.selected }.map { it.id })
                }

                else -> {}
            }

        }
    }

    fun showDataItemsSheet() {
        viewModelScope.launchOnDefault {
            listDataRepo.setShowDataItemsSheet(true)
        }
    }

    fun deleteSelected() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    val state = uiState.value.castTo<Success.Apps>()
                    appsRepo.deleteSelected(state.appList.filter { it.selected }.map { it.id })
                }

                is Success.Files -> {
                    val state = uiState.value.castTo<Success.Files>()
                    when (opType) {
                        OpType.BACKUP -> {
                            filesRepo.delete(state.fileList.filter { it.selected }.map { it.id })
                        }

                        OpType.RESTORE -> {
                            filesRepo.deleteSelected(state.fileList.filter { it.selected }.map { it.id })
                        }
                    }
                }

                else -> {}
            }

        }
    }

    fun addFiles(pathList: List<String>) {
        viewModelScope.launchOnDefault {
            filesRepo.addFiles(pathList)
        }
    }
}

sealed interface ListActionsUiState {
    data object Loading : ListActionsUiState
    sealed class Success(
        open val opType: OpType,
        open val selected: Long,
        open val isUpdating: Boolean,
    ) : ListActionsUiState {
        data class Apps(
            override val opType: OpType,
            override val selected: Long,
            override val isUpdating: Boolean,
            val appList: List<App>,
        ) : Success(opType, selected, isUpdating)

        data class Files(
            override val opType: OpType,
            override val selected: Long,
            override val isUpdating: Boolean,
            val fileList: List<File>,
        ) : Success(opType, selected, isUpdating)
    }
}
