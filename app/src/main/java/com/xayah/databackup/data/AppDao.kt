package com.xayah.databackup.data

import androidx.room.*

@Dao
interface AppDao {
    @Insert
    fun insert(vararg appEntity: AppEntity)

    @Delete
    fun delete(appEntity: AppEntity)

    @Update
    fun update(appEntity: AppEntity)

    @Query("SELECT * FROM AppEntity")
    fun getAll(): List<AppEntity>

    @Query("SELECT * FROM AppEntity WHERE packageName = :packageName ")
    fun findByPackage(packageName: String): List<AppEntity>
}