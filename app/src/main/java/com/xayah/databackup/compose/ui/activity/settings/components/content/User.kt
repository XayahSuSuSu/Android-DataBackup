package com.xayah.databackup.compose.ui.activity.settings.components.content

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.compose.ui.activity.settings.components.SingleChoiceTextClickable
import com.xayah.databackup.compose.ui.activity.settings.components.SingleChoiceTextClickableItem
import com.xayah.databackup.compose.ui.activity.settings.components.Title
import com.xayah.databackup.util.*

fun onUserInitialize(viewModel: SettingsViewModel, context: Context) {
    if (viewModel.userSingleChoiceTextClickableItemsItems.value.isEmpty())
        viewModel.userSingleChoiceTextClickableItemsItems.value.apply {
            add(SingleChoiceTextClickableItem(
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
            ))
            add(SingleChoiceTextClickableItem(
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
