package com.xayah.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.xayah.core.ui.material3.LinearIndicatorHeight
import com.xayah.core.ui.material3.LinearIndicatorWidth
import com.xayah.core.ui.material3.drawLinearIndicator
import com.xayah.core.ui.material3.drawLinearIndicatorTrack
import com.xayah.core.ui.material3.util.fastForEach
import com.xayah.core.ui.model.MultiColorProgress
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens

@Composable
fun LinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    enabled: Boolean = true,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = color.withState(enabled),
        trackColor = trackColor.withState(enabled),
        strokeCap = strokeCap,
    )
}

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    enabled: Boolean = true,
) {
    LinearProgressIndicator(
        modifier = modifier,
        color = color.withState(enabled),
        trackColor = trackColor.withState(enabled),
        strokeCap = strokeCap,
    )
}

@Composable
fun AnimatedLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    enabled: Boolean = true,
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, delayMillis = 100),
        label = AnimationTokens.AnimatedProgressLabel
    )
    LaunchedEffect(progress) {
        targetProgress = progress.coerceIn(0f, 1f).takeIf { it.isNaN().not() } ?: 0f
    }
    LinearProgressIndicator(
        enabled = enabled,
        progress = animatedProgress.value,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = strokeCap,
    )
}

@Composable
fun AnimatedMultiColorLinearProgressIndicator(
    multiColorProgress: List<MultiColorProgress>,
    modifier: Modifier = Modifier,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
) {
    val progress = multiColorProgress.sumOf { it.progress.toDouble() }.toFloat()
    val coercedProgress = progress.coerceIn(0f, 1f).takeIf { it.isNaN().not() } ?: 0f

    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, delayMillis = 100),
        label = AnimationTokens.AnimatedProgressLabel
    )
    LaunchedEffect(coercedProgress) {
        targetProgress = coercedProgress
    }

    Canvas(
        modifier
            .progressSemantics(coercedProgress)
            .size(LinearIndicatorWidth, LinearIndicatorHeight)
    ) {
        val strokeWidth = size.height
        drawLinearIndicatorTrack(trackColor, strokeWidth, strokeCap)

        var currentFraction = 0f
        multiColorProgress.fastForEach {
            if (animatedProgress.value >= currentFraction)
                drawLinearIndicator(
                    currentFraction.coerceIn(0f, 1f),
                    (currentFraction + (animatedProgress.value - currentFraction).coerceIn(0f, it.progress)).coerceIn(0f, 1f),
                    it.color,
                    strokeWidth,
                    strokeCap
                )
            currentFraction += it.progress
        }
    }
}

@Composable
fun SegmentProgressIndicator(modifier: Modifier = Modifier, enabled: Boolean = false, progress: Float, color: ThemedColorSchemeKeyTokens, trackColor: ThemedColorSchemeKeyTokens) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level4)) {
        val corrected = if (progress < 0.05f) 0f else if (progress > 0.95f) 1f else progress
        if (corrected > 0.05f)
            com.xayah.core.ui.component.LinearProgressIndicator(
                modifier = Modifier.weight(corrected),
                color = color.value.withState(enabled),
                trackColor = trackColor.value.withState(enabled),
                progress = 1f,
                strokeCap = StrokeCap.Round
            )
        if (1 - corrected > 0.05f)
            com.xayah.core.ui.component.LinearProgressIndicator(
                modifier = Modifier.weight(1 - corrected),
                color = color.value.withState(enabled),
                trackColor = trackColor.value.withState(enabled),
                progress = 0f,
                strokeCap = StrokeCap.Round
            )
    }
}

@Composable
fun SegmentCircularProgressIndicator(
    segments: Int,
    progress: Float,
    gapPx: Float = 6f,
    size: Dp = SizeTokens.Level128,
    color: Color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
    trackColor: Color = ThemedColorSchemeKeyTokens.Primary.value,
    strokeWidth: Dp = SizeTokens.Level8,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val perSegmentProgress = remember(segments) { 1F / segments }
    var targetProgress by remember { mutableFloatStateOf(if (progress.isNaN()) 0f else progress) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, delayMillis = 100),
        label = AnimationTokens.AnimatedProgressLabel
    )
    LaunchedEffect(progress) {
        targetProgress = if (progress.isNaN()) 0f else progress
    }

    Canvas(modifier = Modifier.size(size)) {
        val strokeWidthPx = strokeWidth.toPx()
        val sizePx = size.toPx() - strokeWidthPx
        repeat(segments) { index ->
            val current = perSegmentProgress * (index + 1)
            val diff = current - animatedProgress
            val segmentProgress = when {
                diff < perSegmentProgress -> {
                    if (diff >= 0) {
                        1 - diff / perSegmentProgress
                    } else {
                        1f
                    }
                }

                else -> 0f
            }

            withTransform({
                rotate(degrees = 0f, pivot = center)
            }) {
                drawArc(
                    color = color,
                    startAngle = 270f + gapPx + index * 360f / segments,
                    sweepAngle = 360f / segments - gapPx * 2,
                    useCenter = false,
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                    size = Size(sizePx, sizePx),
                    style = Stroke(width = strokeWidthPx, cap = strokeCap)
                )
            }

            withTransform({
                rotate(degrees = 0f, pivot = center)
            }) {
                drawArc(
                    color = trackColor,
                    startAngle = 270f + gapPx + index * 360f / segments,
                    sweepAngle = (360f / segments - gapPx * 2) * segmentProgress,
                    useCenter = false,
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                    size = Size(sizePx, sizePx),
                    style = Stroke(width = strokeWidthPx, cap = strokeCap)
                )
            }
        }
    }
}
