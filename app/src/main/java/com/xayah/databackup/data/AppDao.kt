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

    @Query("UPDATE AppEntity SET backupApp = :mBoolean ")
    fun selectAllApp(mBoolean: Boolean)

    @Query("UPDATE AppEntity SET backupData = :mBoolean ")
    fun selectAllData(mBoolean: Boolean)

    @Query("UPDATE AppEntity SET backupApp = NOT backupApp ")
    fun reverseAllApp()

    @Query("UPDATE AppEntity SET backupData = NOT backupData ")
    fun reverseAllData()
}