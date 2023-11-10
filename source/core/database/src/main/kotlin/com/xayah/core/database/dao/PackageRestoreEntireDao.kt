package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.database.model.PackageRestoreManifest
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageRestoreEntireDao {
    @Upsert(entity = PackageRestoreEntire::class)
    suspend fun upsert(item: PackageRestoreEntire): Long

    @Update(entity = PackageRestoreEntire::class)
    suspend fun update(item: PackageRestoreEntire)

    @Upsert(entity = PackageRestoreEntire::class)
    suspend fun upsert(items: List<PackageRestoreEntire>)

    @Query("SELECT DISTINCT timestamp FROM PackageRestoreEntire WHERE savePath = :savePath")
    suspend fun queryTimestamps(savePath: String): List<Long>

    @Query("SELECT * FROM PackageRestoreEntire WHERE active = 1 AND timestamp = :timestamp")
    fun queryPackagesFlow(timestamp: Long): Flow<List<PackageRestoreEntire>>

    @Query("SELECT * FROM PackageRestoreEntire WHERE timestamp = :timestamp")
    suspend fun queryPackages(timestamp: Long): List<PackageRestoreEntire>

    @Query("SELECT * FROM PackageRestoreEntire")
    suspend fun queryAll(): List<PackageRestoreEntire>

    @Query("SELECT * FROM PackageRestoreEntire")
    fun queryAllFlow(): Flow<List<PackageRestoreEntire>>

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

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND operationCode = 3")
    fun countSelectedBoth(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 2 OR operationCode = 3)")
    fun countSelectedAPKs(): Flow<Int>

    @Query("SELECT COUNT(*) FROM PackageRestoreEntire WHERE active = 1 AND (operationCode = 1 OR operationCode = 3)")
    fun countSelectedData(): Flow<Int>

    @Query("UPDATE PackageRestoreEntire SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Query("UPDATE PackageRestoreEntire SET active = :active WHERE timestamp = :timestamp AND savePath = :savePath")
    suspend fun updateActive(active: Boolean, timestamp: Long, savePath: String)

    @Delete(entity = PackageRestoreEntire::class)
    suspend fun delete(items: List<PackageRestoreEntire>)

    @Query("DELETE FROM PackageRestoreEntire")
    suspend fun clearTable()
}
