package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.database.PackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Upsert(entity = PackageEntity::class)
    suspend fun upsert(items: List<PackageEntity>)

    @Upsert(entity = PackageEntity::class)
    suspend fun upsert(item: PackageEntity)

    @Query("SELECT * FROM PackageEntity WHERE packageInfo_packageName = :packageName AND extraInfo_opType = :opType AND extraInfo_userId = :userId AND extraInfo_preserveId = :preserveId LIMIT 1")
    suspend fun query(packageName: String, opType: OpType, userId: Int, preserveId: Long): PackageEntity?

    @Query("SELECT * FROM PackageEntity WHERE packageInfo_packageName = :packageName AND extraInfo_opType = :opType AND extraInfo_userId = :userId")
    suspend fun query(packageName: String, opType: OpType, userId: Int): List<PackageEntity>

    @Query("SELECT * FROM PackageEntity WHERE packageInfo_packageName = :packageName AND extraInfo_opType = :opType AND extraInfo_userId = :userId AND extraInfo_preserveId = :preserveId AND extraInfo_compressionType = :ct LIMIT 1")
    suspend fun query(packageName: String, opType: OpType, userId: Int, preserveId: Long, ct: CompressionType): PackageEntity?

    @Query("SELECT * FROM PackageEntity WHERE extraInfo_activated = 1")
    suspend fun queryActivated(): List<PackageEntity>

    @Query("SELECT COUNT(*) FROM PackageEntity WHERE extraInfo_activated = 1")
    fun countActivatedFlow(): Flow<Long>

    @Query("UPDATE PackageEntity SET extraInfo_activated = 0")
    suspend fun clearActivated()

    @Query("SELECT * FROM PackageEntity WHERE extraInfo_opType = :opType AND extraInfo_preserveId = :preserveId")
    fun queryFlow(opType: OpType, preserveId: Long): Flow<List<PackageEntity>>

    @Query("SELECT * FROM PackageEntity WHERE packageInfo_packageName = :packageName AND extraInfo_opType = :opType AND extraInfo_userId = :userId AND extraInfo_preserveId = :preserveId LIMIT 1")
    fun queryFlow(packageName: String, opType: OpType, userId: Int, preserveId: Long): Flow<PackageEntity?>

    @Query("SELECT * FROM PackageEntity WHERE packageInfo_packageName = :packageName AND extraInfo_opType = :opType AND extraInfo_userId = :userId")
    fun queryFlow(packageName: String, opType: OpType, userId: Int): Flow<List<PackageEntity>>

    @Delete(entity = PackageEntity::class)
    suspend fun delete(item: PackageEntity)
}
