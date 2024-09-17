package com.xayah.core.data.repository

import android.content.Context
import com.xayah.core.database.dao.LabelDao
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.model.database.AppWithLabels
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelWithAppIds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LabelsRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val labelDao: LabelDao,
) {
    fun getLabelWithAppIds(): Flow<List<LabelWithAppIds>> = labelDao.queryLabelWithAppIdsFlow().flowOn(defaultDispatcher)

    fun getAppWithLabels(id: Long): Flow<AppWithLabels?> = labelDao.queryAppWithLabelsFlow(id)

    /**
     * Add a unique label
     */
    suspend fun addLabel(label: String) = labelDao.upsert(LabelEntity(id = 0, label = label))

    /**
     * Add a unique label app cross ref
     */
    suspend fun addLabelAppCrossRef(labelId: Long, appId: Long) = labelDao.upsert(LabelAppCrossRefEntity(labelId = labelId, appId = appId))

    suspend fun deleteLabel(id: Long) {
        labelDao.delete(id)
    }

    suspend fun deleteLabelAppCrossRef(labelId: Long, appId: Long) {
        labelDao.delete(labelId, appId)
    }
}
