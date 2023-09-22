package com.xayah.databackup.ui.activity.operation.page.packages.restore

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.data.PackageRestoreOperation
import com.xayah.databackup.data.PackageRestoreOperationDao
import com.xayah.databackup.service.OperationLocalService
import com.xayah.databackup.ui.activity.operation.page.packages.backup.ProcessingState
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
    var effectFinished: Boolean,
    var effectState: ProcessingState,
    val packageRestoreEntireDao: PackageRestoreEntireDao,
    val packageRestoreOperationDao: PackageRestoreOperationDao,
) {
    val latestPackage: Flow<PackageRestoreOperation> = packageRestoreOperationDao.queryLastOperationPackage(timestamp).distinctUntilChanged()
    val selectedBothCount: Flow<Int> = packageRestoreEntireDao.countSelectedTotal().distinctUntilChanged()
    val operationCount: Flow<Int> = packageRestoreOperationDao.countByTimestamp(timestamp).distinctUntilChanged()
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    val context: Application,
    packageRestoreEntireDao: PackageRestoreEntireDao,
    packageRestoreOperationDao: PackageRestoreOperationDao,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        ProcessingUiState(
            timestamp = DateUtil.getTimestamp(),
            effectLaunched = false,
            effectFinished = false,
            effectState = ProcessingState.Idle,
            packageRestoreEntireDao = packageRestoreEntireDao,
            packageRestoreOperationDao = packageRestoreOperationDao
        )
    )
    val uiState: State<ProcessingUiState>
        get() = _uiState

    fun restorePackages() {
        val uiState by uiState
        if (uiState.effectLaunched.not())
            viewModelScope.launch {
                withIOContext {
                    _uiState.value = uiState.copy(effectLaunched = true)
                    val operationLocalService = OperationLocalService(context = context)
                    operationLocalService.restorePackagesPreparation()

                    _uiState.value = uiState.copy(effectState = ProcessingState.Processing)
                    operationLocalService.restorePackages(timestamp = uiState.timestamp)

                    operationLocalService.destroyService()
                    _uiState.value = uiState.copy(effectFinished = true)
                }
            }
    }
}
