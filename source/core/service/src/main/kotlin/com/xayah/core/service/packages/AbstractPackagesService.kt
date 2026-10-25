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
import com.xayah.core.util.SuspendUtil
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
        val packageName = pkg.packageEntity.packageName
        val userId = pkg.packageEntity.userId
        // Kill the package.
        when (killAppOption) {
            KillAppOption.DISABLED -> {
                log { "Won't kill $packageName." }
            }

            KillAppOption.OPTION_I -> {
                log { "Trying to kill $packageName." }
                BaseUtil.killPackage(context = mContext, userId = userId, packageName = packageName)
            }

            KillAppOption.OPTION_II -> {
                log { "Trying to kill $packageName." }
                mRootService.forceStopPackageAsUser(packageName, userId)
            }

            KillAppOption.OPTION_III -> {
                if (pkg.packageEntity.isSystemApp) {
                    // Suspending a system package can take the whole system down, so only stop it.
                    log { "System package $packageName is force-stopped instead of being paused." }
                    mRootService.forceStopPackageAsUser(packageName, userId)
                } else {
                    log { "Trying to pause $packageName." }
                    val isSuccess = SuspendUtil.suspendPackage(context = mContext, userId = userId, packageName = packageName)
                    log { if (isSuccess) "Paused $packageName." else "Failed to pause $packageName." }
                }
            }
        }
    }

    /**
     * Lifts the suspension applied by [killApp] with [KillAppOption.OPTION_III] once the package is processed.
     */
    protected suspend fun releaseApp(killAppOption: KillAppOption, pkg: TaskDetailPackageEntity) = run {
        when (killAppOption) {
            KillAppOption.OPTION_III -> SuspendUtil.resumePackage(context = mContext, userId = pkg.packageEntity.userId, packageName = pkg.packageEntity.packageName)
            else -> {}
        }
    }

    /**
     * Lifts every suspension that a killed task could not lift by itself.
     */
    protected suspend fun releaseApps() = SuspendUtil.resumeAll(mContext)

    protected abstract val mPackageDao: PackageDao
    protected abstract val mPackageRepo: PackageRepository
    protected abstract val mRootDir: String
    protected abstract val mAppsDir: String
    protected abstract val mConfigsDir: String
}
