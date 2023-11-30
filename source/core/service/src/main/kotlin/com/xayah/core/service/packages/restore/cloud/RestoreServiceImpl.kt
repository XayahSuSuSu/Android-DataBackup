package com.xayah.core.service.packages.restore.cloud

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.gson.reflect.TypeToken
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.dao.PackageRestoreOperationDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreOperation
import com.xayah.core.datastore.readRcloneMainAccountRemote
import com.xayah.core.datastore.readResetRestoreList
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.model.RcloneSizeInfo
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.service.util.PackagesRestoreUtil
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
    lateinit var gsonUtil: GsonUtil

    @Inject
    lateinit var packagesRestoreUtil: PackagesRestoreUtil

    @Inject
    lateinit var packageRestoreDao: PackageRestoreEntireDao

    @Inject
    lateinit var packageRestoreOpDao: PackageRestoreOperationDao

    @Inject
    lateinit var cloudRepository: CloudRepository

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

            val remote = context.readRcloneMainAccountRemote().first()
            val archivesPackagesRelativeDir = PathUtil.getArchivesPackagesRelativeDir()
            val tmpArchivesPackagesDir = "$CloudTmpAbsoluteDir/$archivesPackagesRelativeDir"
            val remoteArchivesPackagesDir = "$remote/$archivesPackagesRelativeDir"

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

                val tmpSrcDir = "${tmpArchivesPackagesDir}/${currentPackage.packageName}/${currentPackage.timestamp}"
                val remoteSrcDir = "${remoteArchivesPackagesDir}/${currentPackage.packageName}/${currentPackage.timestamp}"

                if (currentPackage.apkSelected) {
                    var sizeBytes: Long = 0
                    val remoteApkSrc = packagesRestoreUtil.getApkSrc(srcDir = remoteSrcDir, compressionType = currentPackage.compressionType)
                    Rclone.size(src = remoteApkSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            sizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteApkSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertApk(
                        op = packageRestoreOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = sizeBytes
                    )
                    cloudRepository.download(src = remoteApkSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreApk(
                            packageName = currentPackage.packageName,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertApk(
                                op = packageRestoreOperation,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
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
                    // User
                    var userSizeBytes: Long = 0
                    val remoteUserSrc = packagesRestoreUtil.getDataSrc(remoteSrcDir, DataType.PACKAGE_USER, currentPackage.compressionType)
                    Rclone.size(src = remoteUserSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            userSizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteUserSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertUser(
                        op = packageRestoreOperation,
                        opState = OperationState.PROCESSING,
                        opBytes = userSizeBytes
                    )
                    cloudRepository.download(src = remoteUserSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreData(
                            packageName = currentPackage.packageName,
                            dataType = DataType.PACKAGE_USER,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertUser(
                                op = packageRestoreOperation,
                                opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
                        packageRestoreOpDao.upsertUser(
                            op = packageRestoreOperation,
                            opState = if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                            opLog = result.outString
                        )
                    }

                    // UserDe
                    var userDeSizeBytes: Long = 0
                    val remoteUserDeSrc = packagesRestoreUtil.getDataSrc(remoteSrcDir, DataType.PACKAGE_USER_DE, currentPackage.compressionType)
                    Rclone.size(src = remoteUserDeSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            userDeSizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteUserDeSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertUserDe(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = userDeSizeBytes
                    )
                    cloudRepository.download(src = remoteUserDeSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreData(
                            packageName = currentPackage.packageName,
                            dataType = DataType.PACKAGE_USER_DE,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertUserDe(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
                        packageRestoreOpDao.upsertUserDe(
                            op = packageRestoreOperation,
                            opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                            opLog = result.outString
                        )
                    }

                    // Data
                    var dataSizeBytes: Long = 0
                    val remoteDataSrc = packagesRestoreUtil.getDataSrc(remoteSrcDir, DataType.PACKAGE_DATA, currentPackage.compressionType)
                    Rclone.size(src = remoteDataSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            dataSizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteDataSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertData(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = dataSizeBytes
                    )
                    cloudRepository.download(src = remoteDataSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreData(
                            packageName = currentPackage.packageName,
                            dataType = DataType.PACKAGE_DATA,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertData(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
                        packageRestoreOpDao.upsertData(
                            op = packageRestoreOperation,
                            opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                            opLog = result.outString
                        )
                    }

                    // Obb
                    var obbSizeBytes: Long = 0
                    val remoteObbSrc = packagesRestoreUtil.getDataSrc(remoteSrcDir, DataType.PACKAGE_OBB, currentPackage.compressionType)
                    Rclone.size(src = remoteObbSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            obbSizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteObbSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertObb(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = obbSizeBytes
                    )
                    cloudRepository.download(src = remoteObbSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreData(
                            packageName = currentPackage.packageName,
                            dataType = DataType.PACKAGE_OBB,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertObb(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
                        packageRestoreOpDao.upsertObb(
                            op = packageRestoreOperation,
                            opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                            opLog = result.outString
                        )
                    }

                    // Media
                    var mediaSizeBytes: Long = 0
                    val remoteMediaSrc = packagesRestoreUtil.getDataSrc(remoteSrcDir, DataType.PACKAGE_MEDIA, currentPackage.compressionType)
                    Rclone.size(src = remoteMediaSrc).also { result ->
                        runCatching {
                            val type = object : TypeToken<RcloneSizeInfo>() {}.type
                            val info = gsonUtil.fromJson<RcloneSizeInfo>(result.outString, type)
                            mediaSizeBytes = info.bytes
                        }.onFailure {
                            log { "Failed to calculate the total size of $remoteMediaSrc." }
                        }
                    }
                    packageRestoreOpDao.upsertMedia(
                        op = packageRestoreOperation, opState = OperationState.PROCESSING,
                        opBytes = mediaSizeBytes
                    )
                    cloudRepository.download(src = remoteMediaSrc, dstDir = tmpSrcDir, onDownloaded = {
                        packagesRestoreUtil.restoreData(
                            packageName = currentPackage.packageName,
                            dataType = DataType.PACKAGE_MEDIA,
                            srcDir = tmpSrcDir,
                            compressionType = currentPackage.compressionType
                        ).also { result ->
                            packageRestoreOpDao.upsertMedia(
                                op = packageRestoreOperation,
                                opState = if (result.code == -2) OperationState.SKIP else if (result.isSuccess) OperationState.DONE else OperationState.ERROR,
                                opLog = result.outString
                            )
                        }
                    }).also { result ->
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
