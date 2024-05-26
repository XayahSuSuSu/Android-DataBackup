package com.xayah.feature.main.restore

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readLastRestoreTime
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageMode
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class IndexUiState(
    val storageIndex: Int,
    val storageType: StorageMode,
    val cloudEntity: CloudEntity?,
    val packages: List<PackageEntity>,
    val packagesSize: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateApps : IndexUiIntent()
    data class SetCloudEntity(val name: String) : IndexUiIntent()
    data class ToAppList(val navController: NavHostController) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pkgRepo: PackageRepository,
    private val cloudRepo: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        storageIndex = 0,
        storageType = StorageMode.Local,
        cloudEntity = null,
        packages = listOf(),
        packagesSize = ""
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
                }).filter(pkgRepo.getFlagPredicateNew(index = context.readRestoreFilterFlagIndex().first()))
                var bytes = 0L
                packages.forEach {
                    bytes += it.displayStats.apkBytes
                    bytes += it.displayStats.userBytes
                    bytes += it.displayStats.userDeBytes
                    bytes += it.displayStats.dataBytes
                    bytes += it.displayStats.obbBytes
                    bytes += it.displayStats.mediaBytes
                }
                emitState(state.copy(packages = packages, packagesSize = bytes.toDouble().formatSize()))
            }

            is IndexUiIntent.SetCloudEntity -> {
                context.saveCloudActivatedAccountName(intent.name)
                emitState(state.copy(cloudEntity = cloudRepo.queryByName(intent.name)))
                emitIntent(IndexUiIntent.UpdateApps)
            }

            is IndexUiIntent.ToAppList -> {
                withMainContext {
                    when (state.storageType) {
                        StorageMode.Local -> {
                            intent.navController.navigate(MainRoutes.PackagesRestoreList.getRoute(" ", URLEncoder.encode(context.localBackupSaveDir(), StandardCharsets.UTF_8.toString())))
                        }

                        StorageMode.Cloud -> {
                            if (state.cloudEntity != null) {
                                intent.navController.navigate(
                                    MainRoutes.PackagesRestoreList.getRoute(
                                        state.cloudEntity.name,
                                        URLEncoder.encode(state.cloudEntity.remote, StandardCharsets.UTF_8.toString())
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
                title = StringResourceToken.fromString(it.name),
                desc = StringResourceToken.fromString(it.user),
            )
        }

    }.flowOnIO()
    val accounts: StateFlow<List<DialogRadioItem<Any>>> = _accounts.stateInScope(listOf())
}
