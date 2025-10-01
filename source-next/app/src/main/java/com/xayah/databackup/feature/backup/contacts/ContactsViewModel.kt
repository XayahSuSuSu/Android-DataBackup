package com.xayah.databackup.feature.backup.contacts

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.database.entity.deserialize
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.filterContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data object UiState

open class ContactsViewModel(
    contactRepo: ContactRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val contacts = combine(
        contactRepo.contacts.deserialize(),
        _searchText,
    ) { contacts, searchText ->
        contacts.filterContact(searchText)
    }.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selected =
        contacts.map { list -> list.count { it.selected } }.stateIn(
            scope = viewModelScope,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun selectContact(id: Long, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.contactDao.selectContact(id, selected)
        }
    }

    fun selectAllContacts() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.contactDao.selectAllContacts(contacts.value.map { it.id }, contacts.value.count { it.selected } != contacts.value.size)
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }
}
