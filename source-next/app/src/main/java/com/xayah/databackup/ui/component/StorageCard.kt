package com.xayah.databackup.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.util.formatToStorageSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val StorageCardShimmerColorAlpha = 0.2f
private const val StorageCardShimmerHighlightAlpha = 0.55f
private val StorageCardShape = RoundedCornerShape(18.dp)
private val DistributionBarShape = CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCard(
    modifier: Modifier,
    free: Float,
    other: Float,
    backups: Float,
    freeBytes: Long,
    otherBytes: Long,
    backupsBytes: Long,
    totalBytes: Long,
    isLoading: Boolean = false,
    title: String,
    subtitle: String,
    storage: String,
    onClick: () -> Unit
) {
    val legendEnabled = remember(isLoading, totalBytes) { !isLoading && totalBytes > 0L }
    val backupsTooltip = remember(backupsBytes, backups, totalBytes) {
        formatLegendTooltip(backupsBytes, backups, totalBytes)
    }
    val otherTooltip = remember(otherBytes, other, totalBytes) {
        formatLegendTooltip(otherBytes, other, totalBytes)
    }
    val freeTooltip = remember(freeBytes, free, totalBytes) {
        formatLegendTooltip(freeBytes, free, totalBytes)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier,
        shape = StorageCardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedShimmerText(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    isLoading = isLoading,
                    content = title,
                    loading = "    ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    shimmerColorAlpha = StorageCardShimmerColorAlpha,
                    shimmerHighlightAlpha = StorageCardShimmerHighlightAlpha,
                )
                AnimatedShimmerText(
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    content = subtitle,
                    loading = " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    shimmerColorAlpha = StorageCardShimmerColorAlpha,
                    shimmerHighlightAlpha = StorageCardShimmerHighlightAlpha,
                )
            }

            AnimatedShimmerText(
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                content = storage,
                loading = " ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                shimmerColorAlpha = StorageCardShimmerColorAlpha,
                shimmerHighlightAlpha = StorageCardShimmerHighlightAlpha,
            )

            Crossfade(
                targetState = isLoading,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                label = "storage-distribution-bar"
            ) { loading ->
                if (loading) {
                    LoadingDistributionBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                } else {
                    DistributionBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        backups = backups,
                        other = other,
                        free = free,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                LegendItem(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.backups),
                    tooltip = backupsTooltip,
                    enabled = legendEnabled,
                )
                LegendItem(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.other),
                    tooltip = otherTooltip,
                    enabled = legendEnabled,
                )
                LegendItem(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                    label = stringResource(R.string.free),
                    tooltip = freeTooltip,
                    enabled = legendEnabled,
                )
            }
        }
    }
}

@Composable
private fun DistributionBar(
    modifier: Modifier,
    backups: Float,
    other: Float,
    free: Float,
) {
    val sum = remember(backups, other, free) { (backups + other + free).coerceAtLeast(0f) }
    val backupsProgress = remember(backups, sum) { if (sum == 0f) 0f else (backups / sum).coerceIn(0f, 1f) }
    val otherProgress = remember(other, sum) { if (sum == 0f) 0f else (other / sum).coerceIn(0f, 1f) }
    val freeProgress = remember(free, sum) { if (sum == 0f) 0f else (free / sum).coerceIn(0f, 1f) }

    Row(
        modifier = modifier
            .clip(DistributionBarShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (backupsProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(backupsProgress)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        if (otherProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(otherProgress)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
        if (freeProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(freeProgress)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun LoadingDistributionBar(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "storage-loading-bar")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep"
    )
    val trackPulse = transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "track-pulse"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val glowPrimary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
    val glowSecondary = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    val glowAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .clip(DistributionBarShape)
            .background(baseColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = size.height
            val barWidth = size.width
            val radius = CornerRadius(barHeight / 2f, barHeight / 2f)
            val glowWidth = barWidth * 0.55f
            val centerX = (barWidth + glowWidth) * sweep.value - glowWidth / 2f

            drawRoundRect(
                color = baseColor.copy(alpha = trackPulse.value),
                cornerRadius = radius,
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        glowSecondary,
                        glowAccent,
                        glowPrimary,
                        glowSecondary,
                        Color.Transparent,
                    ),
                    startX = centerX - glowWidth / 2f,
                    endX = centerX + glowWidth / 2f,
                ),
                cornerRadius = radius,
            )

            drawRoundRect(
                color = outlineColor,
                cornerRadius = radius,
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LegendItem(modifier: Modifier, color: Color, label: String, tooltip: String, enabled: Boolean) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val autoDismissJobRef = remember { JobRef() }
    val showJobRef = remember { JobRef() }

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(tooltip) } },
            state = tooltipState,
            enableUserInput = false,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        indication = null,
                        interactionSource = interactionSource,
                    ) {
                        autoDismissJobRef.value?.cancel()
                        if (tooltipState.isVisible) {
                            showJobRef.value?.cancel()
                            scope.launch { tooltipState.dismiss() }
                            return@clickable
                        }
                        showJobRef.value?.cancel()
                        showJobRef.value = scope.launch { tooltipState.show() }
                        autoDismissJobRef.value = scope.launch {
                            delay(1_000)
                            if (tooltipState.isVisible) tooltipState.dismiss()
                        }
                    },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = color,
                    ) {}
                    Text(
                        modifier = Modifier.weight(1f),
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private class JobRef(var value: Job? = null)

private fun formatLegendTooltip(sizeBytes: Long, ratio: Float, totalBytes: Long): String {
    val safeSize = sizeBytes.coerceAtLeast(0L).coerceAtMost(totalBytes.coerceAtLeast(0L))
    val safeRatio = ratio.coerceIn(0f, 1f)
    val percentText = if (safeRatio > 0f && safeRatio < 0.01f) "<1%" else "${(safeRatio * 100f).toInt()}%"
    return "${safeSize.formatToStorageSize} | $percentText"
}

@Composable
private fun AnimatedShimmerText(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    content: String,
    loading: String,
    style: TextStyle,
    color: Color,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    shimmerColorAlpha: Float = 0.2f,
    shimmerHighlightAlpha: Float = 0.55f,
) {
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "storage-card-text"
    ) { loadingState ->
        Text(
            modifier = if (loadingState) {
                modifier.shimmer(true, colorAlpha = shimmerColorAlpha, highlightAlpha = shimmerHighlightAlpha)
            } else {
                modifier
            },
            text = if (loadingState) loading else content,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
