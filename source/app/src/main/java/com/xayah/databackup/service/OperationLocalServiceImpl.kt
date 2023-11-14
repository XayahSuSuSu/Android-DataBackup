package com.xayah.databackup.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.MediaRestoreOperationEntity
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.database.model.PackageRestoreOperation
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.util.DateUtil
import com.xayah.core.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.IAdditionUtilFactory
import com.xayah.databackup.util.command.IMediumBackupAfterwardsUtilFactory
import com.xayah.databackup.util.command.IMediumBackupUtilFactory
import com.xayah.databackup.util.command.IMediumRestoreUtilFactory
import com.xayah.databackup.util.command.IPackagesBackupAfterwardsUtilFactory
import com.xayah.databackup.util.command.IPackagesBackupUtilFactory
import com.xayah.databackup.util.command.IPackagesRestoreUtilFactory
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.command.getSavePath
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

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var packagesBackupUtilFactory: IPackagesBackupUtilFactory

    @Inject
    lateinit var packagesBackupAfterwardsUtilFactory: IPackagesBackupAfterwardsUtilFactory

    @Inject
    lateinit var packagesRestoreUtilFactory: IPackagesRestoreUtilFactory

    @Inject
    lateinit var mediumBackupUtilFactory: IMediumBackupUtilFactory

    @Inject
    lateinit var mediumRestoreUtilFactory: IMediumRestoreUtilFactory

    @Inject
    lateinit var additionUtilFactory: IAdditionUtilFactory

    @Inject
    lateinit var mediumBackupAfterwardsUtilFactory: IMediumBackupAfterwardsUtilFactory

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

    suspend fun backupPackages(timestamp: Long, cloudMode: Boolean) = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup"
            val context = applicationContext

            logUtil.log(logTag, "Started.")
            val packages = packageBackupEntireDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                val packageName = currentPackage.packageName
                val isApkSelected = OperationMask.isApkSelected(currentPackage.operationCode)
                val isDataSelected = OperationMask.isDataSelected(currentPackage.operationCode)
                logUtil.log(logTag, "Current package: $packageName")
                logUtil.log(logTag, "isApkSelected: $isApkSelected")
                logUtil.log(logTag, "isDataSelected: $isDataSelected")

                val packageBackupOperation = PackageBackupOperation(
                    packageName = packageName,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    label = currentPackage.label,
                    packageState = OperationState.PROCESSING,
                ).also { entity -> entity.id = packageBackupOperationDao.upsert(entity) }

                /**
                 * All I/O related ops are in [com.xayah.databackup.util.command.PackagesBackupUtil].
                 */
                val packagesUtil = packagesBackupUtilFactory.createBackupUtil(cloudMode = cloudMode, entity = packageBackupOperation)
                packagesUtil.mkdirs()
                if (isApkSelected) {
                    packagesUtil.backupApk()
                } else {
                    packageBackupOperation.apkOp.state = OperationState.SKIP
                }
                if (isDataSelected) {
                    packagesUtil.backupData(DataType.PACKAGE_USER)
                    packagesUtil.backupData(DataType.PACKAGE_USER_DE)
                    packagesUtil.backupData(DataType.PACKAGE_DATA)
                    packagesUtil.backupData(DataType.PACKAGE_OBB)
                    packagesUtil.backupData(DataType.PACKAGE_MEDIA)

                } else {
                    packageBackupOperation.apply {
                        userOp.state = OperationState.SKIP
                        userDeOp.state = OperationState.SKIP
                        dataOp.state = OperationState.SKIP
                        obbOp.state = OperationState.SKIP
                        mediaOp.state = OperationState.SKIP
                    }
                }

                // Update package state and end time.
                if (packageBackupOperation.isSucceed) {
                    logUtil.log(logTag, "Backup succeed.")
                } else {
                    logUtil.log(logTag, "Backup failed.")
                }
                packageBackupOperation.packageState = if (packageBackupOperation.isSucceed) OperationState.DONE else OperationState.ERROR
                packageBackupOperation.endTimestamp = DateUtil.getTimestamp()
                packageBackupOperationDao.upsert(packageBackupOperation)

                // Insert restore config into database.
                if (packageBackupOperation.isSucceed) {
                    val restoreEntire = PackageRestoreEntire(
                        packageName = currentPackage.packageName,
                        label = currentPackage.label,
                        backupOpCode = currentPackage.operationCode,
                        timestamp = timestamp,
                        versionName = currentPackage.versionName,
                        versionCode = currentPackage.versionCode,
                        flags = currentPackage.flags,
                        compressionType = context.readCompressionType(),
                        savePath = if (cloudMode) packagesUtil.getSavePath() else PathUtil.getBackupSavePath(false),
                    )
                    packageRestoreEntireDao.upsert(restoreEntire)

                    // Save config
                    packagesUtil.backupConfig(restoreEntire)

                    // Reset selected items if enabled.
                    if (context.readResetBackupList()) {
                        currentPackage.operationCode = OperationMask.None
                        packageBackupEntireDao.update(currentPackage)
                    }
                }
            }

            context.saveLastBackupTime(timestamp)
        }
    }

    suspend fun backupPackagesAfterwards(preparation: BackupPreparation, cloudMode: Boolean) = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup afterwards"
            logUtil.log(logTag, "Started.")

            /**
             * All I/O related ops are in [com.xayah.databackup.util.command.PackagesBackupAfterwardsUtil].
             */
            val packagesUtil = packagesBackupAfterwardsUtilFactory.createBackupAfterwardsUtil(logTag = logTag, cloudMode = cloudMode)

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
            val additionUtil = additionUtilFactory.createAdditionUtil(cloudMode = cloudMode, logTag = logTag)
            additionUtil.backupItself(packageName = packageName)

            // Backup others.
            packagesUtil.backupIcons()
            packagesUtil.backupConfigs()

            // Clear up
            packagesUtil.clearUp()
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

                val packageRestoreOperation = PackageRestoreOperation(
                    packageName = packageName,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    label = currentPackage.label,
                    packageState = OperationState.PROCESSING,
                ).also { entity ->
                    entity.id = packageRestoreOperationDao.upsert(entity)
                }

                /**
                 * All I/O related ops are in [com.xayah.databackup.util.command.PackagesRestoreUtil].
                 */
                val packagesUtil = packagesRestoreUtilFactory.createPackagesRestoreUtil(
                    timestamp = packageTimestamp,
                    entity = packageRestoreOperation,
                    compressionType = compressionType
                )

                if (isApkSelected) {
                    packagesUtil.restoreApk()
                } else {
                    packagesUtil.queryInstalled()
                }
                if (isDataSelected) {
                    packagesUtil.restoreData(DataType.PACKAGE_USER)
                    packagesUtil.restoreData(DataType.PACKAGE_USER_DE)
                    packagesUtil.restoreData(DataType.PACKAGE_DATA)
                    packagesUtil.restoreData(DataType.PACKAGE_OBB)
                    packagesUtil.restoreData(DataType.PACKAGE_MEDIA)
                } else {
                    packageRestoreOperation.apply {
                        userOp.state = OperationState.SKIP
                        userDeOp.state = OperationState.SKIP
                        dataOp.state = OperationState.SKIP
                        obbOp.state = OperationState.SKIP
                        mediaOp.state = OperationState.SKIP
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
                packageRestoreOperation.packageState = if (packageRestoreOperation.isSucceed) OperationState.DONE else OperationState.ERROR
                packageRestoreOperation.endTimestamp = DateUtil.getTimestamp()
                packageRestoreOperationDao.upsert(packageRestoreOperation)
            }

            context.saveLastRestoringTime(timestamp)
        }
    }

    suspend fun backupMedium(timestamp: Long, cloudMode: Boolean) = withIOContext {
        mutex.withLock {
            val logTag = "Media backup"
            val context = applicationContext

            logUtil.log(logTag, "Started.")
            val medium = mediaDao.queryBackupSelected()
            medium.forEach { current ->
                val name = current.name
                val path = current.path
                logUtil.log(logTag, "Current media: ${name}: $path")

                val mediaBackupOperationEntity = MediaBackupOperationEntity(
                    id = 0,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    path = path,
                    name = name,
                    mediaState = OperationState.PROCESSING,
                ).also { entity ->
                    entity.id = mediaDao.upsertBackupOp(entity)
                }

                /**
                 * All I/O related ops are in [com.xayah.databackup.util.command.MediumBackupUtil].
                 */
                val mediumBackupUtil = mediumBackupUtilFactory.createMediumBackupUtil(
                    entity = mediaBackupOperationEntity,
                )
                mediumBackupUtil.mkdirs()
                mediumBackupUtil.backupMedia()

                // Update package state and end time.
                if (mediaBackupOperationEntity.isSucceed) {
                    logUtil.log(logTag, "Backup succeed.")
                } else {
                    logUtil.log(logTag, "Backup failed.")
                }
                mediaBackupOperationEntity.mediaState = if (mediaBackupOperationEntity.isSucceed) OperationState.DONE else OperationState.ERROR
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
                        savePath = if (cloudMode) "" else PathUtil.getBackupSavePath(false),
                    )
                    mediaDao.upsertRestore(restoreEntire)

                    // Save config
                    mediumBackupUtil.backupConfig(restoreEntire)

                    // Reset selected items if enabled.
                    if (context.readResetBackupList()) {
                        current.selected = false
                        mediaDao.upsertBackup(current)
                    }
                }
            }

            // Backup itself if enabled.
            val additionUtil = additionUtilFactory.createAdditionUtil(cloudMode = cloudMode, logTag = logTag)
            additionUtil.backupItself(packageName = packageName)

            context.saveLastBackupTime(timestamp)
        }
    }

    suspend fun backupMediumAfterwards(cloudMode: Boolean) = withIOContext {
        mutex.withLock {
            val logTag = "Medium backup afterwards"
            logUtil.log(logTag, "Started.")

            /**
             * All I/O related ops are in [com.xayah.databackup.util.command.PackagesBackupAfterwardsUtil].
             */
            val packagesUtil = mediumBackupAfterwardsUtilFactory.createMediumBackupAfterwardsUtil(logTag = logTag, cloudMode = cloudMode)

            packagesUtil.backupConfigs()
        }
    }

    suspend fun restoreMedium(timestamp: Long) = withIOContext {
        mutex.withLock {
            val logTag = "Media restore"
            val context = applicationContext

            logUtil.log(logTag, "Started.")
            val medium = mediaDao.queryRestoreSelected()
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
                    mediaState = OperationState.PROCESSING,
                ).also { entity ->
                    entity.id = mediaDao.upsertRestoreOp(entity)
                }

                /**
                 * All I/O related ops are in [com.xayah.databackup.util.command.MediumRestoreUtil].
                 */
                val mediumRestoreUtil = mediumRestoreUtilFactory.createMediumRestoreUtil(
                    entity = mediaRestoreOperationEntity,
                )
                mediumRestoreUtil.restoreMedia()

                // Update package state and end time.
                if (mediaRestoreOperationEntity.isSucceed) {
                    logUtil.log(logTag, "Restoring succeed.")
                } else {
                    logUtil.log(logTag, "Restoring failed.")
                }
                mediaRestoreOperationEntity.mediaState = if (mediaRestoreOperationEntity.isSucceed) OperationState.DONE else OperationState.ERROR
                mediaRestoreOperationEntity.endTimestamp = DateUtil.getTimestamp()
                mediaDao.upsertRestoreOp(mediaRestoreOperationEntity)
            }

            context.saveLastRestoringTime(timestamp)
        }
    }

}
