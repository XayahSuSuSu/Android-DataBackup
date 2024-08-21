package com.xayah.core.service.packages

import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.model.DataType
import com.xayah.core.model.KillAppOption
import com.xayah.core.model.OperationState
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.util.set
import com.xayah.core.service.AbstractProcessingService
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.command.BaseUtil

internal abstract class AbstractPackagesService : AbstractProcessingService() {
    protected val mPkgEntities: MutableList<TaskDetailPackageEntity> = mutableListOf()

    protected suspend fun TaskDetailPackageEntity.update(
        state: OperationState? = null,
        processingIndex: Int? = null,
        packageEntity: PackageEntity? = null,
    ) = run {
        set(state, processingIndex, packageEntity)
        mTaskDao.upsert(this)
    }

    protected suspend fun TaskDetailPackageEntity.update(
        dataType: DataType,
        bytes: Long? = null,
        log: String? = null,
        content: String? = null,
        progress: Float? = null,
        state: OperationState? = null
    ) = run {
        set(dataType, bytes, log, content, progress, state)
        mTaskDao.upsert(this)
    }

    protected suspend fun killApp(killAppOption: KillAppOption, pkg: TaskDetailPackageEntity) = run {
        // Kill the package.
        when (killAppOption) {
            KillAppOption.DISABLED -> {
                log { "Won't kill ${pkg.packageEntity.packageName}." }
            }

            KillAppOption.OPTION_I -> {
                log { "Trying to kill ${pkg.packageEntity.packageName}." }
                BaseUtil.killPackage(context = mContext, userId = pkg.packageEntity.userId, packageName = pkg.packageEntity.packageName)
            }

            KillAppOption.OPTION_II -> {
                log { "Trying to kill ${pkg.packageEntity.packageName}." }
                mRootService.forceStopPackageAsUser(pkg.packageEntity.packageName, pkg.packageEntity.userId)
            }
        }
    }

    protected abstract val mPackageDao: PackageDao
    protected abstract val mPackageRepo: PackageRepository
    protected abstract val mRootDir: String
    protected abstract val mAppsDir: String
    protected abstract val mConfigsDir: String
}
