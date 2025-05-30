package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.AppInfo
import com.xayah.databackup.database.entity.AppStorage
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert(entity = App::class)
    suspend fun upsert(apps: List<App>)

    @Upsert(entity = App::class)
    suspend fun upsertInfo(apps: List<AppInfo>)

    @Upsert(entity = App::class)
    suspend fun upsertStorage(apps: List<AppStorage>)

    @Query("SELECT * from apps")
    fun loadFlowApps(): Flow<List<App>>

    @Query("UPDATE apps SET option_apk = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectApk(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_internalData = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectInternalData(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_externalData = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectExternalData(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_obbAndMedia = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectObbAndMedia(packageName: String, userId: Int, selected: Boolean)

    @Query("UPDATE apps SET option_apk = :selected, option_internalData = :selected, option_externalData = :selected, option_obbAndMedia = :selected WHERE packageName = :packageName AND userId = :userId")
    suspend fun selectAll(packageName: String, userId: Int, selected: Boolean)
}
