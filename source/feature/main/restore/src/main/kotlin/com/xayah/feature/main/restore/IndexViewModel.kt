package com.xayah.feature.main.restore

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readLastRestoreTime
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageMode
import com.xayah.core.model.Target
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.encodeURL
import com.xayah.core.util.encodedURLWithSpace
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.navigateSingle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val storageIndex: Int,
    val storageType: StorageMode,
    val cloudEntity: CloudEntity?,
    val packages: List<PackageEntity>,
    val packagesSize: String,
    val medium: List<MediaEntity>,
    val mediumSize: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateApps : IndexUiIntent()
    data object UpdateFiles : IndexUiIntent()
    data class SetCloudEntity(val name: String) : IndexUiIntent()
    data class ToAppList(val navController: NavHostController) : IndexUiIntent()
    data class ToFileList(val navController: NavHostController) : IndexUiIntent()
    data class ToReload(val navController: NavHostController) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pkgRepo: PackageRepository,
    private val mediaRepo: MediaRepository,
    private val cloudRepo: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        storageIndex = 0,
        storageType = StorageMode.Local,
        cloudEntity = null,
        packages = listOf(),
        packagesSize = "",
        medium = listOf(),
        mediumSize = "",
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateApps -> {
                val packages = (when (state.storageType) {
                    StorageMode.Local -> pkgRepo.queryPackages(OpType.RESTORE, "", context.localBackupSaveDir())

                    else -> when {
                        (state.cloudEntity == null) -> {
                            listOf()
                        }

                        else -> {
                            pkgRepo.queryPackages(OpType.RESTORE, state.cloudEntity.name, state.cloudEntity.remote)
                        }
                    }
                })
                var bytes = 0.0
                packages.forEach { bytes += it.displayStatsBytes }
                emitState(state.copy(packages = packages, packagesSize = bytes.formatSize()))
            }

            is IndexUiIntent.UpdateFiles -> {
                val medium = (when (state.storageType) {
                    StorageMode.Local -> mediaRepo.query(OpType.RESTORE, "", context.localBackupSaveDir())

                    else -> when {
                        (state.cloudEntity == null) -> {
                            listOf()
                        }

                        else -> {
                            mediaRepo.query(OpType.RESTORE, state.cloudEntity.name, state.cloudEntity.remote)
                        }
                    }
                })
                var bytes = 0.0
                medium.forEach { bytes += it.displayStatsBytes }
                emitState(state.copy(medium = medium, mediumSize = bytes.formatSize()))
            }

            is IndexUiIntent.SetCloudEntity -> {
                context.saveCloudActivatedAccountName(intent.name)
                emitState(state.copy(cloudEntity = cloudRepo.queryByName(intent.name)))
                emitIntent(IndexUiIntent.UpdateApps)
                emitIntent(IndexUiIntent.UpdateFiles)
            }

            is IndexUiIntent.ToAppList -> {
                withMainContext {
                    when (state.storageType) {
                        StorageMode.Local -> {
                            intent.navController.navigateSingle(
                                MainRoutes.List.getRoute(
                                    target = Target.Apps,
                                    opType = OpType.RESTORE,
                                    backupDir = context.localBackupSaveDir().encodeURL()
                                )
                            )
                        }

                        StorageMode.Cloud -> {
                            if (state.cloudEntity != null) {
                                intent.navController.navigateSingle(
                                    MainRoutes.List.getRoute(
                                        target = Target.Apps,
                                        opType = OpType.RESTORE,
                                        cloudName = state.cloudEntity.name.encodeURL(),
                                        backupDir = state.cloudEntity.remote.encodeURL()
                                    )
                                )
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.ToFileList -> {
                withMainContext {
                    when (state.storageType) {
                        StorageMode.Local -> {
                            intent.navController.navigateSingle(
                                MainRoutes.List.getRoute(
                                    target = Target.Files,
                                    opType = OpType.RESTORE,
                                    backupDir = context.localBackupSaveDir().encodeURL()
                                )
                            )
                        }

                        StorageMode.Cloud -> {
                            if (state.cloudEntity != null) {
                                intent.navController.navigateSingle(
                                    MainRoutes.List.getRoute(
                                        target = Target.Files,
                                        opType = OpType.RESTORE,
                                        cloudName = state.cloudEntity.name.encodeURL(),
                                        backupDir = state.cloudEntity.remote.encodeURL()
                                    )
                                )
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.ToReload -> {
                withMainContext {
                    when (state.storageType) {
                        StorageMode.Local -> {
                            intent.navController.navigateSingle(MainRoutes.Reload.getRoute(encodedURLWithSpace, context.localBackupSaveDir().encodeURL()))
                        }

                        StorageMode.Cloud -> {
                            if (state.cloudEntity != null) {
                                intent.navController.navigateSingle(
                                    MainRoutes.Reload.getRoute(
                                        state.cloudEntity.name.encodeURL(),
                                        state.cloudEntity.remote.encodeURL()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val _lastRestoreTime: Flow<Long> = context.readLastRestoreTime().flowOnIO()
    val lastRestoreTimeState: StateFlow<Long> = _lastRestoreTime.stateInScope(0)

    private val _accounts: Flow<List<DialogRadioItem<Any>>> = cloudRepo.clouds.map { entities ->
        entities.map {
            DialogRadioItem(
                enum = Any(),
                title = it.name,
                desc = it.user,
            )
        }

    }.flowOnIO()
    val accounts: StateFlow<List<DialogRadioItem<Any>>> = _accounts.stateInScope(listOf())
}
