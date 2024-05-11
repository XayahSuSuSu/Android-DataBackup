package com.xayah.core.service.packages.backup.impl

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.packages.backup.BackupService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
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
    override lateinit var packageDao: PackageDao

    @Inject
    override lateinit var packagesBackupUtil: PackagesBackupUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    override lateinit var commonBackupUtil: CommonBackupUtil

    @Inject
    override lateinit var packageRepository: PackageRepository

    @Inject
    lateinit var cloudRepository: CloudRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.BACKUP,
            taskType = TaskType.PACKAGE,
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

    private val tmpArchivesPackagesDir by lazy { pathUtil.getCloudTmpAppsDir() }
    private val tmpConfigsDir by lazy { pathUtil.getCloudTmpConfigsDir() }
    private val tmpDir by lazy { pathUtil.getCloudTmpDir() }

    private lateinit var cloudEntity: CloudEntity
    private lateinit var client: CloudClient
    private lateinit var remote: String
    private lateinit var remoteArchivesPackagesDir: String
    private lateinit var remoteConfigsDir: String

    override suspend fun createTargetDirs() {
        val pair = cloudRepository.getClient()
        cloudEntity = pair.second
        client = pair.first
        remote = cloudEntity.remote
        remoteArchivesPackagesDir = pathUtil.getCloudRemoteAppsDir(remote)
        remoteConfigsDir = pathUtil.getCloudRemoteConfigsDir(remote)
        taskEntity.also {
            it.cloud = cloudEntity.name
            it.backupDir = remote
        }

        log { "Trying to create: $tmpArchivesPackagesDir." }
        log { "Trying to create: $tmpConfigsDir." }
        rootService.mkdirs(tmpArchivesPackagesDir)
        rootService.mkdirs(tmpConfigsDir)

        log { "Trying to create: $remoteArchivesPackagesDir." }
        log { "Trying to create: $remoteConfigsDir." }
        client.mkdirRecursively(remoteArchivesPackagesDir)
        client.mkdirRecursively(remoteConfigsDir)
    }

    override suspend fun backupPackage(t: TaskDetailPackageEntity) {
        val p = t.packageEntity
        val tmpDstDir = "${tmpArchivesPackagesDir}/${p.archivesRelativeDir}"
        rootService.mkdirs(tmpDstDir)

        val remoteDstDir = "${remoteArchivesPackagesDir}/${p.archivesRelativeDir}"
        client.mkdirRecursively(remoteDstDir)

        var restoreEntity = packageRepository.getPackage(p.packageName, OpType.RESTORE, p.userId, p.preserveId, p.indexInfo.compressionType, cloudEntity.name, remote)

        packagesBackupUtil.backupApk(p = p, t = t, r = restoreEntity, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_APK, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_USER, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER_DE, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_USER_DE, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_DATA, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_DATA, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_OBB, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_OBB, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_MEDIA, dstDir = tmpDstDir).apply {
            if (isSuccess)
                packagesBackupUtil.upload(client = client, p = p, t = t, dataType = DataType.PACKAGE_MEDIA, srcDir = tmpDstDir, dstDir = remoteDstDir)
        }

        packagesBackupUtil.backupPermissions(p = p)
        packagesBackupUtil.backupSsaid(p = p)

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = p.copy(
                id = id,
                indexInfo = p.indexInfo.copy(opType = OpType.RESTORE, cloud = cloudEntity.name, backupDir = remote),
                dataStates = restoreEntity?.dataStates?.copy() ?: p.dataStates.copy(),
                extraInfo = p.extraInfo.copy(existed = true, activated = false)
            )
            val dst = PathUtil.getPackageRestoreConfigDst(dstDir = tmpDstDir)
            rootService.writeProtoBuf(data = restoreEntity, dst = dst)
            cloudRepository.upload(client = client, src = dst, dstDir = remoteDstDir)

            packageDao.upsert(restoreEntity)
            packageDao.upsert(p)
            t.apply {
                packageEntity = p
                taskDao.upsert(this)
            }
            packageRepository.updateCloudPackageArchivesSize(restoreEntity.packageName, OpType.RESTORE, restoreEntity.userId, client, cloudEntity)
        }

        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun backupItself() {
        // Backup itself if enabled.
        if (context.readBackupItself().first()) {
            log { "Backup itself enabled." }
            commonBackupUtil.backupItself(dstDir = tmpDir)
            cloudRepository.upload(client = client, src = commonBackupUtil.getItselfDst(tmpDir), dstDir = remote)
        }
    }

    override suspend fun backupIcons() {
        // Backup others.
        log { "Save icons." }
        packagesBackupUtil.backupIcons(dstDir = tmpConfigsDir)
        cloudRepository.upload(client = client, src = packagesBackupUtil.getIconsDst(tmpConfigsDir), dstDir = remoteConfigsDir)
    }

    override suspend fun clear() {
        rootService.deleteRecursively(tmpDir)
        client.disconnect()
    }
}
