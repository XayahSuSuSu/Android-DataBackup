package com.xayah.feature.main.packages.restore.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.PackageRepository
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
import com.xayah.core.util.decodeURL
import com.xayah.core.util.encodeURL
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.packages.R
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
    val cloudName: String,
    val cloudRemote: String,
    val uuid: UUID,
    val isLoading: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
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
    data class BatchSelectData(val apk: Boolean, val user: Boolean, val userDe: Boolean, val data: Boolean, val obb: Boolean, val media: Boolean) :
        IndexUiIntent()
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
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim() ?: "",
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

            is IndexUiIntent.SetUserId -> {
                _userIdIndex.value = intent.userId
            }

            is IndexUiIntent.GetUsers -> {
                _userList.value = packageRepo.queryUserIds(OpType.RESTORE).map { UserInfo(it, context.getString(R.string.user)) }
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
                    intent.navController.navigateSingle(MainRoutes.PackagesRestoreDetail.getRoute(entity.packageName.encodeURL(), entity.userId, entity.preserveId))
                }
            }

            is IndexUiIntent.ToPageSetup -> {
                withMainContext {
                    intent.navController.navigateSingle(
                        MainRoutes.PackagesRestoreProcessingGraph.getRoute(
                            state.cloudName.ifEmpty { " " }.encodeURL(),
                            state.cloudRemote.encodeURL()
                        )
                    )
                }
            }

            is IndexUiIntent.DeleteSelected -> {
                val filtered = displayPackagesState.value.filter { it.extraInfo.activated }
                filtered.forEach {
                    packageRepo.delete(it)
                }
                rootService.clearEmptyDirectoriesRecursively(packageRepo.backupAppsDir)
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

    private val _packages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(OpType.RESTORE, uiState.value.cloudName, uiState.value.cloudRemote).flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private var _userIdIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _userList: MutableStateFlow<List<UserInfo>> = MutableStateFlow(listOf())
    private var _sortIndexState: MutableStateFlow<Int> = MutableStateFlow(0)
    private var _sortTypeState: MutableStateFlow<SortType> = MutableStateFlow(SortType.ASCENDING)
    private val _packagesState: Flow<List<PackageEntity>> =
        combine(_packages, _keyState, _sortIndexState, _sortTypeState) { packages, key, sortIndex, sortType ->
            packages.asSequence()
                .filter(packageRepo.getKeyPredicateNew(key = key))
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
    val userListState: StateFlow<List<UserInfo>> = _userList.stateInScope(listOf())
    val userIdIndexState: StateFlow<Int> = _userIdIndex.stateInScope(0)
    val sortIndexState: StateFlow<Int> = _sortIndexState.stateInScope(0)
    val sortTypeState: StateFlow<SortType> = _sortTypeState.stateInScope(SortType.ASCENDING)
    val srcPackagesEmptyState: StateFlow<Boolean> = _srcPackagesEmptyState.stateInScope(true)
}
