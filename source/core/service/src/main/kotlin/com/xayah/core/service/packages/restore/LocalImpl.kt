package com.xayah.core.service.packages.restore

import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.service.util.PackagesRestoreUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class LocalImpl @Inject constructor() : AbstractService() {
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

    private val archivesPackagesDir by lazy { pathUtil.getLocalBackupArchivesPackagesDir() }

    override suspend fun restorePackage(p: PackageEntity) {
        val srcDir = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"

        val t = TaskDetailPackageEntity(
            id = 0,
            taskId = taskEntity.id,
            packageEntity = p,
        ).apply {
            id = taskDao.upsert(this)
        }

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
            packageEntity = p
            taskDao.upsert(this)
        }
        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }
}
