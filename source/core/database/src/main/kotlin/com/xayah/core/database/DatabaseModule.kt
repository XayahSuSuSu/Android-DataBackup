package com.xayah.core.database

import android.content.Context
import androidx.room.Room
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.database.dao.DirectoryDao
import com.xayah.core.database.dao.LogDao
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.database.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "database-databackup").enableMultiInstanceInvalidation().build()

    @Provides
    @Singleton
    fun provideLogDao(database: AppDatabase): LogDao = database.logDao()

    @Provides
    @Singleton
    fun providePackageBackupEntireDao(database: AppDatabase): PackageBackupEntireDao = database.packageBackupEntireDao()

    @Provides
    @Singleton
    fun providePackageBackupOperationDao(database: AppDatabase): PackageBackupOperationDao = database.packageBackupOperationDao()

    @Provides
    @Singleton
    fun providePackageRestoreEntireDao(database: AppDatabase): PackageRestoreEntireDao = database.packageRestoreEntireDao()

    @Provides
    @Singleton
    fun providePackageRestoreOperationDao(database: AppDatabase): PackageRestoreOperationDao = database.packageRestoreOperationDao()

    @Provides
    @Singleton
    fun provideDirectoryDao(database: AppDatabase): DirectoryDao = database.directoryDao()

    @Provides
    @Singleton
    fun provideMediaDao(database: AppDatabase): MediaDao = database.mediaDao()

    /**
     * Only used for premium build, but reserved in foss.
     */
    @Provides
    @Singleton
    fun provideCloudDao(database: AppDatabase): CloudDao = database.cloudDao()

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
}
