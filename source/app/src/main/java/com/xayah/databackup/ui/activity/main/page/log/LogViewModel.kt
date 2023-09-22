package com.xayah.databackup.ui.activity.main.page.log

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.LogDao
import com.xayah.databackup.util.DateUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LogUiState(
    val logDao: LogDao,
    val logText: String,
    val startTimestamps: List<Long>,
    val selectedIndex: Int,
)

@HiltViewModel
class LogViewModel @Inject constructor(logDao: LogDao) : ViewModel() {
    private val _uiState = mutableStateOf(LogUiState(logDao = logDao, logText = "", startTimestamps = listOf(), selectedIndex = 0))
    val uiState: State<LogUiState>
        get() = _uiState

    private fun setStartTimestamps(startTimestamps: List<Long>) {
        _uiState.value = uiState.value.copy(startTimestamps = startTimestamps)
    }

    suspend fun setSelectedIndex(index: Int) {
        _uiState.value = uiState.value.copy(selectedIndex = index)
        setLogText()
    }

    private suspend fun setLogText() = withIOContext {
        val uiState = uiState.value
        val dao = uiState.logDao
        val startTimestamps = uiState.startTimestamps
        val index = uiState.selectedIndex
        if (index != -1) {
            val logCmdItems = dao.queryLogCmdItems(startTimestamps[index])
            var logText = ""
            logCmdItems.forEach { logCmdEntity ->
                logText += "${DateUtil.formatTimestamp(logCmdEntity.log.startTimestamp)}    ${logCmdEntity.log.tag}: ${logCmdEntity.log.msg}\n"
                logCmdEntity.cmdList.forEach { cmdEntity ->
                    logText += "${DateUtil.formatTimestamp(cmdEntity.timestamp)}    ${cmdEntity.type.name}: ${cmdEntity.msg}\n"
                }
                logText += "\n"
            }
            _uiState.value = uiState.copy(logText = logText)
        } else {
            _uiState.value = uiState.copy(logText = "")
        }
    }

    suspend fun initializeUiState() = withIOContext {
        val uiState = uiState.value
        val dao = uiState.logDao
        val startTimestamps = dao.queryLogStartTimestamps()
        setStartTimestamps(startTimestamps)
        setSelectedIndex(startTimestamps.lastIndex)
    }

    suspend fun deleteCurrentLog() = withIOContext {
        val uiState = uiState.value
        val selectedIndex = uiState.selectedIndex
        if (selectedIndex == -1) return@withIOContext
        val startTimestamp = uiState.startTimestamps[selectedIndex]
        uiState.logDao.delete(startTimestamp = startTimestamp)
        initializeUiState()
    }
}
