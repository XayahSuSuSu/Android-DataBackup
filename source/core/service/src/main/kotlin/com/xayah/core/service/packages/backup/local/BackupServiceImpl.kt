package com.xayah.core.service.packages.backup.local

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.xayah.core.data.repository.PackageRestoreRepository
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.datastore.readResetBackupList
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.model.BackupPreprocessing
import com.xayah.core.service.util.PackagesBackupUtil
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
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import javax.inject.Inject
import com.xayah.core.util.LogUtil.log as KLog

@AndroidEntryPoint
internal class BackupServiceImpl : Service() {
    companion object {
        private const val TAG = "PackagesBackupServiceImpl"
    }

    private val binder = OperationLocalBinder()

    override fun onBind(intent: Intent): IBinder {
        startForeground(1, NotificationUtil.getForegroundNotification(applicationContext))
        return binder
    }

    inner class OperationLocalBinder : Binder() {
        fun getService(): BackupServiceImpl = this@BackupServiceImpl
    }

    private val mutex = Mutex()
    private val context by lazy { applicationContext }
    private fun log(msg: () -> String) = KLog { TAG to msg() }

    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var pathUtil: PathUtil

    @Inject
    lateinit var packagesBackupUtil: PackagesBackupUtil

    @Inject
    lateinit var packageBackupDao: PackageBackupEntireDao

    @Inject
    lateinit var packageRestoreDao: PackageRestoreEntireDao

    @Inject
    lateinit var packageBackupOpDao: PackageBackupOperationDao

    @Inject
    lateinit var packageRestoreRepository: PackageRestoreRepository

    suspend fun preprocessing(): BackupPreprocessing = withIOContext {
        mutex.withLock {
            log { "Preprocessing is starting." }

            /**
             * Somehow the input methods and accessibility services
             * will be changed after backing up on some devices,
             * so we restore them manually.
             */
            val backupPreprocessing = BackupPreprocessing(inputMethods = "", accessibilityServices = "")

            PreparationUtil.getInputMethods().also { result ->
                backupPreprocessing.inputMethods = result.outString.trim()
            }
            PreparationUtil.getAccessibilityServices().also { result ->
                backupPreprocessing.accessibilityServices = result.outString.trim()
            }
            log { "InputMethods: ${backupPreprocessing.inputMethods}." }
            log { "AccessibilityServices: ${backupPreprocessing.accessibilityServices}." }
            backupPreprocessing
        }
    }

    @ExperimentalSerializationApi
    suspend fun processing(timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "Processing is starting." }

            val archivesPackagesDir = pathUtil.getLocalBackupArchivesPackagesDir()
            val configsDir = pathUtil.getLocalBackupConfigsDir()
            log { "Trying to create: $archivesPackagesDir." }
            log { "Trying to create: $configsDir." }
            rootService.mkdirs(archivesPackagesDir)
            rootService.mkdirs(configsDir)

