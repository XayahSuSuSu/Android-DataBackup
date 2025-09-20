package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert(entity = Sms::class)
    suspend fun upsertSms(messages: List<Sms>)

    @Query("SELECT * from messages_sms")
    fun loadFlowSms(): Flow<List<Sms>>

    @Query("UPDATE messages_sms SET selected = :selected WHERE id = :id")
    suspend fun selectSms(id: Long, selected: Boolean)

    @Query("UPDATE messages_sms SET selected = :selected WHERE (id) in (:ids)")
    suspend fun selectAllSms(ids: List<Long>, selected: Boolean)

    @Upsert(entity = Mms::class)
    suspend fun upsertMms(messages: List<Mms>)

    @Query("SELECT * from messages_mms")
    fun loadFlowMms(): Flow<List<Mms>>

    @Query("UPDATE messages_mms SET selected = :selected WHERE id = :id")
    suspend fun selectMms(id: Long, selected: Boolean)

    @Query("UPDATE messages_mms SET selected = :selected WHERE (id) in (:ids)")
    suspend fun selectAllMms(ids: List<Long>, selected: Boolean)
}
