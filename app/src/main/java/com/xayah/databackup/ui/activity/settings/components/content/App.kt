package com.xayah.databackup.ui.activity.settings.components.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.data.UpdateChannel
import com.xayah.databackup.data.ofUpdateChannel
import com.xayah.databackup.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.ui.activity.settings.components.DescItem
import com.xayah.databackup.ui.activity.settings.components.StorageRadioDialogItem
import com.xayah.databackup.ui.activity.settings.components.clickable.*
import com.xayah.databackup.ui.activity.settings.components.onSetBackupSavePath
import com.xayah.databackup.util.*
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

suspend fun onAppInitialize(viewModel: SettingsViewModel, context: Context) {
    if (viewModel.newestVersion.value.isEmpty() || viewModel.newestVersionLink.value.isEmpty()) {
        Server.getInstance().releases(
            successCallback = { releaseList ->
                val mReleaseList = releaseList.appReleaseList()
                if (mReleaseList.isEmpty()) {
                    viewModel.newestVersion.value = context.getString(R.string.fetch_failed)
                } else {
                    viewModel.newestVersion.value = mReleaseList[0].name.replace("App ", "")
                    if (viewModel.newestVersion.value.contains(BuildConfig.VERSION_NAME).not()) {
                        viewModel.newestVersionLink.value = mReleaseList[0].html_url
                    }
                }
            },
            failedCallback = {
                viewModel.newestVersion.value = context.getString(R.string.fetch_failed)
            })
    }
}

/**
 * 应用相关设置项
 */
