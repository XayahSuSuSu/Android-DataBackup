package com.xayah.databackup.model.app

import androidx.room.*


@Dao
interface AppDao {
    @Insert
    fun insertAll(vararg appEntity: AppEntity)

    @Delete
    fun delete(appEntity: AppEntity)

    @Delete
    fun deleteAll(appEntity: List<AppEntity>)

    @Query("SELECT * FROM appEntity")
    fun getAllUsers(): List<AppEntity>

    @Update
    fun updateUser(appEntity: AppEntity)
}
