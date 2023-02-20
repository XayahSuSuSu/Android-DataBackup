package com.xayah.databackup.compose.ui.activity.settings.components.content

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.settings.components.*
import com.xayah.databackup.util.*
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

/**
 * 应用相关设置项
 */
@ExperimentalMaterial3Api
fun LazyListScope.appItems(context: Context, explorer: MaterialYouFileExplorer) {
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
}