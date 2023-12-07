package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.xayah.core.database.model.MediaBackupEntity
import com.xayah.core.database.model.MediaBackupEntityUpsert
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.MediaBackupWithOpEntity
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.MediaRestoreOperationEntity
import com.xayah.core.database.model.MediaRestoreWithOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Upsert(entity = MediaBackupEntity::class)
    suspend fun upsertBackup(item: MediaBackupEntity)

    @Upsert(entity = MediaRestoreEntity::class)
    suspend fun upsertRestore(item: MediaRestoreEntity)

    @Upsert(entity = MediaBackupOperationEntity::class)
    suspend fun upsertBackupOp(item: MediaBackupOperationEntity): Long

    @Query("SELECT * FROM MediaBackupOperationEntity WHERE timestamp = :timestamp ORDER BY id DESC")
    fun observeBackupOp(timestamp: Long): Flow<List<MediaBackupOperationEntity>>

    @Upsert(entity = MediaRestoreOperationEntity::class)
    suspend fun upsertRestoreOp(item: MediaRestoreOperationEntity): Long

    @Upsert(entity = MediaBackupEntity::class)
    suspend fun upsertBackup(items: List<MediaBackupEntityUpsert>)

    @Upsert(entity = MediaRestoreEntity::class)
    suspend fun upsertRestore(items: List<MediaRestoreEntity>)

    @Transaction
    @Query("SELECT * FROM MediaBackupEntity")
    fun queryAllBackupFlow(): Flow<List<MediaBackupWithOpEntity>>

    @Query("SELECT * FROM MediaBackupEntity")
    fun observeBackupMedium(): Flow<List<MediaBackupEntity>>

    @Query("SELECT * FROM MediaBackupEntity")
    suspend fun queryAllBackup(): List<MediaBackupEntity>

    @Transaction
    @Query("SELECT * FROM MediaRestoreEntity WHERE active = 1 AND timestamp = :timestamp AND savePath = :savePath")
    fun queryAllRestoreFlow(timestamp: Long, savePath: String): Flow<List<MediaRestoreWithOpEntity>>

    @Query("SELECT * FROM MediaRestoreEntity")
    suspend fun queryAllRestore(): List<MediaRestoreEntity>

    @Query("SELECT * FROM MediaBackupEntity WHERE selected = 1")
    suspend fun queryBackupSelected(): List<MediaBackupEntity>

    @Query("SELECT * FROM MediaBackupEntity WHERE selected = 1")
    fun observeBackupSelected(): Flow<List<MediaBackupEntity>>

    @Query("SELECT * FROM MediaRestoreEntity WHERE active = 1 AND selected = 1")
    suspend fun queryRestoreSelected(): List<MediaRestoreEntity>

    @Query("SELECT * FROM MediaRestoreEntity WHERE timestamp = :timestamp")
    suspend fun queryRestoreMedium(timestamp: Long): List<MediaRestoreEntity>

    @Query("SELECT DISTINCT timestamp FROM MediaRestoreEntity WHERE savePath = :savePath")
    suspend fun queryTimestamps(savePath: String): List<Long>

    @Query("SELECT COUNT(*) FROM MediaBackupEntity WHERE selected = 1")
    fun countBackupSelected(): Flow<Int>

    @Query("SELECT COUNT(*) FROM MediaRestoreEntity WHERE selected = 1 AND timestamp = :timestamp AND savePath = :savePath")
    fun countRestoreSelected(timestamp: Long, savePath: String): Flow<Int>

    @Query("UPDATE MediaBackupEntity SET selected = :selected")
    suspend fun updateBackupSelected(selected: Boolean)

    @Query("UPDATE MediaRestoreEntity SET selected = :selected")
    suspend fun updateRestoreSelected(selected: Boolean)

    @Delete(entity = MediaBackupEntity::class)
    suspend fun deleteBackup(item: MediaBackupEntity)

    @Delete(entity = MediaRestoreEntity::class)
    suspend fun deleteRestore(item: MediaRestoreEntity)

    @Delete(entity = MediaRestoreEntity::class)
    suspend fun deleteRestore(items: List<MediaRestoreEntity>)

    @Query("DELETE FROM MediaRestoreEntity")
    suspend fun clearRestoreTable()

    @Query("UPDATE MediaBackupEntity SET selected = :selected WHERE path in (:pathList)")
    suspend fun batchSelectOp(selected: Boolean, pathList: List<String>)

    @Query("SELECT DISTINCT timestamp FROM MediaRestoreEntity WHERE  savePath = :savePath")
    fun observeTimestamps(savePath: String): Flow<List<Long>>

    @Query("SELECT * FROM MediaRestoreEntity WHERE active = 1 AND timestamp = :timestamp GROUP BY path")
    fun queryMediumFlow(timestamp: Long): Flow<List<MediaRestoreEntity>>

    @Query("SELECT * FROM MediaRestoreEntity WHERE path = :path AND timestamp = :timestamp AND savePath = :savePath LIMIT 1")
    fun queryMedia(path: String, timestamp: Long, savePath: String): MediaRestoreEntity?

    @Query("UPDATE MediaRestoreEntity SET selected = :selected WHERE path in (:pathList) AND timestamp = :timestamp")
    suspend fun batchSelectOp(selected: Boolean, timestamp: Long, pathList: List<String>)

    @Query("SELECT * FROM MediaRestoreOperationEntity WHERE timestamp = :timestamp ORDER BY id DESC")
    fun observeRestoreOp(timestamp: Long): Flow<List<MediaRestoreOperationEntity>>

    @Query("SELECT * FROM MediaRestoreEntity WHERE active = 1")
    fun observeActiveMedium(): Flow<List<MediaRestoreEntity>>

    @Query("UPDATE MediaRestoreEntity SET active = :active")
    suspend fun updateRestoreActive(active: Boolean)

    @Query("UPDATE MediaRestoreEntity SET active = :active WHERE timestamp = :timestamp AND savePath = :savePath")
    suspend fun updateRestoreActive(active: Boolean, timestamp: Long, savePath: String)

    @Query("DELETE FROM MediaRestoreEntity WHERE id NOT IN (SELECT MIN(id) FROM MediaRestoreEntity GROUP BY path, timestamp, savePath)")
    suspend fun deduplicate()
}
