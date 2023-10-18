package com.xayah.databackup.util.command

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.xayah.databackup.R
import com.xayah.databackup.data.CloudDao
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
import com.xayah.databackup.util.filesPath
import com.xayah.databackup.util.readBackupItself
import com.xayah.databackup.util.readBackupUserId
import com.xayah.databackup.util.readCleanRestoring
import com.xayah.databackup.util.readCompatibleMode
import com.xayah.databackup.util.readCompressionType
import com.xayah.databackup.util.readRestoreUserId
import com.xayah.librootservice.service.RemoteRootService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun List<String>.toLineString() = joinToString(separator = "\n")

@AssistedFactory
interface IAdditionUtilFactory {
    fun createAdditionUtil(cloudMode: Boolean, logTag: String): AdditionUtil
}

class AdditionUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted val cloudMode: Boolean,
    @Assisted private val logTag: String,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var logUtil: LogUtil

    @Inject
    lateinit var cloudDao: CloudDao

    suspend fun backupItself(packageName: String) {
        if (context.readBackupItself()) {
            val outPath = PathUtil.getBackupSavePath(cloudMode)
            val userId = context.readBackupUserId()
            val sourceDirList = rootService.getPackageSourceDir(packageName, userId)
            if (sourceDirList.isNotEmpty()) {
                val apkPath = PathUtil.getParentPath(sourceDirList[0])
                val path = "${apkPath}/base.apk"
                val targetPath = "${outPath}/DataBackup.apk"
                rootService.copyTo(path = path, targetPath = targetPath, overwrite = true).also { result ->
                    if (result.not()) {
                        logUtil.log(logTag, "Failed to copy $path to $targetPath.")
                    } else {
                        logUtil.log(logTag, "Copied from $path to $targetPath.")
                    }
                }

                if (cloudMode) {
                    backupItselfExtension(targetPath)
                }
            } else {
                logUtil.log(logTag, "Failed to get apk path of $packageName.")
            }
        }
    }
}

@AssistedFactory
interface IPackagesBackupUtilFactory {
    fun createBackupUtil(cloudMode: Boolean, entity: PackageBackupOperation): PackagesBackupUtil
}

class PackagesBackupUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted val cloudMode: Boolean,
    @Assisted val entity: PackageBackupOperation,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var opDao: PackageBackupOperationDao

    @Inject
    lateinit var logUtil: LogUtil

    @Inject
    lateinit var gsonUtil: GsonUtil

    @Inject
    lateinit var cloudDao: CloudDao

    private val packageName = entity.packageName
    private val timestamp = entity.timestamp
    private val usePipe = context.readCompatibleMode()
    private val compressionType = context.readCompressionType()
    private val userId = context.readBackupUserId()
    private val packageSavePath = PathUtil.getBackupPackagesSavePath(cloudMode)
    val timestampPath = "${packageSavePath}/${packageName}/$timestamp"
    private val configsPath = PathUtil.getConfigsSavePath(cloudMode)
    val dirs = listOf(timestampPath, configsPath)

    fun getString(@StringRes resId: Int) = context.getString(resId)


    suspend fun mkdirs() = run {
        dirs.forEach {
            rootService.mkdirs(it)
        }

        if (cloudMode) mkdirsExtension()
    }

    private fun getArchivePath(type: DataType) =
        "${timestampPath}/${type.type}.${compressionType.suffix}"

    /**
     * Get the path of apk.
     */
    private suspend fun getPackageSourceDir(): String = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) {
            PathUtil.getParentPath(list[0])
        } else {
            // Failed.
            ""
        }
    }

    private suspend fun onPackageSourceEmpty(logTag: String, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        val msg = "Failed to get apk path of $packageName."
        isSuccess.value = false
        entityLog.add(msg)
        logUtil.log(logTag, msg)
    }

    private suspend fun compressAlso(result: ShellResult, logId: Long) {
        result.logCmd(logUtil = logUtil, logId = logId)
    }

    private suspend fun compressAlso(result: ShellResult, logId: Long, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        compressAlso(result = result, logId = logId)
        if (result.isSuccess.not()) {
            isSuccess.value = false
        }
        entityLog.add(result.outString)
    }

    private suspend fun testAlso(
        result: ShellResult,
        logTag: String,
        logId: Long,
        archivePath: String,
        onFailed: ((msg: String) -> Unit)? = null,
    ) {
        result.logCmd(logUtil = logUtil, logId = logId)

        if (result.isSuccess.not()) {
            val msg = "$archivePath is broken, trying to delete it."
            onFailed?.invoke(msg)
            logUtil.log(logTag, msg)
            // Delete the archive if test failed.
            rootService.deleteRecursively(archivePath)
        } else {
            logUtil.log(logTag, "$archivePath is tested well.")
        }
    }

    private suspend fun testAlso(
        result: ShellResult,
        logTag: String,
        logId: Long,
        archivePath: String,
        isSuccess: MutableState<Boolean>,
        entityLog: MutableList<String>,
    ) {
        testAlso(result, logTag, logId, archivePath) { msg ->
            isSuccess.value = false
            entityLog.add(msg)
        }
    }

    private suspend fun dataExistsAlso(logTag: String, exists: Boolean, type: DataType, src: String): Boolean = if (exists.not()) {
        if (type == DataType.PACKAGE_USER) {
            val msg = "${context.getString(R.string.not_exist)}: $src"
            type.setEntityLog(entity, msg)
            type.setEntityState(entity, OperationState.ERROR)
            logUtil.log(logTag, msg)
        } else {
            val msg = "${context.getString(R.string.not_exist_and_skip)}: $src"
            type.setEntityLog(entity, msg)
            type.setEntityState(entity, OperationState.SKIP)
            logUtil.log(logTag, msg)
        }
        opDao.upsert(entity)
        false
    } else {
        true
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setStartState() {
        setEntityLog(entity, getString(R.string.backing_up))
        setEntityState(entity, OperationState.Processing)
        opDao.upsert(entity)
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setDataStartState(logTag: String): Boolean = if (entity.userState == OperationState.ERROR) {
        val msg = "${context.getString(R.string.failed_and_terminated)}: ${DataType.PACKAGE_USER.type.uppercase()}"
        setEntityLog(entity, msg)
        setEntityState(entity, OperationState.ERROR)
        logUtil.log(logTag, msg)
        opDao.upsert(entity)
        false
    } else {
        setStartState()
        true
    }


    private suspend fun DataType.setEndState(isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        setEntityLog(entity, entityLog.trim().toLineString().trim())
        setEntityState(entity, if (isSuccess.value) OperationState.DONE else OperationState.ERROR)
        opDao.upsert(entity)
    }

    suspend fun backupApk() {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = "APK"
        val logId = logUtil.log(logTag, "Start backing up...")
        val type = DataType.PACKAGE_APK.also { it.setStartState() }

        val archivePath = getArchivePath(type)
        val cur = getPackageSourceDir()
        if (cur.isEmpty()) {
            onPackageSourceEmpty(logTag, isSuccess, entityLog)
        } else {
            Tar.compressInCur(usePipe = usePipe, cur = cur, src = "./*.apk", dst = archivePath, extra = compressionType.compressPara)
                .also { result -> compressAlso(result, logId, isSuccess, entityLog) }
            Tar.test(src = archivePath, extra = compressionType.decompressPara)
                .also { result -> testAlso(result, logTag, logId, archivePath, isSuccess, entityLog) }
        }

        if (cloudMode) backupArchiveExtension(targetPath = archivePath, type = type, isSuccess = isSuccess, entityLog = entityLog)

        type.setEndState(isSuccess, entityLog)
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun backupData(type: DataType) {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = type.type.uppercase()
        val logId = logUtil.log(logTag, "Start backing up...")
        type.also { if (it.setDataStartState(logTag).not()) return }

        val archivePath = getArchivePath(type)
        val srcDir = type.origin(userId)

        // Check the existence of origin path.
        val src = "$srcDir/$packageName"
        rootService.exists(src).also { if (dataExistsAlso(logTag, it, type, src).not()) return }

        // Generate exclusion items.
        val exclusionList = mutableListOf<String>()
        when (type) {
            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                exclusionList.addAll(folders.map { "$QUOTE$packageName/$it$QUOTE" })
            }

            DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf("cache")
                exclusionList.addAll(folders.map { "$QUOTE$packageName/$it$QUOTE" })
                // Exclude Backup_*
                exclusionList.add("${QUOTE}Backup_$QUOTE*")
            }

            else -> {}
        }

        // Compress and test.
        Tar.compress(
            usePipe = usePipe,
            exclusionList = exclusionList,
            srcDir = srcDir,
            src = packageName,
            dst = archivePath,
            extra = compressionType.compressPara
        )
            .also { result -> compressAlso(result, logId, isSuccess, entityLog) }
        Tar.test(src = archivePath, extra = compressionType.decompressPara)
            .also { result -> testAlso(result, logTag, logId, archivePath, isSuccess, entityLog) }

        if (cloudMode) backupArchiveExtension(targetPath = archivePath, type = type, isSuccess = isSuccess, entityLog = entityLog)

        type.setEndState(isSuccess, entityLog)
    }

    suspend fun backupConfig(entity: PackageRestoreEntire) {
        val logTag = "Config"
        val logId = logUtil.log(logTag, "Start backing up...")

        // Create tmp config
        val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = packageName, timestamp = entity.timestamp)
        val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = packageName, timestamp = entity.timestamp)
        rootService.deleteRecursively(tmpConfigPath)
        rootService.mkdirs(tmpConfigPath)
        rootService.writeText(text = gsonUtil.toJson(entity), path = tmpConfigFilePath, context = context)

        // Compress tmp config and test.
        val archivePath = getArchivePath(DataType.PACKAGE_CONFIG)
        Tar.compressInCur(usePipe = usePipe, cur = tmpConfigPath, src = "./*", dst = archivePath, extra = compressionType.compressPara)
            .also { result -> compressAlso(result, logId) }
        Tar.test(src = archivePath, extra = compressionType.decompressPara)
            .also { result -> testAlso(result, logTag, logId, archivePath) }

        if (cloudMode) backupArchiveExtension(targetPath = archivePath)

        // Clean up
        rootService.deleteRecursively(tmpConfigPath)
    }
}

