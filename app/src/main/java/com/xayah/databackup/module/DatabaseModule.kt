package com.xayah.databackup.module

import android.content.Context
import androidx.room.Room
import com.xayah.databackup.data.AppDatabase
import com.xayah.databackup.data.PackageBackupDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "database-databackup").build()

    @Provides
    @Singleton
    fun providePackageBackupDao(database: AppDatabase): PackageBackupDao = database.packageBackupDao()
}
