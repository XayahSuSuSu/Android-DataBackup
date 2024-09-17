package com.xayah.core.database

import android.content.Context
import androidx.room.Room
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.database.dao.DirectoryDao
import com.xayah.core.database.dao.LabelDao
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.PackageDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "database-databackup")
            .enableMultiInstanceInvalidation()
            .build()

    @Provides
    @Singleton
    fun providePackageDao(database: AppDatabase): PackageDao = database.packageDao()

    @Provides
    @Singleton
    fun provideMediaDao(database: AppDatabase): MediaDao = database.mediaDao()

    @Provides
    @Singleton
    fun provideDirectoryDao(database: AppDatabase): DirectoryDao = database.directoryDao()

    @Provides
    @Singleton
    fun provideCloudDao(database: AppDatabase): CloudDao = database.cloudDao()

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideLabelDao(database: AppDatabase): LabelDao = database.labelDao()
}
