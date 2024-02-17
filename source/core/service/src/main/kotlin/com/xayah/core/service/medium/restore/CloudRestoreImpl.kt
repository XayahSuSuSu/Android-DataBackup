package com.xayah.core.service.medium.restore

import com.xayah.core.data.repository.CloudRepository
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
import com.xayah.core.service.util.MediumRestoreUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class CloudRestoreImpl @Inject constructor() : RestoreService() {
    @Inject
    override lateinit var pathUtil: PathUtil

    @Inject
    override lateinit var taskDao: TaskDao

    @Inject
    override lateinit var mediaDao: MediaDao

    @Inject
    override lateinit var mediumRestoreUtil: MediumRestoreUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    override lateinit var rootService: RemoteRootService

    @Inject
    lateinit var cloudRepository: CloudRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.RESTORE,
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

    private val tmpArchivesMediumDir by lazy { pathUtil.getCloudTmpArchivesMediumDir() }
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
    }

    override suspend fun restoreMedia(m: MediaEntity) {
        val tmpDstDir = "${tmpArchivesMediumDir}/${m.archivesPreserveRelativeDir}"
        val remoteSrcDir = "${remoteArchivesMediumDir}/${m.archivesPreserveRelativeDir}"

        val t = TaskDetailMediaEntity(
            id = 0,
            taskId = taskEntity.id,
            mediaEntity = m,
        ).apply {
            id = taskDao.upsert(this)
        }

        mediumRestoreUtil.download(client = client, m = m, t = t, srcDir = remoteSrcDir, dstDir = tmpDstDir) {
            mediumRestoreUtil.restoreData(m = m, t = t, srcDir = tmpDstDir)
        }

        t.apply {
            mediaEntity = m
            taskDao.upsert(this)
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
