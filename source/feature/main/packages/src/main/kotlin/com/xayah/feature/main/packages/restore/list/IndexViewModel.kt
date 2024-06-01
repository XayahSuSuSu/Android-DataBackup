package com.xayah.feature.main.packages.restore.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.saveRestoreFilterFlagIndex
import com.xayah.core.datastore.saveRestoreUserIdIndex
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

data class IndexUiState(
    val cloudName: String,
    val cloudRemote: String,
    val selectAll: Boolean,
    val userIdList: List<Int>,
    val filterMode: Boolean,
    val uuid: UUID,
    val isLoading: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data object GetUserIds : IndexUiIntent()
    data class SetUserIdIndexList(val list: List<Int>) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data object ClearKey : IndexUiIntent()
    data class Select(val entity: PackageEntity) : IndexUiIntent()
    data class SelectAll(val selected: Boolean) : IndexUiIntent()
    data class ToPageDetail(val navController: NavHostController, val packageEntity: PackageEntity) : IndexUiIntent()
    data class ToPageSetup(val navController: NavHostController) : IndexUiIntent()
    data object DeleteSelected : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.trim() ?: "",
        selectAll = false,
        userIdList = listOf(),
        filterMode = true,
        uuid = UUID.randomUUID(),
        isLoading = false
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
                emitState(state.copy(isLoading = true))
                withIOContext {
                    if (state.cloudName.isEmpty()) {
                        // Local
                        packageRepo.loadIconsFromLocal()
                        packageRepo.loadPackagesFromLocal()
                    } else {
                        packageRepo.loadIconsFromCloud(state.cloudName)
                        packageRepo.loadPackagesFromCloud(state.cloudName)
                    }
                }
                emitState(state.copy(isLoading = false, uuid = UUID.randomUUID()))
            }

            is IndexUiIntent.GetUserIds -> {
                emitState(state.copy(userIdList = packageRepo.queryUserIds(OpType.RESTORE)))
            }

            is IndexUiIntent.SetUserIdIndexList -> {
                context.saveRestoreUserIdIndex(intent.list)
                _userIdIndexListState.value = intent.list
            }

            is IndexUiIntent.FilterByFlag -> {
                context.saveRestoreFilterFlagIndex(intent.index)
                _flagIndexState.value = intent.index
            }

            is IndexUiIntent.Sort -> {
                var type = intent.type
                val index = intent.index
                if (_sortIndexState.value == index) {
                    type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                    _sortTypeState.value = type
                }
                _sortIndexState.value = index
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.ClearKey -> {
                _keyState.value = ""
            }

            is IndexUiIntent.Select -> {
                packageRepo.upsert(intent.entity.copy(extraInfo = intent.entity.extraInfo.copy(activated = intent.entity.extraInfo.activated.not())))
            }

            is IndexUiIntent.SelectAll -> {
                packageRepo.upsert(packagesState.value.onEach { it.extraInfo.activated = intent.selected })
            }

            is IndexUiIntent.ToPageDetail -> {
                val entity = intent.packageEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.PackagesRestoreDetail.getRoute(entity.packageName, entity.userId, entity.preserveId))
                }
            }

            is IndexUiIntent.ToPageSetup -> {
                withMainContext {
                    intent.navController.navigate(
                        MainRoutes.PackagesRestoreProcessingGraph.getRoute(
                            state.cloudName.ifEmpty { " " },
                            URLEncoder.encode(state.cloudRemote, StandardCharsets.UTF_8.toString())
                        )
                    )
                }
            }

            is IndexUiIntent.DeleteSelected -> {
                val packages = packageRepo.queryActivated(OpType.RESTORE).filter(packageRepo.getFlagPredicateNew(index = context.readRestoreFilterFlagIndex().first()))
                packages.forEach {
                    packageRepo.delete(it)
                }
                rootService.clearEmptyDirectoriesRecursively(packageRepo.backupAppsDir)
            }
        }
    }

    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(OpType.RESTORE, uiState.value.cloudName, uiState.value.cloudRemote).flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndexState: MutableStateFlow<Int> = MutableStateFlow(1)
    private var _userIdIndexListState: MutableStateFlow<List<Int>> = MutableStateFlow(listOf(0))
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntity>> =
        combine(_packages, _keyState, _flagIndexState, _sortIndexState, _sortTypeState) { packages, key, flagIndex, sortIndex, sortType ->
            packages.filter(packageRepo.getKeyPredicateNew(key = key))
                .filter(packageRepo.getFlagPredicateNew(index = flagIndex))
                .sortedWith(packageRepo.getSortComparatorNew(sortIndex = sortIndex, sortType = sortType))
        }.combine(_userIdIndexListState) { packages, userIdIndexList ->
            packages.filter(packageRepo.getUserIdPredicateNew(indexList = userIdIndexList, userIdList = uiState.value.userIdList))
        }.flowOnIO()
    private val _srcPackagesEmptyState: Flow<Boolean> = _packages.map { packages -> packages.isEmpty() }.flowOnIO()
    private val _packagesSelectedState: Flow<Int> = _packagesState.map { packages -> packages.count { it.extraInfo.activated } }.flowOnIO()

    val packagesState: StateFlow<List<PackageEntity>> = _packagesState.stateInScope(listOf())
    val packagesSelectedState: StateFlow<Int> = _packagesSelectedState.stateInScope(0)
    val flagIndexState: StateFlow<Int> = _flagIndexState.stateInScope(1)
    val userIdIndexListState: StateFlow<List<Int>> = _userIdIndexListState.stateInScope(listOf(0))
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcPackagesEmptyState: StateFlow<Boolean> = _srcPackagesEmptyState.stateInScope(true)
}