@AssistedFactory
interface IPackagesBackupAfterwardsUtilFactory {
    fun createBackupAfterwardsUtil(cloudMode: Boolean, logTag: String): PackagesBackupAfterwardsUtil
}

class PackagesBackupAfterwardsUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted val cloudMode: Boolean,
    @Assisted private val logTag: String,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var logUtil: LogUtil

    @Inject
    lateinit var cloudDao: CloudDao

    private val usePipe = context.readCompatibleMode()
    private val compressionType = CompressionType.TAR // Configs use tar is enough.

    val configsPath = PathUtil.getConfigsSavePath(cloudMode)

    private fun getArchivePath(name: String) =
        "${configsPath}/${name}.${compressionType.suffix}"

    private suspend fun logAlso(result: ShellResult, logId: Long) {
        result.logCmd(logUtil = logUtil, logId = logId)
    }

    suspend fun backupIcons() {
        val logId = logUtil.log(logTag, "Start backing up icons...")

        val name = "icon"
        val archivePath = getArchivePath(name)

        // Compress and test.
        Tar.compress(
            usePipe = usePipe,
            exclusionList = listOf(),
            srcDir = context.filesPath(),
            src = name,
            dst = archivePath,
            extra = compressionType.compressPara
        )
            .also { result -> logAlso(result, logId) }
        Tar.test(src = archivePath, extra = compressionType.decompressPara)
            .also { result -> logAlso(result, logId) }

        if (cloudMode) backupArchiveExtension(targetPath = archivePath)
    }

    suspend fun clearUp() {
        if (cloudMode) clearUpExtension()
    }
}

@AssistedFactory
interface IPackagesRestoreUtilFactory {
    fun createPackagesRestoreUtil(timestamp: Long, entity: PackageRestoreOperation, compressionType: CompressionType): PackagesRestoreUtil
}

class PackagesRestoreUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted private val timestamp: Long,
    @Assisted private val entity: PackageRestoreOperation,
    @Assisted private val compressionType: CompressionType,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var opDao: PackageRestoreOperationDao

    @Inject
    lateinit var logUtil: LogUtil

    private val packageName = entity.packageName
    private val userId = context.readRestoreUserId()
    private val packageRestorePath = PathUtil.getRestorePackagesSavePath()

    private fun getString(@StringRes resId: Int) = context.getString(resId)

    private val timestampPath = "${packageRestorePath}/${packageName}/$timestamp"

    private fun getArchivePath(type: DataType) =
        "${timestampPath}/${type.type}.${compressionType.suffix}"

    private suspend fun decompressAlso(result: ShellResult, logId: Long, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        result.logCmd(logUtil = logUtil, logId = logId)
        if (result.isSuccess.not()) {
            isSuccess.value = false
        }
        entityLog.add(result.outString)
    }

    private suspend fun Boolean.queryInstalledAlso(logTag: String, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        if (this) {
            entityLog.add(getString(R.string.installed))
        } else {
            isSuccess.value = false
            val msg = "Not installed: $packageName."
            entityLog.add(msg)
            logUtil.log(logTag, msg)
        }
    }

    private suspend fun archiveExistsAlso(logTag: String, exists: Boolean, type: DataType, src: String): Boolean = if (exists.not()) {
        if (type == DataType.PACKAGE_APK) {
            val msg = "${context.getString(R.string.not_exist)}: $src"
            type.setEntityLog(entity, msg)
            type.setEntityState(entity, OperationState.ERROR)
            logUtil.log(logTag, msg)
        } else {
            val msg = "${context.getString(R.string.not_exist_and_skip)}: $src"
            type.setEntityLog(entity, msg)
            type.setEntityState(entity, OperationState.SKIP)
            logUtil.log(logTag, msg)
        }
        opDao.upsert(entity)
        false
    } else {
        true
    }

    private suspend fun installApks(
        logTag: String,
        tmpApkPath: String,
        apksPath: List<String>,
        isSuccess: MutableState<Boolean>,
        entityLog: MutableList<String>,
    ) {
        when (apksPath.size) {
            0 -> {
                isSuccess.value = false
                val msg = "$tmpApkPath is empty."
                entityLog.add(msg)
                logUtil.log(logTag, msg)
            }

            1 -> {
                Pm.install(userId, apksPath.first()).also { result ->
                    if (result.isSuccess.not()) isSuccess.value = false
                    val msg = result.outString
                    entityLog.add(msg)
                    logUtil.log(logTag, msg)
                }
            }

            else -> {
                var pmSession: String
                Pm.Install.create(userId).also { result ->
                    if (result.isSuccess.not()) {
                        isSuccess.value = false
                        val msg = "Failed to get install session."
                        entityLog.add(msg)
                        logUtil.log(logTag, msg)
                        return
                    } else {
                        pmSession = result.outString
                        val msg = "Install session: $pmSession."
                        entityLog.add(msg)
                        logUtil.log(logTag, msg)
                    }
                }

                apksPath.forEach { apkPath ->
                    Pm.Install.write(pmSession, PathUtil.getFileName(apkPath), apkPath).also { result ->
                        if (result.isSuccess.not()) {
                            isSuccess.value = false
                            val msg = result.outString
                            entityLog.add(msg)
                            logUtil.log(logTag, msg)
                        }
                    }
                }

                Pm.Install.commit(pmSession).also { result ->
                    if (result.isSuccess.not()) {
                        isSuccess.value = false
                    }
                    val msg = result.outString
                    entityLog.add(msg)
                    logUtil.log(logTag, msg)
                }
            }
        }
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setStartState() {
        setEntityLog(entity, getString(R.string.restoring))
        setEntityState(entity, OperationState.Processing)
        opDao.upsert(entity)
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setDataStartState(logTag: String): Boolean = if (entity.userState == OperationState.ERROR) {
        val msg = "${context.getString(R.string.failed_and_terminated)}: ${DataType.PACKAGE_APK.type.uppercase()}"
        setEntityLog(entity, msg)
        setEntityState(entity, OperationState.ERROR)
        logUtil.log(logTag, msg)
        opDao.upsert(entity)
        false
    } else {
        setStartState()
        true
    }

    private suspend fun DataType.setEndState(isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        setEntityLog(entity, entityLog.trim().toLineString().trim())
        setEntityState(entity, if (isSuccess.value) OperationState.DONE else OperationState.ERROR)
        opDao.upsert(entity)
    }

    private suspend fun queryInstalledInternal(isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) = run {
        val logTag = "APK"

        // Check the installation.
        rootService.queryInstalled(packageName, userId).also {
            it.queryInstalledAlso(logTag, isSuccess, entityLog)
        }
    }

    suspend fun queryInstalled() {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val type = DataType.PACKAGE_APK
        queryInstalledInternal(isSuccess, entityLog)
        type.setEndState(isSuccess, entityLog)
    }

    suspend fun restoreApk() {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = "APK"
        val logId = logUtil.log(logTag, "Start restoring...")
        val type = DataType.PACKAGE_APK.also { it.setStartState() }

        val archivePath = getArchivePath(type)

        // Return if the archive doesn't exist.
        rootService.exists(archivePath).also { if (archiveExistsAlso(logTag, it, type, archivePath).not()) return }

        // Decompress apk archive
        val tmpApkPath = PathUtil.getTmpApkPath(context = context, packageName = packageName)
        rootService.deleteRecursively(tmpApkPath)
        rootService.mkdirs(tmpApkPath)
        Tar.decompress(src = archivePath, dst = tmpApkPath, extra = compressionType.decompressPara)
            .also { result -> decompressAlso(result, logId, isSuccess, entityLog) }

        // Install apks
        rootService.listFilePaths(tmpApkPath).also { apksPath ->
            installApks(logTag = logTag, tmpApkPath = tmpApkPath, apksPath = apksPath, isSuccess = isSuccess, entityLog = entityLog)
        }
        rootService.deleteRecursively(tmpApkPath)

        // Check the installation again.
        queryInstalledInternal(isSuccess = isSuccess, entityLog = entityLog)

        type.setEndState(isSuccess, entityLog)
    }

    private suspend fun restoreContext(
        logTag: String,
        logId: Long,
        context: String,
        path: String,
        uid: Int,
        isSuccess: MutableState<Boolean>,
        entityLog: MutableList<String>,
    ) {
        if (uid == -1) {
            isSuccess.value = false
            val msg = "Failed to get uid of $packageName."
            entityLog.add(msg)
            logUtil.log(logTag, msg)
        } else {
            SELinux.chown(uid = uid, path = path).also { result ->
                result.logCmd(logUtil = logUtil, logId = logId)
                if (result.isSuccess.not()) {
                    isSuccess.value = false
                    entityLog.add(result.outString)
                }
            }
            if (context.isNotEmpty()) {
                SELinux.chcon(context = context, path = path).also { result ->
                    result.logCmd(logUtil = logUtil, logId = logId)
                    if (result.isSuccess.not()) {
                        isSuccess.value = false
                        entityLog.add(result.outString)
                    }
                }
            } else {
                val parentContext: String
                SELinux.getContext(PathUtil.getParentPath(path)).also { result ->
                    result.logCmd(logUtil = logUtil, logId = logId)
                    parentContext = if (result.isSuccess) result.outString.replace("system_data_file", "app_data_file") else ""
                }
                if (parentContext.isNotEmpty()) {
                    SELinux.chcon(context = parentContext, path = path).also { result ->
                        result.logCmd(logUtil = logUtil, logId = logId)
                        if (result.isSuccess.not()) {
                            isSuccess.value = false
                            entityLog.add(result.outString)
                        }
                    }
                } else {
                    isSuccess.value = false
                    val msg = "Failed to restore context: $path"
                    entityLog.add(msg)
                    logUtil.log(logTag, msg)
                }
            }
        }
    }

    /**
     * Package data: USER, USER_DE, DATA, OBB, MEDIA
     */
    suspend fun restoreData(type: DataType) {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = type.type.uppercase()
        val logId = logUtil.log(logTag, "Start restoring...")
        type.also { if (it.setDataStartState(logTag).not()) return }

        val archivePath = getArchivePath(type)
        val dstDir = "${type.origin(userId)}/$packageName"
        val uid = rootService.getPackageUid(packageName, userId)

        // Return if the archive doesn't exist.
        rootService.exists(archivePath).also { if (archiveExistsAlso(logTag, it, type, archivePath).not()) return }

        // Generate exclusion items.
        val exclusionList = mutableListOf<String>()
        when (type) {
            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE, DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                exclusionList.addAll(folders.map { "$QUOTE$packageName/$it$QUOTE" })
                if (type == DataType.PACKAGE_DATA || type == DataType.PACKAGE_OBB || type == DataType.PACKAGE_MEDIA) {
                    // Exclude Backup_*
                    exclusionList.add("${QUOTE}Backup_$QUOTE*")
                }

            }

            else -> {}
        }

        // Get the SELinux context of the path.
        val pathContext: String
        SELinux.getContext(dstDir).also { result ->
            pathContext = if (result.isSuccess) result.outString else ""
        }

        // Decompress the archive.
        Tar.decompress(
            exclusionList = exclusionList,
            clear = if (context.readCleanRestoring()) "--recursive-unlink" else "",
            m = true,
            src = archivePath,
            dst = type.origin(userId),
            extra = compressionType.decompressPara
        )
            .also { result -> decompressAlso(result, logId, isSuccess, entityLog) }

        // Restore the SELinux context of the path.
        restoreContext(logTag = logTag, logId = logId, context = pathContext, path = dstDir, uid = uid, isSuccess = isSuccess, entityLog = entityLog)

        type.setEndState(isSuccess, entityLog)
    }
}

