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

    @Query("SELECT * FROM MediaBackupEntity")
    suspend fun queryAllBackup(): List<MediaBackupEntity>

    @Transaction
    @Query("SELECT * FROM MediaRestoreEntity WHERE timestamp = :timestamp AND savePath = :savePath")
    fun queryAllRestoreFlow(timestamp: Long, savePath: String): Flow<List<MediaRestoreWithOpEntity>>

    @Query("SELECT * FROM MediaBackupEntity WHERE selected = 1")
    suspend fun queryBackupSelected(): List<MediaBackupEntity>

    @Query("SELECT * FROM MediaRestoreEntity WHERE selected = 1 AND timestamp = :timestamp")
    suspend fun queryRestoreSelected(timestamp: Long): List<MediaRestoreEntity>

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

    @Query("DELETE FROM MediaRestoreEntity")
    suspend fun clearRestoreTable()
}
