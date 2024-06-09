package com.xayah.core.service.packages.restore

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
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.PackagesRestoreUtil
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
    override lateinit var packageDao: PackageDao

    @Inject
    override lateinit var packagesRestoreUtil: PackagesRestoreUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var cloudRepository: CloudRepository

    @Inject
    override lateinit var packageRepository: PackageRepository

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

    private val tmpAppsDir by lazy { pathUtil.getCloudTmpAppsDir() }
    private val tmpDir by lazy { pathUtil.getCloudTmpDir() }

    private lateinit var cloudEntity: CloudEntity
    private lateinit var client: CloudClient
    private lateinit var remote: String
    private lateinit var remoteAppsDir: String
    private lateinit var remoteConfigsDir: String

    override suspend fun createTargetDirs() {
        val pair = cloudRepository.getClient()
        cloudEntity = pair.second
        client = pair.first
        remote = cloudEntity.remote
        remoteAppsDir = pathUtil.getCloudRemoteAppsDir(remote)
        remoteConfigsDir = pathUtil.getCloudRemoteConfigsDir(remote)
        taskEntity.also {
            it.cloud = cloudEntity.name
            it.backupDir = remote
            taskDao.upsert(it)
        }
    }

    override suspend fun restorePackage(t: TaskDetailPackageEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val p = t.packageEntity
        val tmpDstDir = "${tmpAppsDir}/${p.archivesRelativeDir}"
        val remoteSrcDir = "${remoteAppsDir}/${p.archivesRelativeDir}"
        val userId = if (restoreUser == -1) p.userId else restoreUser

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_APK, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, _, mPath ->
            packagesRestoreUtil.restoreApk(userId = userId, p = mP, t = mT, srcDir = mPath)
        }

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_USER, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, mDataType, mPath ->
            packagesRestoreUtil.restoreData(userId = userId, p = mP, t = mT, dataType = mDataType, srcDir = mPath)
        }

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_USER_DE, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, mDataType, mPath ->
            packagesRestoreUtil.restoreData(userId = userId, p = mP, t = mT, dataType = mDataType, srcDir = mPath)

        }

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_DATA, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, mDataType, mPath ->
            packagesRestoreUtil.restoreData(userId = userId, p = mP, t = mT, dataType = mDataType, srcDir = mPath)
        }

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_OBB, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, mDataType, mPath ->
            packagesRestoreUtil.restoreData(userId = userId, p = mP, t = mT, dataType = mDataType, srcDir = mPath)
        }

        packagesRestoreUtil.download(client = client, p = p, t = t, dataType = DataType.PACKAGE_MEDIA, srcDir = remoteSrcDir, dstDir = tmpDstDir) { mP, mT, mDataType, mPath ->
            packagesRestoreUtil.restoreData(userId = userId, p = mP, t = mT, dataType = mDataType, srcDir = mPath)
        }

        packagesRestoreUtil.restorePermissions(userId = userId, p = p)
        packagesRestoreUtil.restoreSsaid(userId = userId, p = p)

        t.apply {
            packageEntity = p
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
