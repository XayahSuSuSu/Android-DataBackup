package com.xayah.databackup.ui.activity.settings.components

import android.content.Context
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.settings.SettingsViewModel
import com.xayah.databackup.ui.activity.settings.components.content.appItems
import com.xayah.databackup.ui.activity.settings.components.content.backupItems
import com.xayah.databackup.ui.activity.settings.components.content.restoreItems
import com.xayah.databackup.ui.activity.settings.components.content.userItems
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle
import com.xayah.databackup.util.*
import com.xayah.databackup.librootservice.RootService
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

fun onSetBackupSavePath(context: Context, path: String) {
    context.saveBackupSavePath(path)
    initializeBackupDirectory()
    GlobalObject.getInstance().appInfoBackupMap.value.clear()
    GlobalObject.getInstance().appInfoRestoreMap.value.clear()
    GlobalObject.getInstance().mediaInfoBackupMap.value.clear()
    GlobalObject.getInstance().mediaInfoRestoreMap.value.clear()
}

fun initializeBackupDirectory() {
    RootService.getInstance().mkdirs(Path.getLogPath())
    RootService.getInstance()
        .createNewFile("${App.globalContext.readBackupSavePath()}/.nomedia")
    Logcat.refreshInstance()
}

@ExperimentalMaterial3Api
@Composable
fun SettingsScaffold(
    isInitialized: MutableTransitionState<Boolean>,
    viewModel: SettingsViewModel,
    explorer: MaterialYouFileExplorer,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = stringResource(id = R.string.settings))
                },
                scrollBehavior = this,
                navigationIcon = {
                    IconButton(
                        icon = Icons.Rounded.ArrowBack,
                        onClick = onFinish
                    )
                },
            )
        },
        topPaddingRate = 1,
        isInitialized = isInitialized,
        content = {
            // 应用
            appItems(viewModel = viewModel, context = context, scope = scope, explorer = explorer)

            // 用户
            userItems(list = viewModel.userSingleChoiceTextClickableItemsItems.value)

            // 备份
            backupItems(context = context, list = viewModel.backupSwitchItems.value)

            // 恢复
            restoreItems(list = viewModel.restoreSwitchItems.value)
        }
    )
}
