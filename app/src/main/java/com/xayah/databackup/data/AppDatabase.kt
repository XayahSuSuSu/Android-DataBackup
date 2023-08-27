package com.xayah.databackup.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LogEntity::class, CmdEntity::class, PackageBackupEntire::class, PackageBackupOperation::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packageBackupEntireDao(): PackageBackupEntireDao
    abstract fun packageBackupOperationDao(): PackageBackupOperationDao
}
