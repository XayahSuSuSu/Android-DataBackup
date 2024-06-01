package com.xayah.core.service.packages.backup

import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
internal class BackupServiceLocalImpl @Inject constructor() : BackupService() {
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

    private val localBackupSaveDir by lazy { context.localBackupSaveDir() }
    private val appsDir by lazy { pathUtil.getLocalBackupAppsDir() }
    private val configsDir by lazy { pathUtil.getLocalBackupConfigsDir() }

    override suspend fun createTargetDirs() {
        log { "Trying to create: $appsDir." }
        log { "Trying to create: $configsDir." }
        rootService.mkdirs(appsDir)
        rootService.mkdirs(configsDir)
    }

    override suspend fun backupPackage(t: TaskDetailPackageEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val p = t.packageEntity
        val dstDir = "${appsDir}/${p.archivesRelativeDir}"
        rootService.mkdirs(dstDir)

        var restoreEntity = packageDao.query(p.packageName, OpType.RESTORE, p.userId, p.preserveId, p.indexInfo.compressionType, "", localBackupSaveDir)

        packagesBackupUtil.backupApk(p = p, t = t, r = restoreEntity, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER_DE, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_DATA, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_OBB, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_MEDIA, dstDir = dstDir)
        packagesBackupUtil.backupPermissions(p = p)
        packagesBackupUtil.backupSsaid(p = p)

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = p.copy(
                id = id,
                indexInfo = p.indexInfo.copy(opType = OpType.RESTORE, cloud = "", backupDir = localBackupSaveDir),
                dataStates = restoreEntity?.dataStates?.copy() ?: p.dataStates.copy(),
                displayStats = restoreEntity?.dataStats?.copy() ?: p.dataStats.copy(),
                extraInfo = p.extraInfo.copy(existed = true, activated = false)
            )
            rootService.writeJson(data = restoreEntity, dst = PathUtil.getPackageRestoreConfigDst(dstDir = dstDir))
            packageDao.upsert(restoreEntity)
            packageDao.upsert(p)
            t.apply {
                packageEntity = p
                taskDao.upsert(this)
            }
            packageRepository.updateLocalPackageArchivesSize(restoreEntity.packageName, OpType.RESTORE, restoreEntity.userId)
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

        val dstDir = context.localBackupSaveDir()
        val backupItself = context.readBackupItself().first()
        // Backup itself if enabled.
        if (backupItself) {
            log { "Backup itself enabled." }
            commonBackupUtil.backupItself(dstDir = dstDir)
        }

        postBackupItselfEntity.also {
            it.state = if (backupItself) OperationState.DONE else OperationState.SKIP
            taskDao.upsert(it)
        }
    }

    override suspend fun backupIcons() {
        postSaveIconsEntity.also {
            it.state = OperationState.PROCESSING
            taskDao.upsert(it)
        }

        // Backup others.
        log { "Save icons." }
        packagesBackupUtil.backupIcons(dstDir = configsDir)

        postSaveIconsEntity.also {
            it.state = OperationState.DONE
            taskDao.upsert(it)
        }
    }

    override suspend fun clear() {}
}
