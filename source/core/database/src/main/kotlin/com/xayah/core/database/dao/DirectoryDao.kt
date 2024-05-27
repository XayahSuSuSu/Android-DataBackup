package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.StorageType
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.model.database.DirectoryUpsertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {
    @Upsert(entity = DirectoryEntity::class)
    suspend fun upsert(item: DirectoryEntity)

    @Upsert(entity = DirectoryEntity::class)
    suspend fun upsert(items: List<DirectoryUpsertEntity>)

    @Query("SELECT * FROM DirectoryEntity WHERE active = 1 AND storageType = :storageType ORDER BY parent")
    fun queryActiveDirectoriesFlow(storageType: StorageType): Flow<List<DirectoryEntity>>

    @Query("SELECT * FROM DirectoryEntity WHERE active = 1 ORDER BY parent")
    suspend fun queryActiveDirectories(): List<DirectoryEntity>

    /**
     * Get the directory id of the smallest userId
     */
    @Query("SELECT id FROM DirectoryEntity WHERE storageType = :storageType ORDER BY parent LIMIT 1")
    suspend fun queryDefaultDirectoryId(storageType: StorageType): Long?

    @Query("SELECT * FROM DirectoryEntity WHERE selected = 1 LIMIT 1")
    suspend fun querySelectedByDirectoryType(): DirectoryEntity?

    @Query("SELECT * FROM DirectoryEntity WHERE selected = 1 LIMIT 1")
    fun querySelectedByDirectoryTypeFlow(): Flow<DirectoryEntity?>

    @Query("SELECT id FROM DirectoryEntity WHERE parent = :parent AND child = :child LIMIT 1")
    suspend fun queryId(parent: String, child: String): Long

    @Query("UPDATE DirectoryEntity SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Query("UPDATE DirectoryEntity SET active = :active WHERE storageType != :excludeType")
    suspend fun updateActive(excludeType: StorageType, active: Boolean)

    @Query("UPDATE DirectoryEntity SET selected = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun select(id: Long)

    @Delete(entity = DirectoryEntity::class)
    suspend fun delete(item: DirectoryEntity)
}
