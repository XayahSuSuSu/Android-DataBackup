package com.xayah.core.service.messages.backup

import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingInfoType
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailMessageEntity
import com.xayah.core.service.R
import com.xayah.core.service.messages.AbstractMessagesService
import com.xayah.core.util.NotificationUtil

internal abstract class AbstractBackupService : AbstractMessagesService() {
    override suspend fun onInitializingPreprocessingEntities(entities: MutableList<ProcessingInfoEntity>) {
        entities.apply {
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.necessary_preparations),
                type = ProcessingType.PREPROCESSING,
                infoType = ProcessingInfoType.NECESSARY_PREPARATIONS
            ).apply {
                id = mTaskDao.upsert(this)
            })
        }
    }

    override suspend fun onInitializingPostProcessingEntities(entities: MutableList<ProcessingInfoEntity>) {
    }

    override suspend fun onInitializing() {
        val messages = mMessagesRepo.queryActivated(OpType.BACKUP)

        messages.forEach { message ->
            mMessageEntities.add(
                TaskDetailMessageEntity(
                    taskId = mTaskEntity.id,
                    messageEntity = message,
                ).apply {
                    id = mTaskDao.upsert(this)
                })
        }
    }

    override suspend fun beforePreprocessing() {
        NotificationUtil.notify(mContext, mNotificationBuilder, mContext.getString(R.string.backing_up), mContext.getString(R.string.preprocessing))
    }

    override suspend fun onPreprocessing(entity: ProcessingInfoEntity) {
        when (entity.infoType) {
            ProcessingInfoType.NECESSARY_PREPARATIONS -> {
                log { "Trying to create: $mMessagesDir." }
                mRootService.mkdirs(mMessagesDir)
                val isSuccess = runCatchingOnService { onTargetDirsCreated() }
                entity.update(progress = 1f, state = if (isSuccess) OperationState.DONE else OperationState.ERROR)
            }

            else -> {}
        }
    }

    override suspend fun onProcessing() {
        mTaskEntity.update(rawBytes = mTaskRepo.getRawBytes(TaskType.MESSAGE), availableBytes = mTaskRepo.getAvailableBytes(OpType.BACKUP), totalBytes = mTaskRepo.getTotalBytes(OpType.BACKUP), totalCount = mMessageEntities.size)
        log { "Task count: ${mMessageEntities.size}." }

        mMessageEntities.forEachIndexed { index, message ->
            executeAtLeast {
                val p = message.messageEntity.messageBaseInfo.address

                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.backing_up),
                    p,
                    mMessageEntities.size,
                    index
                )
                log { "Current message: $p" }

                message.update(state = OperationState.PROCESSING)
                val dstDir = mMessagesDir


            }
            mTaskEntity.update(processingIndex = mTaskEntity.processingIndex + 1)
        }
    }

    override suspend fun onPostProcessing(entity: ProcessingInfoEntity) {

    }

    protected open suspend fun onTargetDirsCreated() {}
}