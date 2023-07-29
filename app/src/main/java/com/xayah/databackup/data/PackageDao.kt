package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageBackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = PackageBackupEntire::class)
    suspend fun insert(items: List<PackageBackupUpdate>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(items: List<PackageBackupActivate>)

    @Update(entity = PackageBackupEntire::class)
    suspend fun update(item: PackageBackupEntire)

    @Query("SELECT * FROM PackageBackupEntire WHERE active = 1")
    fun queryActivePackages(): Flow<List<PackageBackupEntire>>

    @Query("UPDATE PackageBackupEntire SET active = :active")
    suspend fun updateActive(active: Boolean)
}
