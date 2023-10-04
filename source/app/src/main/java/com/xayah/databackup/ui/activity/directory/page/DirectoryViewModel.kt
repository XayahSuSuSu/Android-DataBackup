package com.xayah.databackup.ui.activity.directory.page

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.DirectoryDao
import com.xayah.databackup.data.DirectoryEntity
import com.xayah.databackup.data.DirectoryType
import com.xayah.databackup.data.DirectoryUpsertEntity
import com.xayah.databackup.data.StorageType
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.command.toSpaceString
import com.xayah.databackup.util.saveBackupSavePath
import com.xayah.databackup.util.saveRestoreSavePath
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil
import com.xayah.librootservice.util.ExceptionUtil.tryOnScope
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.pathString

data class DirectoryUiState(
    val isLoading: Boolean,
    val directoryType: DirectoryType,
    val directoryDao: DirectoryDao,
) {
    val directories: Flow<List<DirectoryEntity>> = directoryDao.queryActiveDirectoriesFlow().distinctUntilChanged()
}

@HiltViewModel
class DirectoryViewModel @Inject constructor(private val directoryDao: DirectoryDao) : ViewModel() {
    private val _uiState = mutableStateOf(DirectoryUiState(isLoading = true, directoryType = DirectoryType.BACKUP, directoryDao = directoryDao))
    val uiState: State<DirectoryUiState>
        get() = _uiState

    private suspend fun queryId(parent: String, child: String, type: DirectoryType) = directoryDao.queryId(parent, child, type)
    private suspend fun inactivateDirectories() = directoryDao.updateActive(active = false)
    private suspend fun activateDirectories() = directoryDao.updateActive(type = uiState.value.directoryType, excludeType = StorageType.EXTERNAL, active = true)
    private suspend fun querySelectedByDirectoryType() = directoryDao.querySelectedByDirectoryType(type = uiState.value.directoryType)
    suspend fun select(context: Context, path: String, type: DirectoryType, id: Long) = run {
        when (uiState.value.directoryType) {
            DirectoryType.BACKUP -> context.saveBackupSavePath(path)
            DirectoryType.RESTORE -> context.saveRestoreSavePath(path)
        }

        directoryDao.select(type, id)
    }

    suspend fun reset(context: Context) = run {
        select(
            context = context,
            path = "${ConstantUtil.DefaultPathParent}/${ConstantUtil.DefaultPathChild}",
            type = uiState.value.directoryType,
            id = when (uiState.value.directoryType) {
                DirectoryType.BACKUP -> 1
                DirectoryType.RESTORE -> 2
            }
        )
    }

    private suspend fun queryActiveDirectories() = directoryDao.queryActiveDirectories()
    private suspend fun upsert(items: List<DirectoryUpsertEntity>) = directoryDao.upsert(items)
    private suspend fun upsert(item: DirectoryEntity) = directoryDao.upsert(item)
    suspend fun delete(item: DirectoryEntity) = directoryDao.delete(item)

    suspend fun initialize(context: Context, directoryType: DirectoryType) {
        withIOContext {
            // Set directory type
            _uiState.value = uiState.value.copy(directoryType = directoryType)

            // Inactivate all directories
            inactivateDirectories()

            // Internal storage
            val internalDirs = mutableListOf<DirectoryUpsertEntity>()
            val internalDirectory = DirectoryUpsertEntity(
                id = 1,
                title = context.getString(R.string.internal_storage),
                parent = ConstantUtil.DefaultPathParent,
                child = ConstantUtil.DefaultPathChild,
                directoryType = DirectoryType.BACKUP,
                storageType = StorageType.INTERNAL,
            )
            internalDirs.add(internalDirectory)
            internalDirs.add(internalDirectory.copy(id = 2, directoryType = DirectoryType.RESTORE))
            upsert(internalDirs)

            // External storage
            val externalList = PreparationUtil.listExternalStorage()
            val externalDirs = mutableListOf<DirectoryUpsertEntity>()
            for (storageItem in externalList) {
                // e.g. /mnt/media_rw/E7F9-FA61
                tryOnScope {
                    val child = ConstantUtil.DefaultPathChild
                    externalDirs.add(
                        DirectoryUpsertEntity(
                            id = queryId(parent = storageItem, child = child, directoryType),
                            title = context.getString(R.string.external_storage),
                            parent = storageItem,
                            child = child,
                            directoryType = directoryType,
                            storageType = StorageType.EXTERNAL,
                            active = true,
                        )
                    )
                }
            }
            upsert(externalDirs)

            // Activate backup/restore directories except external directories
            activateDirectories()

            // Read statFs of each storage
            queryActiveDirectories().forEach { entity ->
                val parent = entity.parent
                ExceptionUtil.tryService(onFailed = { msg ->
                    entity.error = "${context.getString(R.string.fetch_failed)}: $msg\n${context.getString(R.string.remote_service_err_info)}"
                }) {
                    entity.error = ""
                    val remoteRootService = RemoteRootService(context)
                    val statFs = remoteRootService.readStatFs(parent)
                    entity.availableBytes = statFs.availableBytes
                    entity.totalBytes = statFs.totalBytes
                    remoteRootService.destroyService()
                }
                if (entity.storageType == StorageType.EXTERNAL) {
                    val tags = mutableListOf<String>()
                    val type = PreparationUtil.getExternalStorageType(parent)
                    tags.add(type)
                    // Check the format
                    val supported = type.lowercase() in ConstantUtil.SupportedExternalStorageFormat
                    if (supported.not()) {
                        tags.add(context.getString(R.string.unsupported_format))
                        entity.error = "${context.getString(R.string.supported_format)}: ${ConstantUtil.SupportedExternalStorageFormat.toSpaceString()}"
                        entity.enabled = false
                    } else {
                        entity.error = ""
                        entity.enabled = true
                    }
                    entity.tags = tags
                }

                upsert(entity)
            }

            val selectedDirectory = querySelectedByDirectoryType()
            if (selectedDirectory == null || (selectedDirectory.storageType == StorageType.EXTERNAL && selectedDirectory.enabled.not()) || selectedDirectory.active.not())
                reset(context = context)
            _uiState.value = uiState.value.copy(isLoading = false)
        }
    }

    fun onAdd(context: ComponentActivity) {
        PickYouLauncher().apply {
            setTitle(context.getString(R.string.select_target_directory))
            setType(PickerType.DIRECTORY)
            setLimitation(0)
            launch(context) { pathList ->
                viewModelScope.launch {
                    withIOContext {
                        val customDirList = mutableListOf<DirectoryUpsertEntity>()
                        pathList.forEach { pathString ->
                            if (pathString.isNotEmpty()) {
                                val path = Paths.get(pathString)
                                val parent = path.parent.pathString
                                val child = path.name

                                // Custom storage
                                val dir = DirectoryUpsertEntity(
                                    id = queryId(parent = parent, child = child, type = uiState.value.directoryType),
                                    title = context.getString(R.string.custom_storage),
                                    parent = parent,
                                    child = child,
                                    directoryType = uiState.value.directoryType,
                                    storageType = StorageType.CUSTOM,
                                )
                                customDirList.add(dir)
                            }
                        }

                        upsert(customDirList)
                        initialize(context, uiState.value.directoryType)
                    }
                }
            }
        }
    }
}
