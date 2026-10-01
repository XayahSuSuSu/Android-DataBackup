package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface BackupLibraryUiState {
    data object Loading : BackupLibraryUiState

    data object Empty : BackupLibraryUiState

    data class Content(
        val backups: List<BackupConfig>,
        val searchQuery: String = "",
        val filter: BackupLibraryFilter = BackupLibraryFilter.All,
    ) : BackupLibraryUiState {
        val filteredBackups: List<IndexedValue<BackupConfig>>
            get() = backups
                .withIndex()
                .filter { (_, backup) ->
                    when (filter) {
                        BackupLibraryFilter.All -> true
                        BackupLibraryFilter.Rustic -> backup.backupBackend is BackupBackend.Rustic
                        BackupLibraryFilter.Archive -> backup.backupBackend is BackupBackend.Archive
                    }
                }
                .filter { (_, backup) ->
                    searchQuery.isBlank() ||
                            backup.displayName.contains(searchQuery, ignoreCase = true) ||
                            backup.path.contains(searchQuery, ignoreCase = true)
                }
    }
}

enum class BackupLibraryFilter {
    All,
    Rustic,
    Archive,
}

class BackupLibraryViewModel(
    private val mBackupConfigRepo: BackupConfigRepository,
) : BaseViewModel() {
    private val _isLoading = MutableStateFlow(mBackupConfigRepo.configs.value.isEmpty())
    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(BackupLibraryFilter.All)

    val uiState: StateFlow<BackupLibraryUiState> =
        combine(_isLoading, mBackupConfigRepo.configs, _searchQuery, _filter) { loading, backups, query, filter ->
            when {
                loading -> BackupLibraryUiState.Loading
                backups.isEmpty() -> BackupLibraryUiState.Empty
                else -> BackupLibraryUiState.Content(
                    backups = backups,
                    searchQuery = query,
                    filter = filter,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mBackupConfigRepo.configs.value
                .takeIf { it.isNotEmpty() }
                ?.let { BackupLibraryUiState.Content(it) }
                ?: BackupLibraryUiState.Loading,
        )

    fun initialize() {
        withLock(Dispatchers.IO) {
            mBackupConfigRepo.loadBackupConfigsFromLocal()
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(value: BackupLibraryFilter) {
        _filter.value = value
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _filter.value = BackupLibraryFilter.All
    }
}
