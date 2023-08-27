package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupOperationDao {
    @Upsert(entity = PackageBackupOperation::class)
    suspend fun upsert(item: PackageBackupOperation): Long

    @Query("SELECT timestamp FROM PackageBackupOperation ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationTime(): Long

    @Query("SELECT startTimestamp FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id LIMIT 1")
    suspend fun queryFirstOperationStartTime(timestamp: Long): Long

    @Query("SELECT endTimestamp FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationEndTime(timestamp: Long): Long

    @Query("SELECT * FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    fun queryLastOperationPackage(timestamp: Long): Flow<PackageBackupOperation>

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp")
    fun countByTimestamp(timestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp AND packageState = 1")
    suspend fun countSucceedByTimestamp(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp AND packageState = 0")
    suspend fun countFailedByTimestamp(timestamp: Long): Int
}
