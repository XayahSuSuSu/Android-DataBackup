package com.xayah.databackup.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {
    abstract fun appDao(): AppDao
}