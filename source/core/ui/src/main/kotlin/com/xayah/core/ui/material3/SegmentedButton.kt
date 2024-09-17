/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import com.xayah.core.ui.material3.tokens.MotionTokens
import com.xayah.core.ui.material3.tokens.OutlinedSegmentedButtonTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 *
 * A Layout to correctly position and size [SegmentedButton]s in a Row.
 * It handles overlapping items so that strokes of the item are correctly on top of each other.
 * [SingleChoiceSegmentedButtonRow] is used when the selection only allows one value, for correct
 * semantics.
 *
 * @sample androidx.compose.material3.samples.SegmentedButtonSingleSelectSample
 *
 * @param modifier the [Modifier] to be applied to this row
 * @param space the dimension of the overlap between buttons. Should be equal to the stroke width
 *  used on the items.
 * @param content the content of this Segmented Button Row, typically a sequence of
 * [SegmentedButton]s
 */
@Composable
@ExperimentalMaterial3Api
fun SingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .selectableGroup()
            .defaultMinSize(minHeight = OutlinedSegmentedButtonTokens.ContainerHeight)
            .width(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(-space),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = remember { SingleChoiceSegmentedButtonScopeWrapper(this) }
        scope.content()
    }
}

/**
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 *
 * A Layout to correctly position, size, and add semantics to [SegmentedButton]s in a Row.
 * It handles overlapping items so that strokes of the item are correctly on top of each other.
 *
 * [MultiChoiceSegmentedButtonRow] is used when the selection allows multiple value, for correct
 * semantics.
 *
 * @sample androidx.compose.material3.samples.SegmentedButtonMultiSelectSample
 *
 * @param modifier the [Modifier] to be applied to this row
 * @param space the dimension of the overlap between buttons. Should be equal to the stroke width
 *  used on the items.
 * @param content the content of this Segmented Button Row, typically a sequence of
 * [SegmentedButton]s
 *
 */
@Composable
@ExperimentalMaterial3Api
fun MultiChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable MultiChoiceSegmentedButtonRowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = OutlinedSegmentedButtonTokens.ContainerHeight)
            .width(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(-space),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = remember { MultiChoiceSegmentedButtonScopeWrapper(this) }
        scope.content()
    }
}

@ExperimentalMaterial3Api
@Composable
internal fun SegmentedButtonContent(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(ButtonDefaults.TextButtonContentPadding)
    ) {
        val typography =
            MaterialTheme.typography.fromToken(OutlinedSegmentedButtonTokens.LabelTextFont)
        ProvideTextStyle(typography) {
            val scope = rememberCoroutineScope()
            val measurePolicy = remember { SegmentedButtonContentMeasurePolicy(scope) }

            Layout(
                modifier = Modifier.height(IntrinsicSize.Min),
                contents = listOf(icon, content),
                measurePolicy = measurePolicy
            )
        }
    }
}

internal class SegmentedButtonContentMeasurePolicy(
    val scope: CoroutineScope
) : MultiContentMeasurePolicy {
    var animatable: Animatable<Int, AnimationVector1D>? = null
    private var initialOffset: Int? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val (iconMeasurables, contentMeasurables) = measurables
        val iconPlaceables = iconMeasurables.fastMap { it.measure(constraints) }
        val iconWidth = iconPlaceables.fastMaxBy { it.width }?.width ?: 0
        val contentPlaceables = contentMeasurables.fastMap { it.measure(constraints) }
        val contentWidth = contentPlaceables.fastMaxBy { it.width }?.width
        val height = contentPlaceables.fastMaxBy { it.height }?.height ?: 0
        val width = maxOf(SegmentedButtonDefaults.IconSize.roundToPx(), iconWidth) +
                IconSpacing.roundToPx() +
                (contentWidth ?: 0)
        val offsetX = if (iconWidth == 0) {
            -(SegmentedButtonDefaults.IconSize.roundToPx() + IconSpacing.roundToPx()) / 2
        } else {
            0
        }

        if (initialOffset == null) {
            initialOffset = offsetX
        } else {
            val anim = animatable ?: Animatable(initialOffset!!, Int.VectorConverter)
                .also { animatable = it }
            if (anim.targetValue != offsetX) {
                scope.launch {
                    anim.animateTo(offsetX, tween(MotionTokens.DurationMedium3.toInt()))
                }
            }
        }

        return layout(width, height) {
            iconPlaceables.fastForEach {
                it.place(0, (height - it.height) / 2)
            }

            val contentOffsetX = SegmentedButtonDefaults.IconSize.roundToPx() +
                    IconSpacing.roundToPx() + (animatable?.value ?: offsetX)

            contentPlaceables.fastForEach {
                it.place(
                    contentOffsetX,
                    (height - it.height) / 2
                )
            }
        }
    }
}

@Composable
internal fun InteractionSource.interactionCountAsState(): State<Int> {
    val interactionCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(this) {
        this@interactionCountAsState.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press,
                is FocusInteraction.Focus -> {
                    interactionCount.intValue++
                }

                is PressInteraction.Release,
                is FocusInteraction.Unfocus,
                is PressInteraction.Cancel -> {
                    interactionCount.intValue--
                }
            }
        }
    }

    return interactionCount
}

/** Scope for the children of a [SingleChoiceSegmentedButtonRow] */
@ExperimentalMaterial3Api
interface SingleChoiceSegmentedButtonRowScope : RowScope

/** Scope for the children of a [MultiChoiceSegmentedButtonRow] */
@ExperimentalMaterial3Api
interface MultiChoiceSegmentedButtonRowScope : RowScope

internal fun Modifier.interactionZIndex(checked: Boolean, interactionCount: State<Int>) =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val zIndex = interactionCount.value + if (checked) CheckedZIndexFactor else 0f
            placeable.place(0, 0, zIndex)
        }
    }

private const val CheckedZIndexFactor = 5f
private val IconSpacing = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
private class SingleChoiceSegmentedButtonScopeWrapper(scope: RowScope) :
    SingleChoiceSegmentedButtonRowScope, RowScope by scope

@OptIn(ExperimentalMaterial3Api::class)
private class MultiChoiceSegmentedButtonScopeWrapper(scope: RowScope) :
    MultiChoiceSegmentedButtonRowScope, RowScope by scope