@ExperimentalMaterial3Api
fun LazyListScope.appItems(
    viewModel: SettingsViewModel,
    context: Context,
    explorer: MaterialYouFileExplorer
) {
    item {
        Title(title = stringResource(id = R.string.application))
    }
    item {
        Switch(
            title = stringResource(id = R.string.dynamic_colors),
            subtitle = stringResource(id = R.string.dynamic_colors_title),
            isChecked = remember {
                mutableStateOf(context.readIsDynamicColors())
            },
            icon = ImageVector.vectorResource(id = R.drawable.ic_round_auto_awesome)
        ) {
            context.saveIsDynamicColors(it)
        }
    }
    item {
        SingleChoiceTextClickable(
            title = stringResource(id = R.string.compression_type),
            subtitle = stringResource(id = R.string.compression_type_help),
            icon = ImageVector.vectorResource(id = R.drawable.ic_round_bolt),
            content = context.readCompressionType(),
            onPrepare = {
                val items = listOf("tar", "zstd", "lz4")
                val value = context.readCompressionType()
                Pair(items, value)
            },
            onConfirm = { value ->
                context.saveCompressionType(value)
            }
        )
    }
    item {
        // 检查是否存在外置存储
        val backupPath = remember {
            mutableStateOf(context.readBackupSavePath())
        }
        LaunchedEffect(null) {
            val (_, list) = Bashrc.listExternalStorage()
            if (list.isEmpty()) {
                val customPath = context.readCustomBackupSavePath()
                onSetBackupSavePath(context, customPath)
                backupPath.value = customPath
            }
        }

        StorageRadioClickable(
            subtitle = backupPath,
            onPrepare = {
                // 内置存储
                val internalPath = context.readCustomBackupSavePath()
                val internalItem = StorageRadioDialogItem(
                    title = context.getString(R.string.internal_storage),
                    progress = 0f,
                    path = internalPath,
                    display = internalPath,
                )
                val (internalSuccess, internalSpace) = Bashrc.getStorageSpace(internalPath)
                val string =
                    if (internalSuccess) internalSpace else context.getString(R.string.error)
                if (internalSuccess) {
                    try {
                        internalItem.progress =
                            string.split(" ").last().replace("%", "").toFloat() / 100
                    } catch (e: Exception) {
                        internalItem.display = context.getString(R.string.fetch_failed)
                    }
                } else {
                    internalItem.display = context.getString(R.string.fetch_failed)
                }

                val items = mutableListOf(
                    internalItem
                )

                // 外置存储: /mnt/media_rw/E7F9-FA61 exfat
                val (listExternalStorageSuccess, list) = Bashrc.listExternalStorage()
                if (listExternalStorageSuccess) {
                    for (i in list) {
                        try {
                            val (path, type) = i.split(" ")
                            val item = StorageRadioDialogItem(
                                title = path.split("/").last(),
                                progress = 0f,
                                path = "",
                                display = i,
                            )

                            // 设置目录
                            val outPath = "${path}/DataBackup"
                            item.path = outPath

                            // 检测空间占用
                            val (success, space) = Bashrc.getStorageSpace(outPath)
                            if (success) {
                                try {
                                    item.progress =
                                        space.split(" ").last().replace("%", "")
                                            .toFloat() / 100
                                } catch (e: Exception) {
                                    item.display = context.getString(R.string.fetch_failed)
                                }
                            }

                            // 检测格式是否支持
                            val supportedFormat =
                                mutableListOf(
                                    "sdfat",
                                    "fuseblk",
                                    "exfat",
                                    "ntfs",
                                    "ext4",
                                    "f2fs"
                                )
                            val support = type.lowercase() in supportedFormat
                            item.enabled = support
                            if (support.not()) {
                                item.title = context.getString(R.string.unsupported_format)
                            }

                            items.add(item)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                var value = items.find { it.path == context.readBackupSavePath() }
                if (value == null) value = items[0]
                Pair(items, value)
            },
            onConfirm = { value ->
                onSetBackupSavePath(context, value.path)
            },
            onEdit = {
                explorer.apply {
                    isFile = false
                    toExplorer(context) { actualPath, _ ->
                        val pathList = actualPath.split("/").toMutableList()
                        if (pathList.last() != "DataBackup")
                            pathList.add("DataBackup")
                        val path = pathList.joinToString(separator = "/")
                        backupPath.value = path
                        context.saveCustomBackupSavePath(path)
                        onSetBackupSavePath(context, path)
                    }
                }
            }
        )
    }
    item {
        val items =
            listOf(
                DescItem(
                    title = stringResource(id = R.string.stable),
                    subtitle = stringResource(id = R.string.stable_subtitle),
                    enabled = false
                ),
                DescItem(
                    title = stringResource(id = R.string.test),
                    subtitle = stringResource(id = R.string.test_subtitle),
                )
            )
        val enumItems = arrayOf(UpdateChannel.Stable, UpdateChannel.Test)
        SingleChoiceDescClickable(
            title = stringResource(id = R.string.update_channel),
            subtitle = stringResource(id = R.string.update_channel_subtitle),
            icon = ImageVector.vectorResource(id = R.drawable.ic_round_key),
            content = ofUpdateChannel(context.readUpdateChannel()),
            onPrepare = {
                var value =
                    items.find { it.title == ofUpdateChannel(context.readUpdateChannel()) }
                if (value == null) value = items[0]
                Pair(items, value!!)
            },
            onConfirm = { value ->
                try {
                    context.saveUpdateChannel(enumItems[items.indexOf(value)])
                } catch (e: Exception) {
                    context.saveUpdateChannel(UpdateChannel.Stable)
                }
            }
        )
    }
    item {
        val toLink = {
            if (viewModel.newestVersionLink.value.isNotEmpty()) {
                val uri = Uri.parse(viewModel.newestVersionLink.value)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
        }
        IconButtonClickable(
            title = stringResource(id = R.string.update),
            subtitle = "${stringResource(id = R.string.current)}: ${BuildConfig.VERSION_NAME}\n" +
                    "${stringResource(id = R.string.latest)}: ${viewModel.newestVersion.value}",
            icon = ImageVector.vectorResource(id = R.drawable.ic_outline_light),
            showIconButton = viewModel.newestVersionLink.value.isNotEmpty(),
            iconButton = ImageVector.vectorResource(id = R.drawable.ic_round_download),
            onClick = {
                toLink()
            },
            onIconButtonClick = {
                toLink()
            }
        )
    }
    item {
        val toGitHub = {
            val uri = Uri.parse(context.getString(R.string.project_github_link))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }
        IconButtonClickable(
            title = stringResource(id = R.string.opensource_link),
            subtitle = stringResource(id = R.string.project_github_link),
            icon = ImageVector.vectorResource(id = R.drawable.logo_github),
            iconButton = ImageVector.vectorResource(id = R.drawable.ic_round_link),
            onClick = {
                toGitHub()
            },
            onIconButtonClick = {
                toGitHub()
            }
        )
    }
}