package com.xayah.databackup.feature.backup.rustic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.feature.backup.BackupProcessContent
import com.xayah.databackup.feature.backup.BackupProcessViewModel
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely

@Composable
internal fun RusticBackupProcessScreen(navigator: Navigator, viewModel: BackupProcessViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackupProcessContent(uiState = uiState, onCancel = viewModel::cancel, onFinish = { navigator.popBackStackSafely() })
}
