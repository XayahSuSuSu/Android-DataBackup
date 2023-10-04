package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.service.OperationLocalService
import com.xayah.databackup.util.DateUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

enum class ProcessingState {
    Idle,
    Waiting,
    Processing,
}

data class ProcessingUiState(
    val mutex: Mutex,
    val timestamp: Long,
    var effectLaunched: Boolean,
    var effectFinished: Boolean,
    var effectState: ProcessingState,
    val packageBackupEntireDao: PackageBackupEntireDao,
    val packageBackupOperationDao: PackageBackupOperationDao,
) {
    val latestPackage: Flow<PackageBackupOperation> = packageBackupOperationDao.queryLastOperationPackage(timestamp).distinctUntilChanged()
    val selectedBothCount: Flow<Int> = packageBackupEntireDao.countSelectedTotal().distinctUntilChanged()
    val operationCount: Flow<Int> = packageBackupOperationDao.countByTimestamp(timestamp).distinctUntilChanged()
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    val context: Application,
    packageBackupEntireDao: PackageBackupEntireDao,
    packageBackupOperationDao: PackageBackupOperationDao
) : ViewModel() {
    private val _uiState = mutableStateOf(
        ProcessingUiState(
            mutex = Mutex(),
            timestamp = DateUtil.getTimestamp(),
            effectLaunched = false,
            effectFinished = false,
            effectState = ProcessingState.Idle,
            packageBackupEntireDao = packageBackupEntireDao,
            packageBackupOperationDao = packageBackupOperationDao
        )
    )
    val uiState: State<ProcessingUiState>
        get() = _uiState

    fun backupPackages() {
        val uiState by uiState

        viewModelScope.launch {
            uiState.mutex.withLock {
                if (uiState.effectLaunched.not()) {
                    withIOContext {
                        _uiState.value = uiState.copy(effectLaunched = true)

                        val operationLocalService = OperationLocalService(context = context)
                        val preparation = operationLocalService.backupPackagesPreparation()

                        _uiState.value = uiState.copy(effectState = ProcessingState.Processing)
                        operationLocalService.backupPackages(timestamp = uiState.timestamp)

                        _uiState.value = uiState.copy(effectState = ProcessingState.Waiting)
                        operationLocalService.backupPackagesAfterwards(preparation)

                        operationLocalService.destroyService()
                        _uiState.value = uiState.copy(effectFinished = true)
                    }
                }
            }
        }
    }
}
