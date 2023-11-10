package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.database.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Upsert(entity = TaskEntity::class)
    suspend fun upsert(item: TaskEntity)

    @Query("UPDATE TaskEntity SET startTimestamp = :startTimestamp WHERE timestamp = :timestamp")
    suspend fun updateStartTimestamp(timestamp: Long, startTimestamp: Long)

    @Query("UPDATE TaskEntity SET endTimestamp = :endTimestamp WHERE timestamp = :timestamp")
    suspend fun updateEndTimestamp(timestamp: Long, endTimestamp: Long)

    @Query("SELECT * FROM TaskEntity WHERE timestamp = :timestamp LIMIT 1")
    fun queryFlow(timestamp: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM TaskEntity")
    fun observeAll(): Flow<List<TaskEntity>>
}
