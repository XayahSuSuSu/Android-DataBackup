package com.xayah.core.ui.component

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.xayah.core.ui.material3.DisabledAlpha

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
        progress = progress,
        modifier = modifier,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        trackColor = if (enabled) trackColor else trackColor.copy(alpha = DisabledAlpha),
        strokeCap = strokeCap,
    )
}
