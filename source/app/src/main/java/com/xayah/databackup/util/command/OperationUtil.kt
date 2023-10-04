package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaBackupOperationEntity
import com.xayah.databackup.data.MediaDao
import com.xayah.databackup.data.MediaRestoreEntity
import com.xayah.databackup.data.MediaRestoreOperationEntity
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.data.PackageBackupOperationDao
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreOperation
import com.xayah.databackup.data.PackageRestoreOperationDao
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCompatibleMode
import com.xayah.databackup.util.readCompressionTest
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.readRestoreUserId
import com.xayah.librootservice.service.RemoteRootService

fun List<String>.toLineString() = joinToString(separator = "\n")

class PackagesBackupUtil(
    private val context: Context,
    private val timestamp: Long,
    private val logUtil: LogUtil,
    private val remoteRootService: RemoteRootService,
    private val packageBackupOperationDao: PackageBackupOperationDao,
    private val gsonUtil: GsonUtil,
) {
    private val userId = context.readBackupUserId()
    private val compressionType = context.readCompressionType()
    private val compatibleMode = context.readCompatibleMode()
    private val packageSavePath = PathUtil.getBackupPackagesSavePath()

    fun getPackageItemSavePath(packageName: String): String = "${packageSavePath}/${packageName}/$timestamp"

    suspend fun backupApk(entity: PackageBackupOperation, packageName: String) {
        // Set processing state
        entity.apkLog = context.getString(R.string.backing_up)
        entity.apkState = OperationState.Processing
        packageBackupOperationDao.upsert(entity)

        val logTag = "APK"
        val logId = logUtil.log(logTag, "Start backing up...")
        val archivePath = "${getPackageItemSavePath(packageName)}/${DataType.PACKAGE_APK.type}.${compressionType.suffix}"
        val cmd = if (compatibleMode)
            "- ./*.apk ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.compressPara}"} > $archivePath"
        else
            "$archivePath ./*.apk ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.compressPara}$QUOTE"}"
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Get the path of apk.
        val sourceDirList = remoteRootService.getPackageSourceDir(packageName, userId)
        if (sourceDirList.isNotEmpty()) {
            val apkPath = PathUtil.getParentPath(sourceDirList[0])
            logUtil.executeWithLog(logId, "cd $apkPath").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
            logUtil.executeWithLog(logId, "tar --totals -cpf $cmd").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
            logUtil.executeWithLog(logId, "cd /").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }
        } else {
            // Failed.
            isSuccess = false
            val msg = "Failed to get apk path of $packageName."
            outList.add(msg)
            logUtil.log(logTag, msg)
        }

        // Test the archive if enabled.
        if (context.readCompressionTest()) {
            CompressionUtil.test(
                logUtil = logUtil,
                logId = logId,
                compressionType = compressionType,
                archivePath = archivePath,
                remoteRootService = remoteRootService
            ).also { (succeed, out) ->
                if (succeed.not()) {
                    isSuccess = false
                    outList.add(out)
                    logUtil.log(logTag, out)
                } else {
                    logUtil.log(logTag, "$archivePath is tested well.")
                }
            }
        }

        entity.apkLog = outList.toLineString().trim()
        entity.apkState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        packageBackupOperationDao.upsert(entity)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(entity: PackageBackupOperation, packageName: String, dataType: DataType) {
        val logTag = dataType.type.uppercase()

        // Set processing state
        if (entity.userState == OperationState.ERROR) {
            val msg = "${context.getString(R.string.failed_and_terminated)}: ${DataType.PACKAGE_USER.type.uppercase()}"
            dataType.updateEntityLog(entity, msg)
            dataType.updateEntityState(entity, OperationState.ERROR)
            logUtil.log(logTag, msg)
            packageBackupOperationDao.upsert(entity)
            return
        } else {
            dataType.updateEntityLog(entity, context.getString(R.string.backing_up))
            dataType.updateEntityState(entity, OperationState.Processing)
            packageBackupOperationDao.upsert(entity)
        }

        val logId = logUtil.log(logTag, "Start backing up...")
        val archivePath = "${getPackageItemSavePath(packageName)}/${dataType.type}.${compressionType.suffix}"
        val originPath = dataType.origin(userId)
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Check the existence of origin path.
        "$originPath/$packageName".also { path ->
            val originPathExists = remoteRootService.exists(path)
            if (originPathExists.not()) {
                if (dataType == DataType.PACKAGE_USER) {
                    val msg = "${context.getString(R.string.not_exist)}: $path"
                    dataType.updateEntityLog(entity, msg)
                    dataType.updateEntityState(entity, OperationState.ERROR)
                    logUtil.log(logTag, msg)
                } else {
                    val msg = "${context.getString(R.string.not_exist_and_skip)}: $path"
                    dataType.updateEntityLog(entity, msg)
                    dataType.updateEntityState(entity, OperationState.SKIP)
                    logUtil.log(logTag, msg)
                }
                packageBackupOperationDao.upsert(entity)
                return
            }
        }

        // Compress the dir.
        CompressionUtil.compressPackageData(logUtil, logId, compatibleMode, userId, compressionType, archivePath, packageName, dataType)
            .also { (succeed, out) ->
                if (succeed.not()) isSuccess = false
                outList.add(out)
                logUtil.log(logTag, out)
            }

        // Test the archive if enabled.
        if (context.readCompressionTest()) {
            CompressionUtil.test(
                logUtil = logUtil,
                logId = logId,
                compressionType = compressionType,
                archivePath = archivePath,
                remoteRootService = remoteRootService
            ).also { (succeed, out) ->
                if (succeed.not()) {
                    isSuccess = false
                    outList.add(out)
                    logUtil.log(logTag, out)
                } else {
                    logUtil.log(logTag, "$archivePath is tested well.")
                }
            }
        }

        dataType.updateEntityLog(entity, outList.toLineString().trim())
        dataType.updateEntityState(entity, if (isSuccess) OperationState.DONE else OperationState.ERROR)
        packageBackupOperationDao.upsert(entity)
    }

    suspend fun backupConfig(entity: PackageRestoreEntire, dataType: DataType) {
        val logTag = "Config"
        val logId = logUtil.log(logTag, "Start backing up...")
        val packageName = entity.packageName
        val archivePath = "${getPackageItemSavePath(packageName)}/${dataType.type}.${compressionType.suffix}"
        val outList = mutableListOf<String>()

        val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = packageName, timestamp = entity.timestamp)
        val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = packageName, timestamp = entity.timestamp)
        remoteRootService.deleteRecursively(tmpConfigPath)
        remoteRootService.mkdirs(tmpConfigPath)

        remoteRootService.writeText(text = gsonUtil.toJson(entity), path = tmpConfigFilePath, context = context)
        // Compress the dir.
        CompressionUtil.compressPackageConfig(logUtil, logId, compatibleMode, compressionType, archivePath, tmpConfigPath).also { (_, out) ->
            outList.add(out)
            logUtil.log(logTag, out)
        }

        remoteRootService.deleteRecursively(tmpConfigPath)

        // Test the archive if enabled.
        if (context.readCompressionTest()) {
            CompressionUtil.test(
                logUtil = logUtil,
                logId = logId,
                compressionType = compressionType,
                archivePath = archivePath,
                remoteRootService = remoteRootService
            ).also { (succeed, out) ->
                if (succeed.not()) {
                    outList.add(out)
                    logUtil.log(logTag, out)
                } else {
                    logUtil.log(logTag, "$archivePath is tested well.")
                }
            }
        }
    }
}

