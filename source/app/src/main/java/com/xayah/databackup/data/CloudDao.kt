package com.xayah.databackup.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudDao {
    @Upsert(entity = CloudEntity::class)
    suspend fun upsertMount(item: CloudMountEntity)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertMount(items: List<CloudMountEntity>)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertAccount(items: List<CloudAccountEntity>)

    @Query("SELECT * FROM CloudEntity WHERE active = 1")
    fun queryAccountFlow(): Flow<List<CloudAccountEntity>>

    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryAccountByName(name: String): CloudAccountEntity?

    @Query("SELECT * FROM CloudEntity WHERE active = 1")
    fun queryMountFlow(): Flow<List<CloudMountEntity>>

    @Query("SELECT * FROM CloudEntity")
    fun queryMount(): List<CloudMountEntity>

    @Query("UPDATE CloudEntity SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Delete(entity = CloudEntity::class)
    suspend fun deleteAccount(item: CloudAccountEntity)
}
