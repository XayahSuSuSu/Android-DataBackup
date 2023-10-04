package com.xayah.databackup.ui.activity.main.page.restore

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookRestoreCard
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.command.InstallationUtil
import com.xayah.databackup.util.iconPath
import com.xayah.librootservice.parcelables.PathParcelable
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.ExceptionUtil.tryOnScope
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch

private data class TypedTimestamp(
    val timestamp: Long,
    val archivePathList: MutableList<PathParcelable>,
)

private data class TypedPath(
    val packageName: String,
    val typedTimestampList: MutableList<TypedTimestamp>,
)

@ExperimentalMaterial3Api
private suspend fun DialogState.openReloadDialog(context: Context, logUtil: LogUtil, packageRestoreEntireDao: PackageRestoreEntireDao, gsonUtil: GsonUtil) {
    openLoading(
        title = context.getString(R.string.prompt),
        icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_folder_open),
        onLoading = {
            withIOContext {
                val logTag = "Reload"
                val logId = logUtil.log(logTag, "Start reloading...")
                val remoteRootService = RemoteRootService(context)
                val packageManager = context.packageManager
                val installationUtil = InstallationUtil(logId, logUtil)
                val pathList = remoteRootService.walkFileTree(PathUtil.getRestorePackagesSavePath())
                val typedPathList = mutableListOf<TypedPath>()

                logUtil.log(logTag, "Clear the table and try to create icon dir")
                // Clear table first
                packageRestoreEntireDao.clearTable()
                // Create icon dir if it doesn't exist, then copy icons from backup dir.
                EnvUtil.createIconDirectory(context)
                remoteRootService.copyRecursively(path = PathUtil.getRestoreIconSavePath(), targetPath = context.iconPath(), overwrite = true)

                logUtil.log(logTag, "Classify the paths")
                // Classify the paths
                pathList.forEach { path ->
                    tryOnScope(
                        block = {
                            val pathListSize = path.pathList.size
                            val packageName = path.pathList[pathListSize - 3]
                            val timestamp = path.pathList[pathListSize - 2].toLong()
                            val typedPathListIndex = typedPathList.indexOfLast { it.packageName == packageName }
                            if (typedPathListIndex == -1) {
                                val typedTimestamp = TypedTimestamp(timestamp = timestamp, archivePathList = mutableListOf(path))
                                typedPathList.add(TypedPath(packageName = packageName, typedTimestampList = mutableListOf(typedTimestamp)))
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
                    val packageName = typedPath.packageName
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
                                                installationUtil.decompress(
                                                    archivePath = archivePath.pathString,
                                                    tmpApkPath = tmpApkPath,
                                                    compressionType = type
                                                )
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
                                                installationUtil.decompress(
                                                    archivePath = archivePath.pathString,
                                                    tmpApkPath = tmpConfigPath,
                                                    compressionType = type
                                                )
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
                                label = "",
                                backupOpCode = operationCode,
                                operationCode = OperationMask.None,
                                timestamp = timestamp,
                                versionName = "",
                                versionCode = 0,
                                flags = 0,
                                compressionType = compressionType,
                                active = false
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
                        packageRestoreEntireDao.upsert(packageRestoreEntire)

                        remoteRootService.deleteRecursively(tmpApkPath)
                        remoteRootService.deleteRecursively(tmpConfigPath)
                    }
                }
                remoteRootService.destroyService()
            }
        },
    )
}

@ExperimentalLayoutApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel = hiltViewModel<RestoreViewModel>()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val navController = LocalSlotScope.current!!.navController
    val uiState by viewModel.uiState

    LazyColumn(
        modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
            OverLookRestoreCard()
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val actions = listOf(
                    stringResource(R.string.reload),
                    stringResource(R.string.directory),
                    stringResource(R.string.structure),
                    stringResource(R.string.log)
                )
                val icons = listOf(
                    Icons.Rounded.Refresh,
                    ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                    ImageVector.vectorResource(R.drawable.ic_rounded_account_tree),
                    ImageVector.vectorResource(R.drawable.ic_rounded_bug_report)
                )
                val onClicks = listOf<suspend () -> Unit>(
                    {
                        dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_reload)).also { (confirmed, _) ->
                            if (confirmed) dialogSlot.openReloadDialog(context, uiState.logUtil, uiState.packageRestoreEntireDao, uiState.gsonUtil)
                        }
                    },
                    {
                        IntentUtil.toDirectoryActivity(context = context, route = DirectoryRoutes.DirectoryRestore)
                    },
                    {
                        navController.navigate(MainRoutes.Tree.route)
                    },
                    {
                        navController.navigate(MainRoutes.Log.route)
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(actions.size) { index ->
                        CardActionButton(
                            modifier = Modifier.weight(1f),
                            text = actions[index],
                            icon = icons[index],
                            onClick = {
                                scope.launch {
                                    onClicks[index]()
                                }
                            }
                        )
                        if (index != actions.size - 1) Spacer(modifier = Modifier.weight(0.25f))
                    }
                }
            }
        }

        item {
            Module(title = stringResource(R.string.activities)) {
                val items = listOf(
                    stringResource(R.string.app_and_data),
                    stringResource(R.string.media),
                    stringResource(R.string.telephony)
                )
                val icons = listOf(
                    ImageVector.vectorResource(R.drawable.ic_rounded_palette),
                    ImageVector.vectorResource(R.drawable.ic_rounded_image),
                    ImageVector.vectorResource(R.drawable.ic_rounded_call),
                )
                val onClicks = listOf(
                    {
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.PackageRestore)
                    },
                    {
                        IntentUtil.toOperationActivity(context = context, route = OperationRoutes.MediaRestore)
                    },
                    {}
                )
                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
                ) { index ->
                    AssistChip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClicks[index],
                        label = { Text(items[index]) },
                        leadingIcon = {
                            Icon(
                                imageVector = icons[index],
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
        }
    }
}
