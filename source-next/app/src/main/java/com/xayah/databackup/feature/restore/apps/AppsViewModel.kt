package com.xayah.databackup.feature.restore.apps

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.entity.restore.toAppOptions
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DefStorageSize
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.SortsType
import com.xayah.databackup.util.filterApp
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.sortByA2Z
import com.xayah.databackup.util.sortByDataSize
import com.xayah.databackup.util.sortByInstallTime
import com.xayah.databackup.util.sortBySelectedFirst
import com.xayah.databackup.util.sortByUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class UiState(
    val userId: Int? = null,
    val sortsType: SortsType = SortsType.A2Z,
    val sortsSequence: SortsSequence = SortsSequence.ASCENDING,
    val selectedFirst: Boolean = false,
    val filtersUserApps: Boolean = true,
    val filtersSystemApps: Boolean = false,
)

class AppsViewModel(
    private val mRestoreRepo: RestoreRepository,
) : BaseViewModel() {
    private val mSharingStarted = SharingStarted.WhileSubscribed(5_000)
    val state: StateFlow<RestoreState> = mRestoreRepo.state
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val users: StateFlow<List<Int>> = state.map { it.inventory?.apps.orEmpty().values.map { app -> app.userId }.distinct().sorted() }
        .stateIn(viewModelScope, mSharingStarted, emptyList())
    val items: StateFlow<List<Map.Entry<String, App>>> = combine(state, searchText, uiState) { state, query, filters ->
        val entries = state.inventory?.apps.orEmpty()
        val keys = entries.entries.associate { it.value.pkgUserKey to it.key }
        val currentUser = filters.userId ?: entries.values.minOfOrNull { it.userId } ?: 0
        val apps = entries.map { (id, app) ->
            val parts = state.appParts[id].orEmpty()
            app.copy(option = parts.toAppOptions())
        }.filterApp(currentUser, filters.filtersUserApps, filters.filtersSystemApps)
        when (filters.sortsType) {
            SortsType.A2Z -> apps.sortByA2Z(filters.sortsSequence)
            SortsType.DATA_SIZE -> apps.sortByDataSize(filters.sortsSequence)
            SortsType.INSTALL_TIME -> apps.sortByInstallTime(filters.sortsSequence)
            SortsType.UPDATE_TIME -> apps.sortByUpdateTime(filters.sortsSequence)
        }.sortBySelectedFirst(filters.selectedFirst).filterApp(query).associateBy { keys.getValue(it.pkgUserKey) }.entries.toList()
    }.stateIn(viewModelScope, mSharingStarted, emptyList())
    val selectedBytes: StateFlow<String> = items.map { apps -> apps.sumOf { it.value.selectedBytes }.formatToStorageSize }
        .stateIn(viewModelScope, mSharingStarted, DefStorageSize)

    fun selectUser(id: Int) {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(userId = id) }
        }
    }

    fun toggleSequence() {
        withLock(Dispatchers.Default) {
            _uiState.update {
                it.copy(sortsSequence = if (it.sortsSequence == SortsSequence.ASCENDING) SortsSequence.DESCENDING else SortsSequence.ASCENDING)
            }
        }
    }

    fun toggleSelectedFirst() {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(selectedFirst = !it.selectedFirst) }
        }
    }

    fun selectSortType(type: SortsType) {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(sortsType = type) }
        }
    }

    fun toggleUserApps() {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(filtersUserApps = !it.filtersUserApps) }
        }
    }

    fun toggleSystemApps() {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(filtersSystemApps = !it.filtersSystemApps) }
        }
    }

    fun selectAppPart(id: String, part: BackupSourceCategory, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectAppPart(id, part, checked)
        }
    }

    fun getAllPartsSelected(parts: Set<BackupSourceCategory>): Boolean {
        val available = items.value.map { it.key }.flatMap { id ->
            state.value.inventory?.availableAppParts?.get(id).orEmpty().intersect(parts).map { id to it }
        }
        return available.isNotEmpty() && available.all { (id, part) -> part in state.value.appParts[id].orEmpty() }
    }

    fun selectAllParts(parts: Set<BackupSourceCategory>) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectAppParts(items.value.map { it.key }.toSet(), parts, !getAllPartsSelected(parts))
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }

    fun selectItem(id: String, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectItem(id, checked)
        }
    }
}
