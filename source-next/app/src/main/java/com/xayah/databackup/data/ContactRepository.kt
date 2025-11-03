package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepository {
    companion object {
        private const val TAG = "ContactRepository"
    }

    val isBackupMessagesSelected: Flow<Boolean> = application.readBoolean(ContactsOptionSelectedBackup)

    val contacts: Flow<List<Contact>> = DatabaseHelper.contactDao.loadFlowContacts()
    val contactsSelected: Flow<List<Contact>> = contacts.map { contacts -> contacts.filter { it.selected } }
}
