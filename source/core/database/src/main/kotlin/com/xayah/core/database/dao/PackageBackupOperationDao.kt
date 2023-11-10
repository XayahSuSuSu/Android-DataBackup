package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.database.model.PackageBackupOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupOperationDao {
    @Upsert(entity = PackageBackupOperation::class)
    suspend fun upsert(item: PackageBackupOperation): Long

    @Query("SELECT * FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC")
    fun queryOperationsFlow(timestamp: Long): Flow<List<PackageBackupOperation>>

    @Query("SELECT timestamp FROM PackageBackupOperation ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationTime(): Long

    @Query("SELECT startTimestamp FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id LIMIT 1")
    suspend fun queryFirstOperationStartTime(timestamp: Long): Long

    @Query("SELECT endTimestamp FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    suspend fun queryLastOperationEndTime(timestamp: Long): Long

    @Query("SELECT * FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    fun queryLastOperationPackage(timestamp: Long): Flow<PackageBackupOperation>

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp AND endTimestamp != 0")
    fun countByTimestamp(timestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp AND packageState = 'DONE'")
    suspend fun countSucceedByTimestamp(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp AND packageState = 'ERROR'")
    suspend fun countFailedByTimestamp(timestamp: Long): Int
}
