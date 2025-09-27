package com.xayah.databackup.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.material3.placeholder.PlaceholderHighlight
import com.xayah.databackup.ui.material3.placeholder.fade
import com.xayah.databackup.ui.material3.placeholder.placeholder
import kotlin.math.min

fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    fadingEdge: Dp = 48.dp,
): Modifier = fadingEdges(Direction.HORIZONTAL, scrollState, fadingEdge)

fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    fadingEdge: Dp = 48.dp,
): Modifier = fadingEdges(Direction.VERTICAL, scrollState, fadingEdge)

fun Modifier.horizontalFadingEdges(
    startRange: Float,
    endRange: Float,
    fadingEdge: Dp = 48.dp,
): Modifier = fadingEdges(Direction.HORIZONTAL, startRange, endRange, fadingEdge)

fun Modifier.verticalFadingEdges(
    startRange: Float,
    endRange: Float,
    fadingEdge: Dp = 48.dp,
): Modifier = fadingEdges(Direction.VERTICAL, startRange, endRange, fadingEdge)

private enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

/**
 * @see <a href="https://medium.com/@helmersebastian/fading-edges-modifier-in-jetpack-compose-af94159fdf1f">Fading Edges Modifier in Jetpack Compose</a>
 */
private fun Modifier.fadingEdges(
    direction: Direction,
    scrollState: ScrollState,
    fadingEdge: Dp,
): Modifier = this.then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val startColors = listOf(Color.Transparent, Color.Black)
            var start = scrollState.value.toFloat()
            var end = start + min(fadingEdge.toPx(), start)
            drawRect(
                brush = when (direction) {
                    Direction.HORIZONTAL -> Brush.horizontalGradient(
                        colors = startColors,
                        startX = start,
                        endX = end
                    )

                    Direction.VERTICAL -> Brush.verticalGradient(
                        colors = startColors,
                        startY = start,
                        endY = end
                    )
                },
                blendMode = BlendMode.DstIn
            )

            val endColors = listOf(Color.Black, Color.Transparent)
            val edgeSize = min(fadingEdge.toPx(), scrollState.maxValue.toFloat() - scrollState.value)
            end = when (direction) {
                Direction.HORIZONTAL -> size.width
                Direction.VERTICAL -> size.height
            } - scrollState.maxValue + scrollState.value
            start = end - edgeSize
            if (edgeSize != 0f) {
                drawRect(
                    brush = when (direction) {
                        Direction.HORIZONTAL -> Brush.horizontalGradient(
                            colors = endColors,
                            startX = start,
                            endX = end
                        )

                        Direction.VERTICAL -> Brush.verticalGradient(
                            colors = endColors,
                            startY = start,
                            endY = end
                        )
                    },
                    blendMode = BlendMode.DstIn
                )
            }
        }
)

private fun Modifier.fadingEdges(
    direction: Direction,
    startRange: Float,
    endRange: Float,
    fadingEdge: Dp,
): Modifier = this.then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            var colors = listOf(Color.Transparent, Color.Black)
            val fadingEdgePx = fadingEdge.toPx()
            var start = 0f
            var range = fadingEdgePx * startRange
            var end = range
            if (range != 0f) {
                drawRect(
                    brush = when (direction) {
                        Direction.HORIZONTAL -> Brush.horizontalGradient(
                            colors = colors,
                            startX = start,
                            endX = end
                        )

                        Direction.VERTICAL -> Brush.verticalGradient(
                            colors = colors,
                            startY = start,
                            endY = end
                        )
                    },
                    blendMode = BlendMode.DstIn
                )
            }

            colors = listOf(Color.Black, Color.Transparent)
            range = fadingEdgePx * endRange
            val max = when (direction) {
                Direction.HORIZONTAL -> size.width
                Direction.VERTICAL -> size.height
            }
            start = max - range
            end = max
            if (range != 0f) {
                drawRect(
                    brush = when (direction) {
                        Direction.HORIZONTAL -> Brush.horizontalGradient(
                            colors = colors,
                            startX = start,
                            endX = end
                        )

                        Direction.VERTICAL -> Brush.verticalGradient(
                            colors = colors,
                            startY = start,
                            endY = end
                        )
                    },
                    blendMode = BlendMode.DstIn
                )
            }
        }
)

fun Modifier.shimmer(visible: Boolean = true, colorAlpha: Float = 0.1f, highlightAlpha: Float = 0.3f) = composed {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val alphaColor = if (isSystemInDarkTheme) highlightAlpha else colorAlpha
    val alphaHighlight = if (isSystemInDarkTheme) colorAlpha else highlightAlpha
    placeholder(
        visible = visible,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface
            .copy(alpha = alphaColor)
            .compositeOver(MaterialTheme.colorScheme.surface),
        highlight = PlaceholderHighlight.fade(MaterialTheme.colorScheme.surface.copy(alpha = alphaHighlight)),
    )
}
