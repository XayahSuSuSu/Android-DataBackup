package com.xayah.databackup.feature.restore

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.PathHelper
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

class RestoreSetupViewModel(private val mRestoreRepo: RestoreRepository) : BaseViewModel() {
    internal val uiState: StateFlow<RestoreSetupUiState> = mRestoreRepo.state
        .map { it.toSetupUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mRestoreRepo.state.value.toSetupUiState()
        )

    fun prepareRestore(): Boolean = runCatching { mRestoreRepo.prepareRestore() }.isSuccess

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectCategory(category, checked)
        }
    }
}
