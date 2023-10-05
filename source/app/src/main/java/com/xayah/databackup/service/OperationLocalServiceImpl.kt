package com.xayah.databackup.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.databackup.data.MediaBackupOperationEntity
import com.xayah.databackup.data.MediaDao
import com.xayah.databackup.data.MediaRestoreEntity
import com.xayah.databackup.data.MediaRestoreOperationEntity
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.data.PackageRestoreOperation
import com.xayah.databackup.data.PackageRestoreOperationDao
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.MediumBackupUtil
import com.xayah.databackup.util.command.MediumRestoreUtil
import com.xayah.databackup.util.command.PackagesBackupUtil
import com.xayah.databackup.util.command.PackagesRestoreUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.iconPath
import com.xayah.databackup.util.readBackupItself
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.readResetBackupList
import com.xayah.databackup.util.readResetRestoreList
import com.xayah.databackup.util.saveLastBackupTime
import com.xayah.databackup.util.saveLastRestoringTime
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Local service for I/O operations in background.
 */
@AndroidEntryPoint
class OperationLocalServiceImpl : Service() {
    private val binder = OperationLocalBinder()
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): OperationLocalServiceImpl = this@OperationLocalServiceImpl
    }

    private val mutex = Mutex()

    @Inject
    lateinit var logUtil: LogUtil

    @Inject
    lateinit var packageBackupEntireDao: PackageBackupEntireDao

    @Inject
    lateinit var packageBackupOperationDao: PackageBackupOperationDao

    @Inject
    lateinit var packageRestoreEntireDao: PackageRestoreEntireDao

    @Inject
    lateinit var packageRestoreOperationDao: PackageRestoreOperationDao

    @Inject
    lateinit var gsonUtil: GsonUtil

    @Inject
    lateinit var mediaDao: MediaDao

    suspend fun backupPackagesPreparation(): BackupPreparation = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup preparation"
            logUtil.log(logTag, "Started.")

            /**
             * Somehow the keyboards and accessibility services
             * will be changed after backing up on some devices,
             * so we restore them manually.
             */
            val (_, keyboard) = PreparationUtil.getKeyboard()
            val (_, services) = PreparationUtil.getAccessibilityServices()
            logUtil.log(logTag, "Keyboard: $keyboard")
            logUtil.log(logTag, "Services: $services")
            BackupPreparation(keyboard = keyboard, services = services)
        }
    }

    suspend fun backupPackages(timestamp: Long) = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup"
            val context = applicationContext
            val remoteRootService = RemoteRootService(context)
            val packagesBackupUtil = PackagesBackupUtil(context, timestamp, logUtil, remoteRootService, packageBackupOperationDao, gsonUtil)

            logUtil.log(logTag, "Started.")
            val packages = packageBackupEntireDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                val packageName = currentPackage.packageName
                val isApkSelected = OperationMask.isApkSelected(currentPackage.operationCode)
                val isDataSelected = OperationMask.isDataSelected(currentPackage.operationCode)
                logUtil.log(logTag, "Current package: $packageName")
                logUtil.log(logTag, "isApkSelected: $isApkSelected")
                logUtil.log(logTag, "isDataSelected: $isDataSelected")
                remoteRootService.mkdirs(packagesBackupUtil.getPackageItemSavePath(packageName))

                val packageBackupOperation =
                    PackageBackupOperation(
                        packageName = packageName,
                        timestamp = timestamp,
                        startTimestamp = DateUtil.getTimestamp(),
                        endTimestamp = 0,
                        label = currentPackage.label
                    ).also { entity ->
                        entity.id = packageBackupOperationDao.upsert(entity)
                    }

                if (isApkSelected) {
                    packagesBackupUtil.backupApk(packageBackupOperation, packageName)
                } else {
                    packageBackupOperation.apkState = OperationState.SKIP
                }
                if (isDataSelected) {
                    packagesBackupUtil.apply {
                        backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER)
                        backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER_DE)
                        backupData(packageBackupOperation, packageName, DataType.PACKAGE_DATA)
                        backupData(packageBackupOperation, packageName, DataType.PACKAGE_OBB)
                        backupData(packageBackupOperation, packageName, DataType.PACKAGE_MEDIA)
                    }
                } else {
                    packageBackupOperation.apply {
                        userState = OperationState.SKIP
                        userDeState = OperationState.SKIP
                        dataState = OperationState.SKIP
                        obbState = OperationState.SKIP
                        mediaState = OperationState.SKIP
                    }
                }

                // Update package state and end time.
                if (packageBackupOperation.isSucceed) {
                    logUtil.log(logTag, "Backup succeed.")
                } else {
                    logUtil.log(logTag, "Backup failed.")
                }
                packageBackupOperation.packageState = packageBackupOperation.isSucceed
                packageBackupOperation.endTimestamp = DateUtil.getTimestamp()
                packageBackupOperationDao.upsert(packageBackupOperation)

                // Insert restore config into database.
                if (packageBackupOperation.isSucceed) {
                    val restoreEntire = PackageRestoreEntire(
                        packageName = currentPackage.packageName,
                        label = currentPackage.label,
                        backupOpCode = currentPackage.operationCode,
                        operationCode = OperationMask.None,
                        timestamp = timestamp,
                        versionName = currentPackage.versionName,
                        versionCode = currentPackage.versionCode,
                        flags = currentPackage.flags,
                        compressionType = context.readCompressionType(),
                        active = false
                    )
                    packageRestoreEntireDao.upsert(restoreEntire)

                    // Save config
                    packagesBackupUtil.backupConfig(restoreEntire, DataType.PACKAGE_CONFIG)

                    // Reset selected items if enabled.
                    if (context.readResetBackupList()) {
                        currentPackage.operationCode = OperationMask.None
                        packageBackupEntireDao.update(currentPackage)
                    }
                }
            }

            context.saveLastBackupTime(timestamp)
            remoteRootService.destroyService()
        }
    }

    suspend fun backupPackagesAfterwards(preparation: BackupPreparation) = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup afterwards"
            val context = applicationContext
            val remoteRootService = RemoteRootService(context)
            logUtil.log(logTag, "Started.")

            // Restore keyboard and services.
            if (preparation.keyboard.isNotEmpty()) {
                PreparationUtil.setKeyboard(preparation.keyboard)
                logUtil.log(logTag, "Keyboard restored: ${preparation.keyboard}")
            } else {
                logUtil.log(logTag, "Keyboard is empty, skip restoring.")
            }
            if (preparation.services.isNotEmpty()) {
                PreparationUtil.setAccessibilityServices(preparation.services)
                logUtil.log(logTag, "Services restored: ${preparation.services}")
            } else {
                logUtil.log(logTag, "Service is empty, skip restoring.")
            }

            // Backup itself if enabled.
            if (context.readBackupItself()) {
                val outPath = PathUtil.getBackupSavePath()
                val userId = context.readBackupUserId()
                val sourceDirList = remoteRootService.getPackageSourceDir(packageName, userId)
                if (sourceDirList.isNotEmpty()) {
                    val apkPath = PathUtil.getParentPath(sourceDirList[0])
                    val path = "${apkPath}/base.apk"
                    val targetPath = "${outPath}/DataBackup.apk"
                    remoteRootService.copyTo(path = path, targetPath = targetPath, overwrite = true).also { result ->
                        if (result.not()) {
                            logUtil.log(logTag, "Failed to copy $path to $targetPath.")
                        } else {
                            logUtil.log(logTag, "Copied from $path to $targetPath.")
                        }
                    }
                } else {
                    logUtil.log(logTag, "Failed to get apk path of $packageName.")
                }
            }

            // Backup icons.
            val iconPath = context.iconPath()
            val iconSavePath = PathUtil.getIconSavePath()
            remoteRootService.copyRecursively(path = iconPath, targetPath = iconSavePath, overwrite = true)
            logUtil.log(logTag, "Copied from $iconPath to $iconSavePath.")

            val noMediaSavePath = PathUtil.getIconNoMediaSavePath()
            remoteRootService.createNewFile(path = noMediaSavePath)
            logUtil.log(logTag, "Created $noMediaSavePath.")

            remoteRootService.destroyService()
        }
    }

    suspend fun restorePackagesPreparation() = withIOContext {
        mutex.withLock {
            val logTag = "Packages restore preparation"
            logUtil.log(logTag, "Started.")

            // Enable adb install permissions.
            val (_, output) = PreparationUtil.setInstallEnv()
            logUtil.log(logTag, "Enable adb install permissions: $output")
        }
    }

    suspend fun restorePackages(timestamp: Long) = withIOContext {
        mutex.withLock {
            val logTag = "Packages restore"
            val context = applicationContext
            val remoteRootService = RemoteRootService(context)
            val packagesRestoreUtil = PackagesRestoreUtil(context, logUtil, remoteRootService, packageRestoreOperationDao)

            logUtil.log(logTag, "Started.")
            val packages = packageRestoreEntireDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                val packageName = currentPackage.packageName
                val compressionType = currentPackage.compressionType
                val packageTimestamp = currentPackage.timestamp
                val isApkSelected = OperationMask.isApkSelected(currentPackage.operationCode)
                val isDataSelected = OperationMask.isDataSelected(currentPackage.operationCode)
                logUtil.log(logTag, "Current package: $packageName")
                logUtil.log(logTag, "isApkSelected: $isApkSelected")
                logUtil.log(logTag, "isDataSelected: $isDataSelected")

                val packageRestoreOperation =
                    PackageRestoreOperation(
                        packageName = packageName,
                        timestamp = timestamp,
                        startTimestamp = DateUtil.getTimestamp(),
                        endTimestamp = 0,
                        label = currentPackage.label
                    ).also { entity ->
                        entity.id = packageRestoreOperationDao.upsert(entity)
                    }

                if (isApkSelected) {
                    packagesRestoreUtil.restoreApk(packageRestoreOperation, packageName, packageTimestamp, compressionType)
                } else {
                    packagesRestoreUtil.queryInstalled(packageRestoreOperation, packageName)
                }
                if (isDataSelected) {
                    packagesRestoreUtil.apply {
                        restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_USER)
                        restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_USER_DE)
                        restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_DATA)
                        restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_OBB)
                        restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_MEDIA)
                    }
                } else {
                    packageRestoreOperation.apply {
                        userState = OperationState.SKIP
                        userDeState = OperationState.SKIP
                        dataState = OperationState.SKIP
                        obbState = OperationState.SKIP
                        mediaState = OperationState.SKIP
                    }
                }

                // Update package state and end time.
                if (packageRestoreOperation.isSucceed) {
                    logUtil.log(logTag, "Restoring succeed.")

                    // Reset selected items if enabled.
                    if (context.readResetRestoreList()) {
                        currentPackage.operationCode = OperationMask.None
                        packageRestoreEntireDao.update(currentPackage)
                    }
                } else {
                    logUtil.log(logTag, "Restoring failed.")
                }
                packageRestoreOperation.packageState = packageRestoreOperation.isSucceed
                packageRestoreOperation.endTimestamp = DateUtil.getTimestamp()
                packageRestoreOperationDao.upsert(packageRestoreOperation)
            }

            context.saveLastRestoringTime(timestamp)
            remoteRootService.destroyService()
        }
    }

    suspend fun backupMedium(timestamp: Long) = withIOContext {
        mutex.withLock {
            val logTag = "Media backup"
            val context = applicationContext
            val remoteRootService = RemoteRootService(context)
            val mediumBackupUtil = MediumBackupUtil(context, timestamp, logUtil, remoteRootService, mediaDao, gsonUtil)

            logUtil.log(logTag, "Started.")
            val medium = mediaDao.queryBackupSelected()
            medium.forEach { current ->
                val name = current.name
                val path = current.path
                logUtil.log(logTag, "Current media: ${name}: $path")
                remoteRootService.mkdirs(mediumBackupUtil.getMediaItemSavePath(name))

                val mediaBackupOperationEntity = MediaBackupOperationEntity(
                    id = 0,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    path = path,
                    name = name,
                    opLog = "",
                    opState = OperationState.IDLE,
                    state = false,
                ).also { entity ->
                    entity.id = mediaDao.upsertBackupOp(entity)
                }

                mediumBackupUtil.backupMedia(mediaBackupOperationEntity)

                // Update package state and end time.
                if (mediaBackupOperationEntity.isSucceed) {
                    logUtil.log(logTag, "Backup succeed.")
                } else {
                    logUtil.log(logTag, "Backup failed.")
                }
                mediaBackupOperationEntity.state = mediaBackupOperationEntity.isSucceed
                mediaBackupOperationEntity.endTimestamp = DateUtil.getTimestamp()
                mediaDao.upsertBackupOp(mediaBackupOperationEntity)

                // Insert restore config into database.
                if (mediaBackupOperationEntity.isSucceed) {
                    val restoreEntire = MediaRestoreEntity(
                        id = 0,
                        timestamp = timestamp,
                        path = path,
                        name = name,
                        sizeBytes = 0,
                        selected = true,
                    )
                    mediaDao.upsertRestore(restoreEntire)

                    // Save config
                    mediumBackupUtil.backupConfig(restoreEntire, DataType.MEDIA_CONFIG)

                    // Reset selected items if enabled.
                    if (context.readResetBackupList()) {
                        current.selected = false
                        mediaDao.upsertBackup(current)
                    }
                }
            }

            context.saveLastBackupTime(timestamp)
            remoteRootService.destroyService()
        }
    }

    suspend fun restoreMedium(timestamp: Long) = withIOContext {
        mutex.withLock {
            val logTag = "Media restore"
            val context = applicationContext
            val remoteRootService = RemoteRootService(context)
            val mediumRestoreUtil = MediumRestoreUtil(context, logUtil, remoteRootService, mediaDao)

            logUtil.log(logTag, "Started.")
            val medium = mediaDao.queryRestoreSelected(timestamp)
            medium.forEach { current ->
                val name = current.name
                val path = current.path
                logUtil.log(logTag, "Current media: ${name}: $path")
                if (path.isEmpty()) {
                    mediaDao.upsertRestore(current.copy(selected = false))
                    logUtil.log(logTag, "The target path is empty, skip.")
                    return@forEach
                }

                val mediaRestoreOperationEntity = MediaRestoreOperationEntity(
                    id = 0,
                    entityId = current.id,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    path = path,
                    name = name,
                    opLog = "",
                    opState = OperationState.IDLE,
                    state = false,
                ).also { entity ->
                    entity.id = mediaDao.upsertRestoreOp(entity)
                }

                mediumRestoreUtil.restoreMedia(entity = mediaRestoreOperationEntity)

                // Update package state and end time.
                if (mediaRestoreOperationEntity.isSucceed) {
                    logUtil.log(logTag, "Restoring succeed.")
                } else {
                    logUtil.log(logTag, "Restoring failed.")
                }
                mediaRestoreOperationEntity.state = mediaRestoreOperationEntity.isSucceed
                mediaRestoreOperationEntity.endTimestamp = DateUtil.getTimestamp()
                mediaDao.upsertRestoreOp(mediaRestoreOperationEntity)
            }

            context.saveLastRestoringTime(timestamp)
            remoteRootService.destroyService()
        }
    }

}
