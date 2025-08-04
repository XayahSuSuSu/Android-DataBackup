package com.xayah.databackup.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xayah.databackup.database.dao.AppDao
import com.xayah.databackup.database.dao.NetworkDao
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.Network

@Database(
    entities = [
        App::class,
        Network::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun networkDao(): NetworkDao
}
