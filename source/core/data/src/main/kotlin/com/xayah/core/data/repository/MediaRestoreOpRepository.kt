package com.xayah.core.data.repository

import com.xayah.core.database.dao.MediaDao
import com.xayah.core.model.OperationState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MediaRestoreOpRepository @Inject constructor(
    private val mediaDao: MediaDao,
) {
    fun getOperationsProcessing(timestamp: Long) =
        mediaDao.observeRestoreOp(timestamp).map { ops -> ops.filter { it.mediaState == OperationState.PROCESSING } }.distinctUntilChanged()

    fun getOperationsFailed(timestamp: Long) =
        mediaDao.observeRestoreOp(timestamp).map { ops -> ops.filter { it.mediaState == OperationState.ERROR } }.distinctUntilChanged()

    fun getOperationsSucceed(timestamp: Long) =
        mediaDao.observeRestoreOp(timestamp).map { ops -> ops.filter { it.mediaState == OperationState.DONE } }.distinctUntilChanged()
}
