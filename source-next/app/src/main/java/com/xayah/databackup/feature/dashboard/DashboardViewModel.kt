package com.xayah.databackup.feature.dashboard

import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.formatToStorageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@optics
data class DashboardStorageUiState(
    val isLoading: Boolean = true,
    val free: Float = 1f,
    val other: Float = 0f,
    val backups: Float = 0f,
    val freeBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val backupsBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val subtitle: String = "",
    val storage: String = "",
) {
    companion object
}

class DashboardViewModel : BaseViewModel() {
    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _storageUiState = MutableStateFlow(DashboardStorageUiState())
    val storageUiState: StateFlow<DashboardStorageUiState> = _storageUiState.asStateFlow()

    fun initialize() {
        withLock(Dispatchers.IO) {
            val backupPath = PathHelper.getBackupPath().first()
            if (RemoteRootService.mkdirs(backupPath).not()) {
                LogHelper.e(TAG, "initialize", "Failed to mkdirs: $backupPath.")
            }

            val stat = RemoteRootService.readStatFs(backupPath)
            if (stat == null || stat.totalBytes <= 0L) {
                _storageUiState.update {
                    it.copy {
                        DashboardStorageUiState.isLoading set false
                        DashboardStorageUiState.subtitle set backupPath
                        DashboardStorageUiState.storage set App.application.getString(R.string.unknown)
                        DashboardStorageUiState.free set 1f
                        DashboardStorageUiState.other set 0f
                        DashboardStorageUiState.backups set 0f
                        DashboardStorageUiState.freeBytes set 0L
                        DashboardStorageUiState.otherBytes set 0L
                        DashboardStorageUiState.backupsBytes set 0L
                        DashboardStorageUiState.totalBytes set 0L
                    }
                }
                return@withLock
            }

            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes.coerceIn(0L, totalBytes)
            val backupsBytes = RemoteRootService.calculateTreeSize(backupPath)
                .coerceAtLeast(0L)
                .coerceAtMost(totalBytes)
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val otherBytes = (usedBytes - backupsBytes).coerceAtLeast(0L)

            val backupsRatio = backupsBytes.toFloat() / totalBytes.toFloat()
            val otherRatio = otherBytes.toFloat() / totalBytes.toFloat()
            val freeRatio = freeBytes.toFloat() / totalBytes.toFloat()

            _storageUiState.update {
                it.copy {
                    DashboardStorageUiState.isLoading set false
                    DashboardStorageUiState.subtitle set backupPath
                    DashboardStorageUiState.storage set "${usedBytes.formatToStorageSize} / ${totalBytes.formatToStorageSize}"
                    DashboardStorageUiState.free set freeRatio
                    DashboardStorageUiState.other set otherRatio
                    DashboardStorageUiState.backups set backupsRatio
                    DashboardStorageUiState.freeBytes set freeBytes
                    DashboardStorageUiState.otherBytes set otherBytes
                    DashboardStorageUiState.backupsBytes set backupsBytes
                    DashboardStorageUiState.totalBytes set totalBytes
                }
            }
        }
    }
}
