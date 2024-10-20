package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.database.ProcessingInfoEntity
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

    @Upsert(entity = ProcessingInfoEntity::class)
    suspend fun upsert(item: ProcessingInfoEntity): Long

    @Query("SELECT * FROM TaskEntity WHERE id = :id LIMIT 1")
    fun queryTaskFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM TaskEntity")
    fun queryTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM ProcessingInfoEntity WHERE taskId = :taskId AND type = :type")
    fun queryProcessingInfoFlow(taskId: Long, type: ProcessingType): Flow<List<ProcessingInfoEntity>>

    @Query("SELECT * FROM ProcessingInfoEntity WHERE taskId = :taskId")
    fun queryProcessingInfoFlow(taskId: Long): Flow<List<ProcessingInfoEntity>>

    @Query("SELECT * FROM TaskDetailPackageEntity WHERE taskId = :taskId")
    fun queryPackageFlow(taskId: Long): Flow<List<TaskDetailPackageEntity>>

    @Query("SELECT * FROM TaskDetailMediaEntity WHERE taskId = :taskId")
    fun queryMediaFlow(taskId: Long): Flow<List<TaskDetailMediaEntity>>
}
