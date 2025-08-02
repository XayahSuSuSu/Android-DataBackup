package com.xayah.core.service.packages.backup

import android.annotation.SuppressLint
import com.xayah.core.common.util.toLineString
import com.xayah.core.datastore.readBackupConfigs
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.datastore.readKillAppOption
import com.xayah.core.datastore.readResetBackupList
import com.xayah.core.datastore.saveLastBackupTime
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
import com.xayah.core.model.util.set
import com.xayah.core.service.R
import com.xayah.core.service.model.NecessaryInfo
import com.xayah.core.service.packages.AbstractPackagesService
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.PreparationUtil
import kotlinx.coroutines.flow.first
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal abstract class AbstractBackupService : AbstractPackagesService() {
    override suspend fun onInitializingPreprocessingEntities(entities: MutableList<ProcessingInfoEntity>) {
        entities.apply {
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.necessary_preparations),
                type = ProcessingType.PREPROCESSING,
                infoType = ProcessingInfoType.NECESSARY_PREPARATIONS
            ).apply {
                id = mTaskDao.upsert(this)
            })
        }
    }

    override suspend fun onInitializingPostProcessingEntities(entities: MutableList<ProcessingInfoEntity>) {
        entities.apply {
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.backup_itself),
                type = ProcessingType.POST_PROCESSING,
                infoType = ProcessingInfoType.BACKUP_ITSELF
            ).apply {
                id = mTaskDao.upsert(this)
            })
            add(ProcessingInfoEntity(
                taskId = mTaskEntity.id,
                title = mContext.getString(R.string.save_icons),
                type = ProcessingType.POST_PROCESSING,
                infoType = ProcessingInfoType.SAVE_ICONS
            ).apply {
                id = mTaskDao.upsert(this)
            })
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
        val packages = mPackageRepo.queryActivated(OpType.BACKUP)
        packages.forEach { pkg ->
            mPkgEntities.add(
                TaskDetailPackageEntity(
                    taskId = mTaskEntity.id,
                    packageEntity = pkg,
                    apkInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_APK.type.uppercase())),
                    userInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_USER.type.uppercase())),
                    userDeInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_USER_DE.type.uppercase())),
                    dataInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_DATA.type.uppercase())),
                    obbInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_OBB.type.uppercase())),
                    mediaInfo = Info(title = mContext.getString(com.xayah.core.data.R.string.args_backup, DataType.PACKAGE_MEDIA.type.uppercase())),
                ).apply {
                    id = mTaskDao.upsert(this)
                }
            )
        }
    }

    override suspend fun beforePreprocessing() {
        NotificationUtil.notify(mContext, mNotificationBuilder, mContext.getString(R.string.backing_up), mContext.getString(R.string.preprocessing))
    }

    protected open suspend fun onTargetDirsCreated() {}
    protected open suspend fun onAppDirCreated(archivesRelativeDir: String): Boolean = true
    abstract suspend fun backup(type: DataType, p: PackageEntity, r: PackageEntity?, t: TaskDetailPackageEntity, dstDir: String)
    protected open suspend fun onConfigSaved(path: String, archivesRelativeDir: String) {}
    protected open suspend fun onItselfSaved(path: String, entity: ProcessingInfoEntity) {}
    protected open suspend fun onConfigsSaved(path: String, entity: ProcessingInfoEntity) {}
    protected open suspend fun onIconsSaved(path: String, entity: ProcessingInfoEntity) {}
    protected open suspend fun clear() {}

    protected abstract val mPackagesBackupUtil: PackagesBackupUtil

    private lateinit var necessaryInfo: NecessaryInfo

    override suspend fun onPreprocessing(entity: ProcessingInfoEntity) {
        when (entity.infoType) {
            ProcessingInfoType.NECESSARY_PREPARATIONS -> {
                /**
                 * Somehow the input methods and accessibility services
                 * will be changed after backing up on some devices,
                 * so we restore them manually.
                 */
                necessaryInfo = NecessaryInfo(inputMethods = PreparationUtil.getInputMethods().outString.trim(), accessibilityServices = PreparationUtil.getAccessibilityServices().outString.trim())
                log { "InputMethods: ${necessaryInfo.inputMethods}." }
                log { "AccessibilityServices: ${necessaryInfo.accessibilityServices}." }

                log { "Trying to create: $mAppsDir." }
                log { "Trying to create: $mConfigsDir." }
                mRootService.mkdirs(mAppsDir)
                mRootService.mkdirs(mConfigsDir)
                val isSuccess = runCatchingOnService { onTargetDirsCreated() }
                entity.update(progress = 1f, state = if (isSuccess) OperationState.DONE else OperationState.ERROR)
            }

            else -> {}
        }
    }

    override suspend fun onProcessing() {
        // createTargetDirs() before readStatFs().
        mTaskEntity.update(rawBytes = mTaskRepo.getRawBytes(TaskType.PACKAGE), availableBytes = mTaskRepo.getAvailableBytes(OpType.BACKUP), totalBytes = mTaskRepo.getTotalBytes(OpType.BACKUP), totalCount = mPkgEntities.size)
        log { "Task count: ${mPkgEntities.size}." }

        val killAppOption = mContext.readKillAppOption().first()
        log { "Kill app option: $killAppOption" }

        mPkgEntities.forEachIndexed { index, pkg ->
            executeAtLeast {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.backing_up),
                    pkg.packageEntity.packageInfo.label,
                    mPkgEntities.size,
                    index
                )
                log { "Current package: ${pkg.packageEntity}" }

                killApp(killAppOption, pkg)

                pkg.update(state = OperationState.PROCESSING)
                val p = pkg.packageEntity
                val dstDir = "${mAppsDir}/${p.archivesRelativeDir}"
                var restoreEntity = mPackageDao.query(p.packageName, OpType.RESTORE, p.userId, p.preserveId, p.indexInfo.compressionType, mTaskEntity.cloud, mTaskEntity.backupDir)
                mRootService.mkdirs(dstDir)

                if (p.indexInfo.compressionType == com.xayah.core.model.CompressionType.TWRP_ZIP) {
                    // TWRP ZIP Backup Logic
                    val zipFile = java.io.File(mAppsDir, "${p.packageName}_${p.userId}_${p.preserveId}.zip")
                    val checksums = mutableMapOf<String, Long>() // Map to store checksums
                    try {
                        ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                            val dataTypesToBackup = mutableListOf<DataType>()
                            if (p.backupApk) dataTypesToBackup.add(DataType.PACKAGE_APK)
                            if (p.backupUser) dataTypesToBackup.add(DataType.PACKAGE_USER)
                            if (p.backupUserDe) dataTypesToBackup.add(DataType.PACKAGE_USER_DE)
                            if (p.backupData) dataTypesToBackup.add(DataType.PACKAGE_DATA)
                            if (p.backupObb) dataTypesToBackup.add(DataType.PACKAGE_OBB)
                            if (p.backupMedia) dataTypesToBackup.add(DataType.PACKAGE_MEDIA)

                            dataTypesToBackup.forEach { dataType ->
                                val files = mPackagesBackupUtil.getFilesForDataType(p, dataType)
                                val basePath = when (dataType) {
                                    DataType.PACKAGE_APK -> mPackagesBackupUtil.getPackageSourceDir(p.packageName, p.userId)
                                    else -> mPackagesBackupUtil.packageRepository.getDataSrcDir(dataType, p.userId)
                                }
                                files.forEach { file ->
                                    // Ensure basePath is not empty and file path is correctly relativized
                                    val relativePath = if (basePath.isNotEmpty() && file.absolutePath.startsWith(basePath)) {
                                        file.absolutePath.substring(basePath.length).trimStart('/')
                                    } else {
                                        file.name // Fallback if basePath is tricky or not applicable
                                    }
                                    val entryName = "${dataType.type}/$relativePath"
                                    zos.putNextEntry(ZipEntry(entryName))
                                    // Stream file content directly to ZIP
                                    var crcValue: Long = 0
                                    mRootService.openFileForStreaming(file.absolutePath)?.use { pfd ->
                                        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { fis ->
                                            val checkedInputStream = java.util.zip.CheckedInputStream(fis, java.util.zip.CRC32())
                                            checkedInputStream.copyTo(zos)
                                            crcValue = checkedInputStream.checksum.value
                                        }
                                    } ?: log { "Warning: Could not open file ${file.absolutePath} for streaming into TWRP ZIP." }
                                    zos.closeEntry()
                                    checksums[entryName] = crcValue
                                }
                            }
                            // Add checksums.txt to ZIP
                            zos.putNextEntry(ZipEntry("checksums.txt"))
                            val checksumContent = checksums.entries.joinToString("\n") { "${it.key}:${it.value}" }
                            zos.write(checksumContent.toByteArray())
                            zos.closeEntry()
                        }
                        // Update package entity and task entity for success
                        p.extraInfo.lastBackupTime = DateUtil.getTimestamp()
                        pkg.update(packageEntity = p, state = OperationState.DONE)
                        mTaskEntity.update(successCount = mTaskEntity.successCount + 1)
                    } catch (e: Exception) {
                        log { "Error creating TWRP ZIP for ${p.packageName}: ${e.message}" }
                        pkg.update(state = OperationState.ERROR)
                        mTaskEntity.update(failureCount = mTaskEntity.failureCount + 1)
                    }
                } else {
                    // Existing backup logic
                    if (onAppDirCreated(archivesRelativeDir = p.archivesRelativeDir)) {
                        backup(type = DataType.PACKAGE_APK, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        backup(type = DataType.PACKAGE_USER, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        backup(type = DataType.PACKAGE_USER_DE, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        backup(type = DataType.PACKAGE_DATA, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        backup(type = DataType.PACKAGE_OBB, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        backup(type = DataType.PACKAGE_MEDIA, p = p, r = restoreEntity, t = pkg, dstDir = dstDir)
                        mPackagesBackupUtil.backupPermissions(p = p)
                        mPackagesBackupUtil.backupSsaid(p = p)

                        if (pkg.isSuccess) {
                            // Save config
                            p.extraInfo.lastBackupTime = DateUtil.getTimestamp()
                            val id = restoreEntity?.id ?: 0
                            restoreEntity = p.copy(
                                id = id,
                                indexInfo = p.indexInfo.copy(opType = OpType.RESTORE, cloud = mTaskEntity.cloud, backupDir = mTaskEntity.backupDir),
                                extraInfo = p.extraInfo.copy(activated = false)
                            )
                            val configDst = PathUtil.getPackageRestoreConfigDst(dstDir = dstDir)
                            mRootService.writeJson(data = restoreEntity, dst = configDst)
                            onConfigSaved(path = configDst, archivesRelativeDir = p.archivesRelativeDir)
                            mPackageDao.upsert(restoreEntity)
                            mPackageDao.upsert(p)
                            pkg.update(packageEntity = p)
                            mTaskEntity.update(successCount = mTaskEntity.successCount + 1)
                        } else {
                            mTaskEntity.update(failureCount = mTaskEntity.failureCount + 1)
                        }
                    } else {
                        pkg.update(dataType = DataType.PACKAGE_APK, state = OperationState.ERROR)
                        pkg.update(dataType = DataType.PACKAGE_USER, state = OperationState.ERROR)
                        pkg.update(dataType = DataType.PACKAGE_USER_DE, state = OperationState.ERROR)
                        pkg.update(dataType = DataType.PACKAGE_DATA, state = OperationState.ERROR)
                        pkg.update(dataType = DataType.PACKAGE_OBB, state = OperationState.ERROR)
                        pkg.update(dataType = DataType.PACKAGE_MEDIA, state = OperationState.ERROR)
                    }
                    pkg.update(state = if (pkg.isSuccess) OperationState.DONE else OperationState.ERROR)
                }
            }
            mTaskEntity.update(processingIndex = mTaskEntity.processingIndex + 1)
        }
    }

    override suspend fun onPostProcessing(entity: ProcessingInfoEntity) {
        when (entity.infoType) {
            ProcessingInfoType.BACKUP_ITSELF -> {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.backing_up),
                    mContext.getString(R.string.backup_itself)
                )
                if (mContext.readBackupItself().first()) {
                    log { "Backup itself enabled." }
                    mCommonBackupUtil.backupItself(dstDir = mRootDir).apply {
                        entity.set(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = outString)
                        if (isSuccess) {
                            onItselfSaved(path = mCommonBackupUtil.getItselfDst(mRootDir), entity = entity)
                        }
                    }
                    entity.update(progress = 1f)
                } else {
                    entity.update(progress = 1f, state = OperationState.SKIP)
                }
            }

            ProcessingInfoType.SAVE_ICONS -> {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.backing_up),
                    mContext.getString(R.string.save_icons)
                )
                mPackagesBackupUtil.backupIcons(dstDir = mConfigsDir).apply {
                    entity.set(state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = outString)
                    if (isSuccess) {
                        onIconsSaved(path = mPackagesBackupUtil.getIconsDst(mConfigsDir), entity = entity)
                    }
                }
                entity.update(progress = 1f)
            }

            ProcessingInfoType.NECESSARY_REMAINING_DATA_PROCESSING -> {
                NotificationUtil.notify(
                    mContext,
                    mNotificationBuilder,
                    mContext.getString(R.string.backing_up),
                    mContext.getString(R.string.wait_for_remaining_data_processing)
                )

                var isSuccess = true
                val out = mutableListOf<String>()
                if (mContext.readBackupConfigs().first()) {
                    log { "Backup configs enabled." }
                    mCommonBackupUtil.backupConfigs(dstDir = mConfigsDir).also { result ->
                        if (result.isSuccess.not()) {
                            isSuccess = false
                        }
                        out.add(result.outString)
                        if (result.isSuccess) {
                            onConfigsSaved(path = mCommonBackupUtil.getConfigsDst(mConfigsDir), entity = entity)
                        }
                    }
                }
                entity.update(progress = 0.5f)

                // Restore keyboard and services.
                if (necessaryInfo.inputMethods.isNotEmpty()) {
                    PreparationUtil.setInputMethods(inputMethods = necessaryInfo.inputMethods)
                    log { "InputMethods restored: ${necessaryInfo.inputMethods}." }
                } else {
                    log { "InputMethods is empty, skip restoring." }
                }
                if (necessaryInfo.accessibilityServices.isNotEmpty()) {
                    PreparationUtil.setAccessibilityServices(accessibilityServices = necessaryInfo.accessibilityServices)
                    log { "AccessibilityServices restored: ${necessaryInfo.accessibilityServices}." }
                } else {
                    log { "AccessibilityServices is empty, skip restoring." }
                }
                if (mContext.readResetBackupList().first() && mTaskEntity.failureCount == 0) {
                    mPackageDao.clearActivated(OpType.BACKUP)
                }
                if (runCatchingOnService { clear() }.not()) {
                    isSuccess = false
                }
                entity.set(progress = 1f, state = if (isSuccess) OperationState.DONE else OperationState.ERROR, log = out.toLineString())
            }

            else -> {}
        }
    }

    override suspend fun afterPostProcessing() {
        mContext.saveLastBackupTime(mEndTimestamp)
        val time = DateUtil.getShortRelativeTimeSpanString(context = mContext, time1 = mStartTimestamp, time2 = mEndTimestamp)
        NotificationUtil.notify(
            mContext,
            mNotificationBuilder,
            mContext.getString(R.string.backup_completed),
            "${time}, ${mTaskEntity.successCount} ${mContext.getString(R.string.succeed)}, ${mTaskEntity.failureCount} ${mContext.getString(R.string.failed)}",
            ongoing = false
        )
    }
}
