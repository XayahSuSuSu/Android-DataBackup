package com.xayah.core.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.theme.darkTheme

fun Modifier.paddingStart(start: Dp) = padding(start, 0.dp, 0.dp, 0.dp)

fun Modifier.paddingTop(top: Dp) = padding(0.dp, top, 0.dp, 0.dp)

fun Modifier.paddingEnd(end: Dp) = padding(0.dp, 0.dp, end, 0.dp)

fun Modifier.paddingBottom(bottom: Dp) = padding(0.dp, 0.dp, 0.dp, bottom)

fun Modifier.paddingHorizontal(horizontal: Dp) = padding(horizontal, 0.dp)

fun Modifier.paddingVertical(vertical: Dp) = padding(0.dp, vertical)

fun Modifier.ignorePaddingHorizontal(horizontal: Dp) = layout { measurable, constraints ->
    if (constraints.maxWidth == Int.MAX_VALUE) throw IllegalArgumentException("The measuring failed, maybe you placed this modifier after horizontalScroll(), please place it at the first place.")
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + (horizontal * 2).roundToPx()))
    val x = if (constraints.maxWidth < placeable.width) 0 else -horizontal.roundToPx()
    layout(placeable.width, placeable.height) {
        placeable.place(x, 0)
    }
}

fun Modifier.shimmer(visible: Boolean = true, colorAlpha: Float = 0.1f, highlightAlpha: Float = 0.3f) = composed {
    val alphaColor = if (darkTheme()) highlightAlpha else colorAlpha
    val alphaHighlight = if (darkTheme()) colorAlpha else highlightAlpha
    placeholder(
        visible = visible,
        shape = CircleShape,
        color = ColorSchemeKeyTokens.OnSurface
            .toColor()
            .copy(alpha = alphaColor)
            .compositeOver(ColorSchemeKeyTokens.Surface.toColor()),
        highlight = PlaceholderHighlight.fade(
            ColorSchemeKeyTokens.Surface
                .toColor()
                .copy(alpha = alphaHighlight)
        ),
    )
}

fun Modifier.limitMaxDisplay(itemHeightPx: Int, maxDisplay: Int? = null, scrollState: ScrollState) = composed {
    if (maxDisplay != null) {
        with(LocalDensity.current) {
            /**
             * If [maxDisplay] is non-null, limit the max height.
             */
            heightIn(max = ((itemHeightPx * maxDisplay).toDp())).verticalScroll(scrollState)
        }
    } else {
        this
    }
}

fun Modifier.emphasize(state: Boolean) = composed {
    val offset by emphasizedOffset(targetState = state)
    offset(x = offset)
}

// See https://stackoverflow.com/a/72428903
fun Modifier.intrinsicIcon() = layout { measurable, constraints ->
    if (constraints.maxHeight == Constraints.Infinity) {
        layout(0, 0) {}
    } else {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
