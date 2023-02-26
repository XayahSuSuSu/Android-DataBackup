package com.xayah.databackup.ui.activity.settings.components.content

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupStrategy
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.ui.activity.settings.components.*
import com.xayah.databackup.util.*

fun onBackupInitialize(viewModel: SettingsViewModel, context: Context) {
    if (viewModel.backupSwitchItems.value.isEmpty())
        viewModel.backupSwitchItems.value.apply {
            add(SwitchItem(
                title = context.getString(R.string.backup_itself),
                subtitle = context.getString(R.string.backup_itself_title),
                iconId = R.drawable.ic_round_join_left,
                isChecked = mutableStateOf(context.readIsBackupItself()),
                onCheckedChange = {
                    context.saveIsBackupItself(it)
                }
            ))
            add(SwitchItem(
                title = context.getString(R.string.backup_icon),
                subtitle = context.getString(R.string.backup_icon_title),
                iconId = R.drawable.ic_round_image,
                isChecked = mutableStateOf(context.readIsBackupIcon()),
                onCheckedChange = {
                    context.saveIsBackupIcon(it)
                }
            ))
            add(SwitchItem(
                title = context.getString(R.string.backup_test),
                subtitle = context.getString(R.string.backup_test_title),
                iconId = R.drawable.ic_round_layers,
                isChecked = mutableStateOf(context.readIsBackupTest()),
                onCheckedChange = {
                    context.saveIsBackupTest(it)
                }
            ))
            add(SwitchItem(
                title = context.getString(R.string.compatible_mode),
                subtitle = context.getString(R.string.compatible_mode_subtitle),
                iconId = R.drawable.ic_round_build,
                isChecked = mutableStateOf(context.readCompatibleMode()),
                onCheckedChange = {
                    context.saveCompatibleMode(it)
                }
            ))
        }
}

/**
 * 备份相关设置项
 */
@ExperimentalMaterial3Api
fun LazyListScope.backupItems(context: Context, list: List<SwitchItem>) {
    item {
        Title(title = stringResource(id = R.string.backup))
    }
    items(
        count = list.size,
        key = {
            list[it].title
        }) {
        Switch(
            title = list[it].title,
            subtitle = list[it].subtitle,
            icon = ImageVector.vectorResource(id = list[it].iconId),
            isChecked = list[it].isChecked,
            onCheckedChange = list[it].onCheckedChange
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
                Pair(items, value)
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
}