            val packages = packageBackupDao.querySelectedPackages()
            log { "Task count: ${packages.size}." }
            log { "Task target timestamp: $timestamp." }
            packages.forEach { currentPackage ->
                log { "Current package: ${currentPackage.packageName}, apk: ${currentPackage.apkSelected}, data: ${currentPackage.dataSelected}." }

                val packageBackupOperation = PackageBackupOperation(
                    packageName = currentPackage.packageName,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    label = currentPackage.label,
                    packageState = OperationState.PROCESSING,
                ).also { entity -> entity.id = packageBackupOpDao.upsert(entity) }

                val dstDir = "${archivesPackagesDir}/${currentPackage.packageName}/${timestamp}"
                rootService.mkdirs(dstDir)

                if (currentPackage.apkSelected) {
                    packageBackupOpDao.upsertApk(
                        op = packageBackupOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesBackupUtil.getApkCur(currentPackage.packageName))
                    )
                    packagesBackupUtil.backupApk(packageName = currentPackage.packageName, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertApk(
                                op = packageBackupOperation,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                } else {
                    packageBackupOperation.apkOp.state = OperationState.SKIP
                }
                if (currentPackage.dataSelected) {
                    packageBackupOpDao.upsertUser(
                        op = packageBackupOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(
                            packagesBackupUtil.getDataSrc(
                                packagesBackupUtil.getDataSrcDir(DataType.PACKAGE_USER),
                                currentPackage.packageName
                            )
                        )
                    )
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_USER, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertUser(
                                op = packageBackupOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageBackupOpDao.upsertUserDe(
                        op = packageBackupOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(
                            packagesBackupUtil.getDataSrc(
                                packagesBackupUtil.getDataSrcDir(DataType.PACKAGE_USER_DE),
                                currentPackage.packageName
                            )
                        )
                    )
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_USER_DE, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertUserDe(
                                op = packageBackupOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageBackupOpDao.upsertData(
                        op = packageBackupOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(
                            packagesBackupUtil.getDataSrc(
                                packagesBackupUtil.getDataSrcDir(DataType.PACKAGE_DATA),
                                currentPackage.packageName
                            )
                        )
                    )
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_DATA, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertData(
                                op = packageBackupOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageBackupOpDao.upsertObb(
                        op = packageBackupOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(
                            packagesBackupUtil.getDataSrc(
                                packagesBackupUtil.getDataSrcDir(DataType.PACKAGE_OBB),
                                currentPackage.packageName
                            )
                        )
                    )
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_OBB, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertObb(
                                op = packageBackupOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    packageBackupOpDao.upsertMedia(
                        op = packageBackupOperation, opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(
                            packagesBackupUtil.getDataSrc(
                                packagesBackupUtil.getDataSrcDir(DataType.PACKAGE_MEDIA),
                                currentPackage.packageName
                            )
                        )
                    )
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_MEDIA, dstDir = dstDir)
                        .also { result ->
                            packageBackupOpDao.upsertMedia(
                                op = packageBackupOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
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
                packageBackupOperation.packageState = if (packageBackupOperation.isSucceed) OperationState.DONE else OperationState.ERROR
                packageBackupOperation.endTimestamp = DateUtil.getTimestamp()
                packageBackupOpDao.upsert(packageBackupOperation)

                // Insert restore config into database.
                if (packageBackupOperation.isSucceed) {
                    log { "Succeed." }

                    val restoreEntire = PackageRestoreEntire(
                        packageName = currentPackage.packageName,
                        label = currentPackage.label,
                        backupOpCode = currentPackage.operationCode,
                        timestamp = timestamp,
                        versionName = currentPackage.versionName,
                        versionCode = currentPackage.versionCode,
                        flags = currentPackage.flags,
                        compressionType = packagesBackupUtil.compressionType,
                        savePath = context.localBackupSaveDir(),
                    )
                    packageRestoreDao.upsert(restoreEntire)

                    // Save config
                    rootService.writeProtoBuf(data = restoreEntire, dst = PathUtil.getPackageRestoreConfigDst(dstDir = dstDir))

                    // Reset selected items if enabled.
                    if (context.readResetBackupList().first()) {
                        currentPackage.operationCode = OperationMask.None
                        packageBackupDao.update(currentPackage)
                    }
                } else {
                    log { "Failed." }
                }
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun postProcessing(backupPreprocessing: BackupPreprocessing, timestamp: Long) = withIOContext {
        mutex.withLock {
            log { "PostProcessing is starting." }

            // Restore keyboard and services.
            if (backupPreprocessing.inputMethods.isNotEmpty()) {
                PreparationUtil.setInputMethods(inputMethods = backupPreprocessing.inputMethods)
                log { "InputMethods restored: ${backupPreprocessing.inputMethods}." }
            } else {
                log { "InputMethods is empty, skip restoring." }
            }
            if (backupPreprocessing.accessibilityServices.isNotEmpty()) {
                PreparationUtil.setAccessibilityServices(accessibilityServices = backupPreprocessing.accessibilityServices)
                log { "AccessibilityServices restored: ${backupPreprocessing.accessibilityServices}." }
            } else {
                log { "AccessibilityServices is empty, skip restoring." }
            }

            val dstDir = context.localBackupSaveDir()
            // Backup itself if enabled.
            if (context.readBackupItself().first()) {
                log { "Backup itself enabled." }
                packagesBackupUtil.backupItself(dstDir = dstDir)
            }

            val configsDstDir = pathUtil.getConfigsDir(dstDir)
            // Backup others.
            log { "Save icons." }
            packagesBackupUtil.backupIcons(dstDir = configsDstDir)

            log { "Save configs." }
            val configsDst = PathUtil.getPackageRestoreConfigDst(dstDir = configsDstDir)
            packageRestoreRepository.writePackagesProtoBuf(configsDst) { storedList ->
                storedList.apply { addAll(packageRestoreDao.queryPackages(timestamp)) }.toList()
            }
        }
    }
}