@AssistedFactory
interface IMediumBackupUtilFactory {
    fun createMediumBackupUtil(entity: MediaBackupOperationEntity): MediumBackupUtil
}

class MediumBackupUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted private val entity: MediaBackupOperationEntity,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var opDao: MediaDao

    @Inject
    lateinit var logUtil: LogUtil

    @Inject
    lateinit var gsonUtil: GsonUtil

    private val name = entity.name
    private val path = entity.path
    private val timestamp = entity.timestamp
    private val compressionType = CompressionType.TAR // Medium use tar is enough.
    private val type = DataType.MEDIA_MEDIA
    private val configType = DataType.MEDIA_CONFIG
    private val usePipe = context.readCompatibleMode()
    private val mediumSavePath = PathUtil.getBackupMediumSavePath()

    private fun getString(@StringRes resId: Int) = context.getString(resId)

    private val timestampPath = "${mediumSavePath}/${name}/$timestamp"

    suspend fun mkdirs() = rootService.mkdirs(timestampPath)

    private fun getArchivePath() =
        "${timestampPath}/${type.type}.${compressionType.suffix}"

    private fun getConfigArchivePath() =
        "${timestampPath}/${configType.type}.${compressionType.suffix}"

    private suspend fun compressAlso(result: ShellResult, logId: Long) {
        result.logCmd(logUtil = logUtil, logId = logId)
    }

    private suspend fun compressAlso(result: ShellResult, logId: Long, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        compressAlso(result = result, logId = logId)
        if (result.isSuccess.not()) {
            isSuccess.value = false
        }
        entityLog.add(result.outString)
    }

    private suspend fun testAlso(
        result: ShellResult,
        logTag: String,
        logId: Long,
        archivePath: String,
        onFailed: ((msg: String) -> Unit)? = null,
    ) {
        result.logCmd(logUtil = logUtil, logId = logId)

        if (result.isSuccess.not()) {
            val msg = "$archivePath is broken, trying to delete it."
            onFailed?.invoke(msg)
            logUtil.log(logTag, msg)
            // Delete the archive if test failed.
            rootService.deleteRecursively(archivePath)
        } else {
            logUtil.log(logTag, "$archivePath is tested well.")
        }
    }

    private suspend fun testAlso(
        result: ShellResult,
        logTag: String,
        logId: Long,
        archivePath: String,
        isSuccess: MutableState<Boolean>,
        entityLog: MutableList<String>,
    ) {
        testAlso(result, logTag, logId, archivePath) { msg ->
            isSuccess.value = false
            entityLog.add(msg)
        }
    }

    private suspend fun archiveExistsAlso(logTag: String, exists: Boolean, src: String): Boolean = if (exists.not()) {
        val msg = "${context.getString(R.string.not_exist)}: $src"
        type.setEntityLog(entity, msg)
        type.setEntityState(entity, OperationState.ERROR)
        logUtil.log(logTag, msg)
        opDao.upsertBackupOp(entity)
        false
    } else {
        true
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setStartState() {
        setEntityLog(entity, getString(R.string.backing_up))
        setEntityState(entity, OperationState.Processing)
        opDao.upsertBackupOp(entity)
    }

    private suspend fun DataType.setEndState(isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        setEntityLog(entity, entityLog.trim().toLineString().trim())
        setEntityState(entity, if (isSuccess.value) OperationState.DONE else OperationState.ERROR)
        opDao.upsertBackupOp(entity)
    }

    suspend fun backupMedia() {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = "Media"
        val logId = logUtil.log(logTag, "Start backing up...")
        val type = type.also { it.setStartState() }

        val archivePath = getArchivePath()

        // Check the existence of origin path.
        rootService.exists(path).also { if (archiveExistsAlso(logTag, it, path).not()) return }

        // Compress and test.
        Tar.compress(
            usePipe = usePipe,
            exclusionList = listOf(),
            srcDir = PathUtil.getParentPath(path),
            src = PathUtil.getFileName(path),
            dst = archivePath,
            extra = compressionType.compressPara
        )
            .also { result -> compressAlso(result, logId, isSuccess, entityLog) }
        Tar.test(src = archivePath, extra = compressionType.decompressPara)
            .also { result -> testAlso(result, logTag, logId, archivePath, isSuccess, entityLog) }

        type.setEndState(isSuccess, entityLog)
    }

    suspend fun backupConfig(entity: MediaRestoreEntity) {
        val logTag = "Config"
        val logId = logUtil.log(logTag, "Start backing up...")

        // Create tmp config
        val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = name, timestamp = entity.timestamp)
        val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = name, timestamp = entity.timestamp)
        rootService.deleteRecursively(tmpConfigPath)
        rootService.mkdirs(tmpConfigPath)
        rootService.writeText(text = gsonUtil.toJson(entity), path = tmpConfigFilePath, context = context)

        // Compress tmp config and test.
        val archivePath = getConfigArchivePath()
        Tar.compressInCur(usePipe = usePipe, cur = tmpConfigPath, src = "./*", dst = archivePath, extra = compressionType.compressPara)
            .also { result -> compressAlso(result, logId) }
        Tar.test(src = archivePath, extra = compressionType.decompressPara)
            .also { result -> testAlso(result, logTag, logId, archivePath) }

        // Clean up
        rootService.deleteRecursively(tmpConfigPath)
    }
}

