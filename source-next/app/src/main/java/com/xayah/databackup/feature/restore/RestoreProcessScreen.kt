package com.xayah.databackup.feature.restore

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon

@Composable
internal fun RestoreProcessScreen(viewModel: RestoreProcessViewModel, onFinish: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }
    RestoreProcessContent(
        uiState = state,
        overallProgress = overallProgress,
        onRequestCancel = { showCancelDialog = true },
        onFinish = onFinish,
    )
    if (showCancelDialog && state.isProcessing) {
        DataBackupDialog(
            title = stringResource(R.string.prompt),
            onDismissRequest = { showCancelDialog = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info)) },
            content = { Text(stringResource(R.string.restore_cancel_description)) },
            confirmButton = {
                DialogDestructiveButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancel()
                    },
                )
            },
            dismissButton = {
                DialogDismissButton(text = stringResource(R.string.cancel), onClick = { showCancelDialog = false })
            },
        )
    }
}
