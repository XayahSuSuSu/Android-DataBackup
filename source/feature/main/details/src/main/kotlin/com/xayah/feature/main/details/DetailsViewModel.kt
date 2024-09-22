package com.xayah.feature.main.details

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.data.repository.LabelsRepo
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.model.database.AppWithLabels
import com.xayah.core.model.database.FileWithLabels
import com.xayah.core.model.database.LabelWithAppIds
import com.xayah.core.model.database.LabelWithFileIds
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.decodeURL
import com.xayah.core.util.launchOnDefault
import com.xayah.feature.main.details.DetailsUiState.Error
import com.xayah.feature.main.details.DetailsUiState.Loading
import com.xayah.feature.main.details.DetailsUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val rootService: RemoteRootService,
    private val appsRepo: AppsRepo,
    private val filesRepo: FilesRepo,
    private val labelsRepo: LabelsRepo,
) : ViewModel() {
    private val id: Long = savedStateHandle.get<String>(MainRoutes.ARG_ID)?.toLongOrNull() ?: 0L
    private val target: Target = Target.valueOf(savedStateHandle.get<String>(MainRoutes.ARG_TARGET)!!.decodeURL().trim())
    private val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val uiState: StateFlow<DetailsUiState> = when (target) {
        Target.Apps -> {
            combine(labelsRepo.getAppWithLabels(id), isRefreshing, labelsRepo.getLabelWithAppIds()) { app, isRefreshing, labelWithAppIds ->
                if (app != null) {
                    Success.App(uuid = UUID.randomUUID(), isRefreshing = isRefreshing, labelWithAppIds = labelWithAppIds, appWithLabels = app)
                } else {
                    Error
                }
            }
        }

        Target.Files -> {
            combine(labelsRepo.getFileWithLabels(id), isRefreshing, labelsRepo.getLabelWithFileIds()) { file, isRefreshing, labelWithFileIds ->
                if (file != null) {
                    Success.File(uuid = UUID.randomUUID(), isRefreshing = isRefreshing, labelWithFileIds = labelWithFileIds, fileWithLabels = file)
                } else {
                    Error
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun onResume() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state: Success.App = uiState.value.castTo()
                    when (state.appWithLabels.app.indexInfo.opType) {
                        OpType.BACKUP -> {
                            isRefreshing.emit(true)
                            appsRepo.updateApp(state.appWithLabels.app, state.appWithLabels.app.userId)
                            appsRepo.calculateLocalAppSize(state.appWithLabels.app)
                            isRefreshing.emit(false)
                        }

                        OpType.RESTORE -> {
                            isRefreshing.emit(true)
                            appsRepo.calculateLocalAppArchiveSize(state.appWithLabels.app)
                            isRefreshing.emit(false)
                        }
                    }
                }

                is Success.File -> {
                    val state: Success.File = uiState.value.castTo()
                    when (state.fileWithLabels.file.indexInfo.opType) {
                        OpType.BACKUP -> {
                            isRefreshing.emit(true)
                            filesRepo.calculateLocalFileSize(state.fileWithLabels.file)
                            isRefreshing.emit(false)
                        }

                        OpType.RESTORE -> {
                            isRefreshing.emit(true)
                            filesRepo.calculateLocalFileArchiveSize(state.fileWithLabels.file)
                            isRefreshing.emit(false)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    fun setDataStates(id: Long, dataStates: PackageDataStates) {
        viewModelScope.launchOnDefault {
            appsRepo.setDataItems(listOf(id), dataStates)
        }
    }

    fun addLabel(label: String) {
        viewModelScope.launchOnDefault {
            if (label.trim().isEmpty() || labelsRepo.addLabel(label.trim()) == -1L) {
                Toast.makeText(context, context.getString(R.string.failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteLabel(id: Long) {
        viewModelScope.launchOnDefault {
            labelsRepo.deleteLabel(id)
        }
    }

    fun selectLabel(selected: Boolean, labelId: Long, id: Long) {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    if (selected) {
                        labelsRepo.deleteLabelAppCrossRef(labelId, id)
                    } else {
                        labelsRepo.addLabelAppCrossRef(labelId, id)
                    }
                }

                is Success.File -> {
                    if (selected) {
                        labelsRepo.deleteLabelFileCrossRef(labelId, id)
                    } else {
                        labelsRepo.addLabelFileCrossRef(labelId, id)
                    }
                }

                else -> {}
            }
        }
    }

    fun block(blocked: Boolean) {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state = uiState.value.castTo<Success.App>()
                    appsRepo.setBlocked(state.appWithLabels.app.id, blocked.not())
                }

                is Success.File -> {
                    val state = uiState.value.castTo<Success.File>()
                    filesRepo.setBlocked(state.fileWithLabels.file.id, blocked.not())
                }

                else -> {}
            }

        }
    }

    fun freezeApp(frozen: Boolean) {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state = uiState.value.castTo<Success.App>()
                    val app = state.appWithLabels.app
                    if (frozen) {
                        rootService.setApplicationEnabledSetting(app.packageName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0, app.userId, null)
                    } else {
                        rootService.setApplicationEnabledSetting(app.packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0, app.userId, null)
                    }
                    appsRepo.setEnabled(app.id, rootService.getApplicationEnabledSetting(app.packageName, app.userId) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                }

                else -> {}
            }
        }
    }

    fun launchApp() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state = uiState.value.castTo<Success.App>()
                    val app = state.appWithLabels.app
                    appsRepo.launchApp(app.packageName, app.userId)
                }

                else -> {}
            }
        }
    }

    fun protect() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state = uiState.value.castTo<Success.App>()
                    val app = state.appWithLabels.app
                    appsRepo.protectApp(app.indexInfo.cloud, app)
                }

                else -> {
                    val state = uiState.value.castTo<Success.File>()
                    val file = state.fileWithLabels.file
                    filesRepo.protectFile(file.indexInfo.cloud, file)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.App -> {
                    val state = uiState.value.castTo<Success.App>()
                    val app = state.appWithLabels.app
                    appsRepo.deleteApp(app.indexInfo.cloud, app)
                }

                is Success.File -> {
                    val state = uiState.value.castTo<Success.File>()
                    val file = state.fileWithLabels.file
                    when (file.indexInfo.opType) {
                        OpType.BACKUP -> {
                            filesRepo.delete(file.id)
                        }

                        OpType.RESTORE -> {
                            filesRepo.deleteFile(file.indexInfo.cloud, file)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    sealed class Success(
        open val uuid: UUID,
        open val isRefreshing: Boolean,
    ) : DetailsUiState {
        data class App(
            override val uuid: UUID,
            override val isRefreshing: Boolean,
            val labelWithAppIds: List<LabelWithAppIds>,
            val appWithLabels: AppWithLabels
        ) : Success(uuid, isRefreshing)

        data class File(
            override val uuid: UUID,
            override val isRefreshing: Boolean,
            val labelWithFileIds: List<LabelWithFileIds>,
            val fileWithLabels: FileWithLabels
        ) : Success(uuid, isRefreshing)
    }

    data object Error : DetailsUiState
}
