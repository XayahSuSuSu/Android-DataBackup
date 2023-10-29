package com.xayah.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.database.dao.DirectoryDao
import com.xayah.core.database.dao.LogDao
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.database.model.CmdEntity
import com.xayah.core.database.model.DirectoryEntity
import com.xayah.core.database.model.LogEntity
import com.xayah.core.database.model.MediaBackupEntity
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.MediaRestoreOperationEntity
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.database.model.PackageRestoreOperation
import com.xayah.core.database.util.StringListConverters

@Database(
    version = 3,
    exportSchema = true,
    entities = [
        LogEntity::class,
        CmdEntity::class,
        PackageBackupEntire::class,
        PackageBackupOperation::class,
        PackageRestoreEntire::class,
        PackageRestoreOperation::class,
        DirectoryEntity::class,
        MediaBackupEntity::class,
        MediaRestoreEntity::class,
        MediaBackupOperationEntity::class,
        MediaRestoreOperationEntity::class,
        CloudEntity::class, // Only used for premium build, but reserved in foss.
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = DatabaseMigrations.Schema2to3::class),
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
    abstract fun mediaDao(): MediaDao
    abstract fun cloudDao(): CloudDao // Only used for premium build, but reserved in foss.
}