class PackagesRestoreUtil(
    private val context: Context,
    private val logUtil: LogUtil,
    private val remoteRootService: RemoteRootService,
    private val packageRestoreOperationDao: PackageRestoreOperationDao,
) {
    private val userId = context.readRestoreUserId()
    private val packageRestorePath = PathUtil.getRestorePackagesSavePath()

    private fun getPackageItemSavePath(packageName: String, timestamp: Long): String = "${packageRestorePath}/${packageName}/$timestamp"

    suspend fun queryInstalled(entity: PackageRestoreOperation, packageName: String) {
        val logTag = "APK"
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Check the installation.
        if (remoteRootService.queryInstalled(packageName, userId).not()) {
            isSuccess = false
            val msg = "Not installed: $packageName."
            outList.add(msg)
            logUtil.log(logTag, msg)
        }

        entity.apkLog = if (isSuccess) context.getString(R.string.installed) else outList.toLineString().trim()
        entity.apkState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        packageRestoreOperationDao.upsert(entity)
    }

    suspend fun restoreApk(entity: PackageRestoreOperation, packageName: String, timestamp: Long, compressionType: CompressionType) {
        // Set processing state
        entity.apkLog = context.getString(R.string.restoring)
        entity.apkState = OperationState.Processing
        packageRestoreOperationDao.upsert(entity)

        val logTag = "APK"
        val logId = logUtil.log(logTag, "Start restoring...")
        val archivePath = "${getPackageItemSavePath(packageName = packageName, timestamp = timestamp)}/${DataType.PACKAGE_APK.type}.${compressionType.suffix}"

        // Return if the archive doesn't exist.
        remoteRootService.exists(archivePath).also { exists ->
            if (exists.not()) {
                val msg = "${context.getString(R.string.not_exist)}: $archivePath"
                entity.apkLog = msg
                entity.apkState = OperationState.ERROR
                logUtil.log(logTag, msg)
                packageRestoreOperationDao.upsert(entity)
                return
            }
        }

        var isSuccess = true
        val outList = mutableListOf<String>()
        val installationUtil = InstallationUtil(logId, logUtil)

        // Decompress apk archive
        val tmpApkPath = PathUtil.getTmpApkPath(context = context, packageName = packageName)
        remoteRootService.deleteRecursively(tmpApkPath)
        remoteRootService.mkdirs(tmpApkPath)
        installationUtil.decompress(archivePath = archivePath, tmpApkPath = tmpApkPath, compressionType = compressionType).also { (succeed, out) ->
            if (succeed.not()) isSuccess = false
            outList.add(out)
            logUtil.log(logTag, out)
        }

        // Install apks
        val apksPath = remoteRootService.listFilePaths(tmpApkPath)
        when (apksPath.size) {
            0 -> {
                isSuccess = false
                val msg = "$tmpApkPath is empty."
                outList.add(msg)
                logUtil.log(logTag, msg)
                return
            }

            1 -> {
                installationUtil.pmInstall(userId, apksPath[0]).also { (succeed, out) ->
                    if (succeed.not()) isSuccess = false
                    outList.add(out)
                    logUtil.log(logTag, out)
                }
            }

            else -> {
                var pmSession: String
                installationUtil.pmInstallCreate(userId).also { (succeed, session) ->
                    if (succeed.not()) {
                        isSuccess = false
                        val msg = "Failed to get install session."
                        outList.add(msg)
                        logUtil.log(logTag, msg)
                        return
                    }
                    pmSession = session
                    val msg = "Install session: $session."
                    outList.add(msg)
                    logUtil.log(logTag, msg)
                }

                for (apkPath in apksPath) {
                    installationUtil.pmInstallWrite(pmSession, apkPath).also { (succeed, out) ->
                        if (succeed.not()) {
                            isSuccess = false
                            outList.add(out)
                            logUtil.log(logTag, out)
                            return
                        }
                    }
                }
                installationUtil.pmInstallCommit(pmSession).also { (succeed, out) ->
                    if (succeed.not())
                        isSuccess = false
                    outList.add(out)
                    logUtil.log(logTag, out)
                }
            }
        }

        remoteRootService.deleteRecursively(tmpApkPath)

        // Check the installation again.
        if (remoteRootService.queryInstalled(packageName, userId).not()) {
            isSuccess = false
            val msg = "Not installed: $packageName."
            outList.add(msg)
            logUtil.log(logTag, msg)
        }

        entity.apkLog = if (isSuccess) context.getString(R.string.succeed) else outList.toLineString().trim()
        entity.apkState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        packageRestoreOperationDao.upsert(entity)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun restoreData(entity: PackageRestoreOperation, packageName: String, timestamp: Long, compressionType: CompressionType, dataType: DataType) {
        val logTag = dataType.type.uppercase()

        // Set processing state
        if (entity.apkState == OperationState.ERROR) {
            val msg = "${context.getString(R.string.failed_and_terminated)}: ${DataType.PACKAGE_APK.type.uppercase()}"
            dataType.updateEntityLog(entity, msg)
            dataType.updateEntityState(entity, OperationState.ERROR)
            logUtil.log(logTag, msg)
            packageRestoreOperationDao.upsert(entity)
            return
        } else {
            dataType.updateEntityLog(entity, context.getString(R.string.restoring))
            dataType.updateEntityState(entity, OperationState.Processing)
            packageRestoreOperationDao.upsert(entity)
        }

        val logId = logUtil.log(logTag, "Start restoring...")
        val originPath = "${dataType.origin(userId)}/$packageName"
        val archivePath = "${getPackageItemSavePath(packageName = packageName, timestamp = timestamp)}/${dataType.type}.${compressionType.suffix}"
        val uid = remoteRootService.getPackageUid(packageName, userId)
        var isSuccess = true
        val outList = mutableListOf<String>()
        val seLinuxUtil = SELinuxUtil(logId, logUtil)
        val pathContext: String

        // Return if the archive doesn't exist.
        remoteRootService.exists(archivePath).also { exists ->
            if (exists.not()) {
                val msg = "${context.getString(R.string.not_exist_and_skip)}: $archivePath"
                dataType.updateEntityLog(entity, msg)
                dataType.updateEntityState(entity, OperationState.SKIP)
                logUtil.log(logTag, msg)
                packageRestoreOperationDao.upsert(entity)
                return
            }
        }

        // Get the SELinux context of the path.
        seLinuxUtil.getContext(originPath).also { (isSuccess, context) ->
            pathContext = if (isSuccess) context else ""
        }
        // Decompress the archive.
        CompressionUtil.decompressPackageData(logUtil, logId, context, userId, compressionType, archivePath, packageName, dataType).also { (succeed, out) ->
            if (succeed.not()) isSuccess = false
            outList.add(out)
            logUtil.log(logTag, out)
        }
        // Restore the SELinux context of the path.
        seLinuxUtil.restoreContext(originPath, pathContext, packageName, uid).also { (succeed, out) ->
            if (succeed.not()) isSuccess = false
            outList.add(out)
            logUtil.log(logTag, out)
        }

        dataType.updateEntityLog(entity, outList.toLineString().trim())
        dataType.updateEntityState(entity, if (isSuccess) OperationState.DONE else OperationState.ERROR)
        packageRestoreOperationDao.upsert(entity)
    }
}

class MediumBackupUtil(
    private val context: Context,
    private val timestamp: Long,
    private val logUtil: LogUtil,
    private val remoteRootService: RemoteRootService,
    private val mediaDao: MediaDao,
    private val gsonUtil: GsonUtil,
) {
    // Medium use tar is enough.
    private val compressionType = CompressionType.TAR
    private val compatibleMode = context.readCompatibleMode()
    private val mediumSavePath = PathUtil.getBackupMediumSavePath()

    fun getMediaItemSavePath(name: String): String = "${mediumSavePath}/${name}/$timestamp"

    suspend fun backupMedia(entity: MediaBackupOperationEntity) {
        // Set processing state
        entity.opLog = context.getString(R.string.backing_up)
        entity.opState = OperationState.Processing
        mediaDao.upsertBackupOp(entity)

        val logTag = "Media"
        val logId = logUtil.log(logTag, "Start backing up...")
        val path = entity.path
        val name = entity.name
        val archivePath = "${getMediaItemSavePath(name)}/${DataType.MEDIA_MEDIA.type}.${compressionType.suffix}"
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Check the existence of origin path.
        val originPathExists = remoteRootService.exists(path)
        if (originPathExists.not()) {
            val msg = "${context.getString(R.string.not_exist)}: $path"
            entity.opLog = msg
            entity.opState = OperationState.ERROR
            mediaDao.upsertBackupOp(entity)
            logUtil.log(logTag, msg)
            return
        }

        // Compress the dir.
        CompressionUtil.compressMediaData(logUtil, logId, compatibleMode, compressionType, path, archivePath)
            .also { (succeed, out) ->
                if (succeed.not()) isSuccess = false
                outList.add(out)
                logUtil.log(logTag, out)
            }

        // Test the archive if enabled.
        if (context.readCompressionTest()) {
            CompressionUtil.test(
                logUtil = logUtil,
                logId = logId,
                compressionType = compressionType,
                archivePath = archivePath,
                remoteRootService = remoteRootService
            ).also { (succeed, out) ->
                if (succeed.not()) {
                    isSuccess = false
                    outList.add(out)
                    logUtil.log(logTag, out)
                } else {
                    logUtil.log(logTag, "$archivePath is tested well.")
                }
            }
        }

        entity.opLog = outList.toLineString().trim()
        entity.opState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        mediaDao.upsertBackupOp(entity)
    }

    suspend fun backupConfig(entity: MediaRestoreEntity, dataType: DataType) {
        val logTag = "Config"
        val logId = logUtil.log(logTag, "Start backing up...")
        val name = entity.name
        val archivePath = "${getMediaItemSavePath(name)}/${dataType.type}.${compressionType.suffix}"
        val outList = mutableListOf<String>()

        val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = name, timestamp = entity.timestamp)
        val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = name, timestamp = entity.timestamp)
        remoteRootService.deleteRecursively(tmpConfigPath)
        remoteRootService.mkdirs(tmpConfigPath)

        remoteRootService.writeText(text = gsonUtil.toJson(entity), path = tmpConfigFilePath, context = context)
        // Compress the dir.
        CompressionUtil.compressPackageConfig(logUtil, logId, compatibleMode, compressionType, archivePath, tmpConfigPath).also { (_, out) ->
            outList.add(out)
            logUtil.log(logTag, out)
        }

        remoteRootService.deleteRecursively(tmpConfigPath)

        // Test the archive if enabled.
        if (context.readCompressionTest()) {
            CompressionUtil.test(
                logUtil = logUtil,
                logId = logId,
                compressionType = compressionType,
                archivePath = archivePath,
                remoteRootService = remoteRootService
            ).also { (succeed, out) ->
                if (succeed.not()) {
                    outList.add(out)
                    logUtil.log(logTag, out)
                } else {
                    logUtil.log(logTag, "$archivePath is tested well.")
                }
            }
        }
    }
}

