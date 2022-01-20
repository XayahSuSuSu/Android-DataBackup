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
    fun getAllApps(): List<AppEntity>

    @Query("UPDATE appEntity set isOnly=:isOnly")
    fun selectAllIsOnly(isOnly: Boolean)

    @Query("UPDATE appEntity set isSelected=:isSelected")
    fun selectAllIsSelected(isSelected: Boolean)

    @Update
    fun updateApp(appEntity: AppEntity)
}
