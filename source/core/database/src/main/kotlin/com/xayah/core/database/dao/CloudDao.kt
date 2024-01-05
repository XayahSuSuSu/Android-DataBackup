package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.database.model.CloudEntity
import kotlinx.coroutines.flow.Flow

/**
 * Only used for premium build, but reserved in foss.
 */
@Dao
interface CloudDao {
    @Upsert(entity = CloudEntity::class)
    suspend fun upsert(item: CloudEntity)

    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryByName(name: String): CloudEntity?

    @Query("SELECT * FROM CloudEntity")
    fun queryFlow(): Flow<List<CloudEntity>>

    @Delete(entity = CloudEntity::class)
    suspend fun delete(item: CloudEntity)
}
