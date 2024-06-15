package com.xayah.core.service.medium.backup

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
internal class BackupServiceCloudImpl @Inject constructor() : BackupService() {
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

        log { "Trying to create: $tmpFilesDir." }
        rootService.mkdirs(tmpFilesDir)

        log { "Trying to create: $remoteFilesDir." }
        client.mkdirRecursively(remoteFilesDir)
    }

    override suspend fun backupMedia(t: TaskDetailMediaEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val m = t.mediaEntity
        val tmpDstDir = "${tmpFilesDir}/${m.archivesRelativeDir}"
        rootService.mkdirs(tmpDstDir)

        val remoteDstDir = "${remoteFilesDir}/${m.archivesRelativeDir}"
        runCatching { client.mkdirRecursively(remoteDstDir) }.onSuccess {
            var restoreEntity = mediaDao.query(OpType.RESTORE, m.preserveId, m.name, m.indexInfo.compressionType, cloudEntity.name, remote)

            mediumBackupUtil.backupMedia(m = m, t = t, r = restoreEntity, dstDir = tmpDstDir).apply {
                if (isSuccess)
                    mediumBackupUtil.upload(client = client, m = m, t = t, srcDir = tmpDstDir, dstDir = remoteDstDir)
            }


            if (t.isSuccess) {
                // Save config
                val id = restoreEntity?.id ?: 0
                restoreEntity = m.copy(
                    id = id,
                    indexInfo = m.indexInfo.copy(opType = OpType.RESTORE, cloud = cloudEntity.name, backupDir = remote),
                    extraInfo = m.extraInfo.copy(existed = true, activated = restoreEntity?.extraInfo?.activated ?: false)
                )
                val dst = PathUtil.getPackageRestoreConfigDst(dstDir = tmpDstDir)
                rootService.writeJson(data = restoreEntity, dst = dst)
                cloudRepository.upload(client = client, src = dst, dstDir = remoteDstDir)

                mediaDao.upsert(restoreEntity)
                mediaDao.upsert(m)
                t.apply {
                    mediaEntity = m
                    taskDao.upsert(this)
                }
            }
        }.onFailure {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            it.printStackTrace(printWriter)
            val log = log { stringWriter.toString() }
            t.mediaInfo.state = OperationState.ERROR
            t.mediaInfo.log = log
            taskDao.upsert(t)
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

        postBackupItselfEntity.state = OperationState.SKIP

        // Backup itself if enabled.
        if (context.readBackupItself().first()) {
            log { "Backup itself enabled." }
            commonBackupUtil.backupItself(dstDir = tmpDir).apply {
                postBackupItselfEntity.state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                if (isSuccess.not()) {
                    postBackupItselfEntity.log = outString
                } else {
                    postBackupItselfEntity.also {
                        it.state = OperationState.UPLOADING
                        taskDao.upsert(it)
                    }
                    var flag = true
                    var progress = 0f
                    with(CoroutineScope(coroutineContext)) {
                        launch {
                            while (flag) {
                                postBackupItselfEntity.also {
                                    it.content = "${(progress * 100).toInt()}%"
                                    taskDao.upsert(it)
                                }
                                delay(500)
                            }
                        }
                    }
                    cloudRepository.upload(client = client, src = commonBackupUtil.getItselfDst(tmpDir), dstDir = remote, onUploading = { read, total -> progress = read.toFloat() / total }).apply {
                        postBackupItselfEntity.also {
                            it.state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                            if (isSuccess.not()) it.log = outString
                            it.content = "100%"
                        }
                    }
                    flag = false
                }
            }
        }

        taskDao.upsert(postBackupItselfEntity)
    }

    override suspend fun clear() {
        rootService.deleteRecursively(tmpDir)
        client.disconnect()
    }
}
