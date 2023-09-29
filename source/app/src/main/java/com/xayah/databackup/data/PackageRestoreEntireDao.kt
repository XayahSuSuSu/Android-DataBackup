package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageRestoreEntireDao {
    @Upsert(entity = PackageRestoreEntire::class)
    suspend fun upsert(item: PackageRestoreEntire): Long

    @Update(entity = PackageRestoreEntire::class)
    suspend fun update(item: PackageRestoreEntire)

    @Upsert(entity = PackageRestoreEntire::class)
    suspend fun upsert(items: List<PackageRestoreEntire>)

    @Query("SELECT DISTINCT timestamp FROM PackageRestoreEntire")
    suspend fun queryTimestamps(): List<Long>

    @Query("SELECT * FROM PackageRestoreEntire WHERE timestamp = :timestamp")
    fun queryPackages(timestamp: Long): Flow<List<PackageRestoreEntire>>

    @Query("SELECT * FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    fun queryActiveTotalPackages(): List<PackageRestoreEntire>

    @Query("SELECT id, packageName, label FROM PackageRestoreEntire WHERE active = 1 AND operationCode = 3")
    fun queryActiveBothPackages(): Flow<List<PackageRestoreManifest>>

    @Query("SELECT id, packageName, label FROM PackageRestoreEntire WHERE active = 1 AND operationCode = 2")
    fun queryActiveAPKOnlyPackages(): Flow<List<PackageRestoreManifest>>

    @Query("SELECT id, packageName, label FROM PackageRestoreEntire WHERE active = 1 AND operationCode = 1")
    fun queryActiveDataOnlyPackages(): Flow<List<PackageRestoreManifest>>

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 2 OR operationCode = 3)")
    fun countSelectedTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 2 OR operationCode = 3)")
    fun countSelectedAPKs(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 3)")
    fun countSelectedData(): Flow<Int>

    @Query("UPDATE PackageRestoreEntire SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Query("UPDATE PackageRestoreEntire SET active = :active WHERE timestamp = :timestamp")
    suspend fun updateActive(timestamp: Long, active: Boolean)

    @Delete(entity = PackageRestoreEntire::class)
    suspend fun delete(items: List<PackageRestoreEntire>)

    @Query("DELETE FROM PackageRestoreEntire")
    suspend fun clearTable()
}
