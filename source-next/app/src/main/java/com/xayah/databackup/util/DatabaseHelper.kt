package com.xayah.databackup.util

import androidx.room.Room
import com.xayah.databackup.App
import com.xayah.databackup.database.AppDatabase

object DatabaseHelper {
    private val mDatabase = Room.databaseBuilder(
        App.application,
        AppDatabase::class.java,
        "database-databackup"
    ).build()

    val appDao = mDatabase.appDao()
    val networkDao = mDatabase.networkDao()
    val contactDao = mDatabase.contactDao()
    val callLogDao = mDatabase.callLogDao()
    val messageDao = mDatabase.messageDao()
}
