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
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessingUiState(
    val timestamp: Long,
    var effectLaunched: Boolean,
    val packageBackupEntireDao: PackageBackupEntireDao,
    val packageBackupOperationDao: PackageBackupOperationDao
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
            timestamp = DateUtil.getTimestamp(),
            effectLaunched = false,
            packageBackupEntireDao = packageBackupEntireDao,
            packageBackupOperationDao = packageBackupOperationDao
        )
    )
    val uiState: State<ProcessingUiState>
        get() = _uiState

    fun backupPackages(onCompleted: suspend () -> Unit) {
        val uiState = uiState.value
        if (_uiState.value.effectLaunched.not())
            viewModelScope.launch {
                withIOContext {
                    _uiState.value.effectLaunched = true
                    val operationLocalService = OperationLocalService(context, uiState.timestamp)
                    operationLocalService.backupPackages()
                    operationLocalService.destroyService()
                    onCompleted()
                }
            }
    }
}
