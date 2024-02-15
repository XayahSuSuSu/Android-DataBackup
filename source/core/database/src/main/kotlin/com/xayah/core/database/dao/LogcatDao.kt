package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.database.LogcatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogcatDao {
    @Upsert(entity = LogcatEntity::class)
    suspend fun upsert(item: LogcatEntity)

    @Upsert(entity = LogcatEntity::class)
    suspend fun upsert(items: List<LogcatEntity>)

    @Query("SELECT * FROM LogcatEntity")
    fun queryFlow(): Flow<List<LogcatEntity>>

    @Query("SELECT * FROM LogcatEntity")
    suspend fun query(): List<LogcatEntity>

    @Query("DELETE FROM LogcatEntity")
    suspend fun clear()
}
