package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupDao {
    @Upsert(entity = PackageBackupEntire::class)
    suspend fun insert(items: List<PackageBackupUpdate>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(items: List<PackageBackupActivate>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(item: PackageBackupEntire)

    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1")
    fun queryActivePackages(): Flow<List<PackageBackupEntire>>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE active = 1")
    suspend fun countActivePackages(): Int

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE operationCode = 2 OR operationCode = 3")
    fun countSelectedAPKs(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageBackupEntire WHERE operationCode = 1 OR operationCode = 3")
    fun countSelectedData(): Flow<Int>

    @Query("UPDATE PackageBackupEntire SET active = :active")
    suspend fun updateActive(active: Boolean)
}
