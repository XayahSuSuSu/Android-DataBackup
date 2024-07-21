/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xayah.core.ui.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xayah.core.ui.material3.tokens.DividerTokens
import com.xayah.core.ui.theme.value

/**
 * <a href="https://m3.material.io/components/divider/overview" class="external" target="_blank">Material Design divider</a>.
 *
 * A divider is a thin line that groups content in lists and layouts.
 *
 * ![Divider image](https://developer.android.com/images/reference/androidx/compose/material3/divider.png)
 *
 * @param modifier the [Modifier] to be applied to this divider line.
 * @param thickness thickness of this divider line. Using [Dp.Hairline] will produce a single pixel
 * divider regardless of screen density.
 * @param color color of this divider line.
 */
@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) = Canvas(
    modifier
        .fillMaxWidth()
        .height(thickness)
) {
    drawLine(
        color = color,
        strokeWidth = thickness.toPx(),
        start = Offset(0f, thickness.toPx() / 2),
        end = Offset(size.width, thickness.toPx() / 2),
    )
}

/**
 * <a href="https://m3.material.io/components/divider/overview" class="external" target="_blank">Material Design divider</a>.
 *
 * A divider is a thin line that groups content in lists and layouts.
 *
 * ![Divider image](https://developer.android.com/images/reference/androidx/compose/material3/divider.png)
 *
 * @param modifier the [Modifier] to be applied to this divider line.
 * @param thickness thickness of this divider line. Using [Dp.Hairline] will produce a single pixel
 * divider regardless of screen density.
 * @param color color of this divider line.
 */
@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) = Canvas(
    modifier
        .fillMaxHeight()
        .width(thickness)
) {
    drawLine(
        color = color,
        strokeWidth = thickness.toPx(),
        start = Offset(thickness.toPx() / 2, 0f),
        end = Offset(thickness.toPx() / 2, size.height),
    )
}

@Deprecated(
    message = "Renamed to HorizontalDivider",
    replaceWith = ReplaceWith("HorizontalDivider(modifier, thickness, color)")
)
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    val targetThickness = if (thickness == Dp.Hairline) {
        (1f / LocalDensity.current.density).dp
    } else {
        thickness
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(targetThickness)
            .background(color = color)
    )
}

/** Default values for [Divider] */
object DividerDefaults {
    /** Default thickness of a divider. */
    val Thickness: Dp = DividerTokens.Thickness

    /** Default color of a divider. */
    val color: Color @Composable get() = DividerTokens.Color.value
}