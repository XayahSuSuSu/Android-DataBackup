package com.xayah.databackup.util

import androidx.room.Room
import com.xayah.databackup.App
import com.xayah.databackup.database.AppDatabase

object DatabaseHelper {
    private val database = Room.databaseBuilder(
        App.application,
        AppDatabase::class.java,
        "database-databackup"
    ).build()

    val appDao = database.appDao()
    val networkDao = database.networkDao()
    val contactDao = database.contactDao()
    val callLogDao = database.callLogDao()
    val messageDao = database.messageDao()
}
