package com.xayah.databackup.feature.restore

import com.xayah.databackup.data.restore.RestoreCategory
import com.xayah.databackup.data.restore.RestoreSession
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers

class RestoreSetupViewModel(private val session: RestoreSession) : BaseViewModel() {
    val state = session.state

    fun prepareRestore(): Boolean = runCatching { session.prepareRestore() }.isSuccess

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        withLock(Dispatchers.Default) {
            session.selectCategory(category, checked)
        }
    }
}
