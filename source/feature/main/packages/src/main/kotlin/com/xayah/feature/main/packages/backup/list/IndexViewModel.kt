package com.xayah.feature.main.packages.backup.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readBackupFilterFlagIndex
import com.xayah.core.datastore.readBackupUserIdIndex
import com.xayah.core.datastore.readUserIdList
import com.xayah.core.datastore.saveBackupFilterFlagIndex
import com.xayah.core.datastore.saveBackupUserIdIndex
import com.xayah.core.datastore.saveUserIdList
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.RefreshState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.encodeURL
import com.xayah.core.util.module.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class IndexUiState(
    val isRefreshing: Boolean,
    val selectAll: Boolean,
    val filterMode: Boolean,
    val uuid: UUID,
    val isLoading: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data object OnFastRefresh : IndexUiIntent()
    data object GetUserIds : IndexUiIntent()
    data class SetUserIdIndexList(val list: List<Int>) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data object ClearKey : IndexUiIntent()
    data class Select(val entity: PackageEntity) : IndexUiIntent()
    data class SelectAll(val selected: Boolean) : IndexUiIntent()
    data object BlockSelected : IndexUiIntent()
    data class ToPageDetail(val navController: NavHostController, val packageEntity: PackageEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        isRefreshing = false,
        selectAll = false,
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
                _refreshState.value = RefreshState()
                emitState(state.copy(isRefreshing = true))
                packageRepo.refresh(refreshState = _refreshState)
                emitState(state.copy(isRefreshing = false, uuid = UUID.randomUUID()))
            }

            is IndexUiIntent.OnFastRefresh -> {
                emitState(state.copy(isLoading = true))
                packageRepo.fastRefresh()
                emitState(state.copy(isLoading = false))
            }

            is IndexUiIntent.GetUserIds -> {
                context.saveUserIdList(rootService.getUsers().map { it.id })
            }

            is IndexUiIntent.SetUserIdIndexList -> {
                context.saveBackupUserIdIndex(intent.list)
            }

            is IndexUiIntent.FilterByFlag -> {
                context.saveBackupFilterFlagIndex(intent.index)
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
                    intent.navController.navigate(MainRoutes.PackagesBackupDetail.getRoute(entity.packageName.encodeURL(), entity.userId))
                }
            }

            is IndexUiIntent.BlockSelected -> {
                val packages = packageRepo.filterBackup(packageRepo.queryActivated(OpType.BACKUP))
                packages.forEach {
                    it.extraInfo.blocked = true
                    it.extraInfo.activated = false
                }
                packageRepo.upsert(packages)
            }
        }
    }

    private val _refreshState: MutableStateFlow<RefreshState> =
        MutableStateFlow(RefreshState())
    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(opType = OpType.BACKUP, existed = true, blocked = false).flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndex: Flow<Int> = context.readBackupFilterFlagIndex().flowOnIO()
    private var _userIdList: Flow<List<Int>> = context.readUserIdList().flowOnIO()
    private var _userIdIndexList: Flow<List<Int>> = context.readBackupUserIdIndex().flowOnIO()
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntity>> =
        combine(_packages, _keyState, _flagIndex, _sortIndexState, _sortTypeState, _userIdIndexList, _userIdList) { packages, key, flagIndex, sortIndex, sortType, userIdIndexList, userIdList ->
            packages.filter(packageRepo.getKeyPredicateNew(key = key))
                .filter(packageRepo.getFlagPredicateNew(index = flagIndex))
                .sortedWith(packageRepo.getSortComparatorNew(sortIndex = sortIndex, sortType = sortType))
                .filter(packageRepo.getUserIdPredicateNew(indexList = userIdIndexList, userIdList = userIdList))
        }.flowOnIO()
    private val _srcPackagesEmptyState: Flow<Boolean> = _packages.map { packages -> packages.isEmpty() }.flowOnIO()
    private val _packagesSelectedState: Flow<Int> = _packagesState.map { packages -> packages.count { it.extraInfo.activated } }.flowOnIO()

    val packagesState: StateFlow<List<PackageEntity>> = _packagesState.stateInScope(listOf())
    val packagesSelectedState: StateFlow<Int> = _packagesSelectedState.stateInScope(0)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    val flagIndexState: StateFlow<Int> = _flagIndex.stateInScope(1)
    val userIdListState: StateFlow<List<Int>> = _userIdList.stateInScope(listOf(0))
    val userIdIndexListState: StateFlow<List<Int>> = _userIdIndexList.stateInScope(listOf(0))
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcPackagesEmptyState: StateFlow<Boolean> = _srcPackagesEmptyState.stateInScope(true)
}
