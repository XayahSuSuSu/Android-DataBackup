package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupOperationDao {
    @Upsert(entity = PackageBackupOperation::class)
    suspend fun insert(item: PackageBackupOperation): Long

    @Query("SELECT * FROM PackageBackupOperation WHERE timestamp = :timestamp ORDER BY id DESC LIMIT 1")
    fun queryOperationPackage(timestamp: Long): Flow<PackageBackupOperation>

    @Query("SELECT COUNT(*) FROM PackageBackupOperation WHERE timestamp = :timestamp")
    fun countByTimestamp(timestamp: Long): Flow<Int>
}
