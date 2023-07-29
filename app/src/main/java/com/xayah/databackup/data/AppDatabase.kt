package com.xayah.databackup.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PackageBackupEntire::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageBackupDao(): PackageBackupDao
}
