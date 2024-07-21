package com.xayah.core.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.AnimationTokens.AnimatedOffsetYLabel
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.ScrollBarTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@Composable
fun ScrollBar(modifier: Modifier = Modifier, state: LazyListState) {
    with(LocalDensity.current) {
        var isDragging by remember { mutableStateOf(false) }
        val isScrollInProgress = state.isScrollInProgress
        var offsetY by remember { mutableFloatStateOf(0f) }
        val animatedOffsetY: Float by animateFloatAsState(offsetY, label = AnimatedOffsetYLabel)
        var barSize by remember { mutableStateOf(IntSize.Zero) }
        val rSize by remember(barSize) { mutableFloatStateOf(barSize.height - (ScrollBarTokens.BarHeight.toPx())) }
        val ratio by remember(offsetY, rSize) { mutableFloatStateOf(offsetY / rSize) }
        val totalItemsCount by remember(state.layoutInfo.totalItemsCount) { mutableIntStateOf(state.layoutInfo.totalItemsCount) }
        var visibleItemsCount by remember(totalItemsCount) { mutableIntStateOf(totalItemsCount) }
        LaunchedEffect(state.layoutInfo.visibleItemsInfo.size, totalItemsCount) {
            if (visibleItemsCount > state.layoutInfo.visibleItemsInfo.size && state.layoutInfo.visibleItemsInfo.isNotEmpty()) visibleItemsCount = state.layoutInfo.visibleItemsInfo.size
        }
        val scrollableItemsCount by remember(totalItemsCount, visibleItemsCount) { mutableIntStateOf((totalItemsCount - visibleItemsCount).coerceAtLeast(1)) }
        val perItemBarHeight by remember(rSize, scrollableItemsCount) { mutableFloatStateOf(rSize / scrollableItemsCount) }
        val firstVisibleItemIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }
        var isExpand by remember { mutableStateOf(false) }
        LaunchedEffect(isScrollInProgress, isDragging) {
            isExpand = if ((isScrollInProgress || isDragging) && (state.canScrollForward || state.canScrollBackward)) {
                true
            } else {
                delay(1000)
                false
            }
        }
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(ScrollBarTokens.TouchWidth)
                .onSizeChanged { barSize = it },
            contentAlignment = Alignment.CenterEnd
        ) {
            val dp: Dp by animateDpAsState(if (isExpand) ScrollBarTokens.Width else PaddingTokens.Level0, label = ScrollBarTokens.AnimationLabel)
            LaunchedEffect(ratio) {
                if (isScrollInProgress.not()) state.scrollToItem(runCatching { (scrollableItemsCount * ratio).roundToInt() }.getOrElse { 0 })
            }
            LaunchedEffect(firstVisibleItemIndex, perItemBarHeight) {
                if (isDragging.not()) offsetY = firstVisibleItemIndex * perItemBarHeight
            }

            // Background
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dp),
                color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = ScrollBarTokens.BackgroundAlpha),
                indication = null,
            )

            // Bar
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .height(ScrollBarTokens.BarHeight)
                    .fillMaxWidth()
                    .offset { IntOffset(0, runCatching { animatedOffsetY.roundToInt() }.getOrElse { 0 }) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (isScrollInProgress.not() && isExpand) {
                                offsetY += delta
                                offsetY = offsetY.coerceIn(0f, rSize)
                            }
                        },
                        onDragStarted = { isDragging = true },
                        onDragStopped = {
                            isDragging = false
                        }
                    ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(dp),
                        shape = CircleShape,
                        color = ThemedColorSchemeKeyTokens.Primary.value,
                    )
                }
            }
        }
    }
}
