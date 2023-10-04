package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Upsert(entity = MediaBackupEntity::class)
    suspend fun upsertBackup(item: MediaBackupEntity)

    @Upsert(entity = MediaRestoreEntity::class)
    suspend fun upsertRestore(item: MediaRestoreEntity)

    @Upsert(entity = MediaBackupOperationEntity::class)
    suspend fun upsertBackupOp(item: MediaBackupOperationEntity): Long

    @Upsert(entity = MediaRestoreOperationEntity::class)
    suspend fun upsertRestoreOp(item: MediaRestoreOperationEntity): Long

    @Upsert(entity = MediaBackupEntity::class)
    suspend fun upsertBackup(items: List<MediaBackupEntityUpsert>)

    @Upsert(entity = MediaRestoreEntity::class)
    suspend fun upsertRestore(items: List<MediaRestoreEntity>)

    @Transaction
    @Query("SELECT * FROM MediaBackupEntity")
    fun queryAllBackupFlow(): Flow<List<MediaBackupWithOpEntity>>

    @Transaction
    @Query("SELECT * FROM MediaBackupEntity")
    suspend fun queryAllBackup(): List<MediaBackupEntity>

    @Transaction
    @Query("SELECT * FROM MediaRestoreEntity WHERE timestamp = :timestamp")
    fun queryAllRestoreFlow(timestamp: Long): Flow<List<MediaRestoreWithOpEntity>>

    @Query("SELECT * FROM MediaBackupEntity WHERE selected = 1")
    suspend fun queryBackupSelected(): List<MediaBackupEntity>

    @Query("SELECT * FROM MediaRestoreEntity WHERE selected = 1 AND timestamp = :timestamp")
    suspend fun queryRestoreSelected(timestamp: Long): List<MediaRestoreEntity>

    @Query("SELECT DISTINCT timestamp FROM MediaRestoreEntity")
    suspend fun queryTimestamps(): List<Long>

    @Query("SELECT COUNT(*) FROM MediaBackupEntity WHERE selected = 1")
    fun countBackupSelected(): Flow<Int>

    @Query("SELECT COUNT(*) FROM MediaRestoreEntity WHERE selected = 1 AND timestamp = :timestamp")
    fun countRestoreSelected(timestamp: Long): Flow<Int>

    @Delete(entity = MediaBackupEntity::class)
    suspend fun deleteBackup(item: MediaBackupEntity)

    @Delete(entity = MediaRestoreEntity::class)
    suspend fun deleteRestore(item: MediaRestoreEntity)
}
