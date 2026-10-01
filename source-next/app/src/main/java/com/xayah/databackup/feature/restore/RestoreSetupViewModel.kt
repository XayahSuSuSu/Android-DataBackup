package com.xayah.databackup.feature.restore

import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.restore.RestoreCategory
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

class RestoreSetupViewModel(private val mRestoreRepo: RestoreRepository) : BaseViewModel() {
    val state: StateFlow<RestoreState> = mRestoreRepo.state

    fun prepareRestore(): Boolean = runCatching { mRestoreRepo.prepareRestore() }.isSuccess

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectCategory(category, checked)
        }
    }
}
