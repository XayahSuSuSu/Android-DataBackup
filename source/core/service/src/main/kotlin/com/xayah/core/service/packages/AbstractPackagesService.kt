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
        // Stopping the package is a best-effort helper: a failure here must not decide whether the package
        // itself is backed up, and a running app must never abort the whole task.
        runCatching {
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
        }.onFailure { log { "Failed to kill $packageName: $it" } }
    }

    /**
     * Re-applies the pause of [KillAppOption.OPTION_III] right before the data is read.
     *
     * The suspension can be lifted behind our back: the launcher asks the user whether the app should be
     * started anyway, and some ROMs resume the package on their own after a while. Re-applying the pause
     * right before the data is archived means a launch attempt can neither disturb the archive nor leave a
     * running process behind while its data is being read.
     */
    protected suspend fun reinforcePause(killAppOption: KillAppOption, pkg: TaskDetailPackageEntity) = run {
        if (killAppOption != KillAppOption.OPTION_III || pkg.packageEntity.isSystemApp) {
            return@run
        }
        runCatching {
            SuspendUtil.suspendPackage(context = mContext, userId = pkg.packageEntity.userId, packageName = pkg.packageEntity.packageName)
        }.onFailure { log { "Failed to re-apply the pause of ${pkg.packageEntity.packageName}: $it" } }
    }

    /**
     * Lifts the suspension applied by [killApp] with [KillAppOption.OPTION_III] once the package is processed.
     */
    protected suspend fun releaseApp(killAppOption: KillAppOption, pkg: TaskDetailPackageEntity) = run {
        if (killAppOption != KillAppOption.OPTION_III) {
            return@run
        }
        runCatching {
            SuspendUtil.resumePackage(context = mContext, userId = pkg.packageEntity.userId, packageName = pkg.packageEntity.packageName)
        }.onFailure { log { "Failed to resume ${pkg.packageEntity.packageName}: $it" } }
    }

    /**
     * Lifts every suspension that a killed task could not lift by itself.
     */
    protected suspend fun releaseApps() = run {
        runCatching { SuspendUtil.resumeAll(mContext) }.onFailure { log { "Failed to resume the paused packages: $it" } }
    }

    protected abstract val mPackageDao: PackageDao
    protected abstract val mPackageRepo: PackageRepository
    protected abstract val mRootDir: String
    protected abstract val mAppsDir: String
    protected abstract val mConfigsDir: String
}
