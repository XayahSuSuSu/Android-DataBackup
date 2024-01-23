package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaDetail
import com.xayah.core.model.database.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Upsert(entity = MediaEntity::class)
    suspend fun upsert(items: List<MediaEntity>)

    @Upsert(entity = MediaEntity::class)
    suspend fun upsert(item: MediaEntity)

    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId")
    suspend fun query(opType: OpType, preserveId: Long): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_name = :name")
    suspend fun query(opType: OpType, name: String): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId AND indexInfo_name = :name LIMIT 1")
    suspend fun query(opType: OpType, preserveId: Long, name: String): MediaEntity?

    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId AND indexInfo_name = :name AND indexInfo_compressionType = :ct LIMIT 1")
    suspend fun query(opType: OpType, preserveId: Long, name: String, ct: CompressionType): MediaEntity?

    @Query("SELECT * FROM MediaEntity WHERE extraInfo_activated = 1")
    suspend fun queryActivated(): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_preserveId = :preserveId")
    fun queryFlow(opType: OpType, preserveId: Long): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM MediaEntity WHERE extraInfo_activated = 1")
    fun countActivatedFlow(): Flow<Long>

    @Query("UPDATE MediaEntity SET extraInfo_activated = 0")
    suspend fun clearActivated()

    @Transaction
    @Query("SELECT * FROM MediaEntity WHERE indexInfo_opType = :opType AND indexInfo_name = :name LIMIT 1")
    fun queryFlow(opType: OpType = OpType.BACKUP, name: String): Flow<MediaDetail?>

    @Delete(entity = MediaEntity::class)
    suspend fun delete(item: MediaEntity)
}
