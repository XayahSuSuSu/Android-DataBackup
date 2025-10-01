package com.xayah.databackup.feature.backup.call_logs

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.database.entity.deserialize
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.filterCallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data object UiState

open class CallLogsViewModel(
    callLogRepo: CallLogRepository
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val callLogs = combine(
        callLogRepo.callLogs.deserialize(),
        _searchText,
    ) { contacts, searchText ->
        contacts.filterCallLog(searchText)
    }.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selected =
        callLogs.map { list -> list.count { it.selected } }.stateIn(
            scope = viewModelScope,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun selectCallLog(id: Long, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.callLogDao.selectCallLog(id, selected)
        }
    }

    fun selectAllCallLogs() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.callLogDao.selectAllCallLogs(callLogs.value.map { it.id }, callLogs.value.count { it.selected } != callLogs.value.size)
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }
}