@AssistedFactory
interface IMediumRestoreUtilFactory {
    fun createMediumRestoreUtil(entity: MediaRestoreOperationEntity): MediumRestoreUtil
}

class MediumRestoreUtil @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @Assisted private val entity: MediaRestoreOperationEntity,
) {
    @Inject
    lateinit var rootService: RemoteRootService

    @Inject
    lateinit var opDao: MediaDao

    @Inject
    lateinit var logUtil: LogUtil

    private val name = entity.name
    private val path = entity.path
    private val timestamp = entity.timestamp
    private val compressionType = CompressionType.TAR // Medium use tar is enough.
    private val type = DataType.MEDIA_MEDIA
    private val mediumSavePath = PathUtil.getRestoreMediumSavePath()

    private fun getString(@StringRes resId: Int) = context.getString(resId)

    private val timestampPath = "${mediumSavePath}/${name}/$timestamp"

    private fun getArchivePath() =
        "${timestampPath}/${type.type}.${compressionType.suffix}"

    private suspend fun decompressAlso(result: ShellResult, logId: Long, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        result.logCmd(logUtil = logUtil, logId = logId)
        if (result.isSuccess.not()) {
            isSuccess.value = false
        }
        entityLog.add(result.outString)
    }

    private suspend fun archiveExistsAlso(logTag: String, exists: Boolean, src: String): Boolean = if (exists.not()) {
        val msg = "${context.getString(R.string.not_exist)}: $src"
        type.setEntityLog(entity, msg)
        type.setEntityState(entity, OperationState.ERROR)
        logUtil.log(logTag, msg)
        opDao.upsertRestoreOp(entity)
        false
    } else {
        true
    }

    /**
     * Set processing state
     */
    private suspend fun DataType.setStartState() {
        setEntityLog(entity, getString(R.string.restoring))
        setEntityState(entity, OperationState.Processing)
        opDao.upsertRestoreOp(entity)
    }

    private suspend fun DataType.setEndState(isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {
        setEntityLog(entity, entityLog.trim().toLineString().trim())
        setEntityState(entity, if (isSuccess.value) OperationState.DONE else OperationState.ERROR)
        opDao.upsertRestoreOp(entity)
    }

    suspend fun restoreMedia() {
        val isSuccess = mutableStateOf(true)
        val entityLog = mutableListOf<String>()
        val logTag = "Media"
        val logId = logUtil.log(logTag, "Start restoring...")
        val type = type.also { it.setStartState() }

        val archivePath = getArchivePath()

        // Return if the archive doesn't exist.
        rootService.exists(archivePath).also { if (archiveExistsAlso(logTag, it, archivePath).not()) return }

        // Decompress the archive.
        Tar.decompress(
            exclusionList = listOf(),
            clear = if (context.readCleanRestoring()) "--recursive-unlink" else "",
            m = false,
            src = archivePath,
            dst = PathUtil.getParentPath(path),
            extra = compressionType.decompressPara
        )
            .also { result -> decompressAlso(result, logId, isSuccess, entityLog) }

        type.setEndState(isSuccess, entityLog)
    }
}
