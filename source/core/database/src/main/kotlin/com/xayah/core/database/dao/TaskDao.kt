package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Upsert(entity = TaskEntity::class)
    suspend fun upsert(item: TaskEntity): Long

    @Upsert(entity = TaskDetailPackageEntity::class)
    suspend fun upsert(item: TaskDetailPackageEntity): Long

    @Upsert(entity = TaskDetailMediaEntity::class)
    suspend fun upsert(item: TaskDetailMediaEntity): Long

    @Query("SELECT COUNT(*) FROM TaskEntity WHERE isProcessing = 1")
    fun countProcessingFlow(): Flow<Long>

    @Query("SELECT * FROM TaskEntity WHERE id = :id LIMIT 1")
    fun queryTaskFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM TaskEntity")
    fun queryFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM TaskDetailPackageEntity WHERE taskId = :taskId")
    fun queryPackageFlow(taskId: Long): Flow<List<TaskDetailPackageEntity>>

    @Query("SELECT * FROM TaskDetailMediaEntity WHERE taskId = :taskId")
    fun queryMediaFlow(taskId: Long): Flow<List<TaskDetailMediaEntity>>
}
