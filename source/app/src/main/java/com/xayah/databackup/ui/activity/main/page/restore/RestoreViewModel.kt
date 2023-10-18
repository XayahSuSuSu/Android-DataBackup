package com.xayah.databackup.ui.activity.main.page.restore

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.data.MediaDao
import com.xayah.databackup.data.MediaRestoreEntity
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.command.Tar
import com.xayah.databackup.util.iconPath
import com.xayah.librootservice.parcelables.PathParcelable
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryOnScope
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RestoreUiState(
    val logUtil: LogUtil,
    val packageRestoreEntireDao: PackageRestoreEntireDao,
    val gsonUtil: GsonUtil,
)

private data class TypedTimestamp(
    val timestamp: Long,
    val archivePathList: MutableList<PathParcelable>,
)

private data class TypedPath(
    val name: String,
    val typedTimestampList: MutableList<TypedTimestamp>,
)

@HiltViewModel
class RestoreViewModel @Inject constructor(
    private val logUtil: LogUtil,
    private val packageRestoreEntireDao: PackageRestoreEntireDao,
    private val mediaDao: MediaDao,
    private val gsonUtil: GsonUtil,
) : ViewModel() {
    private val _uiState = mutableStateOf(RestoreUiState(logUtil = logUtil, packageRestoreEntireDao = packageRestoreEntireDao, gsonUtil = gsonUtil))
    val uiState: State<RestoreUiState>
        get() = _uiState

    private suspend fun reloadPackages(context: Context, logTag: String, logId: Long, remoteRootService: RemoteRootService) {
        val packageManager = context.packageManager
        val pathList = remoteRootService.walkFileTree(PathUtil.getRestorePackagesSavePath())
        val typedPathList = mutableListOf<TypedPath>()

        logUtil.log(logTag, "Clear the table")
        // Clear table first
        packageRestoreEntireDao.clearTable()

        logUtil.log(logTag, "Classify the paths")
        // Classify the paths
        pathList.forEach { path ->
            tryOnScope(
                block = {
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
                },
                onException = {
                    logUtil.log(logTag, "Failed: ${it.message}")
                }
            )
        }

        logUtil.log(logTag, "Reload the archives")
        typedPathList.forEach { typedPath ->
            // For each package
            val packageName = typedPath.name
            logUtil.log(logTag, "Package: $packageName")

            typedPath.typedTimestampList.forEach { typedTimestamp ->
                // For each timestamp
                var packageInfo: PackageInfo? = null
                val timestamp = typedTimestamp.timestamp
                val archivePathList = typedTimestamp.archivePathList
                var compressionType: CompressionType = CompressionType.ZSTD
                var operationCode = OperationMask.None

                logUtil.log(logTag, "Timestamp: $timestamp")
                val tmpApkPath = PathUtil.getTmpApkPath(context = context, packageName = packageName)
                val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = packageName, timestamp = timestamp)
                val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = packageName, timestamp = timestamp)
                remoteRootService.deleteRecursively(tmpApkPath)
                remoteRootService.deleteRecursively(tmpConfigPath)
                remoteRootService.mkdirs(tmpApkPath)
                remoteRootService.mkdirs(tmpConfigPath)

                archivePathList.forEach { archivePath ->
                    // For each archive
                    logUtil.log(logTag, "Archive: ${archivePath.pathString}")
                    tryOnScope(
                        block = {
                            when (archivePath.nameWithoutExtension) {
                                DataType.PACKAGE_APK.type -> {
                                    val type = CompressionType.suffixOf(archivePath.extension)
                                    if (type != null) {
                                        compressionType = type
                                        Tar.decompress(src = archivePath.pathString, dst = tmpApkPath, extra = type.decompressPara).also { result ->
                                            result.logCmd(logUtil = logUtil, logId = logId)
                                        }
                                        remoteRootService.listFilePaths(tmpApkPath).also { pathList ->
                                            if (pathList.isNotEmpty()) {
                                                packageInfo = remoteRootService.getPackageArchiveInfo(pathList.first())
                                                operationCode = operationCode or OperationMask.Apk
                                            }
                                        }
                                    } else {
                                        logUtil.log(logTag, "Failed to parse compression type: ${archivePath.extension}")
                                    }
                                }

                                DataType.PACKAGE_CONFIG.type -> {
                                    val type = CompressionType.suffixOf(archivePath.extension)
                                    if (type != null) {
                                        compressionType = type
                                        Tar.decompress(src = archivePath.pathString, dst = tmpConfigPath, extra = type.decompressPara).also { result ->
                                            result.logCmd(logUtil = logUtil, logId = logId)
                                        }
                                    } else {
                                        logUtil.log(logTag, "Failed to parse compression type: ${archivePath.extension}")
                                    }
                                }

                                DataType.PACKAGE_USER.type -> {
                                    operationCode = operationCode or OperationMask.Data
                                }
                            }
                        },
                        onException = {
                            logUtil.log(logTag, "Failed: ${it.message}")
                        }
                    )
                }

                tryOnScope(
                    block = {
                        // Check config first
                        val packageRestoreEntire: PackageRestoreEntire
                        if (remoteRootService.exists(tmpConfigFilePath)) {
                            // Directly read from config
                            val json = remoteRootService.readText(tmpConfigFilePath)
                            val type = object : TypeToken<PackageRestoreEntire>() {}.type
                            packageRestoreEntire = gsonUtil.fromJson(json, type)
                        } else {
                            packageRestoreEntire = PackageRestoreEntire(
                                packageName = packageName,
                                backupOpCode = operationCode,
                                timestamp = timestamp,
                                compressionType = compressionType,
                                savePath = "",
                            )
                            packageInfo?.apply {
                                packageRestoreEntire.also { entity ->
                                    entity.label = applicationInfo.loadLabel(packageManager).toString()
                                    entity.versionName = versionName ?: ""
                                    entity.versionCode = longVersionCode
                                    entity.flags = applicationInfo.flags
                                }
                                val icon = applicationInfo.loadIcon(packageManager)
                                EnvUtil.saveIcon(context, packageName, icon)
                                logUtil.log(logTag, "Icon saved")
                            }
                        }
                        packageRestoreEntire.savePath = PathUtil.getRestoreSavePath()
                        packageRestoreEntireDao.upsert(packageRestoreEntire)
                    },
                    onException = {
                        logUtil.log(logTag, "Failed: ${it.message}")
                    }
                )

                remoteRootService.deleteRecursively(tmpApkPath)
                remoteRootService.deleteRecursively(tmpConfigPath)
            }
        }
    }

    private suspend fun reloadMedium(context: Context, logTag: String, logId: Long, remoteRootService: RemoteRootService) {
        val pathList = remoteRootService.walkFileTree(PathUtil.getRestoreMediumSavePath())
        val typedPathList = mutableListOf<TypedPath>()

        logUtil.log(logTag, "Clear the table")
        // Clear table first
        mediaDao.clearRestoreTable()

        logUtil.log(logTag, "Classify the paths")
        // Classify the paths
        pathList.forEach { path ->
            tryOnScope(
                block = {
                    val pathListSize = path.pathList.size
                    val name = path.pathList[pathListSize - 3]
                    val timestamp = path.pathList[pathListSize - 2].toLong()
                    val typedPathListIndex = typedPathList.indexOfLast { it.name == name }
                    if (typedPathListIndex == -1) {
                        val typedTimestamp = TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
                        typedPathList.add(TypedPath(name = name, typedTimestampList = mutableListOf(typedTimestamp)))
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
                },
                onException = {
                    logUtil.log(logTag, "Failed: ${it.message}")
                }
            )
        }

        logUtil.log(logTag, "Reload the medium")
        typedPathList.forEach { typedPath ->
            // For each media
            val name = typedPath.name
            logUtil.log(logTag, "Media: $name")

            typedPath.typedTimestampList.forEach timestamp@{ typedTimestamp ->
                // For each timestamp
                val timestamp = typedTimestamp.timestamp
                val archivePathList = typedTimestamp.archivePathList
                var mediaExists = false

                logUtil.log(logTag, "Timestamp: $timestamp")
                val tmpConfigPath = PathUtil.getTmpConfigPath(context = context, name = name, timestamp = timestamp)
                val tmpConfigFilePath = PathUtil.getTmpConfigFilePath(context = context, name = name, timestamp = timestamp)
                remoteRootService.deleteRecursively(tmpConfigPath)
                remoteRootService.mkdirs(tmpConfigPath)

                archivePathList.forEach { archivePath ->
                    // For each archive
                    logUtil.log(logTag, "Archive: ${archivePath.pathString}")
                    tryOnScope(
                        block = {
                            when (archivePath.nameWithoutExtension) {
                                DataType.MEDIA_MEDIA.type -> {
                                    mediaExists = true
                                }

                                DataType.PACKAGE_CONFIG.type -> {
                                    val type = CompressionType.suffixOf(archivePath.extension)
                                    if (type != null) {
                                        Tar.decompress(src = archivePath.pathString, dst = tmpConfigPath, extra = type.decompressPara).also { result ->
                                            result.logCmd(logUtil = logUtil, logId = logId)
                                        }
                                    } else {
                                        logUtil.log(logTag, "Failed to parse compression type: ${archivePath.extension}")
                                    }
                                }
                            }
                        },
                        onException = {
                            logUtil.log(logTag, "Failed: ${it.message}")
                        }
                    )
                }

                // If the media archive doesn't exist, continue for the next one.
                if (mediaExists.not()) return@timestamp

                // Check config first
                val mediaRestoreEntity = if (remoteRootService.exists(tmpConfigFilePath)) {
                    // Directly read from config
                    val json = remoteRootService.readText(tmpConfigFilePath)
                    val type = object : TypeToken<MediaRestoreEntity>() {}.type
                    gsonUtil.fromJson(json, type)
                } else {
                    MediaRestoreEntity(
                        timestamp = timestamp,
                        path = "",
                        name = name,
                        sizeBytes = timestamp,
                        selected = false,
                        savePath = ""
                    )
                }
                mediaRestoreEntity.savePath = PathUtil.getRestoreSavePath()
                mediaDao.upsertRestore(mediaRestoreEntity)

                remoteRootService.deleteRecursively(tmpConfigPath)
            }
        }
    }

    suspend fun reload(context: Context) = withIOContext {
        val logTag = "Reload"
        val logId = logUtil.log(logTag, "Start reloading...")
        val remoteRootService = RemoteRootService(context)

        // Create icon dir if it doesn't exist, then copy icons from backup dir.
        logUtil.log(logTag, "Try to create icon dir")
        EnvUtil.createIconDirectory(context)
        remoteRootService.copyRecursively(path = PathUtil.getRestoreIconSavePath(), targetPath = context.iconPath(), overwrite = true)

        // Packages
        reloadPackages(context = context, logTag = logTag, logId = logId, remoteRootService = remoteRootService)

        // Medium
        reloadMedium(context = context, logTag = logTag, logId = logId, remoteRootService = remoteRootService)

        remoteRootService.destroyService()
    }
}
