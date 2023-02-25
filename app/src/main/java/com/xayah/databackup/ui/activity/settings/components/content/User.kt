package com.xayah.databackup.ui.activity.settings.components.content

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.ui.activity.settings.components.SingleChoiceTextClickable
import com.xayah.databackup.ui.activity.settings.components.SingleChoiceTextClickableItem
import com.xayah.databackup.ui.activity.settings.components.Title
import com.xayah.databackup.util.*
import com.xayah.librootservice.RootService

fun onUserInitialize(viewModel: SettingsViewModel, context: Context) {
    if (viewModel.userSingleChoiceTextClickableItemsItems.value.isEmpty())
        viewModel.userSingleChoiceTextClickableItemsItems.value.apply {
            add(SingleChoiceTextClickableItem(
                title = context.getString(R.string.backup_user),
                subtitle = context.getString(R.string.settings_backup_user_subtitle),
                iconId = R.drawable.ic_round_person,
                content = context.readBackupUser(),
                onPrepare = {
                    val users = RootService.getInstance().getUsers(
                        excludePartial = true,
                        excludeDying = false,
                        excludePreCreated = true
                    )
                    var items = users.map { "${it.id}: ${it.name}" }.toMutableList()
                    val idItems = users.map { "${it.id}" }.toMutableList()

                    // 加入备份目录用户集
                    val backupUsers = Command.listBackupUsers()
                    for (i in backupUsers) {
                        if (i !in idItems) {
                            items.add("${i}: ${context.getString(R.string.backup_dir)}")
                            idItems.add(i)
                        }
                    }

                    val defValue = try {
                        items[idItems.indexOf(context.readBackupUser())]
                    } catch (_: Exception) {
                        ""
                    }

                    // 去重排序
                    items = items.toSortedSet().toMutableList()
                    Pair(items, defValue)
                },
                onConfirm = {
                    GlobalObject.getInstance().appInfoBackupMap.value.clear()
                    GlobalObject.getInstance().appInfoRestoreMap.value.clear()
                    try {
                        context.saveBackupUser(it.split(":")[0])
                    } catch (_: Exception) {
                    }
                }
            ))
            add(SingleChoiceTextClickableItem(
                title = context.getString(R.string.restore_user),
                subtitle = context.getString(R.string.settings_restore_user_subtitle),
                iconId = R.drawable.ic_round_iphone,
                content = context.readRestoreUser(),
                onPrepare = {
                    val users = RootService.getInstance().getUsers(
                        excludePartial = true,
                        excludeDying = false,
                        excludePreCreated = true
                    )
                    val items = users.map { "${it.id}: ${it.name}" }.toMutableList()
                    val idItems = users.map { "${it.id}" }.toMutableList()

                    val defValue = try {
                        items[idItems.indexOf(context.readRestoreUser())]
                    } catch (_: Exception) {
                        ""
                    }

                    Pair(items, defValue)
                },
                onConfirm = {
                    try {
                        context.saveRestoreUser(it.split(":")[0])
                    } catch (_: Exception) {
                    }
                }
            ))
        }
}

/**
 * 用户相关设置项
 */
@ExperimentalMaterial3Api
fun LazyListScope.userItems(list: List<SingleChoiceTextClickableItem>) {
    item {
        Title(title = stringResource(id = R.string.user))
    }
    items(
        count = list.size,
        key = {
            list[it].title
        }) {
        SingleChoiceTextClickable(
            title = list[it].title,
            subtitle = list[it].subtitle,
            icon = ImageVector.vectorResource(id = list[it].iconId),
            content = list[it].content,
            onPrepare = {
                list[it].onPrepare()
            },
            onConfirm = { value ->
                list[it].onConfirm(value)
            }
        )
    }
}
