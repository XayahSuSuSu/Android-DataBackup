package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
