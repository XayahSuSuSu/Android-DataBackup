package com.xayah.feature.setup.page.two

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyCheckKeystore
import com.xayah.core.datastore.KeyLoadSystemApps
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.datastore.readBackupSavePathSaved
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.getActivity
import com.xayah.core.util.navigateSingle
import com.xayah.feature.setup.R
import com.xayah.feature.setup.SetupRoutes
import com.xayah.feature.setup.SetupScaffold

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageTwo() {
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val backupSavePathSaved by context.readBackupSavePathSaved().collectAsStateWithLifecycle(initialValue = false)
    val backupSavePath by context.readBackupSavePath().collectAsStateWithLifecycle(initialValue = "")

    SetupScaffold(
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = null,
                title = stringResource(id = R.string.setup)
            )
        },
        actions = {
            AnimatedVisibility(visible = backupSavePathSaved.not()) {
                OutlinedButton(
                    onClick = {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = context.getString(R.string.skip_setup), text = context.getString(R.string.skip_setup_alert))) {
                                viewModel.emitIntent(IndexUiIntent.ToMain(context = context.getActivity()))
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.skip_setup))
                }
            }
            Button(
                enabled = backupSavePathSaved,
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.ToMain(context = context.getActivity()))
                }
            ) {
                Text(text = stringResource(id = R.string.finish))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Clickable(
                title = stringResource(id = R.string.backup_dir),
                value = if (backupSavePathSaved) backupSavePath else context.getString(R.string.not_selected),
                desc = if (backupSavePathSaved) null else stringResource(id = R.string.setup_backup_dir_desc),
            ) {
                navController.navigateSingle(SetupRoutes.Directory.route)
            }
            Title(title = stringResource(id = R.string.optional)) {
                AnimatedVisibility(visible = backupSavePathSaved) {
                    Clickable(
                        title = stringResource(id = R.string.configurations),
                        value = stringResource(id = R.string.configurations_desc),
                    ) {
                        navController.navigateSingle(SetupRoutes.Configurations.route)
                    }
                }
                Switchable(
                    key = KeyLoadSystemApps,
                    defValue = false,
                    title = stringResource(id = R.string.load_system_apps),
                    checkedText = stringResource(id = R.string.enabled),
                    notCheckedText = stringResource(id = R.string.not_enabled),
                )
                Switchable(
                    key = KeyCheckKeystore,
                    defValue = true,
                    title = stringResource(id = R.string.check_keystore),
                    checkedText = stringResource(id = R.string.enabled),
                    notCheckedText = stringResource(id = R.string.not_enabled),
                    desc = stringResource(id = R.string.set_them_later_in_settings)
                )
            }
        }
    }
}
