package com.xayah.feature.main.reload

import android.content.Context
import androidx.annotation.StringRes
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
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.localRestoreSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import javax.inject.Inject

@OptIn(ExperimentalSerializationApi::class)
class ReloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    pathUtil: PathUtil,
    private val mediaDao: MediaDao,
    private val mediumBackupUtil: MediumBackupUtil,
    private val packageRestoreDao: PackageRestoreEntireDao,
    private val packagesBackupUtil: PackagesBackupUtil,
    private val migration3Repository: Migration3Repository,
) {
    fun getString(@StringRes resId: Int) = context.getString(resId)
    val typeList = listOf(context.getString(R.string.existing_files), context.getString(R.string.overall_config))
    val versionList = listOf(Migration3, Migration2, Migration1)
    private val configsDstDir = pathUtil.getConfigsDir(context.localBackupSaveDir())

    suspend fun saveMedium(list: List<MediaRestoreEntity>) {
        list.forEach { mediaInfo ->
            val mediaRestore = mediaDao.queryMedia(path = mediaInfo.path, timestamp = mediaInfo.timestamp, savePath = mediaInfo.savePath)
            val id = mediaRestore?.id ?: 0
            val selected = mediaRestore?.selected ?: false
            mediaDao.upsertRestore(mediaInfo.copy(id = id, selected = selected))
        }
        mediumBackupUtil.backupConfigs(data = list, dstDir = configsDstDir)
    }

    suspend fun savePackages(list: List<PackageRestoreEntire>) {
        list.forEach { packageInfo ->
            val packageRestore =
                packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
            val id = packageRestore?.id ?: 0
            val active = packageRestore?.active ?: false
            val operationCode = packageRestore?.operationCode ?: OperationMask.None
            packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode))
        }
        packagesBackupUtil.backupConfigs(data = list, dstDir = configsDstDir)
    }

    fun getMedium(typeIndex: Int, versionIndex: Int) = when (versionIndex) {
        0 -> when (typeIndex) {
            0 -> migration3Repository.dumpMediaConfigsRecursively()
            1 -> flow { emit(MediumReloadingState()) }
            else -> flow { emit(MediumReloadingState()) }
        }

        1 -> flow { emit(MediumReloadingState()) }
        2 -> flow { emit(MediumReloadingState()) }
        else -> flow { emit(MediumReloadingState()) }
    }

    fun getPackages(typeIndex: Int, versionIndex: Int) = when (versionIndex) {
        0 -> when (typeIndex) {
            0 -> migration3Repository.dumpPackageConfigsRecursively()
            1 -> flow { emit(PackagesReloadingState()) }
            else -> flow { emit(PackagesReloadingState()) }
        }

        1 -> flow { emit(PackagesReloadingState()) }
        2 -> flow { emit(PackagesReloadingState()) }
        else -> flow { emit(PackagesReloadingState()) }
    }
}

interface Reload {
    fun dumpMediaConfigsRecursively(): Flow<MediumReloadingState>
    fun dumpPackageConfigsRecursively(): Flow<PackagesReloadingState>
}

@OptIn(ExperimentalSerializationApi::class)
class Migration3Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val pathUtil: PathUtil,
) : Reload {
    companion object {
        private const val TAG = "Reload($Migration3)"
        private fun log(msg: () -> String) = LogUtil.log { TAG to msg() }
    }

    override fun dumpMediaConfigsRecursively(): Flow<MediumReloadingState> = flow {
        log { "Dumping media configs..." }
        val state = MediumReloadingState(isFinished = false, current = 0, total = 0, medium = mutableListOf())
        val pathList = rootService.walkFileTree(pathUtil.getLocalRestoreArchivesMediumDir())
        val typedPathList = mutableListOf<TypedPath>()
        state.total = pathList.size
        log { "Total paths count: ${pathList.size}" }

        // Classify the paths
        pathList.forEachIndexed { index, path ->
            state.current = index + 1
            emit(state.copy())
            log { "Classifying: ${path.pathString}" }
            runCatching {
                val pathListSize = path.pathList.size
                val name = path.pathList[pathListSize - 3]
                val timestamp = path.pathList[pathListSize - 2].toLong()
                val typedPathListIndex = typedPathList.indexOfLast { it.name == name }
                if (typedPathListIndex == -1) {
                    val typedTimestamp =
                        TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
                    typedPathList.add(TypedPath(name = name, typedTimestampList = mutableListOf(typedTimestamp)))
                } else {
                    val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                    val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                    if (typedTimestampIndex == -1) {
                        val typedTimestamp =
                            TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
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

                val timestampPath = "${pathUtil.getLocalRestoreArchivesMediumDir()}/${mediaRestore.name}/${mediaRestore.timestamp}"
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
            emit(state.copy())
        }

        emit(state.copy(isFinished = true, current = state.medium.size, total = state.medium.size))
    }

    override fun dumpPackageConfigsRecursively(): Flow<PackagesReloadingState> = flow {
        log { "Dumping package configs..." }
        val state = PackagesReloadingState(isFinished = false, current = 0, total = 0, packages = mutableListOf())
        val packageManager = context.packageManager
        val pathList = rootService.walkFileTree(pathUtil.getLocalRestoreArchivesPackagesDir())
        val typedPathList = mutableListOf<TypedPath>()
        state.total = pathList.size
        log { "Total paths count: ${pathList.size}" }

        // Classify the paths
        pathList.forEachIndexed { index, path ->
            state.current = index + 1
            emit(state.copy())
            log { "Classifying: ${path.pathString}" }
            runCatching {
                val pathListSize = path.pathList.size
                val packageName = path.pathList[pathListSize - 3]
                val timestamp = path.pathList[pathListSize - 2].toLong()
                val typedPathListIndex = typedPathList.indexOfLast { it.name == packageName }
                if (typedPathListIndex == -1) {
                    val typedTimestamp = TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
                    typedPathList.add(TypedPath(name = packageName, typedTimestampList = mutableListOf(typedTimestamp)))
                } else {
                    val typedTimestampList = typedPathList[typedPathListIndex].typedTimestampList
                    val typedTimestampIndex = typedTimestampList.indexOfLast { it.timestamp == timestamp }
                    if (typedTimestampIndex == -1) {
                        val typedTimestamp = TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
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

                val timestampPath = "${pathUtil.getLocalRestoreArchivesPackagesDir()}/${packageRestore.packageName}/${packageRestore.timestamp}"
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

                state.packages.add(packageRestore)
            }
            emit(state.copy())
        }

        emit(state.copy(isFinished = true, current = state.packages.size, total = state.packages.size))
    }
}
