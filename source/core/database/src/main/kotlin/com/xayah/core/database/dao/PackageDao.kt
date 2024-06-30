package com.xayah.core.database.dao

import androidx.room.Dao
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

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_packageName = :packageName AND indexInfo_opType = :opType AND indexInfo_userId = :userId AND indexInfo_preserveId = :preserveId" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir" +
                " LIMIT 1"
    )
    suspend fun query(packageName: String, opType: OpType, userId: Int, preserveId: Long, cloud: String, backupDir: String): PackageEntity?

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_packageName = :packageName AND indexInfo_opType = :opType AND indexInfo_userId = :userId" +
                " LIMIT 1"
    )
    suspend fun query(packageName: String, opType: OpType, userId: Int): PackageEntity?

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_packageName = :packageName AND indexInfo_opType = :opType AND indexInfo_userId = :userId" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    suspend fun query(packageName: String, opType: OpType, userId: Int, cloud: String, backupDir: String): List<PackageEntity>

    @Query(
        "SELECT indexInfo_packageName FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND indexInfo_userId = :userId" +
                " AND extraInfo_blocked = 0 AND extraInfo_existed = 1"
    )
    suspend fun queryPackageNamesByUserId(opType: OpType, userId: Int): List<String>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_packageName = :packageName AND indexInfo_opType = :opType AND indexInfo_userId = :userId AND indexInfo_preserveId = :preserveId AND indexInfo_compressionType = :ct" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir" +
                " LIMIT 1"
    )
    suspend fun query(packageName: String, opType: OpType, userId: Int, preserveId: Long, ct: CompressionType, cloud: String, backupDir: String): PackageEntity?

    @Query("SELECT * FROM PackageEntity WHERE extraInfo_activated = 1 AND extraInfo_existed = 1 AND indexInfo_opType = :opType")
    suspend fun queryActivated(opType: OpType): List<PackageEntity>

    @Query("SELECT * FROM PackageEntity WHERE extraInfo_activated = 1 AND extraInfo_existed = 1 AND indexInfo_opType = :opType AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir")
    suspend fun queryActivated(opType: OpType, cloud: String, backupDir: String): List<PackageEntity>

    @Query("UPDATE PackageEntity SET extraInfo_activated = 0")
    suspend fun clearActivated()

    @Query("UPDATE PackageEntity SET extraInfo_existed = 0 WHERE indexInfo_opType = :opType")
    suspend fun clearExisted(opType: OpType)

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_packageName = :packageName AND indexInfo_opType = :opType AND indexInfo_userId = :userId AND indexInfo_preserveId = :preserveId" +
                " LIMIT 1"
    )
    fun queryFlow(packageName: String, opType: OpType, userId: Int, preserveId: Long): Flow<PackageEntity?>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND extraInfo_existed = 1"
    )
    fun queryPackagesFlow(opType: OpType): Flow<List<PackageEntity>>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND extraInfo_existed = :existed AND extraInfo_blocked = :blocked"
    )
    fun queryPackagesFlow(opType: OpType, existed: Boolean, blocked: Boolean): Flow<List<PackageEntity>>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND extraInfo_blocked = :blocked"
    )
    fun queryPackagesFlow(opType: OpType, blocked: Boolean): Flow<List<PackageEntity>>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND extraInfo_existed = 1 AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    fun queryPackagesFlow(opType: OpType, cloud: String, backupDir: String): Flow<List<PackageEntity>>

    @Query(
        "SELECT DISTINCT indexInfo_userId FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType"
    )
    suspend fun queryUserIds(opType: OpType): List<Int>

    @Query(
        "SELECT * FROM PackageEntity WHERE" +
                " indexInfo_opType = :opType AND extraInfo_existed = 1 AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    suspend fun queryPackages(opType: OpType, cloud: String, backupDir: String): List<PackageEntity>

    @Query(
        "UPDATE PackageEntity" +
                " SET extraInfo_blocked = :blocked" +
                " WHERE id = :id"
    )
    suspend fun setBlocked(id: Long, blocked: Boolean)

    @Query(
        "UPDATE PackageEntity" +
                " SET extraInfo_existed = :existed" +
                " WHERE indexInfo_opType = :opType AND indexInfo_packageName = :packageName AND indexInfo_userId = :userId"
    )
    suspend fun setExisted(opType: OpType, packageName: String, userId: Int, existed: Boolean)

    @Query(
        "UPDATE PackageEntity SET extraInfo_blocked = 0"
    )
    suspend fun clearBlocked()

    @Query("DELETE FROM PackageEntity WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM PackageEntity WHERE indexInfo_backupDir = :backupDir")
    suspend fun delete(backupDir: String)
}
