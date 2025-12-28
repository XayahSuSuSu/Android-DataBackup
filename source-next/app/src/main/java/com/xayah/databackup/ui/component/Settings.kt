package com.xayah.databackup.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.util.AutoScreenOff
import com.xayah.databackup.util.ResetBackupList

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
