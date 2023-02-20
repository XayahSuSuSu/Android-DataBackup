package com.xayah.databackup.compose.ui.activity.settings.components

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.compose.ui.activity.settings.components.content.appItems
import com.xayah.databackup.compose.ui.activity.settings.components.content.backupItems
import com.xayah.databackup.compose.ui.activity.settings.components.content.restoreItems
import com.xayah.databackup.compose.ui.activity.settings.components.content.userItems
import com.xayah.databackup.compose.ui.components.Scaffold
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
fun SettingsScaffold(
    viewModel: SettingsViewModel,
    explorer: MaterialYouFileExplorer,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
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
                scrollBehavior = this,
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
        topPaddingRate = 1,
        content = {
            // 应用
            appItems(context = context, explorer = explorer)

            // 用户
            userItems(list = viewModel.userSingleChoiceTextClickableItemsItems.value)

            // 备份
            backupItems(context = context, list = viewModel.backupSwitchItems.value)

            // 恢复
            restoreItems(list = viewModel.restoreSwitchItems.value)
        }
    )
}
