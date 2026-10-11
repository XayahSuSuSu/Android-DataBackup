package com.xayah.databackup.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R

@Composable
fun TaskBusyDialog(onDismiss: () -> Unit, onOpenTask: () -> Unit) {
    DataBackupDialog(
        title = stringResource(R.string.prompt),
        onDismissRequest = onDismiss,
        content = { Text(stringResource(R.string.backup_restore_busy)) },
        confirmButton = { DialogActionButton(text = stringResource(R.string.view_active_task), onClick = onOpenTask) },
        dismissButton = { DialogDismissButton(text = stringResource(R.string.cancel), onClick = onDismiss) },
    )
}
