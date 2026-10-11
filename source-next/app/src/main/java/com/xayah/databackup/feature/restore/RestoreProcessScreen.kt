package com.xayah.databackup.feature.restore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun RestoreProcessScreen(viewModel: RestoreProcessViewModel, onFinish: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RestoreProcessContent(uiState = uiState, onCancel = viewModel::cancel, onFinish = onFinish)
}
