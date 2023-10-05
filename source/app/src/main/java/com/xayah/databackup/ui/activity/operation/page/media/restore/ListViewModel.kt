package com.xayah.databackup.ui.activity.operation.page.media.restore

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.data.MediaDao
import com.xayah.databackup.data.MediaRestoreEntity
import com.xayah.databackup.data.MediaRestoreWithOpEntity
import com.xayah.databackup.service.OperationLocalService
import com.xayah.databackup.ui.activity.operation.page.media.backup.OpType
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class MediaRestoreListUiState(
    val mutex: Mutex,
    val isLoading: Boolean,
    val opType: OpType,
    val timestamps: List<Long>,
    val selectedIndex: Int,
    val mediaDao: MediaDao,
) {
    val timestamp: Long
        get() = timestamps.getOrElse(selectedIndex) { 0 }
    val medium: Flow<List<MediaRestoreWithOpEntity>> = mediaDao.queryAllRestoreFlow(timestamp).distinctUntilChanged()
    val selectedCount: Flow<Int> = mediaDao.countRestoreSelected(timestamp).distinctUntilChanged()
}

@HiltViewModel
class MediaRestoreListViewModel @Inject constructor(private val mediaDao: MediaDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        MediaRestoreListUiState(
            mutex = Mutex(),
            isLoading = false,
            opType = OpType.LIST,
            timestamps = listOf(),
            selectedIndex = 0,
            mediaDao = mediaDao
        )
    )
    val uiState: State<MediaRestoreListUiState>
        get() = _uiState

    suspend fun upsertRestore(item: MediaRestoreEntity) = mediaDao.upsertRestore(item)
    private suspend fun queryTimestamps() = mediaDao.queryTimestamps()
    suspend fun deleteRestore(item: MediaRestoreEntity) = mediaDao.deleteRestore(item)

    private fun setType(type: OpType) = run { _uiState.value = uiState.value.copy(opType = type) }

    private fun setTimestamps(timestamps: List<Long>) {
        _uiState.value = uiState.value.copy(timestamps = timestamps)
    }

    fun setSelectedIndex(index: Int) {
        val uiState by uiState
        _uiState.value = uiState.copy(selectedIndex = index)
    }

    suspend fun initialize() {
        if (uiState.value.opType == OpType.LIST)
            withIOContext {
                val timestamps = queryTimestamps()
                setTimestamps(timestamps)
                setSelectedIndex(timestamps.lastIndex)
            }
    }

    /**
     * Clearly tinkering with the itinerary contained within the flow was a bad idea!
     * @see <a href="https://issuetracker.google.com/issues/291640109#comment9">https://issuetracker.google.com/issues/291640109#comment9</a>
     */
    suspend fun updateMediaSizeBytes(context: Context, media: MediaRestoreEntity) = withIOContext {
        val remoteRootService = RemoteRootService(context)
        val sizeBytes = remoteRootService.calculateSize(media.archivePath)
        if (media.sizeBytes != sizeBytes) {
            upsertRestore(media.copy(sizeBytes = sizeBytes))
        }
        remoteRootService.destroyService()
    }

    fun onProcessing() {
        val uiState by uiState

        viewModelScope.launch {
            uiState.mutex.withLock {
                if (uiState.opType == OpType.LIST) {
                    withIOContext {
                        setType(OpType.PROCESSING)

                        val operationLocalService = OperationLocalService(context = DataBackupApplication.application)
                        operationLocalService.restoreMedium(uiState.timestamp)
                        operationLocalService.destroyService()

                        setType(OpType.LIST)
                    }
                }
            }
        }
    }

    fun setPath(pathString: String, media: MediaRestoreEntity) {
        viewModelScope.launch {
            withIOContext {
                if (pathString.isNotEmpty()) {
                    upsertRestore(media.copy(path = pathString))
                }
            }
        }
    }
}
