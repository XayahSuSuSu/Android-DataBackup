package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.RusticRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.name
import com.xayah.databackup.entity.rustic.RusticSnapshot
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class BackupSnapshotsState(
    val snapshots: List<RusticSnapshot>? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

open class BackupConfigViewModel(
    private val mRoute: BackupConfigRoute,
    private val mBackupConfigRepo: BackupConfigRepository,
    private val mRusticRepo: RusticRepository,
) : BaseViewModel() {
    companion object {
        private val mSharingStarted = SharingStarted.WhileSubscribed(5_000)
    }

    private val mCurrentConfig: BackupConfig?
        get() = mBackupConfigRepo.configs.value.getOrNull(mRoute.index)

    val backupConfig: StateFlow<BackupConfig?> =
        mBackupConfigRepo.configs.map { configs ->
            configs.getOrNull(mRoute.index)
        }.stateIn(
            scope = viewModelScope,
            initialValue = mCurrentConfig,
            started = mSharingStarted,
        )

    private val _snapshots = MutableStateFlow(BackupSnapshotsState())
    val snapshots: StateFlow<BackupSnapshotsState> = _snapshots.asStateFlow()

    private val _deletingSnapshot = MutableStateFlow(false)
    val deletingSnapshot: StateFlow<Boolean> = _deletingSnapshot.asStateFlow()
    private val _snapshotDeleteFailed = MutableStateFlow(false)
    val snapshotDeleteFailed: StateFlow<Boolean> = _snapshotDeleteFailed.asStateFlow()

    fun clearSnapshotDeleteError() {
        _snapshotDeleteFailed.value = false
    }

    fun deleteSnapshot(config: BackupConfig, snapshotId: String, onDeleted: () -> Unit) {
        val backend = config.backupBackend as? BackupBackend.Rustic ?: return
        if (_deletingSnapshot.value) return
        _deletingSnapshot.value = true
        _snapshotDeleteFailed.value = false
        withLock(Dispatchers.IO) {
            try {
                val repositoryPath = PathHelper.getBackupRepoDir(config.path)
                val snapshots = mRusticRepo.deleteSnapshot(repositoryPath, backend.password, snapshotId)
                if (mSnapshotsRepositoryPath == repositoryPath) {
                    _snapshots.value = BackupSnapshotsState(snapshots = snapshots, isLoading = false)
                }
                withContext(Dispatchers.Main) {
                    onDeleted()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _snapshotDeleteFailed.value = true
            } finally {
                _deletingSnapshot.value = false
            }
        }
    }

    private var mSnapshotsRepositoryPath: String? = null

    fun refreshSnapshots(config: BackupConfig) = withLock(Dispatchers.IO) {
        refreshSnapshotsLocked(config)
    }

    private suspend fun refreshSnapshotsLocked(config: BackupConfig) {
        val backend = config.backupBackend as? BackupBackend.Rustic ?: return
        val repositoryPath = PathHelper.getBackupRepoDir(config.path)
        val retained = _snapshots.value.snapshots.takeIf { mSnapshotsRepositoryPath == repositoryPath }
        mSnapshotsRepositoryPath = repositoryPath
        _snapshots.value = BackupSnapshotsState(snapshots = retained)
        val cached = retained ?: mRusticRepo.readCachedSnapshots(repositoryPath)?.sortedByDescending { it.createdAt }
        _snapshots.value = BackupSnapshotsState(snapshots = cached)
        runCatching {
            val snapshots = if (mRusticRepo.repositoryExists(repositoryPath)) {
                mRusticRepo.listSnapshots(repositoryPath, backend.password).sortedByDescending { it.createdAt }
            } else {
                emptyList()
            }
            _snapshots.value = BackupSnapshotsState(snapshots = snapshots, isLoading = false)
        }.onFailure { error ->
            if (error is CancellationException || error !is Exception) throw error
            _snapshots.value = BackupSnapshotsState(snapshots = cached, isLoading = false, hasError = true)
        }
    }

    fun changeName(name: String) {
        withLock(Dispatchers.Default) {
            mCurrentConfig?.let { config ->
                mBackupConfigRepo.updateConfig(config.uuidString) {
                    copy {
                        BackupConfig.name set name
                    }
                }
            }
        }
    }

    fun deleteConfig(onDeleted: suspend () -> Unit) {
        withLock(Dispatchers.Default) {
            mCurrentConfig?.let { config ->
                mBackupConfigRepo.deleteConfig(config.uuidString)
            }
            onDeleted()
        }
    }

    fun selectBackup(onSelected: () -> Unit) {
        withLock(Dispatchers.IO) {
            mBackupConfigRepo.selectBackup(mRoute.index)
            withContext(Dispatchers.Main) {
                onSelected()
            }
        }
    }
}
