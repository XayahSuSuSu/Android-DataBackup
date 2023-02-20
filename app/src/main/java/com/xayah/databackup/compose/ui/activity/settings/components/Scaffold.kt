package com.xayah.databackup.compose.ui.activity.settings.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupStrategy
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.util.*
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

fun onSetBackupSavePath(context: Context, path: String) {
    context.saveBackupSavePath(path)
    RemoteFile.getInstance().mkdirs(Path.getLogPath())
    RemoteFile.getInstance()
        .createNewFile("${App.globalContext.readBackupSavePath()}/.nomedia")
    Logcat.refreshInstance()
    GlobalObject.getInstance().appInfoBackupMap.value.clear()
    GlobalObject.getInstance().appInfoRestoreMap.value.clear()
    GlobalObject.getInstance().mediaInfoBackupMap.value.clear()
    GlobalObject.getInstance().mediaInfoRestoreMap.value.clear()
}

@ExperimentalMaterial3Api
@Composable
fun SettingsScaffold(explorer: MaterialYouFileExplorer, onFinish: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    val singleChoiceTextClickableItems = remember {
        mutableListOf(
            SingleChoiceTextClickableItem(
                title = context.getString(R.string.backup_user),
                subtitle = context.getString(R.string.settings_backup_user_subtitle),
                iconId = R.drawable.ic_round_person,
                content = context.readBackupUser(),
                onPrepare = {
                    var items =
                        if (Bashrc.listUsers().first) Bashrc.listUsers().second else mutableListOf(
                            GlobalObject.defaultUserId
                        )
                    // 加入备份目录用户集
                    items.addAll(Command.listBackupUsers())
                    // 去重排序
                    items = items.toSortedSet().toMutableList()
                    val value = context.readBackupUser()
                    Pair(items, value)
                },
                onConfirm = {
                    GlobalObject.getInstance().appInfoBackupMap.value.clear()
                    GlobalObject.getInstance().appInfoRestoreMap.value.clear()
                    context.saveBackupUser(it)
                }
            ),
            SingleChoiceTextClickableItem(
                title = context.getString(R.string.restore_user),
                subtitle = context.getString(R.string.settings_restore_user_subtitle),
                iconId = R.drawable.ic_round_iphone,
                content = context.readRestoreUser(),
                onPrepare = {
                    val items =
                        if (Bashrc.listUsers().first) Bashrc.listUsers().second else mutableListOf(
                            GlobalObject.defaultUserId
                        )
                    val value = context.readRestoreUser()
                    Pair(items, value)
                },
                onConfirm = {
                    context.saveRestoreUser(it)
                }
            )
        )
    }

    val backupSwitchItems = remember {
        mutableListOf(
            SwitchItem(
                title = context.getString(R.string.backup_itself),
                subtitle = context.getString(R.string.backup_itself_title),
                iconId = R.drawable.ic_round_join_left,
                isChecked = mutableStateOf(context.readIsBackupItself()),
                onCheckedChange = {
                    context.saveIsBackupItself(it)
                }
            ),
            SwitchItem(
                title = context.getString(R.string.backup_icon),
                subtitle = context.getString(R.string.backup_icon_title),
                iconId = R.drawable.ic_round_image,
                isChecked = mutableStateOf(context.readIsBackupIcon()),
                onCheckedChange = {
                    context.saveIsBackupIcon(it)
                }
            ),
            SwitchItem(
                title = context.getString(R.string.backup_test),
                subtitle = context.getString(R.string.backup_test_title),
                iconId = R.drawable.ic_round_layers,
                isChecked = mutableStateOf(context.readIsBackupTest()),
                onCheckedChange = {
                    context.saveIsBackupTest(it)
                }
            )
        )
    }

    val supportAutoFixMultiUserContext = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(null) {
        supportAutoFixMultiUserContext.value = Command.checkLsZd()
        context.saveAutoFixMultiUserContext(supportAutoFixMultiUserContext.value)
    }
    val restoreSwitchItems = remember {
        mutableListOf(
            SwitchItem(
                title = context.getString(R.string.auto_fix_multi_user_context),
                subtitle = context.getString(R.string.auto_fix_multi_user_context_title),
                iconId = R.drawable.ic_round_apps,
                isChecked = supportAutoFixMultiUserContext,
                isEnabled = false,
                onCheckedChange = {}
            ),
            SwitchItem(
                title = context.getString(R.string.read_icon),
                subtitle = context.getString(R.string.read_icon_title),
                iconId = R.drawable.ic_round_image,
                isChecked = mutableStateOf(context.readIsReadIcon()),
                onCheckedChange = {
                    context.saveIsReadIcon(it)
                }
            ),
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = com.xayah.materialyoufileexplorer.R.drawable.ic_round_arrow_back),
                            contentDescription = null
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(mediumPadding, nonePadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateTopPadding())
                )
            }

            // 应用
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

            // 用户
            item {
                Title(title = stringResource(id = R.string.user))
            }
            items(
                count = singleChoiceTextClickableItems.size,
                key = {
                    singleChoiceTextClickableItems[it].title
                }) {
                SingleChoiceTextClickable(
                    title = singleChoiceTextClickableItems[it].title,
                    subtitle = singleChoiceTextClickableItems[it].subtitle,
                    icon = ImageVector.vectorResource(id = singleChoiceTextClickableItems[it].iconId),
                    content = singleChoiceTextClickableItems[it].content,
                    onPrepare = {
                        singleChoiceTextClickableItems[it].onPrepare()
                    },
                    onConfirm = { value ->
                        singleChoiceTextClickableItems[it].onConfirm(value)
                    }
                )
            }

            // 备份
            item {
                Title(title = stringResource(id = R.string.backup))
            }
            items(
                count = backupSwitchItems.size,
                key = {
                    backupSwitchItems[it].title
                }) {
                Switch(
                    title = backupSwitchItems[it].title,
                    subtitle = backupSwitchItems[it].subtitle,
                    icon = ImageVector.vectorResource(id = backupSwitchItems[it].iconId),
                    isChecked = backupSwitchItems[it].isChecked,
                    onCheckedChange = backupSwitchItems[it].onCheckedChange
                )
            }
            item {
                val items =
                    listOf(
                        DescItem(
                            title = context.getString(R.string.cover),
                            subtitle = context.getString(R.string.cover_desc),
                        ),
                        DescItem(
                            title = context.getString(R.string.by_time),
                            subtitle = context.getString(R.string.by_time_desc),
                        )

                    )
                val enumItems = arrayOf(BackupStrategy.Cover, BackupStrategy.ByTime)
                SingleChoiceDescClickable(
                    title = stringResource(id = R.string.backup_strategy),
                    subtitle = stringResource(R.string.backup_storage_method),
                    icon = Icons.Rounded.Place,
                    content = ofBackupStrategy(context.readBackupStrategy()),
                    onPrepare = {
                        var value =
                            items.find { it.title == ofBackupStrategy(context.readBackupStrategy()) }
                        if (value == null) value = items[0]
                        Pair(items, value!!)
                    },
                    onConfirm = { value ->
                        try {
                            context.saveBackupStrategy(enumItems[items.indexOf(value)])
                        } catch (e: Exception) {
                            context.saveBackupStrategy(BackupStrategy.Cover)
                        }
                    }
                )
            }

            // 恢复
            item {
                Title(title = stringResource(id = R.string.restore))
            }
            items(
                count = restoreSwitchItems.size,
                key = {
                    restoreSwitchItems[it].title
                }) {
                Switch(
                    title = restoreSwitchItems[it].title,
                    subtitle = restoreSwitchItems[it].subtitle,
                    icon = ImageVector.vectorResource(id = restoreSwitchItems[it].iconId),
                    isChecked = restoreSwitchItems[it].isChecked,
                    isEnabled = restoreSwitchItems[it].isEnabled,
                    onCheckedChange = restoreSwitchItems[it].onCheckedChange
                )
            }

            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateBottomPadding())
                )
            }
        }
    }
}