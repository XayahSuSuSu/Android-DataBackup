package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.R
import com.xayah.databackup.data.CloudDao
import com.xayah.databackup.data.CloudMountEntity
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.CloudUtil
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withIOContext
import com.xayah.librootservice.util.withMainContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

fun List<String>.toPathString() = joinToString(separator = "/")
fun String.toPathList() = split("/")

data class MountUiState(
    val cloudDao: CloudDao,
    val logUtil: LogUtil,
    val snackbarHostState: SnackbarHostState,
) {
    val cloudEntities: Flow<List<CloudMountEntity>> = cloudDao.queryMountFlow().distinctUntilChanged()
}

@HiltViewModel
class MountViewModel @Inject constructor(cloudDao: CloudDao, logUtil: LogUtil) : ViewModel() {
    private val _uiState = mutableStateOf(
        MountUiState(
            cloudDao = cloudDao,
            logUtil = logUtil,
            snackbarHostState = SnackbarHostState(),
        )
    )
    val uiState: State<MountUiState>
        get() = _uiState

    suspend fun mount(entity: CloudMountEntity) {
        val uiState by uiState

        if (entity.mount.local.isEmpty() || entity.mount.remote.isEmpty()) {
            val context = DataBackupApplication.application
            uiState.snackbarHostState.showSnackbar(message = context.getString(R.string.info_set_remote_or_local))
        } else {
            val (isSuccess, out) = CloudUtil.mount(logUtil = uiState.logUtil, remote = entity.mount.remote, destination = entity.mount.local)
            if (isSuccess) {
                uiState.cloudDao.upsertMount(entity.copy(mount = entity.mount.copy(mounted = true)))
            } else {
                uiState.snackbarHostState.showSnackbar(message = out, duration = SnackbarDuration.Long)
            }
        }
    }

    suspend fun unmount(entity: CloudMountEntity) {
        val uiState by uiState
        val (isSuccess, out) = CloudUtil.unmount(logUtil = uiState.logUtil, name = entity.name, destination = entity.mount.local)
        if (isSuccess) {
            uiState.cloudDao.upsertMount(entity.copy(mount = entity.mount.copy(mounted = false)))
        } else {
            uiState.snackbarHostState.showSnackbar(message = out, duration = SnackbarDuration.Long)
        }
    }

    suspend fun unmountAll() {
        val uiState by uiState
        val (isSuccess, out) = CloudUtil.unmountAll(logUtil = uiState.logUtil)
        if (isSuccess) {
            val mountList = uiState.cloudDao.queryMount().toMutableList()
            mountList.forEachIndexed { index, entity ->
                mountList[index] = entity.copy(mount = entity.mount.copy(mounted = false))
            }
            uiState.cloudDao.upsertMount(mountList)
        } else {
            uiState.snackbarHostState.showSnackbar(message = out, duration = SnackbarDuration.Long)
        }
    }

    suspend fun setRemote(context: ComponentActivity, entity: CloudMountEntity) {
        val uiState by uiState
        val remoteRootService = RemoteRootService(context)

        if (entity.mount.mounted) {
            unmount(entity)
        }

        withIOContext {
            remoteRootService.mkdirs(PathUtil.getTmpMountPath(context, entity.name))
            CloudUtil.mount(logUtil = uiState.logUtil, remote = "${entity.name}:", destination = PathUtil.getTmpMountPath(context, entity.name), "--read-only")
        }

        withMainContext {
            PickYouLauncher().apply {
                setTitle(context.getString(R.string.select_target_directory))
                setType(PickerType.DIRECTORY)
                setLimitation(1)
                val mountPath = PathUtil.getTmpMountPath(context, entity.name)
                val pathSplitList = mountPath.toPathList()
                val pathSize = pathSplitList.size
                setDefaultPath(PathUtil.getParentPath(mountPath))
                setPathPrefixHiddenNum(pathSize - 2)
                launch(context) { pathList ->
                    viewModelScope.launch {
                        withIOContext {
                            pathList.firstOrNull()?.also { pathString ->
                                val pathSplit = pathString.toPathList().toMutableList()
                                // Remove "mount/${name}"
                                repeat(2) {
                                    pathSplit.removeFirst()
                                }
                                // Add "${name}:"
                                val finalPath = "${entity.name}:${pathSplit.toPathString()}"
                                uiState.cloudDao.upsertMount(entity.copy(mount = entity.mount.copy(remote = finalPath)))
                            }
                            CloudUtil.unmount(logUtil = uiState.logUtil, name = entity.name, destination = mountPath)
                            remoteRootService.deleteRecursively(mountPath)
                            remoteRootService.destroyService()
                        }
                    }
                }
            }
        }
    }

    suspend fun setLocal(context: ComponentActivity, entity: CloudMountEntity) {
        val uiState by uiState

        if (entity.mount.mounted) {
            unmount(entity)
        }

        withMainContext {
            PickYouLauncher().apply {
                setTitle(context.getString(R.string.select_target_directory))
                setType(PickerType.DIRECTORY)
                setLimitation(1)
                launch(context) { pathList ->
                    viewModelScope.launch {
                        withIOContext {
                            pathList.firstOrNull()?.also { pathString ->
                                uiState.cloudDao.upsertMount(entity.copy(mount = entity.mount.copy(local = pathString)))
                            }
                        }
                    }
                }
            }
        }
    }
}
