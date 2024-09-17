package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.xayah.core.model.database.AppWithLabels
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelWithAppIds
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Upsert(entity = LabelEntity::class)
    suspend fun upsert(item: LabelEntity): Long

    @Upsert(entity = LabelAppCrossRefEntity::class)
    suspend fun upsert(item: LabelAppCrossRefEntity): Long

    @Query("SELECT * FROM LabelEntity")
    fun queryLabelsFlow(): Flow<List<LabelEntity>>

    @Transaction
    @Query("SELECT * FROM PackageEntity WHERE id = :id")
    fun queryAppWithLabelsFlow(id: Long): Flow<AppWithLabels?>

    @Transaction
    @Query("SELECT * FROM LabelEntity")
    fun queryLabelWithAppIdsFlow(): Flow<List<LabelWithAppIds>>

    @Query("DELETE FROM LabelEntity WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM LabelAppCrossRefEntity WHERE labelId = :labelId AND appId = :appId")
    suspend fun delete(labelId: Long, appId: Long)
}
