package com.xayah.databackup.util

import android.content.Context
import androidx.room.Room
import com.xayah.databackup.data.AppDataBase
import com.xayah.databackup.data.AppEntity

class Room(context: Context) {
    private var db: AppDataBase = Room.databaseBuilder(
        context,
        AppDataBase::class.java, "app"
    ).build()

    fun close() {
        db.close()
    }

    fun insert(appEntity: AppEntity) {
        db.appDao().insert(appEntity)
    }

    fun delete(appEntity: AppEntity) {
        db.appDao().delete(appEntity)
    }

    fun update(appEntity: AppEntity) {
        db.appDao().update(appEntity)
    }

    fun getAll(): List<AppEntity> {
        return db.appDao().getAll()
    }

    fun findByPackage(
        packageName: String,
        onFound: (appEntity: AppEntity) -> Unit = { _ -> }
    ): AppEntity? {
        val appEntityList = db.appDao().findByPackage(packageName)
        if (appEntityList.isEmpty())
            return null
        else {
            val appEntity = appEntityList[0]
            onFound(appEntity)
            return appEntity
        }
    }

    fun insertOrUpdate(appEntity: AppEntity) {
        val oldAppEntity = findByPackage(appEntity.packageName)
        if (oldAppEntity == null)
            insert(appEntity)
        else {
            appEntity.uid = oldAppEntity.uid
            update(appEntity)
        }
    }
}