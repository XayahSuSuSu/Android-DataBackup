package com.xayah.core.ui.model

import androidx.compose.ui.graphics.Color
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens

data class MultiColorProgress(
    val progress: Float,
    val color: Color,
)

data class SegmentProgress(
    val used: Long,
    val total: Long,
) {
    val progress: Float
        get() = used.toFloat() / total

    val usedFormat: String
        get() = used.toDouble().formatSize()

    val totalFormat: String
        get() = total.toDouble().formatSize()
}

data class SegmentCircleProgress(
    val color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.PrimaryContainer,
    val trackColor: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.Primary,
)
