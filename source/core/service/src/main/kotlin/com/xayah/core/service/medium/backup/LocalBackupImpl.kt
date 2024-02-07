package com.xayah.core.service.medium.backup

import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LocalBackupImpl @Inject constructor() : BackupService() {
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
            rawBytes = 0.toDouble(),
            availableBytes = 0.toDouble(),
            totalBytes = 0.toDouble(),
            totalCount = 0,
            successCount = 0,
            failureCount = 0,
            isProcessing = true,
            cloud = "",
        )
    }

    private val localBackupSaveDir by lazy { localBackupSaveDir() }
    private val archivesMediumDir by lazy { pathUtil.getLocalBackupArchivesMediumDir() }
    private val configsDir by lazy { pathUtil.getLocalBackupConfigsDir() }

    override suspend fun createTargetDirs() {
        log { "Trying to create: $archivesMediumDir." }
        log { "Trying to create: $configsDir." }
        rootService.mkdirs(archivesMediumDir)
        rootService.mkdirs(configsDir)
    }

    override suspend fun backupMedia(m: MediaEntity) {
        val dstDir = "${archivesMediumDir}/${m.archivesPreserveRelativeDir}"
        rootService.mkdirs(dstDir)

        val t = TaskDetailMediaEntity(
            id = 0,
            taskId = taskEntity.id,
            mediaEntity = m,
        ).apply {
            id = taskDao.upsert(this)
        }
        var restoreEntity = mediaRepository.query(OpType.RESTORE, m.preserveId, m.name, m.indexInfo.compressionType, "", localBackupSaveDir)

        mediumBackupUtil.backupData(m = m, t = t, r = restoreEntity, dstDir = dstDir)

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = m.copy(
                id = id,
                indexInfo = m.indexInfo.copy(opType = OpType.RESTORE, cloud = "", backupDir = localBackupSaveDir),
                mediaInfo = m.mediaInfo.copy(dataState = restoreEntity?.mediaInfo?.dataState ?: m.mediaInfo.dataState),
                extraInfo = m.extraInfo.copy(activated = false)
            )
            rootService.writeProtoBuf(data = restoreEntity, dst = PathUtil.getMediaRestoreConfigDst(dstDir = dstDir))
            mediaDao.upsert(restoreEntity)
            mediaDao.upsert(m)
            t.apply {
                mediaEntity = m
                taskDao.upsert(this)
            }
            mediaRepository.updateLocalMediaArchivesSize(OpType.RESTORE, m.name)
        }

        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun clear() {}
}
