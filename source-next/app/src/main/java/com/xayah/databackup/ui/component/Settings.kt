package com.xayah.databackup.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.util.AutoScreenOff
import com.xayah.databackup.util.CleanBackup
import com.xayah.databackup.util.IncrementalBackup
import com.xayah.databackup.util.ResetBackupList
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun IncrementalBackupAndCleanBackupSwitches() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val incrementalBackupSelected by context.readBoolean(IncrementalBackup)
        .collectAsStateWithLifecycle(initialValue = IncrementalBackup.second)
    val cleanBackupSelected by context.readBoolean(CleanBackup)
        .collectAsStateWithLifecycle(initialValue = CleanBackup.second)
    val incrementalBackupEnabled by remember(cleanBackupSelected) { mutableStateOf(cleanBackupSelected.not()) }
    val cleanBackupEnabled by remember(incrementalBackupSelected) { mutableStateOf(incrementalBackupSelected.not()) }
    SwitchablePreference(
        enabled = incrementalBackupEnabled,
        checked = incrementalBackupSelected,
        icon = ImageVector.vectorResource(R.drawable.ic_chart_bar_stacked),
        title = stringResource(R.string.incremental_backup),
        subtitle = stringResource(R.string.incremental_backup_desc),
    ) {
        scope.launch(Dispatchers.Default) {
            context.saveBoolean(IncrementalBackup.first, it)
            if (it) {
                context.saveBoolean(CleanBackup.first, false)
            }
        }
    }
    SwitchablePreference(
        enabled = cleanBackupEnabled,
        checked = cleanBackupSelected,
        icon = ImageVector.vectorResource(R.drawable.ic_brush_cleaning),
        title = stringResource(R.string.clean_backup),
        subtitle = stringResource(R.string.clean_backup_desc),
    ) {
        scope.launch(Dispatchers.Default) {
            context.saveBoolean(CleanBackup.first, it)
            if (it) {
                context.saveBoolean(IncrementalBackup.first, false)
            }
        }
    }
}

@Composable
fun AutoScreenOffSwitch() {
    SwitchablePreference(
        icon = ImageVector.vectorResource(R.drawable.ic_eye_off),
        title = stringResource(R.string.auto_screen_off),
        subtitle = stringResource(R.string.auto_screen_off_desc),
        dataStorePair = AutoScreenOff
    )
}


@Composable
fun ResetBackupListSwitch() {
    SwitchablePreference(
        icon = ImageVector.vectorResource(R.drawable.ic_list_restart),
        title = stringResource(R.string.reset_backup_list),
        subtitle = stringResource(R.string.reset_backup_list_desc),
        dataStorePair = ResetBackupList
    )
}
