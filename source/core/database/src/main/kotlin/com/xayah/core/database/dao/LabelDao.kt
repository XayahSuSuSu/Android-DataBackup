package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.xayah.core.model.database.AppWithLabels
import com.xayah.core.model.database.FileWithLabels
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelFileCrossRefEntity
import com.xayah.core.model.database.LabelWithAppIds
import com.xayah.core.model.database.LabelWithFileIds
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Upsert(entity = LabelEntity::class)
    suspend fun upsert(item: LabelEntity): Long

    @Upsert(entity = LabelAppCrossRefEntity::class)
    suspend fun upsert(item: LabelAppCrossRefEntity): Long

    @Upsert(entity = LabelFileCrossRefEntity::class)
    suspend fun upsert(item: LabelFileCrossRefEntity): Long

    @Query("SELECT * FROM LabelEntity")
    fun queryLabelsFlow(): Flow<List<LabelEntity>>

    @Query("SELECT appId FROM LabelAppCrossRefEntity WHERE labelId in (:labelIds)")
    suspend fun queryAppIdsFlow(labelIds: Set<Long>): List<Long>

    @Query("SELECT fileId FROM LabelFileCrossRefEntity WHERE labelId in (:labelIds)")
    suspend fun queryFileIdsFlow(labelIds: Set<Long>): List<Long>

    @Transaction
    @Query("SELECT * FROM PackageEntity WHERE id = :id")
    fun queryAppWithLabelsFlow(id: Long): Flow<AppWithLabels?>

    @Transaction
    @Query("SELECT * FROM MediaEntity WHERE id = :id")
    fun queryFileWithLabelsFlow(id: Long): Flow<FileWithLabels?>

    @Transaction
    @Query("SELECT * FROM LabelEntity")
    fun queryLabelWithAppIdsFlow(): Flow<List<LabelWithAppIds>>

    @Transaction
    @Query("SELECT * FROM LabelEntity")
    fun queryLabelWithFileIdsFlow(): Flow<List<LabelWithFileIds>>

    @Query("DELETE FROM LabelEntity WHERE id = :id")
    suspend fun deleteAppRef(id: Long)

    @Query("DELETE FROM LabelAppCrossRefEntity WHERE labelId = :labelId AND appId = :appId")
    suspend fun deleteAppRef(labelId: Long, appId: Long)

    @Query("DELETE FROM LabelFileCrossRefEntity WHERE labelId = :labelId AND fileId = :fileId")
    suspend fun deleteFileRef(labelId: Long, fileId: Long)
}
