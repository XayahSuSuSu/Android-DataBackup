package com.xayah.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import com.xayah.core.ui.material3.util.lerp
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.darkTheme
import com.xayah.core.ui.theme.value
import kotlin.math.absoluteValue

fun Modifier.paddingStart(start: Dp) = padding(start, 0.dp, 0.dp, 0.dp)

fun Modifier.paddingTop(top: Dp) = padding(0.dp, top, 0.dp, 0.dp)

fun Modifier.paddingEnd(end: Dp) = padding(0.dp, 0.dp, end, 0.dp)

fun Modifier.paddingBottom(bottom: Dp) = padding(0.dp, 0.dp, 0.dp, bottom)

fun Modifier.paddingHorizontal(horizontal: Dp) = padding(horizontal, 0.dp)

fun Modifier.paddingVertical(vertical: Dp) = padding(0.dp, vertical)

fun Modifier.shimmer(visible: Boolean = true, colorAlpha: Float = 0.1f, highlightAlpha: Float = 0.3f) = composed {
    val alphaColor = if (darkTheme()) highlightAlpha else colorAlpha
    val alphaHighlight = if (darkTheme()) colorAlpha else highlightAlpha
    placeholder(
        visible = visible,
        shape = CircleShape,
        color = ThemedColorSchemeKeyTokens.OnSurface
            .value
            .copy(alpha = alphaColor)
            .compositeOver(ThemedColorSchemeKeyTokens.Surface.value),
        highlight = PlaceholderHighlight.fade(
            ThemedColorSchemeKeyTokens.Surface
                .value
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

@ExperimentalFoundationApi
fun Modifier.pagerAnimation(pagerState: PagerState, page: Int) = graphicsLayer {
    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
    alpha = lerp(
        start = 0.7f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f),
    )
    cameraDistance = 8 * density
    rotationY = lerp(
        start = 0f,
        stop = 0f,
        fraction = pageOffset.coerceIn(-1f, 1f),
    )

    lerp(
        start = 0.8f,
        stop = 1f,
        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f),
    ).also { scale ->
        scaleX = scale
        scaleY = scale
    }
}
