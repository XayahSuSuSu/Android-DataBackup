package com.xayah.feature.setup.page.two

import androidx.activity.ComponentActivity
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
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
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
                title = StringResourceToken.fromStringId(R.string.setup)
            )
        },
        actions = {
            AnimatedVisibility(visible = backupSavePathSaved.not()) {
                OutlinedButton(
                    onClick = {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.skip_setup), text = StringResourceToken.fromStringId(R.string.skip_setup_alert))) {
                                viewModel.emitIntent(IndexUiIntent.ToMain(context = context as ComponentActivity))
                            }
                        }
                    }
                ) {
                    Text(text = StringResourceToken.fromStringId(R.string.skip_setup).value)
                }
            }
            Button(
                enabled = backupSavePathSaved,
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.ToMain(context = context as ComponentActivity))
                }
            ) {
                Text(text = StringResourceToken.fromStringId(R.string.finish).value)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Clickable(
                title = StringResourceToken.fromStringId(R.string.backup_dir),
                value = StringResourceToken.fromString(if (backupSavePathSaved) backupSavePath else context.getString(R.string.not_selected)),
                desc = if (backupSavePathSaved) null else StringResourceToken.fromStringId(R.string.setup_backup_dir_desc),
            ) {
                navController.navigate(SetupRoutes.Directory.route)
            }
            Title(title = StringResourceToken.fromStringId(R.string.optional)) {
                Switchable(
                    key = KeyLoadSystemApps,
                    defValue = false,
                    title = StringResourceToken.fromStringId(R.string.load_system_apps),
                    checkedText = StringResourceToken.fromStringId(R.string.enabled),
                    notCheckedText = StringResourceToken.fromStringId(R.string.not_enabled),
                )
                Switchable(
                    key = KeyCheckKeystore,
                    title = StringResourceToken.fromStringId(R.string.check_keystore),
                    checkedText = StringResourceToken.fromStringId(R.string.enabled),
                    notCheckedText = StringResourceToken.fromStringId(R.string.not_enabled),
                    desc = StringResourceToken.fromStringId(R.string.set_them_later_in_settings)
                )
            }
        }
    }
}
