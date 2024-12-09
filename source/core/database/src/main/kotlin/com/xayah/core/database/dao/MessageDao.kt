package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert(entity = MessageEntity::class)
    suspend fun upsert(items: List<MessageEntity>)

    @Upsert(entity = MessageEntity::class)
    suspend fun upsert(item: MessageEntity)

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_opType = :opType"
    )
    suspend fun query(opType: OpType): List<MessageEntity>

    @Query(
        "SELECT * FROM MessageEntity" +
                " WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    suspend fun query(opType: OpType, preserveId: Long, cloud: String, backupDir: String): List<MessageEntity>

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_opType = :opType AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    suspend fun query(opType: OpType, cloud: String, backupDir: String): List<MessageEntity>

    @Query(
        "SELECT * FROM MessageEntity" +
                " WHERE indexInfo_opType = :opType AND indexInfo_name = :name" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    suspend fun query(opType: OpType, name: String, cloud: String, backupDir: String): List<MessageEntity>

    @Query(
        "SELECT * FROM MessageEntity" +
                " WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId AND indexInfo_name = :name" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir" +
                " LIMIT 1"
    )
    suspend fun query(opType: OpType, preserveId: Long, name: String, cloud: String, backupDir: String): MessageEntity?

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_name = :name AND indexInfo_opType = :opType" +
                " LIMIT 1"
    )
    suspend fun query(name: String, opType: OpType): MessageEntity?

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId AND indexInfo_name = :name AND indexInfo_compressionType = :ct" +
                " AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir" +
                " LIMIT 1"
    )
    suspend fun query(opType: OpType, preserveId: Long, name: String, ct: CompressionType, cloud: String, backupDir: String): MessageEntity?

    @Query("SELECT * FROM MessageEntity WHERE indexInfo_opType = :opType")
    suspend fun queryActivated(opType: OpType): List<MessageEntity>

    @Query("SELECT * FROM MessageEntity WHERE indexInfo_opType = :opType AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir")
    suspend fun queryActivated(opType: OpType, cloud: String, backupDir: String): List<MessageEntity>

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_opType = :opType"
    )
    fun queryFlow(opType: OpType): Flow<List<MessageEntity>>

    @Query(
        "SELECT * FROM MessageEntity WHERE" +
                " indexInfo_opType = :opType AND indexInfo_cloud = :cloud AND indexInfo_backupDir = :backupDir"
    )
    fun queryFlow(opType: OpType, cloud: String, backupDir: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId")
    fun queryFlow(opType: OpType, preserveId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity WHERE indexInfo_name = :name AND indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId LIMIT 1")
    fun queryFlow(name: String, opType: OpType, preserveId: Long): Flow<MessageEntity?>

    @Query("SELECT * FROM MessageEntity WHERE indexInfo_name = :name AND indexInfo_opType = :opType")
    fun queryFlow(name: String, opType: OpType): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM MessageEntity")
    suspend fun count(): Long

    @Delete(entity = MessageEntity::class)
    suspend fun delete(item: MessageEntity)

    @Query("DELETE FROM MessageEntity WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM MessageEntity WHERE id in (:ids)")
    suspend fun delete(ids: List<Long>)

    @Update(MessageEntity::class)
    suspend fun update(item: MessageEntity)
}