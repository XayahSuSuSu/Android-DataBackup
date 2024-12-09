package com.xayah.core.data.repository

import com.xayah.core.database.dao.LabelDao
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelFileCrossRefEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LabelsRepo @Inject constructor(
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val labelDao: LabelDao,
) {
    fun getLabelsFlow(): Flow<List<LabelEntity>> = labelDao.queryLabelsFlow().flowOn(defaultDispatcher)
    fun getAppRefsFlow(): Flow<List<LabelAppCrossRefEntity>> = labelDao.queryAppRefsFlow()
    fun getFileRefsFlow(): Flow<List<LabelFileCrossRefEntity>> = labelDao.queryFileRefsFlow()
    suspend fun getLabels(): List<LabelEntity> = labelDao.queryLabels()
    suspend fun getAppRefs(labelIds: Set<String>): List<LabelAppCrossRefEntity> = labelDao.queryAppRefs(labelIds)
    suspend fun getAppRefs(): List<LabelAppCrossRefEntity> = labelDao.queryAppRefs()
    suspend fun getFileRefs(labelIds: Set<String>): List<LabelFileCrossRefEntity> = labelDao.queryFileRefs(labelIds)
    suspend fun getFileRefs(): List<LabelFileCrossRefEntity> = labelDao.queryFileRefs()

    /**
     * Add a unique label
     */
    suspend fun addLabel(label: String) = labelDao.upsert(LabelEntity(label = label))

    suspend fun addLabels(items: List<LabelEntity>) = labelDao.upsertLabels(items)

    /**
     * Add a unique label app cross ref
     */
    suspend fun addLabelAppCrossRef(item: LabelAppCrossRefEntity) = labelDao.upsert(item)

    suspend fun addLabelAppCrossRefs(items: List<LabelAppCrossRefEntity>) = labelDao.upsertAppRefs(items)

    /**
     * Add a unique label file cross ref
     */
    suspend fun addLabelFileCrossRef(item: LabelFileCrossRefEntity) = labelDao.upsert(item)

    suspend fun addLabelFileCrossRefs(items: List<LabelFileCrossRefEntity>) = labelDao.upsertFileRefs(items)


    suspend fun deleteLabel(label: String) {
        labelDao.delete(label)
    }

    suspend fun deleteLabelAppCrossRef(item: LabelAppCrossRefEntity) {
        labelDao.deleteAppRef(item)
    }

    suspend fun deleteLabelFileCrossRef(item: LabelFileCrossRefEntity) {
        labelDao.deleteFileRef(item)
    }
}
