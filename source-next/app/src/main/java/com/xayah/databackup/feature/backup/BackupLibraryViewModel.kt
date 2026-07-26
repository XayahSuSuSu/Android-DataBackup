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
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    private val isLoading = MutableStateFlow(backupConfigRepository.configs.value.isEmpty())
    private val searchQuery = MutableStateFlow("")
    private val filter = MutableStateFlow(BackupLibraryFilter.All)

    val uiState: StateFlow<BackupLibraryUiState> =
        combine(isLoading, backupConfigRepository.configs, searchQuery, filter) { loading, backups, query, filter ->
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
            initialValue = backupConfigRepository.configs.value
                .takeIf { it.isNotEmpty() }
                ?.let { BackupLibraryUiState.Content(it) }
                ?: BackupLibraryUiState.Loading,
        )

    fun initialize() {
        withLock(Dispatchers.IO) {
            backupConfigRepository.loadBackupConfigsFromLocal()
            isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateFilter(value: BackupLibraryFilter) {
        filter.value = value
    }

    fun clearFilters() {
        searchQuery.value = ""
        filter.value = BackupLibraryFilter.All
    }
}
