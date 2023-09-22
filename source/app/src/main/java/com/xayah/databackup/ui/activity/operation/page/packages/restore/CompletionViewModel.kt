package com.xayah.databackup.ui.activity.operation.page.packages.restore

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageRestoreOperationDao
import com.xayah.databackup.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class CompletionUiState(
    val context: Application,
    val packageRestoreOperationDao: PackageRestoreOperationDao,
    val relativeTime: String,
    val succeedNum: Int,
    val failedNum: Int,
)

@HiltViewModel
class CompletionViewModel @Inject constructor(
    val context: Application,
    packageRestoreOperationDao: PackageRestoreOperationDao,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        CompletionUiState(
            context = context,
            packageRestoreOperationDao = packageRestoreOperationDao,
            relativeTime = "",
            succeedNum = 0,
            failedNum = 0
        )
    )
    val uiState: State<CompletionUiState>
        get() = _uiState

    suspend fun initializeUiState() {
        val uiState = uiState.value
        val dao = uiState.packageRestoreOperationDao
        val timestamp = dao.queryLastOperationTime()
        val startTimestamp = dao.queryFirstOperationStartTime(timestamp)
        val endTimestamp = dao.queryLastOperationEndTime(timestamp)
        val relativeTime = DateUtil.getShortRelativeTimeSpanString(context, startTimestamp, endTimestamp)
        val succeedNum = dao.countSucceedByTimestamp(timestamp)
        val failedNum = dao.countFailedByTimestamp(timestamp)
        _uiState.value = uiState.copy(relativeTime = relativeTime, succeedNum = succeedNum, failedNum = failedNum)
    }
}
