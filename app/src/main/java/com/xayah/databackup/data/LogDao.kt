package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Upsert(entity = LogEntity::class)
    suspend fun insert(item: LogEntity): Long

    @Upsert(entity = CmdEntity::class)
    suspend fun insert(item: CmdEntity): Long

    @Transaction
    @Query("SELECT * FROM LogEntity")
    fun getLogCmdItems(): Flow<List<LogEntity>>
}
