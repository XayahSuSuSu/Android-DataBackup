package com.xayah.core.service.packages.restore

import android.annotation.SuppressLint
import com.xayah.core.datastore.readKillAppOption
import com.xayah.core.datastore.readResetRestoreList
import com.xayah.core.datastore.readRestorePermissions
import com.xayah.core.datastore.readRestoreSsaid
import com.xayah.core.datastore.readRestoreUser
import com.xayah.core.datastore.saveLastRestoreTime
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.ProcessingInfoType
import com.xayah.core.model.ProcessingType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.service.R
import com.xayah.core.service.packages.AbstractPackagesService
import com.xayah.core.service.util.PackagesRestoreUtil
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.command.PreparationUtil
import kotlinx.coroutines.flow.first

internal abstract class AbstractRestoreService : AbstractPackagesService() {
    override suspend fun onInitializingPreprocessingEntities(entities: MutableList<ProcessingInfoEntity>) {
        entities.apply {
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.set_up_inst_env),
                type = ProcessingType.PREPROCESSING,
                infoType = ProcessingInfoType.SET_UP_INST_ENV
            ).apply {
                id = mTaskDao.upsert(this)
            })
        }
    }

    override suspend fun onInitializingPostProcessingEntities(entities: MutableList<ProcessingInfoEntity>) {
        entities.apply {
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.necessary_remaining_data_processing),
                type = ProcessingType.POST_PROCESSING,
                infoType = ProcessingInfoType.NECESSARY_REMAINING_DATA_PROCESSING
            ).apply {
                id = mTaskDao.upsert(this)
            })
        }
    }

    @SuppressLint("StringFormatInvalid")
    override suspend fun onInitializing() {
        val packages = getPackages()
        packages.forEach { pkg ->
            mPkgEntities.add(
                TaskDetailPackageEntity(
                    taskId = mTaskEntity.id,
                    packageEntity = pkg,
                    apkInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_APK.type.uppercase())),
                    userInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_USER.type.uppercase())),
                    userDeInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_USER_DE.type.uppercase())),
                    dataInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_DATA.type.uppercase())),
                    obbInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_OBB.type.uppercase())),
                    mediaInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_restore, DataType.PACKAGE_MEDIA.type.uppercase())),
                ).apply {
                    id = mTaskDao.upsert(this)
                }
            )
        }
    }

    override suspend fun beforePreprocessing() {
        NotificationUtil.notify(mContext, mNotificationBuilder, mContext.getString(R.string.restoring), mContext.getString(R.string.preprocessing))
    }

    abstract suspend fun getPackages(): List<PackageEntity>
    protected open suspend fun beforeSettingUpEnv() {}
    abstract suspend fun restore(type: DataType, userId: Int, p: PackageEntity, t: TaskDetailPackageEntity, srcDir: String)
    protected open suspend fun clear() {}

    protected abstract val mPackagesRestoreUtil: PackagesRestoreUtil

    private var restoreUser = -1

    override suspend fun onPreprocessing(entity: ProcessingInfoEntity) {
        restoreUser = mContext.readRestoreUser().first()
        when (entity.infoType) {
            ProcessingInfoType.SET_UP_INST_ENV -> {
                if (runCatchingOnService { beforeSettingUpEnv() }) {
                    log { "Trying to grant adb install permissions." }
                    PreparationUtil.setInstallEnv().apply {
                        entity.update(state = if (isSuccess) OperationState.DONE else OperationState.ERROR)
                    }
                } else {
                    entity.update(state = OperationState.ERROR, log = log { "Failed to grant adb install permissions." })
                }
                entity.update(progress = 1f)
            }

            else -> {}
        }
    }

    override suspend fun onProcessing() {
        mTaskEntity.update(rawBytes = mTaskRepo.getRawBytes(TaskType.PACKAGE), availableBytes = mTaskRepo.getAvailableBytes(OpType.RESTORE), totalBytes = mTaskRepo.getTotalBytes(OpType.RESTORE), totalCount = mPkgEntities.size)
        log { "Task count: ${mPkgEntities.size}." }

        val killAppOption = mContext.readKillAppOption().first()
        log { "Kill app option: $killAppOption" }

        mPkgEntities.forEachIndexed { index, pkg ->
            executeAtLeast {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.restoring),
                    pkg.packageEntity.packageInfo.label,
                    mPkgEntities.size,
                    index
                )
                log { "Current package: ${pkg.packageEntity}" }

                killApp(killAppOption, pkg)

                pkg.update(state = OperationState.PROCESSING)
                val p = pkg.packageEntity
                val srcDir = "${mAppsDir}/${p.archivesRelativeDir}"
                val userId = if (restoreUser == -1) p.userId else restoreUser
                restore(type = DataType.PACKAGE_APK, userId = userId, p = p, t = pkg, srcDir = srcDir)
                restore(type = DataType.PACKAGE_USER, userId = userId, p = p, t = pkg, srcDir = srcDir)
                restore(type = DataType.PACKAGE_USER_DE, userId = userId, p = p, t = pkg, srcDir = srcDir)
                restore(type = DataType.PACKAGE_DATA, userId = userId, p = p, t = pkg, srcDir = srcDir)
                restore(type = DataType.PACKAGE_OBB, userId = userId, p = p, t = pkg, srcDir = srcDir)
                restore(type = DataType.PACKAGE_MEDIA, userId = userId, p = p, t = pkg, srcDir = srcDir)
                if (mContext.readRestorePermissions().first()) {
                    mPackagesRestoreUtil.restorePermissions(userId = userId, p = p)
                }
                if (mContext.readRestoreSsaid().first()) {
                    mPackagesRestoreUtil.restoreSsaid(userId = userId, p = p)
                }

                if (pkg.isSuccess) {
                    pkg.update(packageEntity = p)
                    mTaskEntity.update(successCount = mTaskEntity.successCount + 1)
                } else {
                    mTaskEntity.update(failureCount = mTaskEntity.failureCount + 1)
                }
                pkg.update(state = if (pkg.isSuccess) OperationState.DONE else OperationState.ERROR)
            }
            mTaskEntity.update(processingIndex = mTaskEntity.processingIndex + 1)
        }
    }

    override suspend fun onPostProcessing(entity: ProcessingInfoEntity) {
        when (entity.infoType) {
            ProcessingInfoType.NECESSARY_REMAINING_DATA_PROCESSING -> {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.restoring),
                    mContext.getString(R.string.wait_for_remaining_data_processing)
                )

                if (mContext.readResetRestoreList().first() && mTaskEntity.failureCount == 0) {
                    mPackageDao.clearActivated(OpType.RESTORE)
                }
                val isSuccess = runCatchingOnService { clear() }
                entity.update(progress = 1f, state = if (isSuccess) OperationState.DONE else OperationState.ERROR)
            }

            else -> {}
        }
    }

    override suspend fun afterPostProcessing() {
        mContext.saveLastRestoreTime(mEndTimestamp)
        val time = DateUtil.getShortRelativeTimeSpanString(context = mContext, time1 = mStartTimestamp, time2 = mEndTimestamp)
        NotificationUtil.notify(
            mContext,
            mNotificationBuilder,
            mContext.getString(R.string.restore_completed),
            "${time}, ${mTaskEntity.successCount} ${mContext.getString(R.string.succeed)}, ${mTaskEntity.failureCount} ${mContext.getString(R.string.failed)}",
            ongoing = false
        )
    }
}
