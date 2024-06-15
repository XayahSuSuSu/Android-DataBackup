package com.xayah.core.service.medium.backup

import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
internal class BackupServiceLocalImpl @Inject constructor() : BackupService() {
    @Inject
    override lateinit var rootService: RemoteRootService

    @Inject
    override lateinit var pathUtil: PathUtil

    @Inject
    override lateinit var taskDao: TaskDao

    @Inject
    override lateinit var mediaDao: MediaDao

    @Inject
    override lateinit var mediumBackupUtil: MediumBackupUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    override lateinit var commonBackupUtil: CommonBackupUtil

    @Inject
    override lateinit var mediaRepository: MediaRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.BACKUP,
            taskType = TaskType.MEDIA,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            backupDir = context.localBackupSaveDir(),
            isProcessing = true,
        )
    }

    private val localBackupSaveDir by lazy { context.localBackupSaveDir() }
    private val filesDir by lazy { pathUtil.getLocalBackupFilesDir() }

    override suspend fun createTargetDirs() {
        log { "Trying to create: $filesDir." }
        rootService.mkdirs(filesDir)
    }

    override suspend fun backupMedia(t: TaskDetailMediaEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val m = t.mediaEntity
        val dstDir = "${filesDir}/${m.archivesRelativeDir}"
        rootService.mkdirs(dstDir)

        var restoreEntity = mediaDao.query(OpType.RESTORE, m.preserveId, m.name, m.indexInfo.compressionType, "", localBackupSaveDir)

        mediumBackupUtil.backupMedia(m = m, t = t, r = restoreEntity, dstDir = dstDir)

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = m.copy(
                id = id,
                indexInfo = m.indexInfo.copy(opType = OpType.RESTORE, cloud = "", backupDir = localBackupSaveDir),
                extraInfo = m.extraInfo.copy(existed = true, activated = restoreEntity?.extraInfo?.activated ?: false)
            )
            rootService.writeJson(data = restoreEntity, dst = PathUtil.getMediaRestoreConfigDst(dstDir = dstDir))
            mediaDao.upsert(restoreEntity)
            mediaDao.upsert(m)
            t.apply {
                mediaEntity = m
                taskDao.upsert(this)
            }
        }

        taskEntity.also {
            t.apply {
                state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                taskDao.upsert(this)
            }
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun backupItself() {
        postBackupItselfEntity.also {
            it.state = OperationState.PROCESSING
            taskDao.upsert(it)
        }

        val dstDir = context.localBackupSaveDir()
        val backupItself = context.readBackupItself().first()

        postBackupItselfEntity.state = OperationState.SKIP
        // Backup itself if enabled.
        if (backupItself) {
            log { "Backup itself enabled." }
            commonBackupUtil.backupItself(dstDir = dstDir).apply {
                postBackupItselfEntity.state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                if (isSuccess.not()) postBackupItselfEntity.log = outString
            }
        }
        taskDao.upsert(postBackupItselfEntity)
    }

    override suspend fun clear() {}
}
