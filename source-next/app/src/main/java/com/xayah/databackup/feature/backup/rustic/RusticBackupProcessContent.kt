package com.xayah.databackup.feature.backup.rustic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.BackupProgressHeader
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.InlineNotice
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.theme.DataBackupTheme

@Composable
internal fun RusticBackupProcessContent(
    modifier: Modifier = Modifier,
    uiState: RusticBackupProcessUiState,
    sources: List<RusticBackupSourceUiItem>,
    overallProgress: String,
    statusLabel: String,
    onFinish: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        BackupProgressHeader(
            progress = overallProgress,
            statusLabel = statusLabel,
            showLoading = uiState.isProcessing,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .verticalFadingEdges(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            AnimatedVisibility(visible = uiState.isFailed) {
                InlineNotice(
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.errorMessage,
                    icon = ImageVector.vectorResource(R.drawable.ic_circle_x),
                )
            }

            SourcesSection(sources = sources)

            Spacer(modifier = Modifier.height(16.dp))
        }

        FadeVisibility(visible = uiState.isTerminal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onFinish) {
                    Text(text = stringResource(R.string.finish))
                }
            }
        }
    }
}

@Composable
private fun SourcesSection(sources: List<RusticBackupSourceUiItem>) {
    val selectedSources = remember(sources) { sources.filter { it.enabled } }
    if (selectedSources.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        selectedSources.forEach { source ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = ImageVector.vectorResource(source.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(source.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.items_selected,
                                source.selectedCount,
                                source.totalCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Phone", widthDp = 400, heightDp = 800)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun RusticBackupProcessPreview() {
    RusticBackupProcessPreviewContent(RusticBackupProcessStatus.Processing)
}

@Preview(name = "Finished", widthDp = 400, heightDp = 800)
@Composable
private fun RusticBackupFinishedPreview() {
    RusticBackupProcessPreviewContent(RusticBackupProcessStatus.Finished)
}

@Preview(name = "Failed", widthDp = 400, heightDp = 800)
@Composable
private fun RusticBackupFailedPreview() {
    RusticBackupProcessPreviewContent(RusticBackupProcessStatus.Failed)
}

@Composable
private fun RusticBackupProcessPreviewContent(status: RusticBackupProcessStatus) {
    DataBackupTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RusticBackupProcessContent(
                uiState = RusticBackupProcessUiState(
                    status = status,
                    progress = RusticBackupProgressUiState(bytesDone = 268435456, speed = 8388608),
                    errorMessage = stringResource(R.string.failed),
                ),
                sources = listOf(
                    RusticBackupSourceUiItem(R.string.apps, R.drawable.ic_layout_grid, 1, 2),
                    RusticBackupSourceUiItem(R.string.files, R.drawable.ic_folder, 0, 0),
                    RusticBackupSourceUiItem(R.string.networks, R.drawable.ic_wifi, 1, 1),
                    RusticBackupSourceUiItem(R.string.contacts, R.drawable.ic_user_round, 0, 0),
                    RusticBackupSourceUiItem(R.string.call_logs, R.drawable.ic_phone, 0, 0),
                    RusticBackupSourceUiItem(R.string.messages, R.drawable.ic_message_circle, 0, 0),
                ),
                overallProgress = if (status == RusticBackupProcessStatus.Finished) "100" else "42",
                statusLabel = stringResource(
                    when (status) {
                        RusticBackupProcessStatus.Processing -> R.string.backing_up
                        RusticBackupProcessStatus.Finished -> R.string.finished
                        RusticBackupProcessStatus.Failed -> R.string.failed
                    }
                ),
                onFinish = {},
            )
        }
    }
}
