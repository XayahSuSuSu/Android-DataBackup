package com.xayah.core.data.repository

import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.model.OperationState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PackageRestoreOpRepository @Inject constructor(
    private val packageRestoreOpDao: PackageRestoreOperationDao,
) {
    fun getOperationsProcessing(timestamp: Long) =
        packageRestoreOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.PROCESSING } }.distinctUntilChanged()

    fun getOperationsFailed(timestamp: Long) =
        packageRestoreOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.ERROR } }.distinctUntilChanged()

    fun getOperationsSucceed(timestamp: Long) =
        packageRestoreOpDao.queryOperationsFlow(timestamp).map { ops -> ops.filter { it.packageState == OperationState.DONE } }.distinctUntilChanged()
}
