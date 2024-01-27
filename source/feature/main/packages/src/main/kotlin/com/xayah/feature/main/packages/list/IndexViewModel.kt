package com.xayah.feature.main.packages.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageEntityWithCount
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.packages.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class IndexUiState(
    val isRefreshing: Boolean,
    val userIdList: List<Int>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class ToPagePackageDetail(val navController: NavHostController, val packageEntity: PackageEntity) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data class Sort(val index: Int, val type: SortType) : IndexUiIntent()
    data class FilterByFlag(val index: Int) : IndexUiIntent()
    data object GetUserIds : IndexUiIntent()
    data class SetUserIdIndexList(val list: List<Int>) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isRefreshing = false, userIdList = listOf())) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitStateSuspend(state.copy(isRefreshing = true))
                packageRepo.refresh(topBarState = _topBarState)
                emitStateSuspend(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.ToPagePackageDetail -> {
                val entity = intent.packageEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.PackageDetail.getRoute(entity.packageName, entity.userId))
                }
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
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

            is IndexUiIntent.FilterByFlag -> {
                _flagIndexState.value = intent.index
            }

            is IndexUiIntent.GetUserIds -> {
                emitState(state.copy(userIdList = rootService.getUsers().map { it.id }))
            }

            is IndexUiIntent.SetUserIdIndexList -> {
                _userIdIndexListState.value = intent.list
            }
        }
    }

    private val _topBarState: MutableStateFlow<TopBarState> =
        MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.app_and_data)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _packages: Flow<List<PackageEntityWithCount>> = packageRepo.packages.flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _flagIndexState: MutableStateFlow<Int> = MutableStateFlow(1)
    val flagIndexState: StateFlow<Int> = _flagIndexState.stateInScope(1)
    private var _userIdIndexListState: MutableStateFlow<List<Int>> = MutableStateFlow(listOf(0))
    val userIdIndexListState: StateFlow<List<Int>> = _userIdIndexListState.stateInScope(listOf(0))
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntityWithCount>> =
        combine(_packages, _keyState, _flagIndexState, _sortIndexState, _sortTypeState) { packages, key, flagIndex, sortIndex, sortType ->
            packages.filter(packageRepo.getKeyPredicate(key = key))
                .filter(packageRepo.getFlagPredicate(index = flagIndex))
                .sortedWith(packageRepo.getSortComparator(sortIndex = sortIndex, sortType = sortType))
        }.combine(_userIdIndexListState) { packages, userIdIndexList ->
            packages.filter(packageRepo.getUserIdPredicate(indexList = userIdIndexList, userIdList = uiState.value.userIdList))
        }.flowOnIO()
    val packagesState: StateFlow<List<PackageEntityWithCount>> = _packagesState.stateInScope(listOf())
}
