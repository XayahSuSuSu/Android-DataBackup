package com.xayah.databackup.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.data.PackageRestoreOperation
import com.xayah.databackup.data.PackageRestoreOperationDao
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.OperationBackupUtil
import com.xayah.databackup.util.command.OperationRestoreUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.databasePath
import com.xayah.databackup.util.iconPath
import com.xayah.databackup.util.readBackupItself
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.readResetBackupList
import com.xayah.databackup.util.readResetRestoreList
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
            val operationBackupUtil = OperationBackupUtil(context, timestamp, logUtil, remoteRootService, packageBackupOperationDao)

            logUtil.log(logTag, "Started.")
            val packages = packageBackupEntireDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                val packageName = currentPackage.packageName
                val isApkSelected = OperationMask.isApkSelected(currentPackage.operationCode)
                val isDataSelected = OperationMask.isDataSelected(currentPackage.operationCode)
                logUtil.log(logTag, "Current package: $packageName")
                logUtil.log(logTag, "isApkSelected: $isApkSelected")
                logUtil.log(logTag, "isDataSelected: $isDataSelected")
                remoteRootService.mkdirs(operationBackupUtil.getPackageItemSavePath(packageName))

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

                if (isApkSelected)
                    operationBackupUtil.backupApk(packageBackupOperation, packageName)
                if (isDataSelected) {
                    operationBackupUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER)
                    operationBackupUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER_DE)
                    operationBackupUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_DATA)
                    operationBackupUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_OBB)
                    operationBackupUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_MEDIA)
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

                    // Reset selected items if enabled.
                    if (context.readResetBackupList()) {
                        currentPackage.operationCode = OperationMask.None
                        packageBackupEntireDao.update(currentPackage)
                    }
                }
            }
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

            // Backup database and icons.
            val iconPath = context.iconPath()
            val iconSavePath = PathUtil.getIconSavePath()
            remoteRootService.copyRecursively(path = iconPath, targetPath = iconSavePath, overwrite = true)
            logUtil.log(logTag, "Copied from $iconPath to $iconSavePath.")

            val noMediaSavePath = PathUtil.getIconNoMediaSavePath()
            remoteRootService.createNewFile(path = noMediaSavePath)
            logUtil.log(logTag, "Created $noMediaSavePath.")

            val databasePath = context.databasePath()
            val databaseSavePath = PathUtil.getDatabaseSavePath()
            remoteRootService.copyRecursively(path = databasePath, targetPath = databaseSavePath, overwrite = true)
            logUtil.log(logTag, "Copied from $databasePath to $databaseSavePath.")

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
            val operationRestoreUtil = OperationRestoreUtil(context, logUtil, remoteRootService, packageRestoreOperationDao)

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

                if (isApkSelected)
                    operationRestoreUtil.restoreApk(packageRestoreOperation, packageName, packageTimestamp, compressionType)
                if (isDataSelected) {
                    operationRestoreUtil.restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_USER)
                    operationRestoreUtil.restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_USER_DE)
                    operationRestoreUtil.restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_DATA)
                    operationRestoreUtil.restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_OBB)
                    operationRestoreUtil.restoreData(packageRestoreOperation, packageName, packageTimestamp, compressionType, DataType.PACKAGE_MEDIA)
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
            remoteRootService.destroyService()
        }
    }
}
