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
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.OperationUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.databasePath
import com.xayah.databackup.util.iconPath
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

    suspend fun backupPackagesPreparation(): BackupPreparation = withIOContext {
        mutex.withLock {
            val logTag = "Packages backup preparation"
            logUtil.log(logTag, "Started.")

            /**
             * Somehow the keyboards and accessibility services
             * will be changed after backing up on some devices,
             * so we restore them manually.
             */
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
            val operationUtil = OperationUtil(context, timestamp, logUtil, remoteRootService, packageBackupOperationDao)

            logUtil.log(logTag, "Started.")
            val packages = packageBackupEntireDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                val packageName = currentPackage.packageName
                val isApkSelected = OperationMask.isApkSelected(currentPackage.operationCode)
                val isDataSelected = OperationMask.isDataSelected(currentPackage.operationCode)
                logUtil.log(logTag, "Current package: $packageName")
                logUtil.log(logTag, "isApkSelected: $isApkSelected")
                logUtil.log(logTag, "isDataSelected: $isDataSelected")
                remoteRootService.mkdirs(operationUtil.getPackageItemSavePath(packageName))

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
                    operationUtil.backupApk(packageBackupOperation, packageName)
                if (isDataSelected) {
                    operationUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER)
                    operationUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_USER_DE)
                    operationUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_DATA)
                    operationUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_OBB)
                    operationUtil.backupData(packageBackupOperation, packageName, DataType.PACKAGE_MEDIA)
                }

                // Update package state and end time.
                if (packageBackupOperation.isSucceed)
                    logUtil.log(logTag, "Backup succeed.")
                else
                    logUtil.log(logTag, "Backup failed.")
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
                    )
                    packageRestoreEntireDao.upsert(restoreEntire)
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
}
