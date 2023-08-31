package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface LogDao {
    @Upsert(entity = LogEntity::class)
    suspend fun upsert(item: LogEntity): Long

    @Upsert(entity = CmdEntity::class)
    suspend fun upsert(item: CmdEntity): Long


    @Query("SELECT DISTINCT startTimestamp FROM LogEntity")
    suspend fun queryLogStartTimestamps(): List<Long>

    @Transaction
    @Query("SELECT * FROM LogEntity WHERE startTimestamp = :startTimestamp")
    suspend fun queryLogCmdItems(startTimestamp: Long): List<LogCmdEntity>

    @Query("DELETE FROM LogEntity WHERE startTimestamp = :startTimestamp")
    fun delete(startTimestamp: Long)
}
