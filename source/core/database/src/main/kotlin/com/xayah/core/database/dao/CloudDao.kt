package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.database.CloudEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudDao {
    @Upsert(entity = CloudEntity::class)
    suspend fun upsert(item: CloudEntity)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsert(items: List<CloudEntity>)

    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryByName(name: String): CloudEntity?

    @Query("SELECT * FROM CloudEntity WHERE activated = 1")
    suspend fun queryActivated(): List<CloudEntity>

    @Query("SELECT * FROM CloudEntity")
    fun queryFlow(): Flow<List<CloudEntity>>

    @Query("SELECT * FROM CloudEntity")
    suspend fun query(): List<CloudEntity>

    @Delete(entity = CloudEntity::class)
    suspend fun delete(item: CloudEntity)
}
