package com.xayah.core.service.messages

import com.xayah.core.data.repository.MessagesRepository
import com.xayah.core.database.dao.MessageDao
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.MessageEntity
import com.xayah.core.model.database.TaskDetailMessageEntity
import com.xayah.core.model.util.set
import com.xayah.core.service.AbstractProcessingService

internal abstract class AbstractMessagesService : AbstractProcessingService() {
    protected val mMessageEntities: MutableList<TaskDetailMessageEntity> = mutableListOf()

    protected suspend fun TaskDetailMessageEntity.update(
        state: OperationState? = null,
        processingIndex: Int? = null,
        messageEntity: MessageEntity? = null,
    ) = run {
        set(state, processingIndex, messageEntity)
        mTaskDao.upsert(this)
    }

    protected abstract val mMessageDao: MessageDao
    protected abstract val mMessagesRepo: MessagesRepository
    protected abstract val mRootDir: String
    protected abstract val mMessagesDir: String
}