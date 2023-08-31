package com.xayah.databackup.ui.activity.main.page.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.LogDao
import com.xayah.databackup.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PageLogUiState(
    val logDao: LogDao,
    val logText: String,
    val startTimestamps: List<Long>,
    val selectedIndex: Int,
)

@HiltViewModel
class PageLogViewModel @Inject constructor(logDao: LogDao) : ViewModel() {
    private val _uiState = mutableStateOf(PageLogUiState(logDao = logDao, logText = "", startTimestamps = listOf(), selectedIndex = 0))
    val uiState: State<PageLogUiState>
        get() = _uiState

    private fun setStartTimestamps(startTimestamps: List<Long>) {
        _uiState.value = uiState.value.copy(startTimestamps = startTimestamps)
    }

    suspend fun setSelectedIndex(index: Int) {
        _uiState.value = uiState.value.copy(selectedIndex = index)
        setLogText()
    }

    private suspend fun setLogText() = withContext(Dispatchers.IO) {
        val dao = uiState.value.logDao
        val startTimestamps = uiState.value.startTimestamps
        val index = uiState.value.selectedIndex
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
            _uiState.value = uiState.value.copy(logText = logText)
        } else {
            _uiState.value = uiState.value.copy(logText = "")
        }
    }

    suspend fun initializeUiState() = withContext(Dispatchers.IO) {
        val dao = uiState.value.logDao
        val startTimestamps = dao.queryLogStartTimestamps()
        setStartTimestamps(startTimestamps)
        setSelectedIndex(startTimestamps.lastIndex)
    }

    suspend fun deleteCurrentLog() = withContext(Dispatchers.IO) {
        val selectedIndex = uiState.value.selectedIndex
        if (selectedIndex == -1) return@withContext
        val startTimestamp = uiState.value.startTimestamps[selectedIndex]
        uiState.value.logDao.delete(startTimestamp = startTimestamp)
        initializeUiState()
    }
}
