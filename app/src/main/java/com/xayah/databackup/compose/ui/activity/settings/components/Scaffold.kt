package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.xayah.databackup.R
import com.xayah.databackup.util.*

@ExperimentalMaterial3Api
@Composable
fun SettingsScaffold(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    val singleChoiceClickableItems = remember {
        mutableListOf(
            SingleChoiceClickableItem(
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
            SingleChoiceClickableItem(
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
            item {
                Title(title = stringResource(id = R.string.application))
            }
            item {
                Switch(
                    title = stringResource(id = R.string.dynamic_colors),
                    subtitle = stringResource(id = R.string.dynamic_colors_title),
                    isChecked = context.readIsDynamicColors(),
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_auto_awesome)
                ) {
                    context.saveIsDynamicColors(it)
                }
            }
            item {
                SingleChoiceClickable(
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
                Title(title = stringResource(id = R.string.user))
            }
            items(count = singleChoiceClickableItems.size) {
                SingleChoiceClickable(
                    title = singleChoiceClickableItems[it].title,
                    subtitle = singleChoiceClickableItems[it].subtitle,
                    icon = ImageVector.vectorResource(id = singleChoiceClickableItems[it].iconId),
                    content = singleChoiceClickableItems[it].content,
                    onPrepare = {
                        singleChoiceClickableItems[it].onPrepare()
                    },
                    onConfirm = { value ->
                        singleChoiceClickableItems[it].onConfirm(value)
                    }
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