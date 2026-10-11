package com.xayah.databackup.feature.restore

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.TaskInfo
import com.xayah.databackup.data.TaskRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.service.RestoreService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class RestoreSourceUiState(
    val name: String?,
    val directory: String?,
    @StringRes val backendRes: Int,
    @StringRes val detailTitleRes: Int,
    val detailTimestamp: Long?,
    val detail: String?,
    val totalBytes: Long?,
)

internal data class RestoreSetupUiState(
    val restoreState: RestoreState,
    val sourceInfo: RestoreSourceUiState,
)

internal fun RestoreState.toSetupUiState(): RestoreSetupUiState {
    val backend = config?.backupBackend
    val backendRes: Int
    val detailTitleRes: Int
    val detailTimestamp: Long?
    val detail: String?
    when (backend) {
        is BackupBackend.Archive -> {
            backendRes = R.string.archive
            detailTitleRes = R.string.updated_at
            detailTimestamp = config.updatedAt.takeIf { it != 0L }
            detail = null
        }

        is BackupBackend.Rustic -> {
            backendRes = R.string.rustic
            detailTitleRes = R.string.snapshot
            detailTimestamp = sourceInfo?.createdAt?.takeIf { it > 0 }
            detail = sourceInfo?.id?.take(8)
        }

        null -> {
            backendRes = R.string.unknown
            detailTitleRes = R.string.snapshot
            detailTimestamp = null
            detail = null
        }
    }
    return RestoreSetupUiState(
        restoreState = this,
        sourceInfo = RestoreSourceUiState(
            name = config?.name?.takeIf { it.isNotEmpty() },
            directory = config?.let { PathHelper.getChildPath(it.path).ifEmpty { it.path } },
            backendRes = backendRes,
            detailTitleRes = detailTitleRes,
            detailTimestamp = detailTimestamp,
            detail = detail,
            totalBytes = sourceInfo?.totalBytes,
        ),
    )
}

internal class RestoreSetupViewModel(
    private val mRestoreRepo: RestoreRepository,
    private val mProcessRepo: RestoreProcessRepository,
    private val mTaskRepo: TaskRepository,
) : BaseViewModel() {
    val activeTask: StateFlow<TaskInfo?> = mTaskRepo.activeTask
    private var mIsStarting = false
    internal val uiState: StateFlow<RestoreSetupUiState> = mRestoreRepo.state
        .map { it.toSetupUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mRestoreRepo.state.value.toSetupUiState()
        )

    fun startRestore(): String? {
        if (mIsStarting) {
            return null
        }
        mIsStarting = true
        try {
            if (mTaskRepo.activeTask.value != null) {
                return null
            }
            val initialState = mRestoreRepo.prepareRestore()
            val request = checkNotNull(mRestoreRepo.consumeRestoreRequest())
            val state = mProcessRepo.prepare(request, initialState)
            runCatching { RestoreService.start(App.application, state.id) }.onFailure { error ->
                mProcessRepo.failToStart(state.id)
                if (error is CancellationException) {
                    throw error
                }
            }
            return state.id
        } catch (error: Exception) {
            if (error is CancellationException) {
                throw error
            }
            return null
        } finally {
            mIsStarting = false
        }
    }

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectCategory(category, checked)
        }
    }
}
