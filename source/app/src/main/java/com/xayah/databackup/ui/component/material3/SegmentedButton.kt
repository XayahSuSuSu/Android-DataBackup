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

package com.xayah.databackup.ui.component.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.token.material3.MotionTokens
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.DisabledLabelTextColor
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.DisabledLabelTextOpacity
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.DisabledOutlineOpacity
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.OutlineColor
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.SelectedContainerColor
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.SelectedLabelTextColor
import com.xayah.databackup.ui.token.material3.OutlinedSegmentedButtonTokens.UnselectedLabelTextColor
import com.xayah.databackup.ui.util.material3.fastFold
import com.xayah.databackup.ui.util.material3.fastForEach
import com.xayah.databackup.ui.util.material3.fastMap
import com.xayah.databackup.ui.util.material3.fastMaxBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 * Segmented buttons help people select options, switch views, or sort elements.
 *
 * A default Toggleable Segmented Button. Also known as Outlined Segmented Button.
 * See [Modifier.toggleable].
 *
 * Toggleable segmented buttons should be used for cases where the selection is not mutually
 * exclusive.
 *
 * This should typically be used inside of a [MultiChoiceSegmentedButtonRow]
 *
 * For a sample showing Segmented button with only checked icons see:
 * @sample androidx.compose.material3.samples.SegmentedButtonMultiSelectSample
 *
 * @param checked whether this button is checked or not
 * @param onCheckedChange callback to be invoked when the button is clicked.
 * therefore the change of checked state in requested.
 * @param shape the shape for this button
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [SegmentedButtonColors] that will be used to resolve the colors used for this
 * @param border the border for this button, see [SegmentedButtonColors]
 * Button in different states
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param icon the icon slot for this button, you can pass null in unchecked, in which case
 * the content will displace to show the checked icon, or pass different icon lambdas for
 * unchecked and checked in which case the icons will crossfade.
 * @param label content to be rendered inside this button
 */
@Composable
@ExperimentalMaterial3Api
fun MultiChoiceSegmentedButtonRowScope.SegmentedButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: SegmentedButtonBorder = SegmentedButtonDefaults.Border,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(checked) },
    label: @Composable () -> Unit,
) {
    val containerColor = colors.containerColor(enabled, checked)
    val contentColor = colors.contentColor(enabled, checked)
    val interactionCount = interactionSource.interactionCountAsState()

    Surface(
        modifier = modifier
            .weight(1f)
            .interactionZIndex(checked, interactionCount)
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            ),
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border.borderStroke(enabled, checked, colors),
        interactionSource = interactionSource
    ) {
        SegmentedButtonContent(icon, label)
    }
}

/**
 * <a href="https://m3.material.io/components/segmented-buttons/overview" class="external" target="_blank">Material Segmented Button</a>.
 * Segmented buttons help people select options, switch views, or sort elements.
 *
 * A default Toggleable Segmented Button. Also known as Outlined Segmented Button.
 * See [Modifier.selectable].
 *
 * Selectable segmented buttons should be used for cases where the selection is mutually
 * exclusive, when only one button can be selected at a time.
 *
 * This should typically be used inside of a [SingleChoiceSegmentedButtonRow]
 *
 * For a sample showing Segmented button with only checked icons see:
 * @sample androidx.compose.material3.samples.SegmentedButtonSingleSelectSample
 *
 * @param selected whether this button is selected or not
 * @param onClick callback to be invoked when the button is clicked.
 * therefore the change of checked state in requested.
 * @param shape the shape for this button
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [SegmentedButtonColors] that will be used to resolve the colors used for this
 * @param border the border for this button, see [SegmentedButtonColors]
 * Button in different states
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param icon the icon slot for this button, you can pass null in unchecked, in which case
 * the content will displace to show the checked icon, or pass different icon lambdas for
 * unchecked and checked in which case the icons will crossfade.
 * @param label content to be rendered inside this button
 */
