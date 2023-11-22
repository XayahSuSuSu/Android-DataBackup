package com.xayah.feature.main.reload

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import com.google.gson.reflect.TypeToken
import com.xayah.core.database.dao.MediaDao
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.util.suffixOf
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.MediumBackupUtil
import com.xayah.core.service.util.PackagesBackupUtil
import com.xayah.core.util.ConfigsMediaRestoreName
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.iconDir
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.localRestoreSaveDir
import com.xayah.feature.main.reload.model.AppInfoRestoreMap
import com.xayah.feature.main.reload.model.MediaInfoRestoreMap
import com.xayah.feature.main.reload.model.MediumReloadingState
import com.xayah.feature.main.reload.model.Migration1Version
import com.xayah.feature.main.reload.model.Migration2Version
import com.xayah.feature.main.reload.model.PackagesReloadingState
import com.xayah.feature.main.reload.model.TypedPath
import com.xayah.feature.main.reload.model.TypedTimestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import javax.inject.Inject

class ReloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val migration1Repository: Migration1Repository,
    private val migration2Repository: Migration2Repository,
) {
    fun getString(@StringRes resId: Int) = context.getString(resId)
    val typeList = listOf(context.getString(R.string.overall_config), context.getString(R.string.existing_files))
    val versionList = listOf(Migration2Version, Migration1Version)

    suspend fun saveMedium(medium: List<MediaRestoreEntity>, versionIndex: Int) = when (versionIndex) {
        1 -> migration1Repository.saveMedium(medium = medium)
        else -> migration2Repository.saveMedium(medium = medium)
    }


    suspend fun savePackages(packages: List<PackageRestoreEntire>, versionIndex: Int) = when (versionIndex) {
        1 -> migration1Repository.savePackages(packages = packages)
        else -> migration2Repository.savePackages(packages = packages)
    }

    suspend fun getMedium(typeIndex: Int, versionIndex: Int, mutableState: MutableStateFlow<MediumReloadingState>) = when (versionIndex) {
        1 -> when (typeIndex) {
            1 -> migration1Repository.dumpMediaConfigsRecursively(mutableState)
            else -> migration1Repository.dumpMediumOverallConfig(mutableState)
        }

        else -> when (typeIndex) {
            1 -> migration2Repository.dumpMediaConfigsRecursively(mutableState)
            else -> migration2Repository.dumpMediumOverallConfig(mutableState)
        }
    }

    suspend fun getPackages(typeIndex: Int, versionIndex: Int, mutableState: MutableStateFlow<PackagesReloadingState>) = when (versionIndex) {
        1 -> when (typeIndex) {
            1 -> migration1Repository.dumpPackageConfigsRecursively(mutableState)
            else -> migration1Repository.dumpPackagesOverallConfig(mutableState)
        }

        else -> when (typeIndex) {
            1 -> migration2Repository.dumpPackageConfigsRecursively(mutableState)
            else -> migration2Repository.dumpPackagesOverallConfig(mutableState)
        }
    }
}

interface Reload {
    suspend fun dumpMediaConfigsRecursively(mutableState: MutableStateFlow<MediumReloadingState>)
    suspend fun dumpPackageConfigsRecursively(mutableState: MutableStateFlow<PackagesReloadingState>)
    suspend fun dumpMediumOverallConfig(mutableState: MutableStateFlow<MediumReloadingState>)
    suspend fun dumpPackagesOverallConfig(mutableState: MutableStateFlow<PackagesReloadingState>)
    suspend fun saveMedium(medium: List<MediaRestoreEntity>)
    suspend fun savePackages(packages: List<PackageRestoreEntire>)
}