class MediumRestoreUtil(
    private val context: Context,
    private val logUtil: LogUtil,
    private val remoteRootService: RemoteRootService,
    private val mediaDao: MediaDao,
) {
    companion object {
        // Medium use tar is enough.
        val compressionType = CompressionType.TAR
        private val mediumSavePath = PathUtil.getRestoreMediumSavePath()

        fun getMediaItemSavePath(name: String, timestamp: Long): String = "${mediumSavePath}/${name}/$timestamp"
    }

    suspend fun restoreMedia(entity: MediaRestoreOperationEntity) {
        // Set processing state
        entity.opLog = context.getString(R.string.restoring)
        entity.opState = OperationState.Processing
        mediaDao.upsertRestoreOp(entity)

        val logTag = "Media"
        val logId = logUtil.log(logTag, "Start restoring...")
        val path = entity.path
        val archivePath = entity.archivePath
        var isSuccess = true
        val outList = mutableListOf<String>()

        // Return if the archive doesn't exist.
        remoteRootService.exists(archivePath).also { exists ->
            if (exists.not()) {
                val msg = "${context.getString(R.string.not_exist_and_skip)}: $archivePath"
                entity.opLog = msg
                entity.opState = OperationState.ERROR
                logUtil.log(logTag, msg)
                mediaDao.upsertRestoreOp(entity)
                return
            }
        }

        // Decompress the archive.
        CompressionUtil.decompressMediaData(logUtil, logId, context, compressionType, PathUtil.getParentPath(path), archivePath).also { (succeed, out) ->
            if (succeed.not()) isSuccess = false
            outList.add(out)
            logUtil.log(logTag, out)
        }

        entity.opLog = outList.toLineString().trim()
        entity.opState = if (isSuccess) OperationState.DONE else OperationState.ERROR
        mediaDao.upsertRestoreOp(entity)
    }
}
