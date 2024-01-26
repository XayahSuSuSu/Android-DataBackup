package com.xayah.core.service.packages.backup

import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LocalImpl @Inject constructor() : AbstractService() {
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

    private val archivesPackagesDir by lazy { pathUtil.getLocalBackupArchivesPackagesDir() }
    private val configsDir by lazy { pathUtil.getLocalBackupConfigsDir() }

    override suspend fun createTargetDirs() {
        log { "Trying to create: $archivesPackagesDir." }
        log { "Trying to create: $configsDir." }
        rootService.mkdirs(archivesPackagesDir)
        rootService.mkdirs(configsDir)
    }

    override suspend fun backupPackage(p: PackageEntity) {
        val dstDir = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"
        rootService.mkdirs(dstDir)

        val t = TaskDetailPackageEntity(
            id = 0,
            taskId = taskEntity.id,
            packageEntity = p,
        ).apply {
            id = taskDao.upsert(this)
        }
        var restoreEntity = packageDao.query(p.packageName, OpType.RESTORE, p.userId, p.preserveId, p.indexInfo.compressionType)

        packagesBackupUtil.backupApk(p = p, t = t, r = restoreEntity, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_USER_DE, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_DATA, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_OBB, dstDir = dstDir)
        packagesBackupUtil.backupData(p = p, t = t, r = restoreEntity, dataType = DataType.PACKAGE_MEDIA, dstDir = dstDir)
        packagesBackupUtil.backupPermissions(p = p)

        if (t.isSuccess) {
            // Save config
            val id = restoreEntity?.id ?: 0
            restoreEntity = p.copy(
                id = id,
                indexInfo = p.indexInfo.copy(opType = OpType.RESTORE),
                dataStates = restoreEntity?.dataStates?.copy() ?: p.dataStates.copy()
            )
            rootService.writeProtoBuf(data = restoreEntity, dst = PathUtil.getPackageRestoreConfigDst(dstDir = dstDir))
            packageDao.upsert(restoreEntity)
            packageDao.upsert(p)
            t.apply {
                packageEntity = p
                taskDao.upsert(this)
            }
            packageRepository.updatePackageArchivesSize(restoreEntity.packageName, OpType.RESTORE, restoreEntity.userId)
        }

        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }
}
