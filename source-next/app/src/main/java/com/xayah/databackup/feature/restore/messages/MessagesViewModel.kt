package com.xayah.databackup.feature.restore.messages

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.SmsDeserialized
import com.xayah.databackup.entity.restore.RestoreState
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.filterMms
import com.xayah.databackup.util.filterSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class UiState(val selectedIndex: Int = 0)

class MessagesViewModel(
    private val mRestoreRepo: RestoreRepository,
) : BaseViewModel() {
    private val mSharingStarted = SharingStarted.WhileSubscribed(5_000)
    val state: StateFlow<RestoreState> = mRestoreRepo.state
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val sms: StateFlow<Map<String, SmsDeserialized>> = combine(state, searchText) { state, query ->
        state.inventory?.sms.orEmpty().filterSms(query)
    }.stateIn(viewModelScope, mSharingStarted, state.value.inventory?.sms.orEmpty())
    val mms: StateFlow<Map<String, MmsDeserialized>> = combine(state, searchText) { state, query ->
        state.inventory?.mms.orEmpty().filterMms(query)
    }.stateIn(viewModelScope, mSharingStarted, state.value.inventory?.mms.orEmpty())
    val items: StateFlow<List<String>> = combine(sms, mms) { sms, mms -> (sms.keys + mms.keys).toList() }
        .stateIn(viewModelScope, mSharingStarted, emptyList())
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val selectedIndex: StateFlow<Int> = uiState.map { it.selectedIndex }.stateIn(viewModelScope, mSharingStarted, 0)
    val visibleItems: StateFlow<List<String>> = combine(sms, mms, selectedIndex) { sms, mms, index ->
        (if (index == 0) sms.keys else mms.keys).toList()
    }.stateIn(viewModelScope, mSharingStarted, emptyList())

    fun selectTab(index: Int) {
        withLock(Dispatchers.Default) {
            _uiState.update { it.copy(selectedIndex = index) }
        }
    }
    fun selectAllMessages() {
        withLock(Dispatchers.Default) {
            val messages = visibleItems.value
            mRestoreRepo.selectItems(messages.toSet(), messages.any { it !in state.value.selected })
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }

    fun selectItem(id: String, checked: Boolean) {
        withLock(Dispatchers.Default) {
            mRestoreRepo.selectItem(id, checked)
        }
    }
}
