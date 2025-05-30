package com.xayah.databackup.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xayah.databackup.database.dao.AppDao
import com.xayah.databackup.database.entity.App

@Database(
    entities = [App::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