@OptIn(ExperimentalSerializationApi::class)
class Migration2Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
    private val mediaDao: MediaDao,
    private val mediumBackupUtil: MediumBackupUtil,
    private val packageRestoreDao: PackageRestoreEntireDao,
    private val packagesBackupUtil: PackagesBackupUtil,
) : Reload {
    private val configsDstDir = pathUtil.getConfigsDir(context.localBackupSaveDir())
    private val mutex = Mutex()

    companion object {
        private const val TAG = "Reload($Migration2Version)"
        private fun log(msg: () -> String) = LogUtil.log { TAG to msg() }
    }

    override suspend fun dumpMediaConfigsRecursively(mutableState: MutableStateFlow<MediumReloadingState>) {
        mutex.withLock {
            log { "Dumping media configs..." }
            val state = MediumReloadingState(isFinished = false, current = 0, total = 0, medium = mutableListOf())
            val mediumDir = pathUtil.getLocalRestoreArchivesMediumDir()
            val pathList = rootService.walkFileTree(mediumDir)
            val typedPathList = mutableListOf<TypedPath>()
            state.total = pathList.size
            log { "Total paths count: ${pathList.size}" }

            // Classify the paths
            pathList.forEachIndexed { index, path ->
                state.current = index + 1
                mutableState.value = state.copy()
                log { "Classifying: ${path.pathString}" }
                runCatching {
                    val pathListSize = path.pathList.size
                    val name = path.pathList[pathListSize - 3]
                    val timestampName = path.pathList[pathListSize - 2]
                    val timestamp = timestampName.toLong()
                    val typedPathListIndex = typedPathList.indexOfLast { it.name == name }
                    if (typedPathListIndex == -1) {
                        val typedTimestamp =
                            TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                        typedPathList.add(TypedPath(name = name, typedTimestampList = mutableListOf(typedTimestamp)))
                    } else {
                        val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                        val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                        if (typedTimestampIndex == -1) {
                            val typedTimestamp =
                                TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                            typedPathList[typedPathListIndex].typedTimestampList.add(typedTimestamp)
                        } else {
                            typedPathList[typedPathListIndex].typedTimestampList[typedTimestampIndex].archivePathList.add(path)
                        }
                    }
                }.onFailure {
                    log { "Failed: ${it.message}" }
                }
            }

            log { "Medium count: ${typedPathList.size}" }
            state.total = typedPathList.size
            typedPathList.forEachIndexed { index, typedPath ->
                state.current = index + 1
                // For each media
                val name = typedPath.name
                log { "Media name: $name" }

                typedPath.typedTimestampList.forEach timestamp@{ typedTimestamp ->
                    // For each timestamp
                    val timestamp = typedTimestamp.timestamp
                    val archivePathList = typedTimestamp.archivePathList
                    var mediaExists = false
                    var mediaRestore = MediaRestoreEntity(
                        timestamp = timestamp,
                        path = "",
                        name = name,
                        sizeBytes = 0,
                        selected = false,
                        savePath = context.localRestoreSaveDir()
                    )
                    log { "Media timestamp: $timestamp" }

                    val timestampPath = "${mediumDir}/${mediaRestore.name}/${mediaRestore.timestamp}"
                    val configPath = "${timestampPath}/$ConfigsMediaRestoreName"

                    runCatching {
                        if (rootService.exists(configPath)) {
                            val bytes = rootService.readBytes(configPath)
                            mediaRestore = ProtoBuf.decodeFromByteArray(bytes)
                            log { "Config is reloaded from ProtoBuf." }
                        } else {
                            log { "Config is missing." }
                        }
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }

                    archivePathList.forEach { archivePath ->
                        // For each archive
                        log { "Media archive: ${archivePath.pathString}" }
                        runCatching {
                            when (archivePath.nameWithoutExtension) {
                                DataType.MEDIA_MEDIA.type -> {
                                    log { "Dumping media data..." }
                                    mediaExists = true
                                }

                                else -> {
                                    log { "${archivePath.nameWithoutExtension} dumped." }
                                }
                            }
                        }.onFailure {
                            log { "Failed: ${it.message}" }
                        }
                    }

                    // If the media archive doesn't exist, continue for the next one.
                    if (mediaExists.not()) {
                        log { "Media not exists." }
                        return@timestamp
                    } else {
                        mediaRestore.sizeBytes = rootService.calculateSize(timestampPath)
                        log { "Media exists, size: ${mediaRestore.sizeBytes}" }
                    }

                    state.medium.add(mediaRestore)
                }
                mutableState.value = state.copy()
            }

            mutableState.value = state.copy(isFinished = true, current = state.medium.size, total = state.medium.size)
        }
    }

    override suspend fun dumpPackageConfigsRecursively(mutableState: MutableStateFlow<PackagesReloadingState>) {
        mutex.withLock {
            log { "Dumping package configs..." }
            val state = PackagesReloadingState(isFinished = false, current = 0, total = 0, packages = mutableListOf())
            val packageManager = context.packageManager
            val packagesDir = pathUtil.getLocalRestoreArchivesPackagesDir()
            val pathList = rootService.walkFileTree(packagesDir)
            val typedPathList = mutableListOf<TypedPath>()
            state.total = pathList.size
            log { "Total paths count: ${pathList.size}" }

            BaseUtil.mkdirs(context.iconDir())

            // Classify the paths
            pathList.forEachIndexed { index, path ->
                state.current = index + 1
                mutableState.value = state.copy()
                log { "Classifying: ${path.pathString}" }
                runCatching {
                    val pathListSize = path.pathList.size
                    val packageName = path.pathList[pathListSize - 3]
                    val timestampName = path.pathList[pathListSize - 2]
                    val timestamp = timestampName.toLong()
                    val typedPathListIndex = typedPathList.indexOfLast { it.name == packageName }
                    if (typedPathListIndex == -1) {
                        val typedTimestamp = TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                        typedPathList.add(TypedPath(name = packageName, typedTimestampList = mutableListOf(typedTimestamp)))
                    } else {
                        val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                        val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                        if (typedTimestampIndex == -1) {
                            val typedTimestamp = TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                            typedPathList[typedPathListIndex].typedTimestampList.add(typedTimestamp)
                        } else {
                            typedPathList[typedPathListIndex].typedTimestampList[typedTimestampIndex].archivePathList.add(path)
                        }
                    }
                }.onFailure {
                    log { "Failed: ${it.message}" }
                }
            }

            log { "Packages count: ${typedPathList.size}" }
            state.total = typedPathList.size
            typedPathList.forEachIndexed { index, typedPath ->
                state.current = index + 1
                // For each package
                val packageName = typedPath.name
                log { "Package name: $packageName" }

                typedPath.typedTimestampList.forEach timestamp@{ typedTimestamp ->
                    // For each timestamp
                    val timestamp = typedTimestamp.timestamp
                    val archivePathList = typedTimestamp.archivePathList
                    var packageRestore = PackageRestoreEntire(
                        packageName = packageName,
                        backupOpCode = OperationMask.None,
                        timestamp = timestamp,
                        compressionType = CompressionType.ZSTD,
                        savePath = context.localRestoreSaveDir(),
                    )
                    log { "Package timestamp: $timestamp" }

                    val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
                    rootService.deleteRecursively(tmpApkPath)
                    rootService.mkdirs(tmpApkPath)

                    val timestampPath = "${packagesDir}/${packageRestore.packageName}/${packageRestore.timestamp}"
                    val configPath = "${timestampPath}/$ConfigsPackageRestoreName"
                    var loadedFromConfig = false

                    runCatching {
                        if (rootService.exists(configPath)) {
                            val bytes = rootService.readBytes(configPath)
                            packageRestore = ProtoBuf.decodeFromByteArray(bytes)
                            log { "Config is reloaded from ProtoBuf." }
                            loadedFromConfig = true
                        } else {
                            log { "Config is missing." }
                        }
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }

                    // Reset backupOpCode, actual archives may differ from config.
                    packageRestore.backupOpCode = OperationMask.None

                    archivePathList.forEach { archivePath ->
                        // For each archive
                        log { "Package archive: ${archivePath.pathString}" }
                        runCatching {
                            when (archivePath.nameWithoutExtension) {
                                DataType.PACKAGE_APK.type -> {
                                    log { "Dumping apk..." }
                                    val type = CompressionType.suffixOf(archivePath.extension)
                                    if (type != null) {
                                        log { "Archive compression type: ${type.type}" }
                                        packageRestore.compressionType = type
                                        packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Apk
                                        if (loadedFromConfig.not()) {
                                            Tar.decompress(src = archivePath.pathString, dst = tmpApkPath, extra = type.decompressPara)
                                            rootService.listFilePaths(tmpApkPath).also { pathList ->
                                                if (pathList.isNotEmpty()) {
                                                    rootService.getPackageArchiveInfo(pathList.first())?.apply {
                                                        packageRestore.label = applicationInfo.loadLabel(packageManager).toString()
                                                        packageRestore.versionName = versionName ?: ""
                                                        packageRestore.versionCode = longVersionCode
                                                        packageRestore.flags = applicationInfo.flags
                                                        val iconPath = pathUtil.getPackageIconPath(packageName)
                                                        val iconExists = rootService.exists(iconPath)
                                                        if (iconExists.not()) {
                                                            val icon = applicationInfo.loadIcon(packageManager)
                                                            BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                        }
                                                        log { "Icon and config updated." }
                                                    }
                                                } else {
                                                    log { "Archive is empty." }
                                                }
                                            }
                                        }
                                    } else {
                                        log { "Failed to parse compression type: ${archivePath.extension}" }
                                    }
                                }

                                DataType.PACKAGE_USER.type -> {
                                    log { "Dumping user..." }
                                    val type = CompressionType.suffixOf(archivePath.extension)
                                    if (type != null) {
                                        log { "Archive compression type: ${type.type}" }
                                        packageRestore.compressionType = type
                                        packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Data
                                    } else {
                                        log { "Failed to parse compression type: ${archivePath.extension}" }
                                    }
                                }

                                else -> {
                                    log { "${archivePath.nameWithoutExtension} dumped." }
                                }
                            }
                        }.onFailure {
                            log { "Failed: ${it.message}" }
                        }
                    }

                    // If the package archives don't exist, continue for the next one.
                    if (packageRestore.backupOpCode == OperationMask.None) {
                        log { "Package has no data." }
                        return@timestamp
                    } else {
                        packageRestore.sizeBytes = rootService.calculateSize(timestampPath)
                        log { "Package data exists, size: ${packageRestore.sizeBytes}" }
                    }

                    rootService.deleteRecursively(tmpApkPath)
                    state.packages.add(packageRestore)
                }
                mutableState.value = state.copy()
            }

            mutableState.value = state.copy(isFinished = true, current = state.packages.size, total = state.packages.size)
        }
    }

    override suspend fun dumpMediumOverallConfig(mutableState: MutableStateFlow<MediumReloadingState>) {
        mutex.withLock {
            log { "Dumping medium overall config..." }
            val state = MediumReloadingState(isFinished = false, current = 0, total = 0, medium = mutableListOf())
            val configsDir = pathUtil.getLocalBackupConfigsDir()

            runCatching {
                val configPath = mediumBackupUtil.getConfigsDst(dstDir = configsDir)
                val bytes = rootService.readBytes(configPath)
                state.medium.addAll(ProtoBuf.decodeFromByteArray<List<MediaRestoreEntity>>(bytes).toMutableList())
            }.onFailure {
                log { "Failed: ${it.message}" }
            }

            mutableState.value = state.copy(isFinished = true, current = state.medium.size, total = state.medium.size)
        }
    }

    override suspend fun dumpPackagesOverallConfig(mutableState: MutableStateFlow<PackagesReloadingState>) {
        mutex.withLock {
            log { "Dumping packages overall config..." }
            val state = PackagesReloadingState(isFinished = false, current = 0, total = 0, packages = mutableListOf())
            val configsDir = pathUtil.getLocalBackupConfigsDir()

            runCatching {
                val configPath = packagesBackupUtil.getConfigsDst(dstDir = configsDir)
                val bytes = rootService.readBytes(configPath)
                state.packages.addAll(ProtoBuf.decodeFromByteArray<List<PackageRestoreEntire>>(bytes).toMutableList())
            }.onFailure {
                log { "Failed: ${it.message}" }
            }

            log { "Dumping packages icons..." }
            // Restore icons.
            val archivePath = packagesBackupUtil.getIconsDst(dstDir = configsDir)
            Tar.decompress(src = archivePath, dst = context.filesDir(), extra = packagesBackupUtil.tarCompressionType.decompressPara)

            mutableState.value = state.copy(isFinished = true, current = state.packages.size, total = state.packages.size)
        }
    }

    override suspend fun saveMedium(medium: List<MediaRestoreEntity>) {
        mutex.withLock {
            medium.forEach { mediaInfo ->
                val mediaRestore = mediaDao.queryMedia(path = mediaInfo.path, timestamp = mediaInfo.timestamp, savePath = mediaInfo.savePath)
                val id = mediaRestore?.id ?: 0
                val selected = mediaRestore?.selected ?: false
                mediaDao.upsertRestore(mediaInfo.copy(id = id, selected = selected))
            }
            mediumBackupUtil.backupConfigs(data = medium, dstDir = configsDstDir)
        }
    }

    override suspend fun savePackages(packages: List<PackageRestoreEntire>) {
        mutex.withLock {
            packages.forEach { packageInfo ->
                val packageRestore =
                    packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
                val id = packageRestore?.id ?: 0
                val active = packageRestore?.active ?: false
                val operationCode = packageRestore?.operationCode ?: OperationMask.None
                packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode))
            }
            packagesBackupUtil.backupConfigs(data = packages, dstDir = configsDstDir)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
class Migration1Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
    private val mediaDao: MediaDao,
    private val mediumBackupUtil: MediumBackupUtil,
    private val packageRestoreDao: PackageRestoreEntireDao,
    private val packagesBackupUtil: PackagesBackupUtil,
    private val gsonUtil: GsonUtil,
) : Reload {
    private val configsDstDir = pathUtil.getConfigsDir(context.localBackupSaveDir())
    private val mutex = Mutex()

    companion object {
        private const val TAG = "Reload($Migration1Version)"
        private fun log(msg: () -> String) = LogUtil.log { TAG to msg() }
    }

    override suspend fun dumpMediaConfigsRecursively(mutableState: MutableStateFlow<MediumReloadingState>) {
        mutex.withLock {
            log { "Dumping media configs..." }
            val state = MediumReloadingState(isFinished = false, current = 0, total = 0, medium = mutableListOf())
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            var serial: Long = -1
            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++
                log { "Dumping: $userPath" }

                val mediumDir = "${userPath}/media"
                val pathList = rootService.walkFileTree(mediumDir)
                val typedPathList = mutableListOf<TypedPath>()
                state.total = pathList.size
                log { "Total paths count: ${pathList.size}" }

                // Classify the paths
                pathList.forEachIndexed { index, path ->
                    state.current = index + 1
                    mutableState.value = state.copy()
                    log { "Classifying: ${path.pathString}" }
                    runCatching {
                        val pathListSize = path.pathList.size
                        val name = path.pathList[pathListSize - 3]
                        val timestampName = path.pathList[pathListSize - 2]

                        /**
                         * In old versions, the timestamp may be named as "Cover" or something else,
                         * we need to convert it to number.
                         */
                        val timestamp = runCatching { timestampName.toLong() }.getOrElse { serial }
                        val typedPathListIndex = typedPathList.indexOfLast { it.name == name }
                        if (typedPathListIndex == -1) {
                            val typedTimestamp =
                                TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                            typedPathList.add(TypedPath(name = name, typedTimestampList = mutableListOf(typedTimestamp)))
                        } else {
                            val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                            val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                            if (typedTimestampIndex == -1) {
                                val typedTimestamp =
                                    TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                                typedPathList[typedPathListIndex].typedTimestampList.add(typedTimestamp)
                            } else {
                                typedPathList[typedPathListIndex].typedTimestampList[typedTimestampIndex].archivePathList.add(path)
                            }
                        }
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }
                }

                log { "Medium count: ${typedPathList.size}" }
                state.total = typedPathList.size
                typedPathList.forEachIndexed { index, typedPath ->
                    state.current = index + 1
                    // For each media
                    val name = typedPath.name
                    log { "Media name: $name" }

                    typedPath.typedTimestampList.forEach timestamp@{ typedTimestamp ->
                        // For each timestamp
                        val timestamp = typedTimestamp.timestamp
                        val timestampName = typedTimestamp.timestampName
                        val archivePathList = typedTimestamp.archivePathList
                        var mediaExists = false
                        val mediaRestore = MediaRestoreEntity(
                            timestamp = timestamp,
                            path = "",
                            name = name,
                            sizeBytes = 0,
                            selected = false,
                            savePath = context.localRestoreSaveDir()
                        )
                        log { "Media timestamp: $timestamp" }

                        val tmpMediaPath = pathUtil.getTmpApkPath(packageName = name)
                        rootService.deleteRecursively(tmpMediaPath)
                        rootService.mkdirs(tmpMediaPath)

                        val timestampPath = "${mediumDir}/${mediaRestore.name}/${timestampName}"
                        val targetName = "com.xayah.databackup.PATH"

                        archivePathList.forEach { archivePath ->
                            // For each archive
                            log { "Media archive: ${archivePath.pathString}" }
                            runCatching {
                                when (archivePath.nameWithoutExtension.lowercase()) {
                                    name.lowercase() -> {
                                        log { "Dumping media data..." }
                                        mediaExists = true

                                        // Get target path from archive.
                                        Tar.decompress(
                                            src = archivePath.pathString,
                                            dst = tmpMediaPath,
                                            extra = CompressionType.TAR.decompressPara,
                                            target = targetName
                                        )
                                        mediaRestore.name = name
                                        mediaRestore.path = rootService.readText("${tmpMediaPath}/${name}/${targetName}")
                                        log { "Dumped target path: ${mediaRestore.path}" }
                                    }

                                    else -> {
                                        log { "${archivePath.nameWithoutExtension} dumped." }
                                    }
                                }
                            }.onFailure {
                                log { "Failed: ${it.message}" }
                            }
                        }

                        // If the media archive doesn't exist, continue for the next one.
                        if (mediaExists.not()) {
                            log { "Media not exists." }
                            return@timestamp
                        } else {
                            mediaRestore.sizeBytes = rootService.calculateSize(timestampPath)
                            log { "Media exists, size: ${mediaRestore.sizeBytes}" }
                        }

                        rootService.deleteRecursively(tmpMediaPath)
                        state.medium.add(mediaRestore)
                    }
                    mutableState.value = state.copy()
                }
            }

            mutableState.value = state.copy(isFinished = true, current = state.medium.size, total = state.medium.size)
        }
    }

    override suspend fun dumpPackageConfigsRecursively(mutableState: MutableStateFlow<PackagesReloadingState>) {
        mutex.withLock {
            log { "Dumping package configs..." }
            val state = PackagesReloadingState(isFinished = false, current = 0, total = 0, packages = mutableListOf())
            val packageManager = context.packageManager
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            var serial: Long = -1

            BaseUtil.mkdirs(context.iconDir())

            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++
                log { "Dumping: $userPath" }

                val packagesDir = "${userPath}/data"
                val pathList = rootService.walkFileTree(packagesDir)
                val typedPathList = mutableListOf<TypedPath>()
                state.total = pathList.size
                log { "Total paths count: ${pathList.size}" }

                // Classify the paths
                pathList.forEachIndexed { index, path ->
                    state.current = index + 1
                    mutableState.value = state.copy()
                    log { "Classifying: ${path.pathString}" }
                    runCatching {
                        // Skip icon.png
                        if (path.pathList.last() == "icon.png") {
                            return@forEachIndexed
                        }
                        val pathListSize = path.pathList.size
                        val packageName = path.pathList[pathListSize - 3]

                        val timestampName = path.pathList[pathListSize - 2]

                        /**
                         * In old versions, the timestamp may be named as "Cover" or something else,
                         * we need to convert it to number.
                         */
                        val timestamp = runCatching { timestampName.toLong() }.getOrElse { serial }
                        val typedPathListIndex = typedPathList.indexOfLast { it.name == packageName }
                        if (typedPathListIndex == -1) {
                            val typedTimestamp = TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                            typedPathList.add(TypedPath(name = packageName, typedTimestampList = mutableListOf(typedTimestamp)))
                        } else {
                            val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                            val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                            if (typedTimestampIndex == -1) {
                                val typedTimestamp = TypedTimestamp(timestamp = timestamp, timestampName = timestampName, archivePathList = mutableListOf(path))
                                typedPathList[typedPathListIndex].typedTimestampList.add(typedTimestamp)
                            } else {
                                typedPathList[typedPathListIndex].typedTimestampList[typedTimestampIndex].archivePathList.add(path)
                            }
                        }
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }
                }

                log { "Packages count: ${typedPathList.size}" }
                state.total = typedPathList.size
                typedPathList.forEachIndexed { index, typedPath ->
                    state.current = index + 1
                    // For each package
                    val packageName = typedPath.name
                    log { "Package name: $packageName" }

                    typedPath.typedTimestampList.forEach timestamp@{ typedTimestamp ->
                        // For each timestamp
                        val timestamp = typedTimestamp.timestamp
                        val timestampName = typedTimestamp.timestampName
                        val archivePathList = typedTimestamp.archivePathList
                        val packageRestore = PackageRestoreEntire(
                            packageName = packageName,
                            backupOpCode = OperationMask.None,
                            timestamp = timestamp,
                            compressionType = CompressionType.ZSTD,
                            savePath = context.localRestoreSaveDir(),
                        )
                        log { "Package timestamp: $timestamp" }

                        val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
                        rootService.deleteRecursively(tmpApkPath)
                        rootService.mkdirs(tmpApkPath)

                        val timestampPath = "${packagesDir}/${packageRestore.packageName}/${timestampName}"

                        // Reset backupOpCode, actual archives may differ from config.
                        packageRestore.backupOpCode = OperationMask.None

                        archivePathList.forEach { archivePath ->
                            // For each archive
                            log { "Package archive: ${archivePath.pathString}" }
                            runCatching {
                                when (archivePath.nameWithoutExtension) {
                                    DataType.PACKAGE_APK.type -> {
                                        log { "Dumping apk..." }
                                        val type = CompressionType.suffixOf(archivePath.extension)
                                        if (type != null) {
                                            log { "Archive compression type: ${type.type}" }
                                            packageRestore.compressionType = type
                                            packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Apk
                                            Tar.decompress(src = archivePath.pathString, dst = tmpApkPath, extra = type.decompressPara)
                                            rootService.listFilePaths(tmpApkPath).also { pathList ->
                                                if (pathList.isNotEmpty()) {
                                                    val src = pathList.first()
                                                    log { "Loading $src..." }
                                                    rootService.getPackageArchiveInfo(src)?.apply {
                                                        packageRestore.label = applicationInfo.loadLabel(packageManager).toString()
                                                        packageRestore.versionName = versionName ?: ""
                                                        packageRestore.versionCode = longVersionCode
                                                        packageRestore.flags = applicationInfo.flags
                                                        val iconPath = pathUtil.getPackageIconPath(packageName)
                                                        val iconExists = rootService.exists(iconPath)
                                                        if (iconExists.not()) {
                                                            val icon = applicationInfo.loadIcon(packageManager)
                                                            BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                        }
                                                        log { "Icon and config updated." }
                                                    }
                                                } else {
                                                    log { "Archive is empty." }
                                                }
                                            }
                                        } else {
                                            log { "Failed to parse compression type: ${archivePath.extension}" }
                                        }
                                    }

                                    DataType.PACKAGE_USER.type -> {
                                        log { "Dumping user..." }
                                        val type = CompressionType.suffixOf(archivePath.extension)
                                        if (type != null) {
                                            log { "Archive compression type: ${type.type}" }
                                            packageRestore.compressionType = type
                                            packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Data
                                        } else {
                                            log { "Failed to parse compression type: ${archivePath.extension}" }
                                        }
                                    }

                                    else -> {
                                        log { "${archivePath.nameWithoutExtension} dumped." }
                                    }
                                }
                            }.onFailure {
                                log { "Failed: ${it.message}" }
                            }
                        }

                        // If the package archives don't exist, continue for the next one.
                        if (packageRestore.backupOpCode == OperationMask.None) {
                            log { "Package has no data." }
                            return@timestamp
                        } else {
                            packageRestore.sizeBytes = rootService.calculateSize(timestampPath)
                            log { "Package data exists, size: ${packageRestore.sizeBytes}" }
                        }

                        rootService.deleteRecursively(tmpApkPath)
                        state.packages.add(packageRestore)
                    }
                    mutableState.value = state.copy()
                }
            }

            mutableState.value = state.copy(isFinished = true, current = state.packages.size, total = state.packages.size)
        }
    }

    override suspend fun dumpMediumOverallConfig(mutableState: MutableStateFlow<MediumReloadingState>) {
        mutex.withLock {
            log { "Dumping medium overall config..." }
            val state = MediumReloadingState(isFinished = false, current = 0, total = 0, medium = mutableListOf())
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            var serial: Long = -1
            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++
                log { "Dumping: $userPath" }

                val configDir = "${userPath}/config/mediaRestoreMap"
                val mediumDir = "${userPath}/media"

                runCatching {
                    val json = rootService.readText(configDir)
                    val type = object : TypeToken<MediaInfoRestoreMap>() {}.type
                    val mediaInfoRestoreMap: MediaInfoRestoreMap = gsonUtil.fromJson(json, type)
                    mediaInfoRestoreMap.forEach { (_, base) ->
                        base.detailRestoreList.forEach timestamp@{
                            val name = base.name
                            val path = base.path
                            log { "Media name: $name" }
                            val timestamp = runCatching { it.date.toLong() }.getOrElse { serial }
                            val mediaRestore = MediaRestoreEntity(
                                timestamp = timestamp,
                                path = path,
                                name = name,
                                sizeBytes = 0,
                                selected = false,
                                savePath = context.localRestoreSaveDir()
                            )
                            val timestampPath = "${mediumDir}/${name}/${it.date}"
                            val archivePath = "${timestampPath}/${name}.tar"
                            val archiveExists = rootService.exists(archivePath)
                            log { "$archivePath exists: $archiveExists" }
                            // If the media archive doesn't exist, continue for the next one.
                            if (archiveExists.not()) {
                                log { "Media not exists." }
                                return@timestamp
                            } else {
                                mediaRestore.sizeBytes = rootService.calculateSize(timestampPath)
                                log { "Media exists, size: ${mediaRestore.sizeBytes}" }
                            }
                            state.medium.add(mediaRestore)
                        }
                        mutableState.value = state.copy()
                    }
                }.onFailure {
                    log { "Failed: ${it.message}" }
                }
            }

            mutableState.value = state.copy(isFinished = true, current = state.medium.size, total = state.medium.size)
        }
    }

    override suspend fun dumpPackagesOverallConfig(mutableState: MutableStateFlow<PackagesReloadingState>) {
        mutex.withLock {
            log { "Dumping packages overall config..." }
            val state = PackagesReloadingState(isFinished = false, current = 0, total = 0, packages = mutableListOf())
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            var serial: Long = -1
            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++
                log { "Dumping: $userPath" }

                val configDir = "${userPath}/config/appRestoreMap"
                val packagesDir = "${userPath}/data"

                runCatching {
                    val json = rootService.readText(configDir)
                    val type = object : TypeToken<AppInfoRestoreMap>() {}.type
                    val appInfoRestoreMap: AppInfoRestoreMap = gsonUtil.fromJson(json, type)
                    appInfoRestoreMap.forEach { (_, base) ->
                        base.detailRestoreList.forEach timestamp@{
                            val label = base.detailBase.appName
                            val packageName = base.detailBase.packageName
                            val isSystemApp = base.detailBase.isSystemApp
                            log { "Package name: $packageName" }
                            val timestamp = runCatching { it.date.toLong() }.getOrElse { serial }
                            val packageRestore = PackageRestoreEntire(
                                label = label,
                                packageName = packageName,
                                backupOpCode = OperationMask.None,
                                timestamp = timestamp,
                                versionName = it.versionName,
                                versionCode = it.versionCode,
                                flags = if (isSystemApp) ApplicationInfo.FLAG_SYSTEM else 0,
                                compressionType = CompressionType.ZSTD,
                                savePath = context.localRestoreSaveDir(),
                            )

                            val timestampPath = "${packagesDir}/${packageName}/${it.date}"

                            // Dump the first file and get the compression type.
                            val firstFile = rootService.walkFileTree(timestampPath).first()
                            val compressionType = CompressionType.suffixOf(firstFile.extension)
                            if (compressionType == null) {
                                log { "Failed to parse compression type: ${firstFile.extension}" }
                                return@timestamp
                            } else {
                                if (it.hasApp)
                                    packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Apk
                                if (it.hasData)
                                    packageRestore.backupOpCode = packageRestore.backupOpCode or OperationMask.Data
                                packageRestore.compressionType = compressionType
                                packageRestore.sizeBytes = rootService.calculateSize(timestampPath)
                                log { "Package data exists, size: ${packageRestore.sizeBytes}" }
                            }

                            state.packages.add(packageRestore)
                        }
                        mutableState.value = state.copy()
                    }
                }.onFailure {
                    log { "Failed: ${it.message}" }
                }
            }

            mutableState.value = state.copy(isFinished = true, current = state.packages.size, total = state.packages.size)
        }
    }

    override suspend fun saveMedium(medium: List<MediaRestoreEntity>) {
        mutex.withLock {
            /**
             * Adapt directory structure.
             *        /.../DataBackup/backup/$userId/media/$mediaName/$coverOrTimestamp/${mediaName}.tar
             * --->   /.../DataBackup/archive/medium/$mediaName/$serialOrTimestamp/media.tar
             */
            log { "Adapting directory structure..." }
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            val dstDir = pathUtil.getLocalRestoreArchivesMediumDir()
            var serial: Long = -1
            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++

                val mediumDir = "${userPath}/media"
                val pathList = rootService.walkFileTree(mediumDir)

                pathList.forEach { path ->
                    runCatching {
                        val pathListSize = path.pathList.size
                        val name = path.pathList[pathListSize - 3]
                        val timestampName = path.pathList[pathListSize - 2]

                        /**
                         * In old versions, the timestamp may be named as "Cover" or something else,
                         * we need to convert it to number.
                         */
                        val timestamp = runCatching { timestampName.toLong() }.getOrElse { serial }
                        val dst = "${dstDir}/${name}/${timestamp}/media.${path.extension}"
                        log { "Trying to move ${path.pathString} to $dst." }
                        rootService.mkdirs(path = PathUtil.getParentPath(dst))
                        rootService.renameTo(src = path.pathString, dst = dst)
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }
                }
            }

            medium.forEach { mediaInfo ->
                val mediaRestore = mediaDao.queryMedia(path = mediaInfo.path, timestamp = mediaInfo.timestamp, savePath = mediaInfo.savePath)
                val id = mediaRestore?.id ?: 0
                val selected = mediaRestore?.selected ?: false
                mediaDao.upsertRestore(mediaInfo.copy(id = id, selected = selected))
            }
            mediumBackupUtil.backupConfigs(data = medium, dstDir = configsDstDir)
        }
    }

    override suspend fun savePackages(packages: List<PackageRestoreEntire>) {
        mutex.withLock {
            /**
             * Adapt directory structure.
             *        /.../DataBackup/backup/$userId/data/$packageName/$coverOrTimestamp/${dataType}.tar.*
             * --->   /.../DataBackup/archive/packages/$packageName/$serialOrTimestamp/${dataType}.tar.*
             */
            log { "Adapting directory structure..." }
            val backupDir = "${context.localRestoreSaveDir()}/backup"
            val dstDir = pathUtil.getLocalRestoreArchivesPackagesDir()
            var serial: Long = -1
            rootService.listFilePaths(backupDir).forEach { userPath ->
                // Timestamp serial for "Cover".
                serial++

                val packagesDir = "${userPath}/data"
                val pathList = rootService.walkFileTree(packagesDir)

                pathList.forEach pathList@{ path ->
                    runCatching {
                        // Skip icon.png
                        if (path.pathList.last() == "icon.png") {
                            return@pathList
                        }
                        val pathListSize = path.pathList.size
                        val packageName = path.pathList[pathListSize - 3]
                        val timestampName = path.pathList[pathListSize - 2]

                        /**
                         * In old versions, the timestamp may be named as "Cover" or something else,
                         * we need to convert it to number.
                         */
                        val timestamp = runCatching { timestampName.toLong() }.getOrElse { serial }

                        val dst = "${dstDir}/${packageName}/${timestamp}/${path.pathList.last()}"
                        log { "Trying to move ${path.pathString} to $dst." }
                        rootService.mkdirs(path = PathUtil.getParentPath(dst))
                        rootService.renameTo(src = path.pathString, dst = dst)
                    }.onFailure {
                        log { "Failed: ${it.message}" }
                    }
                }
            }

            packages.forEach { packageInfo ->
                val packageRestore =
                    packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
                val id = packageRestore?.id ?: 0
                val active = packageRestore?.active ?: false
                val operationCode = packageRestore?.operationCode ?: OperationMask.None
                packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode))
            }
            packagesBackupUtil.backupConfigs(data = packages, dstDir = configsDstDir)
        }
    }
}
