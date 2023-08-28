package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Upsert

@Dao
interface PackageRestoreEntireDao {
    @Upsert(entity = PackageRestoreEntire::class)
    suspend fun upsert(item: PackageRestoreEntire): Long
}
