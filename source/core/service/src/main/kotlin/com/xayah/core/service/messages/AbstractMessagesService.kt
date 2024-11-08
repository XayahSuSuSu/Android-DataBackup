package com.xayah.core.service.messages

import com.xayah.core.database.dao.MessageDao
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.MessageEntity
import com.xayah.core.model.util.set
import com.xayah.core.service.AbstractProcessingService

internal abstract class AbstractMessagesService : AbstractProcessingService() {
    protected val mMessageEntities: MutableList<MessageEntity> = mutableListOf()

    protected suspend fun MessageEntity.update(
        state: OperationState? = null,
        processingIndex: Int? = null,
        messageEntity: MessageEntity? = null,
    ) = run {
        set(state, processingIndex, messageEntity)
        mTaskDao.upsert(this)
    }

    protected abstract val mMessageDao: MessageDao
    protected abstract val mRootDir: String
    protected abstract val mMessagesDir: String
}