package com.xayah.databackup.data

import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreRequest
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.entity.restore.selectAppPart
import com.xayah.databackup.entity.restore.selectAppParts
import com.xayah.databackup.entity.restore.selectCategory
import com.xayah.databackup.entity.restore.selectItem
import com.xayah.databackup.entity.restore.selectItems
import com.xayah.databackup.entity.restore.toRestoreRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class RestoreRepository(
    private val mBackupConfigRepo: BackupConfigRepository,
    private val mRusticRepo: RusticRepository,
    initialState: RestoreState = RestoreState(),
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<RestoreState> = _state.asStateFlow()
    private var mPendingRequest: RestoreRequest? = null

    suspend fun loadSnapshot(configUuid: String, snapshotId: String) {
        withContext(Dispatchers.IO) {
            if (mBackupConfigRepo.isLoaded.value.not()) mBackupConfigRepo.loadBackupConfigsFromLocal()
            val config = requireNotNull(mBackupConfigRepo.configs.value.find { it.uuidString == configUuid })
            val state = when (config.backupBackend) {
                is BackupBackend.Rustic -> mRusticRepo.loadRestoreState(config, snapshotId)
                is BackupBackend.Archive -> error("Archive restore is not implemented.")
            }
            _state.value = state
        }
    }

    internal fun prepareRestore() {
        mPendingRequest = null
        mPendingRequest = state.value.toRestoreRequest()
    }

    internal fun consumeRestoreRequest(): RestoreRequest? = mPendingRequest.also { mPendingRequest = null }

    internal fun updateState(state: RestoreState) {
        _state.value = state
    }

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        _state.update { it.selectCategory(category, checked) }
    }

    fun selectItem(id: String, checked: Boolean) {
        _state.update { it.selectItem(id, checked) }
    }

    fun selectItems(ids: Set<String>, checked: Boolean) {
        _state.update { it.selectItems(ids, checked) }
    }

    fun selectAppPart(id: String, part: BackupSourceCategory, checked: Boolean) {
        _state.update { it.selectAppPart(id, part, checked) }
    }

    fun selectAppParts(ids: Set<String>, parts: Set<BackupSourceCategory>, checked: Boolean) {
        _state.update { it.selectAppParts(ids, parts, checked) }
    }
}
