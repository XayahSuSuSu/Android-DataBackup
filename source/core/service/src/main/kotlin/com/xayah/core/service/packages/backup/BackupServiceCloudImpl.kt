package com.xayah.core.service.packages.backup

import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
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
            isProcessing = true,
        )
    }

    private val tmpAppsDir by lazy { pathUtil.getCloudTmpAppsDir() }
    private val tmpConfigsDir by lazy { pathUtil.getCloudTmpConfigsDir() }
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

        log { "Trying to create: $tmpAppsDir." }
        log { "Trying to create: $tmpConfigsDir." }
        rootService.mkdirs(tmpAppsDir)
        rootService.mkdirs(tmpConfigsDir)

        log { "Trying to create: $remoteAppsDir." }
        log { "Trying to create: $remoteConfigsDir." }
        client.mkdirRecursively(remoteAppsDir)
        client.mkdirRecursively(remoteConfigsDir)
    }

    override suspend fun backupPackage(t: TaskDetailPackageEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val p = t.packageEntity
        val tmpDstDir = "${tmpAppsDir}/${p.archivesRelativeDir}"
        rootService.mkdirs(tmpDstDir)

        val remoteDstDir = "${remoteAppsDir}/${p.archivesRelativeDir}"
        runCatching { client.mkdirRecursively(remoteDstDir) }.onSuccess {
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
                rootService.writeJson(data = restoreEntity, dst = dst)
                cloudRepository.upload(client = client, src = dst, dstDir = remoteDstDir)

                packageDao.upsert(restoreEntity)
                packageDao.upsert(p)
                t.apply {
                    packageEntity = p
                    taskDao.upsert(this)
                }
                packageRepository.updateCloudPackageArchivesSize(restoreEntity.packageName, OpType.RESTORE, restoreEntity.userId, client, cloudEntity)
            }
        }.onFailure {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            it.printStackTrace(printWriter)
            val log = log { stringWriter.toString() }
            t.apkInfo.state = OperationState.ERROR
            t.userInfo.state = OperationState.ERROR
            t.userDeInfo.state = OperationState.ERROR
            t.dataInfo.state = OperationState.ERROR
            t.obbInfo.state = OperationState.ERROR
            t.mediaInfo.state = OperationState.ERROR
            t.apkInfo.log = log
            t.userInfo.log = log
            t.userDeInfo.log = log
            t.dataInfo.log = log
            t.obbInfo.log = log
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

        val backupItself = context.readBackupItself().first()
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

    override suspend fun backupIcons() {
        postSaveIconsEntity.also {
            it.state = OperationState.PROCESSING
            taskDao.upsert(it)
        }

        // Backup others.
        log { "Save icons." }
        packagesBackupUtil.backupIcons(dstDir = tmpConfigsDir).apply {
            postSaveIconsEntity.state = if (isSuccess) OperationState.DONE else OperationState.ERROR
            if (isSuccess.not()) {
                postSaveIconsEntity.log = outString
            } else {
                postSaveIconsEntity.also {
                    it.state = OperationState.UPLOADING
                    taskDao.upsert(it)
                }
                var flag = true
                var progress = 0f
                with(CoroutineScope(coroutineContext)) {
                    launch {
                        while (flag) {
                            postSaveIconsEntity.also {
                                it.content = "${(progress * 100).toInt()}%"
                                taskDao.upsert(it)
                            }
                            delay(500)
                        }
                    }
                }
                cloudRepository.upload(
                    client = client,
                    src = packagesBackupUtil.getIconsDst(tmpConfigsDir),
                    dstDir = remoteConfigsDir,
                    onUploading = { read, total -> progress = read.toFloat() / total }).apply {
                    postSaveIconsEntity.also {
                        it.state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                        if (isSuccess.not()) it.log = outString
                        it.content = "100%"
                    }
                }
                flag = false
            }
        }

        taskDao.upsert(postSaveIconsEntity)
    }

    override suspend fun clear() {
        rootService.deleteRecursively(tmpDir)
        client.disconnect()
    }
}
