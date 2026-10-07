package com.xayah.databackup.feature.restore

import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.feature.RestoreRoute
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException

class RestoreViewModel(
    private val mRoute: RestoreRoute,
    val repository: RestoreRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "RestoreViewModel"
    }

    private var mIsLoading = false

    init {
        load()
    }

    fun load() {
        if (mIsLoading) return
        mIsLoading = true
        withLock {
            try {
                repository.updateState(RestoreState())
                repository.loadBackup(mRoute.configUuid, mRoute.snapshotId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogHelper.e(TAG, "load", "Failed to read backup inventory", error)
                repository.updateState(RestoreState(loading = false, failed = true))
            } finally {
                mIsLoading = false
            }
        }
    }
}
