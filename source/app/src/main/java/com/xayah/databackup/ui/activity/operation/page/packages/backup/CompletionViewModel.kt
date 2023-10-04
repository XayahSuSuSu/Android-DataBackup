package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class CompletionUiState(
    val context: Application,
    val packageBackupOperationDao: PackageBackupOperationDao,
    val relativeTime: String,
    val succeedNum: Int,
    val failedNum: Int,
)

@HiltViewModel
class CompletionViewModel @Inject constructor(
    val context: Application,
    packageBackupOperationDao: PackageBackupOperationDao,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        CompletionUiState(
            context = context,
            packageBackupOperationDao = packageBackupOperationDao,
            relativeTime = "",
            succeedNum = 0,
            failedNum = 0
        )
    )
    val uiState: State<CompletionUiState>
        get() = _uiState

    suspend fun initializeUiState() {
        val uiState by uiState
        val dao = uiState.packageBackupOperationDao
        val timestamp = dao.queryLastOperationTime()
        val startTimestamp = dao.queryFirstOperationStartTime(timestamp)
        val endTimestamp = dao.queryLastOperationEndTime(timestamp)
        val relativeTime = DateUtil.getShortRelativeTimeSpanString(context, startTimestamp, endTimestamp)
        val succeedNum = dao.countSucceedByTimestamp(timestamp)
        val failedNum = dao.countFailedByTimestamp(timestamp)
        _uiState.value = uiState.copy(relativeTime = relativeTime, succeedNum = succeedNum, failedNum = failedNum)
    }
}
