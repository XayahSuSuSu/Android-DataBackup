package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Upsert(entity = Contact::class)
    suspend fun upsert(contacts: List<Contact>)

    @Query("SELECT * from contacts")
    fun loadFlowContacts(): Flow<List<Contact>>

    @Query("UPDATE contacts SET selected = :selected WHERE id = :id")
    suspend fun selectContact(id: Long, selected: Boolean)

    @Query("UPDATE contacts SET selected = :selected WHERE (id) in (:ids)")
    suspend fun selectAllContacts(ids: List<Long>, selected: Boolean)
}
