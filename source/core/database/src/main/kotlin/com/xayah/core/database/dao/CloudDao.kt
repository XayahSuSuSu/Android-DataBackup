package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Upsert
import com.xayah.core.database.model.CloudAccountEntity
import com.xayah.core.database.model.CloudBaseEntity
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.database.model.CloudMountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Only used for premium build, but reserved in foss.
 */
@Dao
interface CloudDao {
    @Upsert(entity = CloudEntity::class)
    suspend fun upsertCloud(item: CloudEntity)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertMount(item: CloudMountEntity)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertMount(items: List<CloudMountEntity>)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertBase(item: CloudBaseEntity)

    @Upsert(entity = CloudEntity::class)
    suspend fun upsertAccount(items: List<CloudAccountEntity>)

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE active = 1")
    fun queryAccountFlow(): Flow<List<CloudAccountEntity>>

    @Query("SELECT * FROM CloudEntity WHERE active = 1")
    fun queryActiveCloudsFlow(): Flow<List<CloudEntity>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryAccountByName(name: String): CloudAccountEntity?

    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryCloudByName(name: String): CloudEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    suspend fun queryMountByName(name: String): CloudMountEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    fun queryBaseByName(name: String): CloudBaseEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE name = :name LIMIT 1")
    fun queryBaseByNameFlow(name: String): Flow<CloudBaseEntity?>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity WHERE active = 1")
    fun queryMountFlow(): Flow<List<CloudMountEntity>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM CloudEntity")
    fun queryMount(): List<CloudMountEntity>

    @Query("UPDATE CloudEntity SET active = :active")
    suspend fun updateActive(active: Boolean)

    @Delete(entity = CloudEntity::class)
    suspend fun deleteAccount(item: CloudAccountEntity)

    @Delete(entity = CloudEntity::class)
    suspend fun deleteCloud(item: CloudEntity)
}
