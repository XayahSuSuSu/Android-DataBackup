package com.xayah.core.service.packages.backup.cloud

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.gson.reflect.TypeToken
import com.xayah.core.common.util.toLineString
import com.xayah.core.common.util.trim
import com.xayah.core.data.repository.PackageRestoreRepository
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.datastore.readBackupUserId
import com.xayah.core.datastore.readRcloneMainAccountRemote
import com.xayah.core.datastore.readResetBackupList
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.model.RcloneSizeInfo
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
import com.xayah.core.util.CloudTmpAbsoluteDir
import com.xayah.core.util.DateUtil
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.NotificationUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.PreparationUtil
import com.xayah.core.util.command.Rclone
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
    lateinit var gsonUtil: GsonUtil

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

            val userId = context.readBackupUserId().first()
            val remote = context.readRcloneMainAccountRemote().first()
            val archivesPackagesRelativeDir = PathUtil.getArchivesPackagesRelativeDir()
            val configsRelativeDir = PathUtil.getConfigsRelativeDir()
            val tmpArchivesPackagesDir = "$CloudTmpAbsoluteDir/$archivesPackagesRelativeDir"
            val tmpConfigsDir = "$CloudTmpAbsoluteDir/$configsRelativeDir"
            val remoteArchivesPackagesDir = "$remote/$archivesPackagesRelativeDir"
            val remoteConfigsDir = "$CloudTmpAbsoluteDir/$configsRelativeDir"
            log { "Trying to create: $tmpArchivesPackagesDir." }
            log { "Trying to create: $tmpConfigsDir." }
            rootService.mkdirs(tmpArchivesPackagesDir)
            rootService.mkdirs(tmpConfigsDir)
            log { "Trying to create: $remoteArchivesPackagesDir." }
            log { "Trying to create: $remoteConfigsDir." }
            Rclone.mkdir(remoteArchivesPackagesDir)
            Rclone.mkdir(remoteConfigsDir)

            val packages = packageBackupDao.querySelectedPackages()
            log { "Task count: ${packages.size}." }
            log { "Task target timestamp: $timestamp." }
            packages.forEach { currentPackage ->
                log { "Current package: ${currentPackage.packageName}, apk: ${currentPackage.apkSelected}, data: ${currentPackage.dataSelected}." }

                // Kill the package.
                log { "Trying to kill ${currentPackage.packageName}." }
                BaseUtil.killPackage(userId = userId, packageName = currentPackage.packageName)

                val packageBackupOperation = PackageBackupOperation(
                    packageName = currentPackage.packageName,
                    timestamp = timestamp,
                    startTimestamp = DateUtil.getTimestamp(),
                    endTimestamp = 0,
                    label = currentPackage.label,
                    packageState = OperationState.PROCESSING,
                ).also { entity -> entity.id = packageBackupOpDao.upsert(entity) }

                val tmpDstDir = "${tmpArchivesPackagesDir}/${currentPackage.packageName}/${timestamp}"
                val remoteDstDir = "${remoteArchivesPackagesDir}/${currentPackage.packageName}/${timestamp}"
                rootService.mkdirs(tmpDstDir)
                Rclone.mkdir(remoteDstDir)

                if (currentPackage.apkSelected) {
                    packageBackupOpDao.upsertApk(
                        op = packageBackupOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = rootService.calculateSize(packagesBackupUtil.getApkCur(currentPackage.packageName))
                    )
                    packagesBackupUtil.backupApk(packageName = currentPackage.packageName, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.isSuccess) {
                                packageBackupOpDao.upsertApk(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(src = packagesBackupUtil.getApkDst(dstDir = tmpDstDir), dstDir = remoteDstDir)
                                    .also {
                                        packageBackupOpDao.upsertApk(
                                            op = packageBackupOperation,
                                            opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                            opLog = (result.out + it.out).trim().toLineString()
                                        )
                                    }
                            } else {
                                packageBackupOpDao.upsertApk(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_USER, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.code == -2) {
                                packageBackupOpDao.upsertUser(
                                    op = packageBackupOperation,
                                    opState = OperationState.SKIP,
                                    opLog = result.outString
                                )
                            } else if (result.isSuccess) {
                                packageBackupOpDao.upsertUser(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(
                                    src = packagesBackupUtil.getDataDst(dstDir = tmpDstDir, dataType = DataType.PACKAGE_USER),
                                    dstDir = remoteDstDir
                                ).also {
                                    packageBackupOpDao.upsertUser(
                                        op = packageBackupOperation,
                                        opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                        opLog = (result.out + it.out).trim().toLineString()
                                    )
                                }
                            } else {
                                packageBackupOpDao.upsertUser(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_USER_DE, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.code == -2) {
                                packageBackupOpDao.upsertUserDe(
                                    op = packageBackupOperation,
                                    opState = OperationState.SKIP,
                                    opLog = result.outString
                                )
                            } else if (result.isSuccess) {
                                packageBackupOpDao.upsertUserDe(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(
                                    src = packagesBackupUtil.getDataDst(dstDir = tmpDstDir, dataType = DataType.PACKAGE_USER_DE),
                                    dstDir = remoteDstDir
                                ).also {
                                    packageBackupOpDao.upsertUserDe(
                                        op = packageBackupOperation,
                                        opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                        opLog = (result.out + it.out).trim().toLineString()
                                    )
                                }
                            } else {
                                packageBackupOpDao.upsertUserDe(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_DATA, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.code == -2) {
                                packageBackupOpDao.upsertData(
                                    op = packageBackupOperation,
                                    opState = OperationState.SKIP,
                                    opLog = result.outString
                                )
                            } else if (result.isSuccess) {
                                packageBackupOpDao.upsertData(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(
                                    src = packagesBackupUtil.getDataDst(dstDir = tmpDstDir, dataType = DataType.PACKAGE_DATA),
                                    dstDir = remoteDstDir
                                ).also {
                                    packageBackupOpDao.upsertData(
                                        op = packageBackupOperation,
                                        opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                        opLog = (result.out + it.out).trim().toLineString()
                                    )
                                }
                            } else {
                                packageBackupOpDao.upsertData(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_OBB, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.code == -2) {
                                packageBackupOpDao.upsertObb(
                                    op = packageBackupOperation,
                                    opState = OperationState.SKIP,
                                    opLog = result.outString
                                )
                            } else if (result.isSuccess) {
                                packageBackupOpDao.upsertObb(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(
                                    src = packagesBackupUtil.getDataDst(dstDir = tmpDstDir, dataType = DataType.PACKAGE_OBB),
                                    dstDir = remoteDstDir
                                ).also {
                                    packageBackupOpDao.upsertObb(
                                        op = packageBackupOperation,
                                        opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                        opLog = (result.out + it.out).trim().toLineString()
                                    )
                                }
                            } else {
                                packageBackupOpDao.upsertObb(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                    packagesBackupUtil.backupData(packageName = currentPackage.packageName, dataType = DataType.PACKAGE_MEDIA, dstDir = tmpDstDir)
                        .also { result ->
                            if (result.code == -2) {
                                packageBackupOpDao.upsertMedia(
                                    op = packageBackupOperation,
                                    opState = OperationState.SKIP,
                                    opLog = result.outString
                                )
                            } else if (result.isSuccess) {
                                packageBackupOpDao.upsertMedia(
                                    op = packageBackupOperation,
                                    opState = OperationState.UPLOADING,
                                )
                                packagesBackupUtil.upload(
                                    src = packagesBackupUtil.getDataDst(dstDir = tmpDstDir, dataType = DataType.PACKAGE_MEDIA),
                                    dstDir = remoteDstDir
                                ).also {
                                    packageBackupOpDao.upsertMedia(
                                        op = packageBackupOperation,
                                        opState = if (it.isSuccess) OperationState.DONE else OperationState.ERROR,
                                        opLog = (result.out + it.out).trim().toLineString()
                                    )
                                }
                            } else {
                                packageBackupOpDao.upsertMedia(
                                    op = packageBackupOperation,
                                    opState = OperationState.ERROR,
                                    opLog = result.outString
                                )
                            }
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
                        savePath = remote,
                    ).apply {
                        Rclone.size(src = remoteDstDir).also { result ->
                            runCatching {
                                val type = object : TypeToken<RcloneSizeInfo>() {}.type
                                val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                                sizeBytes = info.bytes
                            }.onFailure {
                                log { "Failed to calculate the total size of $remoteDstDir." }
                            }
                        }
                    }
                    packageRestoreDao.upsert(restoreEntire)

                    // Save config
                    val tmpConfigPath = PathUtil.getPackageRestoreConfigDst(tmpDstDir)
                    rootService.writeProtoBuf(data = restoreEntire, dst = tmpConfigPath)
                    packagesBackupUtil.upload(src = tmpConfigPath, dstDir = remoteDstDir)

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

            val remoteDir = context.readRcloneMainAccountRemote().first()
            val tmpDstDir = CloudTmpAbsoluteDir

            // Backup itself if enabled.
            if (context.readBackupItself().first()) {
                log { "Backup itself enabled." }
                packagesBackupUtil.backupItself(dstDir = tmpDstDir)
                val src = packagesBackupUtil.getItselfDst(tmpDstDir)
                packagesBackupUtil.upload(src = src, dstDir = remoteDir)
            }

            val tmpConfigsDstDir = pathUtil.getConfigsDir(tmpDstDir)
            val remoteConfigsDstDir = pathUtil.getConfigsDir(remoteDir)
            // Backup others.
            log { "Save icons." }
            packagesBackupUtil.backupIcons(dstDir = tmpConfigsDstDir)
            val src = packagesBackupUtil.getIconsDst(dstDir = tmpConfigsDstDir)
            packagesBackupUtil.upload(src = src, dstDir = remoteConfigsDstDir)

            log { "Save configs." }
            val configsDst = PathUtil.getPackageRestoreConfigDst(dstDir = tmpConfigsDstDir)
            packageRestoreRepository.writePackagesProtoBuf(configsDst) { storedList ->
                storedList.apply { addAll(packageRestoreDao.queryPackages(timestamp)) }.toList()
            }
            packagesBackupUtil.upload(src = configsDst, dstDir = remoteConfigsDstDir)

            rootService.deleteRecursively(CloudTmpAbsoluteDir)
        }
    }
}
