package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.service.OperationLocalService
import com.xayah.databackup.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProcessingUiState(
    val timestamp: Long,
    var effectLaunched: Boolean,
    val packageBackupEntireDao: PackageBackupEntireDao,
    val packageBackupOperationDao: PackageBackupOperationDao
) {
    val latestPackage: Flow<PackageBackupOperation> = packageBackupOperationDao.queryOperationPackage(timestamp)
    val selectedBothCount: Flow<Int> = packageBackupEntireDao.countSelectedTotal()
    val operationCount: Flow<Int> = packageBackupOperationDao.countByTimestamp(timestamp)
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    val context: Application,
    packageBackupEntireDao: PackageBackupEntireDao,
    packageBackupOperationDao: PackageBackupOperationDao
) : ViewModel() {
    private val _uiState = mutableStateOf(
        ProcessingUiState(
            timestamp = DateUtil.getTimestamp(),
            effectLaunched = false,
            packageBackupEntireDao = packageBackupEntireDao,
            packageBackupOperationDao = packageBackupOperationDao
        )
    )
    val uiState: State<ProcessingUiState>
        get() = _uiState

    fun backupPackages() {
        if (_uiState.value.effectLaunched.not())
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    _uiState.value.effectLaunched = true
                    val operationLocalService = OperationLocalService(context, uiState.value.timestamp)
                    operationLocalService.backupPackages()
                    operationLocalService.destroyService()
                }
            }
    }
}
