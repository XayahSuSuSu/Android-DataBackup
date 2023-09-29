package com.xayah.databackup.ui.activity.main.page.restore

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.util.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RestoreUiState(
    val logUtil: LogUtil,
    val packageRestoreEntireDao: PackageRestoreEntireDao,
)

@HiltViewModel
class RestoreViewModel @Inject constructor(logUtil: LogUtil, packageRestoreEntireDao: PackageRestoreEntireDao) : ViewModel() {
    private val _uiState = mutableStateOf(RestoreUiState(logUtil = logUtil, packageRestoreEntireDao = packageRestoreEntireDao))
    val uiState: State<RestoreUiState>
        get() = _uiState
}
