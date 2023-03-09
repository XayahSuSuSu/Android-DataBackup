package com.xayah.databackup.ui.activity.list.common.components.item

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.xayah.databackup.R
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.BlackListItem
import com.xayah.databackup.ui.components.TextButton
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.readBlackListMapPath
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun AppBackupItem(
    appInfoBackup: AppInfoBackup,
    modifier: Modifier = Modifier,
    onItemUpdate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ListItem(
        modifier = modifier,
        icon = rememberDrawablePainter(drawable = appInfoBackup.detailBase.appIcon),
        title = appInfoBackup.detailBase.appName,
        subtitle = appInfoBackup.detailBase.packageName,
        appSelected = appInfoBackup.selectApp,
        dataSelected = appInfoBackup.selectData,
        chipContent = {
            if (appInfoBackup.detailBackup.versionName.isNotEmpty()) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(appInfoBackup.detailBackup.versionName) }
                )
            }
            if (appInfoBackup.sizeBytes != 0.0) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(appInfoBackup.sizeDisplay) }
                )
            }
        },
        actionContent = {
            TextButton(
                text = stringResource(R.string.blacklist),
                onClick = {
                    scope.launch {
                        Command.addBlackList(
                            context.readBlackListMapPath(), BlackListItem(
                                appName = appInfoBackup.detailBase.appName,
                                packageName = appInfoBackup.detailBase.packageName
                            )
                        )
                        GlobalObject.getInstance().appInfoBackupMap.value.remove(
                            appInfoBackup.detailBase.packageName
                        )
                        onItemUpdate()
                    }

                }
            )
        },
        onClick = {
            if ((appInfoBackup.selectApp.value && appInfoBackup.selectData.value).not() &&
                (appInfoBackup.selectApp.value || appInfoBackup.selectData.value)
            ) {
                if (appInfoBackup.selectApp.value.not()) {
                    appInfoBackup.selectApp.value = appInfoBackup.selectApp.value.not()
                } else {
                    appInfoBackup.selectData.value = appInfoBackup.selectData.value.not()
                }
            } else {
                appInfoBackup.selectApp.value = appInfoBackup.selectApp.value.not()
                appInfoBackup.selectData.value = appInfoBackup.selectData.value.not()
            }
        }
    )
}
