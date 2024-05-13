package com.xayah.core.service.packages.restore.impl

import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.packages.restore.RestoreService
import com.xayah.core.service.util.PackagesRestoreUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LocalRestoreImpl @Inject constructor() : RestoreService() {
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
    override lateinit var packageRepository: PackageRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.RESTORE,
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

    override suspend fun createTargetDirs() {}

    private val archivesPackagesDir by lazy { pathUtil.getLocalBackupAppsDir() }

    override suspend fun restorePackage(t: TaskDetailPackageEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val p = t.packageEntity
        val srcDir = "${archivesPackagesDir}/${p.archivesRelativeDir}"

        packagesRestoreUtil.restoreApk(p = p, t = t, srcDir = srcDir)
        packagesRestoreUtil.restoreData(p = p, t = t, dataType = DataType.PACKAGE_USER, srcDir = srcDir)
        packagesRestoreUtil.restoreData(p = p, t = t, dataType = DataType.PACKAGE_USER_DE, srcDir = srcDir)
        packagesRestoreUtil.restoreData(p = p, t = t, dataType = DataType.PACKAGE_DATA, srcDir = srcDir)
        packagesRestoreUtil.restoreData(p = p, t = t, dataType = DataType.PACKAGE_OBB, srcDir = srcDir)
        packagesRestoreUtil.restoreData(p = p, t = t, dataType = DataType.PACKAGE_MEDIA, srcDir = srcDir)
        packagesRestoreUtil.updatePackage(p = p)
        packagesRestoreUtil.restorePermissions(p = p)
        packagesRestoreUtil.restoreSsaid(p = p)

        t.apply {
            t.apply {
                state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                taskDao.upsert(this)
            }
            packageEntity = p
            taskDao.upsert(this)
        }
        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun clear() {}
}
