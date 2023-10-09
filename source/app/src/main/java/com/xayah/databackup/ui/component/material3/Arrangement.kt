/*
 * Copyright 2020 The Android Open Source Project
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

package com.xayah.databackup.ui.component.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

private data class SpacedAlignedWithFooter(
    val space: Dp,
    val rtlMirror: Boolean,
) : Arrangement.HorizontalOrVertical {
    private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
        if (!reversed) {
            forEachIndexed(action)
        } else {
            for (i in (size - 1) downTo 0) {
                action(i, get(i))
            }
        }
    }

    override val spacing = space

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return
        val spacePx = space.roundToPx()

        var occupied = 0
        var lastSpace = 0
        val reversed = rtlMirror && layoutDirection == LayoutDirection.Rtl
        sizes.forEachIndexed(reversed) { index, it ->
            outPositions[index] = min(occupied, totalSize - it)
            lastSpace = min(spacePx, totalSize - outPositions[index] - it)
            occupied = outPositions[index] + it + lastSpace
        }
        occupied -= lastSpace

        if (occupied < totalSize) {
            outPositions[outPositions.lastIndex] += totalSize - occupied
        }
    }

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) = arrange(totalSize, sizes, LayoutDirection.Ltr, outPositions)

    override fun toString() =
        "${if (rtlMirror) "" else "Absolute"}Arrangement#spacedAligned($space)"
}

fun spacedByWithFooter(space: Dp): Arrangement.HorizontalOrVertical = SpacedAlignedWithFooter(space, true)