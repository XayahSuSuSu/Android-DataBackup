package com.xayah.feature.setup.page.two

import androidx.activity.ComponentActivity
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
import com.xayah.core.datastore.KeyAppsEnabled
import com.xayah.core.datastore.KeyFilesEnabled
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.datastore.readBackupSavePathSaved
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.Switch
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.openConfirm
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.feature.setup.R
import com.xayah.feature.setup.SetupScaffold

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageTwo() {
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
            OutlinedButton(
                enabled = backupSavePathSaved.not(),
                onClick = {
                    viewModel.launchOnIO {
                        if (dialogState.openConfirm(title = StringResourceToken.fromStringId(R.string.skip_setup), text = StringResourceToken.fromStringId(R.string.skip_setup_alert))) {
                            viewModel.emitIntent(IndexUiIntent.ToMain(context = context as ComponentActivity))
                        }
                    }
                }
            ) {
                Text(text = StringResourceToken.fromStringId(R.string.skip_setup).value)
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
                viewModel.emitIntent(IndexUiIntent.SelectBackupDir(context = context))
            }
            Title(backupSavePathSaved, StringResourceToken.fromStringId(R.string.backup_categories)) {
                Switch(
                    enabled = backupSavePathSaved,
                    key = KeyAppsEnabled,
                    title = StringResourceToken.fromStringId(R.string.app_and_data),
                    checkedText = StringResourceToken.fromStringId(R.string.enabled),
                    notCheckedText = StringResourceToken.fromStringId(R.string.not_enabled),
                )
                Switch(
                    enabled = backupSavePathSaved,
                    key = KeyFilesEnabled,
                    title = StringResourceToken.fromStringId(R.string.media),
                    checkedText = StringResourceToken.fromStringId(R.string.enabled),
                    notCheckedText = StringResourceToken.fromStringId(R.string.not_enabled),
                )
            }
        }
    }
}
