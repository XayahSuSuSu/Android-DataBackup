package com.xayah.feature.main.packages.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.ContextRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.DataType
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.OpType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.feature.main.packages.PackageDataChipItem
import com.xayah.feature.main.packages.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.xayah.core.service.packages.backup.CloudProcessingImpl as CloudBackupService
import com.xayah.core.service.packages.backup.LocalProcessingImpl as LocalBackupService
import com.xayah.core.service.packages.restore.CloudProcessingImpl as CloudRestoreService
import com.xayah.core.service.packages.restore.LocalProcessingImpl as LocalRestoreService

data class IndexUiState(
    val isRefreshing: Boolean,
    val packageName: String,
    val userId: Int,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class UpdatePackage(val packageEntity: PackageEntity) : IndexUiIntent()
    data class BackupToLocal(val packageEntity: PackageEntity, val navController: NavHostController) : IndexUiIntent()
    data class BackupToCloud(val packageEntity: PackageEntity, val name: String, val navController: NavHostController) : IndexUiIntent()
    data class RestoreFromLocal(val packageEntity: PackageEntity, val navController: NavHostController) : IndexUiIntent()
    data class RestoreFromCloud(val packageEntity: PackageEntity, val name: String, val navController: NavHostController) : IndexUiIntent()
    data class Preserve(val packageEntity: PackageEntity) : IndexUiIntent()
    data class ActiveCloud(val cloudEntity: CloudEntity) : IndexUiIntent()

    data class Delete(val packageEntity: PackageEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    args: SavedStateHandle,
    private val packageRepo: PackageRepository,
    private val cloudRepo: CloudRepository,
    rootService: RemoteRootService,
    private val localBackupService: LocalBackupService,
    private val cloudBackupService: CloudBackupService,
    private val localRestoreService: LocalRestoreService,
    private val cloudRestoreService: CloudRestoreService,
    private val contextRepository: ContextRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        isRefreshing = false,
        packageName = args.get<String>(MainRoutes.ArgPackageName) ?: "",
        userId = args.get<String>(MainRoutes.ArgUserId)?.toIntOrNull() ?: 0,
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitStateSuspend(state.copy(isRefreshing = true))
                packageRepo.refreshFromLocalPackage(state.packageName)
                packageRepo.refreshFromCloudPackage(state.packageName)
                packageRepo.updateLocalPackageDataSize(state.packageName, OpType.BACKUP, state.userId, 0)
                packageRepo.updateLocalPackageArchivesSize(state.packageName, OpType.RESTORE, state.userId)
                packageRepo.updateCloudPackageArchivesSize(state.packageName, OpType.RESTORE, state.userId)
                emitStateSuspend(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.UpdatePackage -> {
                packageRepo.upsert(intent.packageEntity)
            }

            is IndexUiIntent.BackupToLocal -> {
                launchOnGlobal {
                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.task_is_in_progress),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Indefinite,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                    packageRepo.upsert(intent.packageEntity.copy(extraInfo = intent.packageEntity.extraInfo.copy(activated = true)))
                    val backupPreprocessing = localBackupService.preprocessing()
                    localBackupService.processing()
                    localBackupService.postProcessing(backupPreprocessing = backupPreprocessing)
                    localBackupService.destroyService()

                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.backup_completed),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Long,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                }
            }

            is IndexUiIntent.BackupToCloud -> {
                launchOnGlobal {
                    contextRepository.withContext {
                        it.saveCloudActivatedAccountName(intent.name)
                    }
                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.task_is_in_progress),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Indefinite,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                    packageRepo.upsert(intent.packageEntity.copy(extraInfo = intent.packageEntity.extraInfo.copy(activated = true)))
                    val backupPreprocessing = cloudBackupService.preprocessing()
                    cloudBackupService.processing()
                    cloudBackupService.postProcessing(backupPreprocessing = backupPreprocessing)
                    cloudBackupService.destroyService()

                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.backup_completed),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Long,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                }
            }

            is IndexUiIntent.RestoreFromLocal -> {
                launchOnGlobal {
                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.task_is_in_progress),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Indefinite,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                    packageRepo.upsert(intent.packageEntity.copy(extraInfo = intent.packageEntity.extraInfo.copy(activated = true)))
                    localRestoreService.preprocessing()
                    localRestoreService.processing()
                    localRestoreService.postProcessing()
                    localRestoreService.destroyService()

                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.restore_completed),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Long,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                }
            }

            is IndexUiIntent.RestoreFromCloud -> {
                launchOnGlobal {
                    contextRepository.withContext {
                        it.saveCloudActivatedAccountName(intent.name)
                    }
                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.task_is_in_progress),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Indefinite,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                    packageRepo.upsert(intent.packageEntity.copy(extraInfo = intent.packageEntity.extraInfo.copy(activated = true)))
                    cloudRestoreService.preprocessing()
                    cloudRestoreService.processing()
                    cloudRestoreService.postProcessing()
                    cloudRestoreService.destroyService()

                    emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                    emitEffectSuspend(
                        IndexUiEffect.ShowSnackbar(
                            message = contextRepository.getString(R.string.restore_completed),
                            actionLabel = contextRepository.getString(R.string.details),
                            duration = SnackbarDuration.Long,
                            onActionPerformed = {
                                withMainContext {
                                    intent.navController.navigate(MainRoutes.TaskList.route)
                                }
                            }
                        )
                    )
                }
            }

            is IndexUiIntent.Preserve -> {
                packageRepo.preserve(intent.packageEntity)
            }

            is IndexUiIntent.ActiveCloud -> {
                cloudRepo.upsert(intent.cloudEntity.copy(activated = intent.cloudEntity.activated.not()))
            }

            is IndexUiIntent.Delete -> {
                packageRepo.delete(intent.packageEntity)
            }
        }
    }

    private val _activatedCount: Flow<Long> = packageRepo.activatedCount.flowOnIO()
    val activatedState: StateFlow<Boolean> = _activatedCount.map { it != 0L }.stateInScope(false)

    private val _backupItem: Flow<PackageEntity?> = packageRepo.queryPackageFlow(
        packageName = uiState.value.packageName,
        opType = OpType.BACKUP,
        userId = uiState.value.userId,
        preserveId = DefaultPreserveId
    ).flowOnIO()
    val backupItemState: StateFlow<PackageEntity?> = _backupItem.stateInScope(null)
    private val _backupChips: Flow<List<PackageDataChipItem>> = _backupItem.map { p ->
        listOf(
            PackageDataChipItem(
                dataType = DataType.PACKAGE_APK,
                dataBytes = p?.displayStats?.apkBytes,
                selected = p?.apkSelected ?: true,
            ),
            PackageDataChipItem(
                dataType = DataType.PACKAGE_USER,
                dataBytes = p?.displayStats?.userBytes,
                selected = p?.userSelected ?: true,
            ),
            PackageDataChipItem(
                dataType = DataType.PACKAGE_USER_DE,
                dataBytes = p?.displayStats?.userDeBytes,
                selected = p?.userDeSelected ?: true,
            ),
            PackageDataChipItem(
                dataType = DataType.PACKAGE_DATA,
                dataBytes = p?.displayStats?.dataBytes,
                selected = p?.dataSelected ?: true,
            ),
            PackageDataChipItem(
                dataType = DataType.PACKAGE_OBB,
                dataBytes = p?.displayStats?.obbBytes,
                selected = p?.obbSelected ?: true,
            ),
            PackageDataChipItem(
                dataType = DataType.PACKAGE_MEDIA,
                dataBytes = p?.displayStats?.mediaBytes,
                selected = p?.mediaSelected ?: true,
            ),
        )
    }.flowOnIO()
    val backupChipsState: StateFlow<List<PackageDataChipItem>> = _backupChips.stateInScope(listOf())

    private val _restoreItems: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(
        packageName = uiState.value.packageName,
        opType = OpType.RESTORE,
        userId = uiState.value.userId,
    ).map { packages -> packages.sortedBy { it.preserveId }.sortedBy { it.indexInfo.cloud } }.flowOnIO()
    val restoreItemsState: StateFlow<List<PackageEntity>> = _restoreItems.stateInScope(listOf())
    private val _restoreChips: Flow<List<List<PackageDataChipItem>>> = _restoreItems.map { items ->
        items.map { p ->
            listOf(
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_APK,
                    dataBytes = p.displayStats.apkBytes,
                    selected = p.apkSelected,
                ),
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_USER,
                    dataBytes = p.displayStats.userBytes,
                    selected = p.userSelected,
                ),
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_USER_DE,
                    dataBytes = p.displayStats.userDeBytes,
                    selected = p.userDeSelected,
                ),
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_DATA,
                    dataBytes = p.displayStats.dataBytes,
                    selected = p.dataSelected,
                ),
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_OBB,
                    dataBytes = p.displayStats.obbBytes,
                    selected = p.obbSelected,
                ),
                PackageDataChipItem(
                    dataType = DataType.PACKAGE_MEDIA,
                    dataBytes = p.displayStats.mediaBytes,
                    selected = p.mediaSelected,
                ),
            )
        }
    }.flowOnIO()
    val restoreChipsState: StateFlow<List<List<PackageDataChipItem>>> = _restoreChips.stateInScope(listOf())

    private val _accountMenuItems: Flow<List<ActionMenuItem>> = cloudRepo.cloudsMenuItems.flowOnIO()
    val accountMenuItemsState: StateFlow<List<ActionMenuItem>> = _accountMenuItems.stateInScope(listOf())

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accountsState: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
