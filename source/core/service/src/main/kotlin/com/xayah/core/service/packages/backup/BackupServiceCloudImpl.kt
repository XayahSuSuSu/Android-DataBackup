package com.xayah.core.service.packages.backup

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.get
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
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
            taskType = TaskType.PACKAGE,
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
        mRemoteAppsDir = mPathUtil.getCloudRemoteAppsDir(mRemotePath)
        mRemoteConfigsDir = mPathUtil.getCloudRemoteConfigsDir(mRemotePath)
        mTaskEntity.update(cloud = mCloudEntity.name, backupDir = mRemotePath)

        log { "Trying to create: $mRemoteAppsDir." }
        log { "Trying to create: $mRemoteConfigsDir." }
        mClient.mkdirRecursively(mRemoteAppsDir)
        mClient.mkdirRecursively(mRemoteConfigsDir)
    }

    private fun getRemoteAppDir(archivesRelativeDir: String) = "${mRemoteAppsDir}/${archivesRelativeDir}"

    override suspend fun onAppDirCreated(archivesRelativeDir: String): Boolean = runCatchingOnService {
        mClient.mkdirRecursively(getRemoteAppDir(archivesRelativeDir))
    }

    override suspend fun backup(type: DataType, p: PackageEntity, r: PackageEntity?, t: TaskDetailPackageEntity, dstDir: String) {
        val remoteAppDir = getRemoteAppDir(p.archivesRelativeDir)
        val result = if (type == DataType.PACKAGE_APK) {
            mPackagesBackupUtil.backupApk(p = p, t = t, r = r, dstDir = dstDir)
        } else {
            mPackagesBackupUtil.backupData(p = p, t = t, r = r, dataType = type, dstDir = dstDir)
        }
        if (result.isSuccess && t.get(type).state != OperationState.SKIP) {
            mPackagesBackupUtil.upload(client = mClient, p = p, t = t, dataType = type, srcDir = dstDir, dstDir = remoteAppDir)
        }
        t.update(dataType = type, progress = 1f)
        t.update(processingIndex = t.processingIndex + 1)
    }

    override suspend fun onConfigSaved(path: String, archivesRelativeDir: String) {
        mCloudRepo.upload(client = mClient, src = path, dstDir = getRemoteAppDir(archivesRelativeDir))
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

    override suspend fun onIconsSaved(path: String, entity: ProcessingInfoEntity) {
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
        mCloudRepo.upload(client = mClient, src = path, dstDir = mRemoteConfigsDir, onUploading = { read, total -> progress = read.toFloat() / total }).apply {
            entity.update(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = if (isSuccess) null else outString, content = "100%")
        }
        flag = false
    }

    override suspend fun clear() {
        mRootService.deleteRecursively(mRootDir)
        mClient.disconnect()
    }

    @Inject
    override lateinit var mPackageDao: PackageDao

    @Inject
    override lateinit var mPackageRepo: PackageRepository

    @Inject
    override lateinit var mPackagesBackupUtil: PackagesBackupUtil

    override val mRootDir by lazy { mPathUtil.getCloudTmpDir() }
    override val mAppsDir by lazy { mPathUtil.getCloudTmpAppsDir() }
    override val mConfigsDir by lazy { mPathUtil.getCloudTmpConfigsDir() }

    @Inject
    lateinit var mCloudRepo: CloudRepository

    private lateinit var mCloudEntity: CloudEntity
    private lateinit var mClient: CloudClient
    private lateinit var mRemotePath: String
    private lateinit var mRemoteAppsDir: String
    private lateinit var mRemoteConfigsDir: String
}
