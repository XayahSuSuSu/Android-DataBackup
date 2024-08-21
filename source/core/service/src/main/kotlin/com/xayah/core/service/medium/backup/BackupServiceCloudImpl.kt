package com.xayah.core.service.medium.backup

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
internal class BackupServiceCloudImpl @Inject constructor() : AbstractBackupService() {
    override val mTAG: String = "BackupServiceCloudImpl"

    @Inject
    override lateinit var mRootService: RemoteRootService

    @Inject
    override lateinit var mPathUtil: PathUtil

    @Inject
    override lateinit var mCommonBackupUtil: CommonBackupUtil

    @Inject
    override lateinit var mTaskDao: TaskDao

    @Inject
    override lateinit var mTaskRepo: TaskRepository

    override val mTaskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.BACKUP,
            taskType = TaskType.MEDIA,
            startTimestamp = mStartTimestamp,
            endTimestamp = mEndTimestamp,
            backupDir = mRootDir,
            isProcessing = true,
        )
    }

    override suspend fun onTargetDirsCreated() {
        mCloudRepo.getClient().also { (c, e) ->
            mCloudEntity = e
            mClient = c
        }

        mRemotePath = mCloudEntity.remote
        mRemoteFilesDir = mPathUtil.getCloudRemoteFilesDir(mRemotePath)
        mTaskEntity.update(cloud = mCloudEntity.name, backupDir = mRemotePath)

        log { "Trying to create: $mRemoteFilesDir." }
        mClient.mkdirRecursively(mRemoteFilesDir)
    }

    private fun getRemoteFileDir(archivesRelativeDir: String) = "${mRemoteFilesDir}/${archivesRelativeDir}"

    override suspend fun onFileDirCreated(archivesRelativeDir: String): Boolean = runCatchingOnService {
        mClient.mkdirRecursively(getRemoteFileDir(archivesRelativeDir))
    }

    override suspend fun backup(m: MediaEntity, r: MediaEntity?, t: TaskDetailMediaEntity, dstDir: String) {
        val remoteFileDir = getRemoteFileDir(m.archivesRelativeDir)

        val result = mMediumBackupUtil.backupMedia(m = m, t = t, r = r, dstDir = dstDir)
        if (result.isSuccess && t.mediaInfo.state != OperationState.SKIP) {
            mMediumBackupUtil.upload(client = mClient, m = m, t = t, srcDir = dstDir, dstDir = remoteFileDir)
        }
        t.update(progress = 1f)
        t.update(processingIndex = t.processingIndex + 1)
    }

    override suspend fun onConfigSaved(path: String, archivesRelativeDir: String) {
        mCloudRepo.upload(client = mClient, src = path, dstDir = getRemoteFileDir(archivesRelativeDir))
    }

    override suspend fun onItselfSaved(path: String, entity: ProcessingInfoEntity) {
        entity.update(state = OperationState.UPLOADING)
        var flag = true
        var progress = 0f
        with(CoroutineScope(coroutineContext)) {
            launch {
                while (flag) {
                    entity.update(content = "${(progress * 100).toInt()}%")
                    delay(500)
                }
            }
        }
        mCloudRepo.upload(client = mClient, src = path, dstDir = mRemotePath, onUploading = { read, total -> progress = read.toFloat() / total }).apply {
            entity.update(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = if (isSuccess) null else outString, content = "100%")
        }
        flag = false
    }

    override suspend fun clear() {
        mRootService.deleteRecursively(mRootDir)
        mClient.disconnect()
    }

    @Inject
    override lateinit var mMediaDao: MediaDao

    @Inject
    override lateinit var mMediaRepo: MediaRepository

    @Inject
    override lateinit var mMediumBackupUtil: MediumBackupUtil

    override val mRootDir by lazy { mPathUtil.getCloudTmpDir() }
    override val mFilesDir by lazy { mPathUtil.getCloudTmpFilesDir() }

    @Inject
    lateinit var mCloudRepo: CloudRepository

    private lateinit var mCloudEntity: CloudEntity
    private lateinit var mClient: CloudClient
    private lateinit var mRemotePath: String
    private lateinit var mRemoteFilesDir: String
}
