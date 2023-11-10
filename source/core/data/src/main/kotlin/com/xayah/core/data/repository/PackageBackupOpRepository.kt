package com.xayah.core.data.repository

import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.model.OperationState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PackageBackupOpRepository @Inject constructor(
    private val packageBackupOpDao: PackageBackupOperationDao,
) {
    fun getOperationsProcessing(timestamp: Long) =
        packageBackupOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.PROCESSING } }.distinctUntilChanged()

    fun getOperationsFailed(timestamp: Long) =
        packageBackupOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.ERROR } }.distinctUntilChanged()

    fun getOperationsSucceed(timestamp: Long) =
        packageBackupOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.DONE } }.distinctUntilChanged()
}