@Composable
@ExperimentalMaterial3Api
fun SingleChoiceSegmentedButtonRowScope.SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
    border: SegmentedButtonBorder = SegmentedButtonDefaults.Border,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: @Composable () -> Unit = { SegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit,
) {
    val containerColor = colors.containerColor(enabled, selected)
    val contentColor = colors.contentColor(enabled, selected)
    val interactionCount = interactionSource.interactionCountAsState()

    Surface(
        modifier = modifier
            .weight(1f)
            .interactionZIndex(selected, interactionCount)
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            )
            .semantics { role = Role.RadioButton },
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border.borderStroke(enabled, selected, colors),
        interactionSource = interactionSource
    ) {
        SegmentedButtonContent(icon, label)
    }
}

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
    space: Dp = SegmentedButtonDefaults.Border.width,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .selectableGroup()
            .height(OutlinedSegmentedButtonTokens.ContainerHeight)
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
    space: Dp = SegmentedButtonDefaults.Border.width,
    content: @Composable MultiChoiceSegmentedButtonRowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .height(OutlinedSegmentedButtonTokens.ContainerHeight)
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
private fun SegmentedButtonContent(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        val typography =
            MaterialTheme.typography.fromToken(OutlinedSegmentedButtonTokens.LabelTextFont)
        ProvideTextStyle(typography) {
            val scope = rememberCoroutineScope()
            val measurePolicy = remember { SegmentedButtonContentMeasurePolicy(scope) }

            Layout(
                modifier = Modifier.padding(ButtonDefaults.TextButtonContentPadding),
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
        val iconDesiredWidth = iconMeasurables.fastFold(0) { acc, it ->
            maxOf(acc, it.maxIntrinsicWidth(Constraints.Infinity))
        }
        val iconWidth = iconPlaceables.fastMaxBy { it.width }?.width ?: 0
        val contentPlaceables = contentMeasurables.fastMap { it.measure(constraints) }
        val contentWidth = contentPlaceables.fastMaxBy { it.width }?.width
        val width = maxOf(SegmentedButtonDefaults.IconSize.roundToPx(), iconDesiredWidth) +
                IconSpacing.roundToPx() +
                (contentWidth ?: 0)

        val offsetX = if (iconWidth == 0) {
            -(SegmentedButtonDefaults.IconSize.roundToPx() + IconSpacing.roundToPx()) / 2
        } else {
            iconDesiredWidth - SegmentedButtonDefaults.IconSize.roundToPx()
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

        return layout(width, constraints.maxHeight) {
            iconPlaceables.fastForEach {
                it.place(0, (constraints.maxHeight - it.height) / 2)
            }

            val contentOffsetX = SegmentedButtonDefaults.IconSize.roundToPx() +
                    IconSpacing.roundToPx() + (animatable?.value ?: offsetX)

            contentPlaceables.fastForEach {
                it.place(
                    contentOffsetX,
                    (constraints.maxHeight - it.height) / 2
                )
            }
        }
    }
}

@Composable
private fun InteractionSource.interactionCountAsState(): State<Int> {
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

/* Contains defaults to be used with [SegmentedButtonRow] and [SegmentedButton] */
@ExperimentalMaterial3Api
@Stable
object SegmentedButtonDefaults {
    /**
     * Creates a [SegmentedButtonColors] that represents the different colors
     * used in a [SegmentedButton] in different states.
     *
     * @param activeContainerColor the color used for the container when enabled and active
     * @param activeContentColor the color used for the content when enabled and active
     * @param activeBorderColor the color used for the border when enabled and active
     * @param inactiveContainerColor the color used for the container when enabled and inactive
     * @param inactiveContentColor the color used for the content when enabled and inactive
     * @param inactiveBorderColor the color used for the border when enabled and active
     * @param disabledActiveContainerColor the color used for the container
     * when disabled and active
     * @param disabledActiveContentColor the color used for the content when disabled and active
     * @param disabledActiveBorderColor the color used for the border when disabled and active
     * @param disabledInactiveContainerColor the color used for the container
     * when disabled and inactive
     * @param disabledInactiveContentColor the color used for the content when disabled and
     * unchecked
     * @param disabledInactiveBorderColor the color used for the border when disabled and inactive
     */
    @Composable
    fun colors(
        activeContainerColor: Color = SelectedContainerColor.value,
        activeContentColor: Color = SelectedLabelTextColor.value,
        activeBorderColor: Color = OutlineColor.value,
        inactiveContainerColor: Color = MaterialTheme.colorScheme.surface,
        inactiveContentColor: Color = UnselectedLabelTextColor.value,
        inactiveBorderColor: Color = activeBorderColor,
        disabledActiveContainerColor: Color = activeContainerColor,
        disabledActiveContentColor: Color = DisabledLabelTextColor.value
            .copy(alpha = DisabledLabelTextOpacity),
        disabledActiveBorderColor: Color = OutlineColor.value
            .copy(alpha = DisabledOutlineOpacity),
        disabledInactiveContainerColor: Color = inactiveContainerColor,
        disabledInactiveContentColor: Color = disabledActiveContentColor,
        disabledInactiveBorderColor: Color = activeBorderColor,
    ): SegmentedButtonColors = SegmentedButtonColors(
        activeContainerColor = activeContainerColor,
        activeContentColor = activeContentColor,
        activeBorderColor = activeBorderColor,
        inactiveContainerColor = inactiveContainerColor,
        inactiveContentColor = inactiveContentColor,
        inactiveBorderColor = inactiveBorderColor,
        disabledActiveContainerColor = disabledActiveContainerColor,
        disabledActiveContentColor = disabledActiveContentColor,
        disabledActiveBorderColor = disabledActiveBorderColor,
        disabledInactiveContainerColor = disabledInactiveContainerColor,
        disabledInactiveContentColor = disabledInactiveContentColor,
        disabledInactiveBorderColor = disabledInactiveBorderColor
    )

    /** The default [BorderStroke] factory used by [SegmentedButton]. */
    val Border = SegmentedButtonBorder(width = OutlinedSegmentedButtonTokens.OutlineWidth)

    /**
     * The shape of the segmented button container, for correct behavior this should or the desired
     * [CornerBasedShape] should be used with [itemShape] and passed to each segmented button.
     */
    val baseShape: CornerBasedShape
        @Composable
        @ReadOnlyComposable
        get() = OutlinedSegmentedButtonTokens.Shape.value as CornerBasedShape

    /**
     * A shape constructor that the button in [index] should have when there are [count] buttons in
     * the container.
     *
     * @param index the index for this button in the row
     * @param count the count of buttons in this row
     * @param baseShape the [CornerBasedShape] the base shape that should be used in buttons that
     * are not in the start or the end.
     */
    @Composable
    @ReadOnlyComposable
    fun itemShape(index: Int, count: Int, baseShape: CornerBasedShape = this.baseShape): Shape {
        if (count == 1) {
            return baseShape
        }

        return when (index) {
            0 -> baseShape.start()
            count - 1 -> baseShape.end()
            else -> RectangleShape
        }
    }

    /**
     * Icon size to use for icons used in [SegmentedButton]
     */
    val IconSize = OutlinedSegmentedButtonTokens.IconSize

    /** And icon to indicate the segmented button is checked or selected */
    @Composable
    fun ActiveIcon() {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }

    /**
     * The default implementation of icons for Segmented Buttons.
     *
     * @param active whether the button is activated or not.
     * @param activeContent usually a checkmark icon of [IconSize] dimensions.
     * @param inactiveContent typically an icon of [IconSize]. It shows only when the button is not
     * checked.
     */
    @Composable
    fun Icon(
        active: Boolean,
        activeContent: @Composable () -> Unit = { ActiveIcon() },
        inactiveContent: (@Composable () -> Unit)? = null
    ) {
        if (inactiveContent == null) {
            AnimatedVisibility(
                visible = active,
                exit = ExitTransition.None,
                enter = fadeIn(tween(MotionTokens.DurationMedium3.toInt())) + scaleIn(
                    initialScale = 0f,
                    transformOrigin = TransformOrigin(0f, 1f),
                    animationSpec = tween(MotionTokens.DurationMedium3.toInt()),
                ),
            ) {
                activeContent()
            }
        } else {
            Crossfade(targetState = active) {
                if (it) activeContent() else inactiveContent()
            }
        }
    }
}

/**
 * Class to create border stroke for segmented button, see [SegmentedButtonColors], for
 * customization of colors.
 */
@ExperimentalMaterial3Api
@Immutable
class SegmentedButtonBorder(val width: Dp) {

    /** The default [BorderStroke] used by [SegmentedButton]. */
    fun borderStroke(
        enabled: Boolean,
        checked: Boolean,
        colors: SegmentedButtonColors
    ): BorderStroke = BorderStroke(
        width = width,
        color = colors.borderColor(enabled, checked)
    )
}

/**
 * The different colors used in parts of the [SegmentedButton] in different states
 *
 * @constructor create an instance with arbitrary colors, see [SegmentedButtonDefaults] for a
 * factory method using the default material3 spec
 *
 * @param activeContainerColor the color used for the container when enabled and active
 * @param activeContentColor the color used for the content when enabled and active
 * @param activeBorderColor the color used for the border when enabled and active
 * @param inactiveContainerColor the color used for the container when enabled and inactive
 * @param inactiveContentColor the color used for the content when enabled and inactive
 * @param inactiveBorderColor the color used for the border when enabled and active
 * @param disabledActiveContainerColor the color used for the container when disabled and active
 * @param disabledActiveContentColor the color used for the content when disabled and active
 * @param disabledActiveBorderColor the color used for the border when disabled and active
 * @param disabledInactiveContainerColor the color used for the container
 * when disabled and inactive
 * @param disabledInactiveContentColor the color used for the content when disabled and inactive
 * @param disabledInactiveBorderColor the color used for the border when disabled and inactive
 */
@Immutable
@ExperimentalMaterial3Api
class SegmentedButtonColors constructor(
    // enabled & active
    val activeContainerColor: Color,
    val activeContentColor: Color,
    val activeBorderColor: Color,
    // enabled & inactive
    val inactiveContainerColor: Color,
    val inactiveContentColor: Color,
    val inactiveBorderColor: Color,
    // disable & active
    val disabledActiveContainerColor: Color,
    val disabledActiveContentColor: Color,
    val disabledActiveBorderColor: Color,
    // disable & inactive
    val disabledInactiveContainerColor: Color,
    val disabledInactiveContentColor: Color,
    val disabledInactiveBorderColor: Color
) {
    /**
     * Represents the color used for the SegmentedButton's border,
     * depending on [enabled] and [active].
     *
     * @param enabled whether the [SegmentedButton] is enabled or not
     * @param active whether the [SegmentedButton] item is checked or not
     */
    internal fun borderColor(enabled: Boolean, active: Boolean): Color {
        return when {
            enabled && active -> activeBorderColor
            enabled && !active -> inactiveBorderColor
            !enabled && active -> disabledActiveContentColor
            else -> disabledInactiveContentColor
        }
    }

    /**
     * Represents the content color passed to the items
     *
     * @param enabled whether the [SegmentedButton] is enabled or not
     * @param checked whether the [SegmentedButton] item is checked or not
     */
    internal fun contentColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> activeContentColor
            enabled && !checked -> inactiveContentColor
            !enabled && checked -> disabledActiveContentColor
            else -> disabledInactiveContentColor
        }
    }

    /**
     * Represents the container color passed to the items
     *
     * @param enabled whether the [SegmentedButton] is enabled or not
     * @param active whether the [SegmentedButton] item is active or not
     */
    internal fun containerColor(enabled: Boolean, active: Boolean): Color {
        return when {
            enabled && active -> activeContainerColor
            enabled && !active -> inactiveContainerColor
            !enabled && active -> disabledActiveContainerColor
            else -> disabledInactiveContainerColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as SegmentedButtonColors

        if (activeBorderColor != other.activeBorderColor) return false
        if (activeContentColor != other.activeContentColor) return false
        if (activeContainerColor != other.activeContainerColor) return false
        if (inactiveBorderColor != other.inactiveBorderColor) return false
        if (inactiveContentColor != other.inactiveContentColor) return false
        if (inactiveContainerColor != other.inactiveContainerColor) return false
        if (disabledActiveBorderColor != other.disabledActiveBorderColor) return false
        if (disabledActiveContentColor != other.disabledActiveContentColor) return false
        if (disabledActiveContainerColor != other.disabledActiveContainerColor) return false
        if (disabledInactiveBorderColor != other.disabledInactiveBorderColor) return false
        if (disabledInactiveContentColor != other.disabledInactiveContentColor) return false
        if (disabledInactiveContainerColor != other.disabledInactiveContainerColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activeBorderColor.hashCode()
        result = 31 * result + activeContentColor.hashCode()
        result = 31 * result + activeContainerColor.hashCode()
        result = 31 * result + inactiveBorderColor.hashCode()
        result = 31 * result + inactiveContentColor.hashCode()
        result = 31 * result + inactiveContainerColor.hashCode()
        result = 31 * result + disabledActiveBorderColor.hashCode()
        result = 31 * result + disabledActiveContentColor.hashCode()
        result = 31 * result + disabledActiveContainerColor.hashCode()
        result = 31 * result + disabledInactiveBorderColor.hashCode()
        result = 31 * result + disabledInactiveContentColor.hashCode()
        result = 31 * result + disabledInactiveContainerColor.hashCode()
        return result
    }
}

private fun Modifier.interactionZIndex(checked: Boolean, interactionCount: State<Int>) =
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
