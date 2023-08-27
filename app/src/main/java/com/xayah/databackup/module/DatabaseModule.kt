package com.xayah.databackup.module

import android.content.Context
import androidx.room.Room
import com.xayah.databackup.data.AppDatabase
import com.xayah.databackup.data.LogDao
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperationDao
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
}
