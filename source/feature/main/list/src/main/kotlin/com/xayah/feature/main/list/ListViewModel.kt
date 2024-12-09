package com.xayah.feature.main.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.ListData
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.decodeURL
import com.xayah.core.util.ifEmptyEncodeURLWithSpace
import com.xayah.core.util.launchOnDefault
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.navigateSingle
import com.xayah.core.work.WorkManagerInitializer
import com.xayah.feature.main.list.ListUiState.Loading
import com.xayah.feature.main.list.ListUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    listDataRepo: ListDataRepo,
) : ViewModel() {
    private val target: Target = Target.valueOf(savedStateHandle.get<String>(MainRoutes.ARG_TARGET)!!.decodeURL().trim())
    private val opType: OpType = OpType.of(savedStateHandle.get<String>(MainRoutes.ARG_OP_TYPE)?.decodeURL()?.trim())
    private val cloudName: String = savedStateHandle.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: ""
    private val backupDir: String = savedStateHandle.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim()?.ifEmpty { context.localBackupSaveDir() } ?: context.localBackupSaveDir()

    init {
        // Reset list data
        listDataRepo.initialize(target, opType, cloudName, backupDir)
    }

    val uiState: StateFlow<ListUiState> = when (target) {
        Target.Apps -> listDataRepo.getListData().map {
            val listData = it.castTo<ListData.Apps>()
            Success.Apps(
                opType = opType,
                selected = listData.selected,
                isUpdating = listData.isUpdating,
                cloudName = cloudName,
                backupDir = backupDir,
            )
        }

        Target.Files -> listDataRepo.getListData().map {
            val listData = it.castTo<ListData.Files>()
            Success.Files(
                opType = opType,
                selected = listData.selected,
                isUpdating = listData.isUpdating,
                cloudName = cloudName,
                backupDir = backupDir,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun onResume() {
        viewModelScope.launchOnDefault {
            when (uiState.value) {
                is Success.Apps -> {
                    when (opType) {
                        OpType.BACKUP -> {
                            val state = uiState.value.castTo<Success.Apps>()
                            if (state.isUpdating.not()) {
                                WorkManagerInitializer.fastInitializeAndUpdateApps(context)
                            }
                        }

                        OpType.RESTORE -> {}
                    }
                }

                is Success.Files -> {
                    when (opType) {
                        OpType.BACKUP -> {
                            val state = uiState.value.castTo<Success.Files>()
                            if (state.isUpdating.not()) {
                                WorkManagerInitializer.fastInitializeAndUpdateFiles(context)
                            }
                        }

                        OpType.RESTORE -> {}
                    }
                }

                else -> {}
            }
        }
    }

    fun toNextPage(navController: NavHostController) {
        when (target) {
            Target.Apps -> {
                when (opType) {
                    OpType.BACKUP -> {
                        navController.navigateSingle(MainRoutes.PackagesBackupProcessingGraph.route)
                    }

                    OpType.RESTORE -> {
                        navController.navigateSingle(
                            MainRoutes.PackagesRestoreProcessingGraph.getRoute(
                                cloudName = cloudName.ifEmptyEncodeURLWithSpace(),
                                backupDir = backupDir.ifEmptyEncodeURLWithSpace()
                            )
                        )
                    }
                }
            }

            Target.Files -> {
                when (opType) {
                    OpType.BACKUP -> {
                        navController.navigateSingle(MainRoutes.MediumBackupProcessingGraph.route)
                    }

                    OpType.RESTORE -> {
                        navController.navigateSingle(
                            MainRoutes.MediumRestoreProcessingGraph.getRoute(
                                cloudName = cloudName.ifEmptyEncodeURLWithSpace(),
                                backupDir = backupDir.ifEmptyEncodeURLWithSpace()
                            )
                        )
                    }
                }
            }
        }
    }
}

sealed interface ListUiState {
    data object Loading : ListUiState
    sealed class Success(
        open val opType: OpType,
        open val selected: Long,
        open val isUpdating: Boolean,
        open val cloudName: String,
        open val backupDir: String,
    ) : ListUiState {
        data class Apps(
            override val opType: OpType,
            override val selected: Long,
            override val isUpdating: Boolean,
            override val cloudName: String,
            override val backupDir: String,
        ) : Success(opType, selected, isUpdating, cloudName, backupDir)

        data class Files(
            override val opType: OpType,
            override val selected: Long,
            override val isUpdating: Boolean,
            override val cloudName: String,
            override val backupDir: String,
        ) : Success(opType, selected, isUpdating, cloudName, backupDir)
    }
}
