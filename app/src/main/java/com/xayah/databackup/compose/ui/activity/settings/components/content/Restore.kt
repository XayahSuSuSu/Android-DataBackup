package com.xayah.databackup.compose.ui.activity.settings.components.content

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.compose.ui.activity.settings.components.Switch
import com.xayah.databackup.compose.ui.activity.settings.components.SwitchItem
import com.xayah.databackup.compose.ui.activity.settings.components.Title
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.readIsReadIcon
import com.xayah.databackup.util.saveAutoFixMultiUserContext
import com.xayah.databackup.util.saveIsReadIcon

suspend fun onRestoreInitialize(viewModel: SettingsViewModel, context: Context) {
    viewModel.supportAutoFixMultiUserContext.value = Command.checkLsZd()
    context.saveAutoFixMultiUserContext(viewModel.supportAutoFixMultiUserContext.value)
    if (viewModel.restoreSwitchItems.value.isEmpty())
        viewModel.restoreSwitchItems.value.apply {
            add(SwitchItem(
                title = context.getString(R.string.auto_fix_multi_user_context),
                subtitle = context.getString(R.string.auto_fix_multi_user_context_title),
                iconId = R.drawable.ic_round_apps,
                isChecked = viewModel.supportAutoFixMultiUserContext,
                isEnabled = false,
                onCheckedChange = {}
            ))
            add(SwitchItem(
                title = context.getString(R.string.read_icon),
                subtitle = context.getString(R.string.read_icon_title),
                iconId = R.drawable.ic_round_image,
                isChecked = mutableStateOf(context.readIsReadIcon()),
                onCheckedChange = {
                    context.saveIsReadIcon(it)
                }
            ))
        }
}

/**
 * 恢复相关设置项
 */
@ExperimentalMaterial3Api
fun LazyListScope.restoreItems(list: List<SwitchItem>) {
    item {
        Title(title = stringResource(id = R.string.restore))
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
            isEnabled = list[it].isEnabled,
            onCheckedChange = list[it].onCheckedChange
        )
    }
}
