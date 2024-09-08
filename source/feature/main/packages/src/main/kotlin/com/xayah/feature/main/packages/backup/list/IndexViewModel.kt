package com.xayah.feature.main.packages.backup.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readLoadSystemApps
import com.xayah.core.model.DataState
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.UserInfo
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.encodeURL
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.packages.selectAll
import com.xayah.feature.main.packages.selectApkOnly
import com.xayah.feature.main.packages.selectDataOnly
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class IndexUiState(
    val uuid: UUID,
    val isLoading: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data object OnFastRefresh : IndexUiIntent()
    data class SetUserId(val userId: Int) : IndexUiIntent()
    data object GetUsers : IndexUiIntent()
    data class SortByIndex(val index: Int) : IndexUiIntent()
    data class SortByType(val type: SortType) : IndexUiIntent()
    data class FilterByKey(val key: String) : IndexUiIntent()
    data object ClearKey : IndexUiIntent()
    data class Select(val entity: PackageEntity) : IndexUiIntent()
    data class SelectAll(val selected: Boolean) : IndexUiIntent()
    data object Reverse : IndexUiIntent()
    data class ChangeFlag(val flag: Int, val entity: PackageEntity) : IndexUiIntent()
    data object BlockSelected : IndexUiIntent()
    data class BatchSelectData(val apk: Boolean, val user: Boolean, val userDe: Boolean, val data: Boolean, val obb: Boolean, val media: Boolean) : IndexUiIntent()
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
        uuid = UUID.randomUUID(),
        isLoading = false,
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
                packageRepo.refresh()
                emitState(state.copy(isLoading = false, uuid = UUID.randomUUID()))
            }

            is IndexUiIntent.OnFastRefresh -> {
                emitState(state.copy(isLoading = true))
                packageRepo.fastRefresh()
                emitState(state.copy(isLoading = false))
            }

            is IndexUiIntent.SetUserId -> {
                _userIdIndex.value = intent.userId
            }

            is IndexUiIntent.GetUsers -> {
                _userList.value = rootService.getUsers().map { UserInfo(it.id, it.name) }
            }

            is IndexUiIntent.SortByIndex -> {
                val index = intent.index
                _sortIndexState.value = index
            }

            is IndexUiIntent.SortByType -> {
                var type = intent.type
                type = if (type == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING
                _sortTypeState.value = type
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
                packageRepo.upsert(displayPackagesState.value.onEach { it.extraInfo.activated = intent.selected })
            }

            is IndexUiIntent.Reverse -> {
                packageRepo.upsert(displayPackagesState.value.onEach { it.extraInfo.activated = it.extraInfo.activated.not() })
            }

            is IndexUiIntent.ChangeFlag -> {
                when (intent.flag) {
                    PackageEntity.FLAG_APK -> {
                        packageRepo.upsert(intent.entity.selectDataOnly())
                    }

                    PackageEntity.FLAG_ALL -> {
                        packageRepo.upsert(intent.entity.selectApkOnly())
                    }

                    else -> {
                        packageRepo.upsert(intent.entity.selectAll())
                    }
                }
            }

            is IndexUiIntent.ToPageDetail -> {
                val entity = intent.packageEntity
                withMainContext {
                    intent.navController.navigateSingle(MainRoutes.PackagesBackupDetail.getRoute(entity.packageName.encodeURL(), entity.userId))
                }
            }

            is IndexUiIntent.BlockSelected -> {
                val filtered = displayPackagesState.value.filter { it.extraInfo.activated }
                packageRepo.upsert(filtered.onEach {
                    it.extraInfo.blocked = true
                    it.extraInfo.activated = false
                })
            }

            is IndexUiIntent.BatchSelectData -> {
                val filtered = displayPackagesState.value.filter { it.extraInfo.activated }
                packageRepo.upsert(filtered.onEach {
                    it.dataStates.apkState = if (intent.apk) DataState.Selected else DataState.NotSelected
                    it.dataStates.userState = if (intent.user) DataState.Selected else DataState.NotSelected
                    it.dataStates.userDeState = if (intent.userDe) DataState.Selected else DataState.NotSelected
                    it.dataStates.dataState = if (intent.data) DataState.Selected else DataState.NotSelected
                    it.dataStates.obbState = if (intent.obb) DataState.Selected else DataState.NotSelected
                    it.dataStates.mediaState = if (intent.media) DataState.Selected else DataState.NotSelected
                })
            }
        }
    }

    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(opType = OpType.BACKUP, blocked = false).flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _loadSystemApps: Flow<Boolean> = context.readLoadSystemApps().flowOnIO()
    private var _userIdIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _userList: MutableStateFlow<List<UserInfo>> = MutableStateFlow(listOf())
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntity>> =
        combine(
            _packages,
            _keyState,
            _loadSystemApps,
            _sortIndexState,
            _sortTypeState,
        ) { packages, key, loadSystemApps, sortIndex, sortType ->
            packages.asSequence()
                .filter(packageRepo.getKeyPredicateNew(key = key))
                .filter(packageRepo.getShowSystemAppsPredicate(value = loadSystemApps))
                .sortedWith(packageRepo.getSortComparatorNew(sortIndex = sortIndex, sortType = sortType))
                .sortedByDescending { it.extraInfo.activated }.toList()
        }.flowOnIO()
    private val _displayPackagesState: Flow<List<PackageEntity>> =
        combine(_packagesState, _userIdIndex, _userList) { packages, userIdIndex, userIdList ->
            packages.filter(packageRepo.getUserIdPredicateNew(userId = userIdList.getOrNull(userIdIndex)?.id))
        }.flowOnIO()
    private val _srcPackagesEmptyState: Flow<Boolean> = _packages.map { packages -> packages.isEmpty() }.flowOnIO()
    private val _packagesSelectedState: Flow<Int> = _packagesState.map { packages -> packages.count { it.extraInfo.activated } }.flowOnIO()
    private val _displayPackagesSelectedState: Flow<Map<Int, Int?>> =
        combine(_packagesState, _userList) { packages, userList ->
            val map = mutableMapOf<Int, Int?>()
            userList.forEach { user ->
                val count = packages.count { it.extraInfo.activated && it.indexInfo.userId == user.id }
                map[user.id] = if (count == 0) null else count
            }
            map
        }.flowOnIO()

    val packagesState: StateFlow<List<PackageEntity>> = _packagesState.stateInScope(listOf())
    val displayPackagesState: StateFlow<List<PackageEntity>> = _displayPackagesState.stateInScope(listOf())
    val packagesSelectedState: StateFlow<Int> = _packagesSelectedState.stateInScope(0)
    val displayPackagesSelectedState: StateFlow<Map<Int, Int?>> = _displayPackagesSelectedState.stateInScope(mapOf())
    val loadSystemApps: StateFlow<Boolean> = _loadSystemApps.stateInScope(false)
    val userListState: StateFlow<List<UserInfo>> = _userList.stateInScope(listOf())
    val userIdIndexState: StateFlow<Int> = _userIdIndex.stateInScope(0)
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcPackagesEmptyState: StateFlow<Boolean> = _srcPackagesEmptyState.stateInScope(true)
}
