package com.xayah.databackup.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.command.CommonUtil.runOnIO
import com.xayah.databackup.util.command.OperationUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.librootservice.service.RemoteRootService
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

    suspend fun backupPackages(timestamp: Long) {
        val logTag = "Packages backup"

        runOnIO {
            mutex.withLock {
                val context = applicationContext
                val remoteRootService = RemoteRootService(context)
                val operationUtil = OperationUtil(context, timestamp, logUtil, remoteRootService, packageBackupOperationDao)

                logUtil.log(logTag, "Backup started, timestamp: $timestamp, date: ${DateUtil.formatTimestamp(timestamp)}")

                /**
                 * Somehow the keyboards and accessibility services
                 * will be changed after backing up on some devices,
                 * so we restore them manually.
                 */
                val (_, keyboard) = PreparationUtil.getKeyboard()
                val (_, services) = PreparationUtil.getAccessibilityServices()
                logUtil.log(logTag, "Keyboard: $keyboard")
                logUtil.log(logTag, "Services: $services")

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
                    packageBackupOperation.packageState = packageBackupOperation.isSucceed
                    packageBackupOperation.endTimestamp = DateUtil.getTimestamp()
                    packageBackupOperationDao.upsert(packageBackupOperation)
                }
            }
        }
    }
}
