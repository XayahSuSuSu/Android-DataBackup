package com.xayah.databackup.ui.activity.operation.page.media.backup

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaBackupEntity
import com.xayah.databackup.data.MediaBackupEntityUpsert
import com.xayah.databackup.data.MediaBackupWithOpEntity
import com.xayah.databackup.data.MediaDao
import com.xayah.databackup.service.OperationLocalService
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withIOContext
import com.xayah.librootservice.util.withMainContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

enum class OpType {
    LIST,
    PROCESSING,
}

data class MediaBackupListUiState(
    val mutex: Mutex,
    val isLoading: Boolean,
    val opType: OpType,
    val timestamp: Long,
    val mediaDao: MediaDao,
) {
    val medium: Flow<List<MediaBackupWithOpEntity>> = mediaDao.queryAllBackupFlow().distinctUntilChanged()
    val selectedCount: Flow<Int> = mediaDao.countBackupSelected().distinctUntilChanged()
}

@HiltViewModel
class MediaBackupListViewModel @Inject constructor(private val mediaDao: MediaDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        MediaBackupListUiState(
            mutex = Mutex(),
            isLoading = true,
            opType = OpType.LIST,
            timestamp = DateUtil.getTimestamp(),
            mediaDao = mediaDao
        )
    )
    val uiState: State<MediaBackupListUiState>
        get() = _uiState

    private suspend fun queryAllBackup() = mediaDao.queryAllBackup()
    suspend fun upsertBackup(item: MediaBackupEntity) = mediaDao.upsertBackup(item)
    private suspend fun upsertBackup(items: List<MediaBackupEntityUpsert>) = mediaDao.upsertBackup(items)
    suspend fun updateBackupSelected(selected: Boolean) = mediaDao.updateBackupSelected(selected)
    suspend fun deleteBackup(item: MediaBackupEntity) = mediaDao.deleteBackup(item)

    suspend fun initialize() {
        if (uiState.value.opType == OpType.LIST)
            withIOContext {
                upsertBackup(ConstantUtil.DefaultMediaList.map { (name, path) -> MediaBackupEntityUpsert(path = path, name = name) })
                _uiState.value = uiState.value.copy(isLoading = false)
            }
    }

    private fun setType(type: OpType) = run { _uiState.value = uiState.value.copy(opType = type) }
    private fun updateTimestamp() = run { _uiState.value = uiState.value.copy(timestamp = DateUtil.getTimestamp()) }

    private fun renameDuplicateMedia(name: String): String {
        val nameList = name.split("_").toMutableList()
        val index = nameList.first().toIntOrNull()
        if (index == null) {
            nameList.add("0")
        } else {
            nameList[nameList.lastIndex] = (index + 1).toString()
        }
        return nameList.joinToString(separator = "_")
    }

    fun onAdd(context: ComponentActivity) {
        PickYouLauncher().apply {
            setTitle(context.getString(R.string.select_target_directory))
            setType(PickerType.DIRECTORY)
            setLimitation(0)
            launch(context) { pathList ->
                viewModelScope.launch {
                    withIOContext {
                        val customMediaList = mutableListOf<MediaBackupEntityUpsert>()
                        pathList.forEach { pathString ->
                            if (pathString.isNotEmpty() && ConstantUtil.DefaultMediaList.indexOfFirst { it.second == pathString } == -1) {
                                if (pathString == context.readBackupSavePath().first()) {
                                    withMainContext {
                                        Toast.makeText(context, context.getString(R.string.backup_dir_as_media_error), Toast.LENGTH_SHORT).show()
                                    }
                                    return@forEach
                                }
                                var name = PathUtil.getFileName(pathString)
                                queryAllBackup().forEach {
                                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                                }
                                customMediaList.forEach {
                                    if (it.name == name && it.path != pathString) name = renameDuplicateMedia(name)
                                }
                                customMediaList.add(MediaBackupEntityUpsert(path = pathString, name = name))
                            }
                        }
                        upsertBackup(customMediaList)
                    }
                }
            }
        }
    }

    /**
     * Clearly tinkering with the itinerary contained within the flow was a bad idea!
     * @see <a href="https://issuetracker.google.com/issues/291640109#comment9">https://issuetracker.google.com/issues/291640109#comment9</a>
     */
    suspend fun updateMediaSizeBytes(context: Context, media: MediaBackupEntity) = withIOContext {
        val remoteRootService = RemoteRootService(context)
        val sizeBytes = remoteRootService.calculateSize(media.path)
        if (media.sizeBytes != sizeBytes) {
            upsertBackup(media.copy(sizeBytes = sizeBytes))
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

                        updateTimestamp()

                        val operationLocalService = OperationLocalService(context = DataBackupApplication.application)
                        operationLocalService.backupMedium(uiState.timestamp)
                        operationLocalService.destroyService()

                        setType(OpType.LIST)
                    }
                }
            }
        }
    }
}
