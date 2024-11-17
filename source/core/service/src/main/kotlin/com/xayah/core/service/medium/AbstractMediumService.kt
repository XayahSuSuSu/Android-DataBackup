package com.xayah.core.service.medium

import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.util.set
import com.xayah.core.service.AbstractProcessingService

internal abstract class AbstractMediumService : AbstractProcessingService() {
    protected val mMediaEntities: MutableList<TaskDetailMediaEntity> = mutableListOf()

    protected suspend fun TaskDetailMediaEntity.update(
        state: OperationState? = null,
        processingIndex: Int? = null,
        mediaEntity: MediaEntity? = null,
    ) = run {
        set(state, processingIndex, mediaEntity)
        mTaskDao.upsert(this)
    }

    protected suspend fun TaskDetailMediaEntity.update(
        bytes: Long? = null,
        log: String? = null,
        content: String? = null,
        progress: Float? = null,
        state: OperationState? = null
    ) = run {
        set(bytes, log, content, progress, state)
        mTaskDao.upsert(this)
    }

    protected abstract val mMediaDao: MediaDao
    protected abstract val mMediaRepo: MediaRepository
    protected abstract val mRootDir: String
    protected abstract val mFilesDir: String
    protected abstract val mConfigsDir: String
}
