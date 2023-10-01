package com.xayah.databackup.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = 2,
    exportSchema = true,
    entities = [
        LogEntity::class,
        CmdEntity::class,
        PackageBackupEntire::class,
        PackageBackupOperation::class,
        PackageRestoreEntire::class,
        PackageRestoreOperation::class,
        DirectoryEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(StringListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun packageBackupEntireDao(): PackageBackupEntireDao
    abstract fun packageBackupOperationDao(): PackageBackupOperationDao
    abstract fun packageRestoreEntireDao(): PackageRestoreEntireDao
    abstract fun packageRestoreOperationDao(): PackageRestoreOperationDao
    abstract fun directoryDao(): DirectoryDao
}
