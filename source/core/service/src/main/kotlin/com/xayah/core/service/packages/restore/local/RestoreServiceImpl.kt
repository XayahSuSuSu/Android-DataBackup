package com.xayah.core.service.packages.restore.local

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreOperation
import com.xayah.core.datastore.readResetRestoreList
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.util.PackagesRestoreUtil
import com.xayah.core.service.util.upsertApk
import com.xayah.core.service.util.upsertData
import com.xayah.core.service.util.upsertMedia
import com.xayah.core.service.util.upsertObb
import com.xayah.core.service.util.upsertUser
import com.xayah.core.service.util.upsertUserDe
import com.xayah.core.util.DateUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.PreparationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject
import com.xayah.core.util.LogUtil.log as KLog

@AndroidEntryPoint
internal class RestoreServiceImpl : Service() {
    companion object {
        private const val TAG = "PackagesRestoreServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): RestoreServiceImpl = this@RestoreServiceImpl
    }

    private val mutex = Mutex()
    private val context by lazy { applicationContext }
    private fun log(msg: () -> String) = KLog { TAG to msg() }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    @Inject
    lateinit var packagesRestoreUtil: PackagesRestoreUtil

    @Inject
    lateinit var packageRestoreDao: PackageRestoreEntireDao

    @Inject
    lateinit var packageRestoreOpDao: PackageRestoreOperationDao

    suspend fun preprocessing() = withIOContext {
        mutex.withLock {
            log { "Preprocessing is starting." }

            log { "Trying to enable adb install permissions." }
            PreparationUtil.setInstallEnv()
        }
    }

    @ExperimentalSerializationApi
    suspend fun processing(timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }

            val packages = packageRestoreDao.queryActiveTotalPackages()
            packages.forEach { currentPackage ->
                log { "Current package: ${currentPackage.packageName}, apk: ${currentPackage.apkSelected}, data: ${currentPackage.dataSelected}." }

                val packageRestoreOperation = PackageRestoreOperation(
                    packageName = currentPackage.packageName,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    label = currentPackage.label,
                    packageState = OperationState.PROCESSING,
                ).also { entity -> entity.id = packageRestoreOpDao.upsert(entity) }

                val srcDir = "${pathUtil.getLocalRestoreArchivesPackagesDir()}/${currentPackage.packageName}/${currentPackage.timestamp}"

                if (currentPackage.apkSelected) {
                    packageRestoreOpDao.upsertApk(
                        op = packageRestoreOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getApkSrc(srcDir, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreApk(packageName = currentPackage.packageName, srcDir = srcDir, compressionType = currentPackage.compressionType)
                        .also { result ->
                            packageRestoreOpDao.upsertApk(
                                op = packageRestoreOperation,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                } else {
                    packageRestoreOperation.apkOp.state = OperationState.SKIP
                }
                if (currentPackage.dataSelected) {
                    packageRestoreOpDao.upsertUser(
                        op = packageRestoreOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getDataSrc(srcDir, DataType.PACKAGE_USER, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreData(
                        packageName = currentPackage.packageName,
                        dataType = DataType.PACKAGE_USER,
                        srcDir = srcDir,
                        compressionType = currentPackage.compressionType
                    )
                        .also { result ->
                            packageRestoreOpDao.upsertUser(
                                op = packageRestoreOperation,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageRestoreOpDao.upsertUserDe(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getDataSrc(srcDir, DataType.PACKAGE_USER_DE, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreData(
                        packageName = currentPackage.packageName,
                        dataType = DataType.PACKAGE_USER_DE,
                        srcDir = srcDir,
                        compressionType = currentPackage.compressionType
                    )
                        .also { result ->
                            packageRestoreOpDao.upsertUserDe(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageRestoreOpDao.upsertData(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getDataSrc(srcDir, DataType.PACKAGE_DATA, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreData(
                        packageName = currentPackage.packageName,
                        dataType = DataType.PACKAGE_DATA,
                        srcDir = srcDir,
                        compressionType = currentPackage.compressionType
                    )
                        .also { result ->
                            packageRestoreOpDao.upsertData(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageRestoreOpDao.upsertObb(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getDataSrc(srcDir, DataType.PACKAGE_OBB, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreData(
                        packageName = currentPackage.packageName,
                        dataType = DataType.PACKAGE_OBB,
                        srcDir = srcDir,
                        compressionType = currentPackage.compressionType
                    )
                        .also { result ->
                            packageRestoreOpDao.upsertObb(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageRestoreOpDao.upsertMedia(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesRestoreUtil.getDataSrc(srcDir, DataType.PACKAGE_MEDIA, currentPackage.compressionType))
                    )
                    packagesRestoreUtil.restoreData(
                        packageName = currentPackage.packageName,
                        dataType = DataType.PACKAGE_MEDIA,
                        srcDir = srcDir,
                        compressionType = currentPackage.compressionType
                    )
                        .also { result ->
                            packageRestoreOpDao.upsertMedia(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
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
                packageRestoreOperation.packageState = if (packageRestoreOperation.isSucceed) OperationState.DONE else OperationState.ERROR
                packageRestoreOperation.endTimestamp = DateUtil.getTimestamp()
                packageRestoreOpDao.upsert(packageRestoreOperation)

                // Insert restore config into database.
                if (packageRestoreOperation.isSucceed) {
                    log { "Succeed." }

                    // Reset selected items if enabled.
                    if (context.readResetRestoreList().first()) {
                        currentPackage.operationCode = OperationMask.None
                        packageRestoreDao.update(currentPackage)
                    }
                } else {
                    log { "Failed." }
                }
            }
        }
    }
}
