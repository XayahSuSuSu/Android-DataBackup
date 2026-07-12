package com.xayah.databackup.feature.backup.rustic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.BackupProgressHeader
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.InlineNotice
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.verticalFadingEdges

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

        AnimatedVisibility(visible = uiState.isFailed) {
            InlineNotice(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = uiState.errorMessage,
                icon = ImageVector.vectorResource(R.drawable.ic_circle_x),
            )
        }

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

            TransferMetrics(progress = uiState.progress)

            CurrentStageSection(uiState = uiState)

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
private fun TransferMetrics(progress: RusticBackupProgressUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.bytes_written),
            value = progress.bytesDoneText,
            iconRes = R.drawable.ic_hard_drive_download,
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.transfer_speed),
            value = progress.speedText,
            iconRes = R.drawable.ic_gauge,
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularIcon(
                iconRes = iconRes,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CurrentStageSection(uiState: RusticBackupProcessUiState) {
    val step = uiState.currentStep ?: return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.current_stage),
            trailing = stringResource(R.string.step_count, uiState.currentStepNumber, uiState.steps.size),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularIcon(
                    iconRes = if (uiState.isFinished) {
                        R.drawable.ic_square_check_big
                    } else {
                        step.iconRes
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(step.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(if (uiState.isFinished) R.string.finished else R.string.processing),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesSection(sources: List<RusticBackupSourceUiItem>) {
    if (sources.isEmpty()) return

    val summary = remember(sources) { sources.toSourceSummary() }
    val summaryText = stringResource(
        R.string.items_selected,
        summary.selectedCount,
        summary.totalCount,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.source_queue),
            trailing = summaryText,
        )
        AdaptiveGrid(
            items = sources,
            minCellWidth = 148.dp,
        ) { source ->
            SourceCard(source = source)
        }
    }
}

@Composable
private fun SourceCard(source: RusticBackupSourceUiItem) {
    val containerColor = if (source.enabled) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (source.enabled) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularIcon(
                iconRes = source.iconRes,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = contentColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(source.titleRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Text(
                    text = source.countText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun CircularIcon(
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    size: Dp = 36.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = containerColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun <T> AdaptiveGrid(
    items: List<T>,
    minCellWidth: Dp,
    content: @Composable (T) -> Unit,
) {
    val spacing = 12.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = adaptiveGridColumnCount(
            availableWidth = maxWidth.value,
            minCellWidth = minCellWidth.value,
            spacing = spacing.value,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            content(item)
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun adaptiveGridColumnCount(
    availableWidth: Float,
    minCellWidth: Float,
    spacing: Float,
): Int {
    if (availableWidth <= 0f || minCellWidth <= 0f) return 1
    val nonNegativeSpacing = spacing.coerceAtLeast(0f)
    return ((availableWidth + nonNegativeSpacing) / (minCellWidth + nonNegativeSpacing))
        .toInt()
        .coerceAtLeast(1)
}
