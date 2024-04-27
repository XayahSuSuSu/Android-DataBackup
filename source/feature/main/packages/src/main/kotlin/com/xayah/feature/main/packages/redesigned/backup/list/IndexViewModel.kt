package com.xayah.feature.main.packages.redesigned.backup.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.RefreshState
import com.xayah.core.ui.route.MainRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val isRefreshing: Boolean,
    val selectAll: Boolean,
    val userIdList: List<Int>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data object GetUserIds : IndexUiIntent()
    data class SetUserIdIndexList(val list: List<Int>) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data class Select(val entity: PackageEntity) : IndexUiIntent()
    data class SelectAll(val selected: Boolean) : IndexUiIntent()
    data class ToPageDetail(val navController: NavHostController, val packageEntity: PackageEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isRefreshing = false, selectAll = false, userIdList = listOf())) {
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
                _refreshState.value = RefreshState()
                emitStateSuspend(state.copy(isRefreshing = true))
                packageRepo.refresh(refreshState = _refreshState)
                emitStateSuspend(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.GetUserIds -> {
                emitState(state.copy(userIdList = rootService.getUsers().map { it.id }))
            }

            is IndexUiIntent.SetUserIdIndexList -> {
                _userIdIndexListState.value = intent.list
            }

            is IndexUiIntent.FilterByFlag -> {
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

            is IndexUiIntent.Select -> {
                packageRepo.upsert(intent.entity.copy(extraInfo = intent.entity.extraInfo.copy(activated = intent.entity.extraInfo.activated.not())))
            }

            is IndexUiIntent.SelectAll -> {
                packageRepo.upsert(packagesState.value.onEach { it.extraInfo.activated = intent.selected })
            }

            is IndexUiIntent.ToPageDetail -> {
                val entity = intent.packageEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.PackagesBackupDetail.getRoute(entity.packageName, entity.userId))
                }
            }
        }
    }

    private val _refreshState: MutableStateFlow<RefreshState> =
        MutableStateFlow(RefreshState())
    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(OpType.BACKUP).flowOnIO()
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

    val packagesState: StateFlow<List<PackageEntity>> = _packagesState.stateInScope(listOf())
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    val flagIndexState: StateFlow<Int> = _flagIndexState.stateInScope(1)
    val userIdIndexListState: StateFlow<List<Int>> = _userIdIndexListState.stateInScope(listOf(0))
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcPackagesEmptyState: StateFlow<Boolean> = _srcPackagesEmptyState.stateInScope(true)
}
