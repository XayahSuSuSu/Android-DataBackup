package com.xayah.core.service.medium.backup

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class CloudBackupImpl @Inject constructor() : BackupService() {
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

    @Inject
    lateinit var cloudRepository: CloudRepository

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
            processingIndex = 0,
            isProcessing = true,
            cloud = "",
        )
    }

    private val tmpArchivesMediumDir by lazy { pathUtil.getCloudTmpArchivesMediumDir() }
    private val tmpConfigsDir by lazy { pathUtil.getCloudTmpConfigsDir() }
    private val tmpDir by lazy { pathUtil.getCloudTmpDir() }

    private lateinit var cloudEntity: CloudEntity
    private lateinit var client: CloudClient
    private lateinit var remote: String
    private lateinit var remoteArchivesMediumDir: String
    private lateinit var remoteConfigsDir: String

    override suspend fun createTargetDirs() {
        val pair = cloudRepository.getClient()
        cloudEntity = pair.second
        client = pair.first
        remote = cloudEntity.remote
        remoteArchivesMediumDir = pathUtil.getCloudRemoteArchivesMediumDir(remote)
        remoteConfigsDir = pathUtil.getCloudRemoteConfigsDir(remote)
        taskEntity.also {
            it.cloud = cloudEntity.name
            it.backupDir = remote
        }

        log { "Trying to create: $tmpArchivesMediumDir." }
        log { "Trying to create: $tmpConfigsDir." }
        rootService.mkdirs(tmpArchivesMediumDir)
        rootService.mkdirs(tmpConfigsDir)

        log { "Trying to create: $remoteArchivesMediumDir." }
        log { "Trying to create: $remoteConfigsDir." }
        client.mkdirRecursively(remoteArchivesMediumDir)
        client.mkdirRecursively(remoteConfigsDir)
    }

    override suspend fun backupMedia(m: MediaEntity) {
        val tmpDstDir = "${tmpArchivesMediumDir}/${m.archivesPreserveRelativeDir}"
        rootService.mkdirs(tmpDstDir)

        val remoteDstDir = "${remoteArchivesMediumDir}/${m.archivesPreserveRelativeDir}"
        client.mkdirRecursively(remoteDstDir)

        val t = TaskDetailMediaEntity(
            id = 0,
            taskId = taskEntity.id,
            mediaEntity = m,
        ).apply {
            id = taskDao.upsert(this)
        }
        var restoreEntity = mediaRepository.query(OpType.RESTORE, m.preserveId, m.name, m.indexInfo.compressionType, cloudEntity.name, remote)
        mediumBackupUtil.backupData(m = m, t = t, r = restoreEntity, dstDir = tmpDstDir).apply {
            mediumBackupUtil.upload(client = client, m = m, t = t, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = m.copy(
                id = id,
                indexInfo = m.indexInfo.copy(opType = OpType.RESTORE, cloud = cloudEntity.name, backupDir = remote),
                mediaInfo = m.mediaInfo.copy(dataState = restoreEntity?.mediaInfo?.dataState ?: m.mediaInfo.dataState),
                extraInfo = m.extraInfo.copy(activated = false)
            )
            val dst = PathUtil.getMediaRestoreConfigDst(dstDir = tmpDstDir)
            rootService.writeProtoBuf(data = restoreEntity, dst = dst)
            cloudRepository.upload(client = client, src = dst, dstDir = remoteDstDir)

            mediaDao.upsert(restoreEntity)
            mediaDao.upsert(m)
            t.apply {
                mediaEntity = m
                taskDao.upsert(this)
            }
            mediaRepository.updateCloudMediaArchivesSize(OpType.RESTORE, restoreEntity.name, client, cloudEntity)
        }

        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }


    override suspend fun clear() {
        rootService.deleteRecursively(tmpDir)
        client.disconnect()
    }
}
