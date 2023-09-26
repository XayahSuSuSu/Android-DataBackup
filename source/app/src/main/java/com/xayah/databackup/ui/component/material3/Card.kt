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

package com.xayah.databackup.ui.component.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.material3.FilledCardTokens
import com.xayah.databackup.ui.token.material3.OutlinedCardTokens

/**
 * Represents the container and content colors used in a card in different states.
 *
 * - See [CardDefaults.cardColors] for the default colors used in a [Card].
 * - See [CardDefaults.elevatedCardColors] for the default colors used in a [ElevatedCard].
 * - See [CardDefaults.outlinedCardColors] for the default colors used in a [OutlinedCard].
 */
@Immutable
class CardColors internal constructor(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) {
    /**
     * Represents the container color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    /**
     * Represents the content color for this card, depending on [enabled].
     *
     * @param enabled whether the card is enabled
     */
    @Composable
    internal fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        return result
    }
}

/**
 * Creates a [CardColors] that represents the default container and content colors used in a
 * [Card].
 *
 * @param containerColor the container color of this [Card] when enabled.
 * @param contentColor the content color of this [Card] when enabled.
 * @param disabledContainerColor the container color of this [Card] when not enabled.
 * @param disabledContentColor the content color of this [Card] when not enabled.
 */
@Composable
internal fun cardColors(
    containerColor: Color = ColorScheme.surfaceVariant(),
    contentColor: Color = contentColorFor(containerColor),
    disabledContainerColor: Color = ColorScheme.surfaceVariant().copy(alpha = FilledCardTokens.DisabledContainerOpacity)
        .compositeOver(MaterialTheme.colorScheme.surfaceColorAtElevation(FilledCardTokens.DisabledContainerElevation)),
    disabledContentColor: Color = contentColorFor(containerColor).copy(FilledCardTokens.DisabledAlpha),
): CardColors = CardColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor
)

/**
 * Represents the elevation for a card in different states.
 *
 * - See [CardDefaults.cardElevation] for the default elevation used in a [Card].
 * - See [CardDefaults.elevatedCardElevation] for the default elevation used in an [ElevatedCard].
 * - See [CardDefaults.outlinedCardElevation] for the default elevation used in an [OutlinedCard].
 */
@Immutable
class CardElevation internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the tonal elevation used in a card, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the card.
     *
     * @param enabled whether the card is enabled
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    internal fun tonalElevation(
        enabled: Boolean,
        interactionSource: InteractionSource?,
    ): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    /**
     * Represents the shadow elevation used in a card, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the card to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the card is enabled
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource?,
    ): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }

                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }

                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }

                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }

                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }

                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }

                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }

                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }

                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }

                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    is DragInteraction.Start -> draggedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (enabled) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    draggedElevation -> DragInteraction.Start()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction,
                    to = interaction,
                    target = target
                )
            } else {
                // No transition when moving to a disabled state.
                animatable.snapTo(target)
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CardElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Creates a [CardElevation] that will animate between the provided values according to the
 * Material specification for a [Card].
 *
 * @param defaultElevation the elevation used when the [Card] is has no other [Interaction]s.
 * @param pressedElevation the elevation used when the [Card] is pressed.
 * @param focusedElevation the elevation used when the [Card] is focused.
 * @param hoveredElevation the elevation used when the [Card] is hovered.
 * @param draggedElevation the elevation used when the [Card] is dragged.
 */
@Composable
fun cardElevation(
    defaultElevation: Dp = FilledCardTokens.ContainerElevation,
    pressedElevation: Dp = FilledCardTokens.PressedContainerElevation,
    focusedElevation: Dp = FilledCardTokens.FocusContainerElevation,
    hoveredElevation: Dp = FilledCardTokens.HoverContainerElevation,
    draggedElevation: Dp = FilledCardTokens.DraggedContainerElevation,
    disabledElevation: Dp = FilledCardTokens.DisabledContainerElevation,
): CardElevation = CardElevation(
    defaultElevation = defaultElevation,
    pressedElevation = pressedElevation,
    focusedElevation = focusedElevation,
    hoveredElevation = hoveredElevation,
    draggedElevation = draggedElevation,
    disabledElevation = disabledElevation
)

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun Card(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = cardColors(),
    elevation: CardElevation = cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = colors.contentColor(enabled).value,
        tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}

/**
 * Creates a [BorderStroke] that represents the default border used in [OutlinedCard].
 *
 * @param enabled whether the card is enabled
 */
@Composable
fun outlinedCardBorder(enabled: Boolean = true, lineColor: Color? = null): BorderStroke {
    val color = if (enabled) {
        lineColor ?: ColorScheme.outline()
    } else {
        lineColor ?: ColorScheme.outline()
            .copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
            .compositeOver(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    OutlinedCardTokens.DisabledContainerElevation
                )
            )
    }
    return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
}
