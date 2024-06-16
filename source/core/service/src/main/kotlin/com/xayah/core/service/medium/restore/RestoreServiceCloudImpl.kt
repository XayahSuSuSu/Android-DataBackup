package com.xayah.core.service.medium.restore

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
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
internal class RestoreServiceCloudImpl @Inject constructor() : RestoreService() {
    @Inject
    override lateinit var rootService: RemoteRootService

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
    lateinit var cloudRepository: CloudRepository

    @Inject
    override lateinit var mediaRepository: MediaRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.RESTORE,
            taskType = TaskType.PACKAGE,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            backupDir = context.localBackupSaveDir(),
            isProcessing = true,
        )
    }

    private val tmpFilesDir by lazy { pathUtil.getCloudTmpFilesDir() }
    private val tmpDir by lazy { pathUtil.getCloudTmpDir() }

    private lateinit var cloudEntity: CloudEntity
    private lateinit var client: CloudClient
    private lateinit var remote: String
    private lateinit var remoteFilesDir: String

    override suspend fun createTargetDirs() {
        val pair = cloudRepository.getClient()
        cloudEntity = pair.second
        client = pair.first
        remote = cloudEntity.remote
        remoteFilesDir = pathUtil.getCloudRemoteFilesDir(remote)
        taskEntity.also {
            it.cloud = cloudEntity.name
            it.backupDir = remote
            taskDao.upsert(it)
        }
    }

    override suspend fun restoreMedia(t: TaskDetailMediaEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val m = t.mediaEntity
        val tmpDstDir = "${tmpFilesDir}/${m.archivesRelativeDir}"
        val remoteSrcDir = "${remoteFilesDir}/${m.archivesRelativeDir}"

        mediumRestoreUtil.download(client = client, m = m, t = t, dataType = DataType.PACKAGE_MEDIA, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mM, mT, mPath ->
            mediumRestoreUtil.restoreMedia(m = mM, t = mT, srcDir = mPath)
        }

        t.apply {
            mediaEntity = m
            taskDao.upsert(this)
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

    override suspend fun clear() {
        rootService.deleteRecursively(tmpDir)
        client.disconnect()
    }
}
