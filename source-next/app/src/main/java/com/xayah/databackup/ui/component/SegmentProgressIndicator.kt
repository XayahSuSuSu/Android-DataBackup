package com.xayah.databackup.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private object SegmentProgressIndicatorTokens {
    val SegmentGap = 0.03f
    val TrackThickness = 4.dp
    val CircleSize = 48.dp
}

private val SegmentCircularIndicatorDiameter = SegmentProgressIndicatorTokens.CircleSize - SegmentProgressIndicatorTokens.TrackThickness * 2

object SegmentProgressIndicatorDefaults {
    /** Default gap of each segment for a circular progress indicator. */
    val segmentGap: Float = SegmentProgressIndicatorTokens.SegmentGap

    /** Default stroke width for a circular progress indicator. */
    val CircularStrokeWidth: Dp = SegmentProgressIndicatorTokens.TrackThickness

    /** Default stroke cap for an indeterminate circular progress indicator. */
    val CircularIndeterminateStrokeCap: StrokeCap = StrokeCap.Round
}

data class Segment(
    var progress: () -> Float,
    var trackProgress: () -> Float = { 1f },
    var color: Color,
    var trackColor: Color,
)

@Composable
fun SegmentCircularProgressIndicator(
    modifier: Modifier = Modifier,
    segments: List<Segment>,
    segmentGap: Float = SegmentProgressIndicatorDefaults.segmentGap,
    strokeWidth: Dp = SegmentProgressIndicatorDefaults.CircularStrokeWidth,
    strokeCap: StrokeCap = SegmentProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
) {
    val totalProgress = remember(segments) { segments.fold(0f) { total, segment -> total + segment.progress() } }
    if (totalProgress > 1f || totalProgress < 0f) {
        throw IllegalArgumentException("The total progress of segments are not in [0, 1].")
    }
    val stroke = with(LocalDensity.current) { remember(strokeWidth, strokeCap) { Stroke(width = strokeWidth.toPx(), cap = strokeCap) } }

    Canvas(
        modifier
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(totalProgress, 0f..1f)
            }
            .size(SegmentCircularIndicatorDiameter)
    ) {
        var baseProgress = 0f
        segments.forEach { segment ->
            val coercedProgress = { segment.progress().coerceIn(0f, 1f) }
            // Start at 12 o'clock
            val startAngle = 270f + (baseProgress + segmentGap) * 360f
            val sweepAngle = (coercedProgress() - segmentGap * 2) * 360f
            drawSegmentCircularIndicator(
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                color = segment.color,
                stroke = stroke,
            )

            val coercedTrackProgress = { segment.trackProgress().coerceIn(0f, 1f) }
            val sweepTrackAngle = sweepAngle * coercedTrackProgress()
            drawSegmentCircularIndicator(
                startAngle = startAngle,
                sweepAngle = sweepTrackAngle,
                color = segment.trackColor,
                stroke = stroke,
            )

            baseProgress += coercedProgress()
        }
    }
}

private fun DrawScope.drawSegmentCircularIndicator(
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

@Preview(showBackground = true)
@Composable
fun SegmentCircularProgressIndicatorPreview() {
    val color: Color = MaterialTheme.colorScheme.primaryContainer
    val trackColor: Color = MaterialTheme.colorScheme.primary
    SegmentCircularProgressIndicator(
        modifier = Modifier,
        segments = listOf(
            Segment(progress = { 0.25f }, color = color, trackColor = trackColor),
            Segment(progress = { 0.5f }, color = MaterialTheme.colorScheme.secondaryContainer, trackColor = MaterialTheme.colorScheme.secondary),
            Segment(progress = { 0.25f }, trackProgress = { 0f }, color = color, trackColor = trackColor),
        )
    )
}
