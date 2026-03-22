# Compose Foundation Source Reference

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Clickable.kt
```kotlin
/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.ComposeFoundationFlags.isDelayPressesUsingGestureConsumptionEnabled
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.ScrollableContainerNode
import androidx.compose.foundation.gestures.changedToDownIgnoreConsumed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.isChangedToDown
import androidx.compose.foundation.gestures.isDeepPress
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this clickable modifier inside composition, consider using the other
 * overload and explicitly passing `LocalIndication.current` for improved performance. For more
 * information see the documentation on the other overload.
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
@Deprecated(
    message =
        "Replaced with new overload that only supports IndicationNodeFactory instances inside LocalIndication, and does not use composed",
    level = DeprecationLevel.HIDDEN,
)
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "clickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
        )
    }

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This overload will use the [Indication] from [LocalIndication]. Use the other overload to
 * explicitly provide an [Indication] instance. Note that this overload only supports
 * [IndicationNodeFactory] instances provided through [LocalIndication] - it is strongly recommended
 * to migrate to [IndicationNodeFactory], but you can use the other overload if you still need to
 * support [Indication] instances that are not [IndicationNodeFactory].
 *
 * If [interactionSource] is `null`, an internal [MutableInteractionSource] will be lazily created
 * only when needed. This reduces the performance cost of clickable during composition, as creating
 * the [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of clickable, it is recommended to
 * instead provide `null` to enable lazy creation. If you need the [Indication] to be created
 * eagerly, provide a remembered [MutableInteractionSource].
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    return this.then(
        ClickableElement(
            interactionSource = interactionSource,
            indicationNodeFactory = null,
            useLocalIndication = true,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    )
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show an indication as
 * specified in [indication] parameter.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an internal
 * [MutableInteractionSource] will be lazily created along with the [indication] only when needed.
 * This reduces the performance cost of clickable during composition, as creating the [indication]
 * can be delayed until there is an incoming [androidx.compose.foundation.interaction.Interaction].
 * If you are only passing a remembered [MutableInteractionSource] and you are never using it
 * outside of clickable, it is recommended to instead provide `null` to enable lazy creation. If you
 * need [indication] to be created eagerly, provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside clickable.
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. By default, indication
 *   from [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 *   [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        ClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable]
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this combinedClickable modifier inside composition, consider using the
 * other overload and explicitly passing `LocalIndication.current` for improved performance. For
 * more information see the documentation on the other overload.
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param onClick will be called when user clicks on the element
 */
@Deprecated(
    message =
        "Replaced with new overload that only supports IndicationNodeFactory instances inside LocalIndication, and does not use composed",
    level = DeprecationLevel.HIDDEN,
)
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "combinedClickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
                properties["onDoubleClick"] = onDoubleClick
                properties["onLongClick"] = onLongClick
                properties["onLongClickLabel"] = onLongClickLabel
                properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable]
 *
 * This overload will use the [Indication] from [LocalIndication]. Use the other overload to
 * explicitly provide an [Indication] instance. Note that this overload only supports
 * [IndicationNodeFactory] instances provided through [LocalIndication] - it is strongly recommended
 * to migrate to [IndicationNodeFactory], but you can use the other overload if you still need to
 * support [Indication] instances that are not [IndicationNodeFactory].
 *
 * If [interactionSource] is `null`, an internal [MutableInteractionSource] will be lazily created
 * only when needed. This reduces the performance cost of combinedClickable during composition, as
 * creating the [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of combinedClickable, it is
 * recommended to instead provide `null` to enable lazy creation. If you need the [Indication] to be
 * created eagerly, provide a remembered [MutableInteractionSource].
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    return this.then(
        CombinedClickableElement(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
            role = role,
            interactionSource = interactionSource,
            indicationNodeFactory = null,
            useLocalIndication = true,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    )
}

@Deprecated(message = "Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "combinedClickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
                properties["onDoubleClick"] = onDoubleClick
                properties["onLongClick"] = onLongClick
                properties["onLongClickLabel"] = onLongClickLabel
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
            hapticFeedbackEnabled = true,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable].
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an internal
 * [MutableInteractionSource] will be lazily created along with the [indication] only when needed.
 * This reduces the performance cost of clickable during composition, as creating the [indication]
 * can be delayed until there is an incoming [androidx.compose.foundation.interaction.Interaction].
 * If you are only passing a remembered [MutableInteractionSource] and you are never using it
 * outside of clickable, it is recommended to instead provide `null` to enable lazy creation. If you
 * need [indication] to be created eagerly, provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside clickable.
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. By default, indication
 *   from [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 *   [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        CombinedClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    }

@Deprecated(message = "Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        CombinedClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = true,
        )
    }

/**
 * Utility Modifier factory that handles edge cases for [interactionSource], and [indication].
 * [createClickable] is the lambda that creates the actual clickable element, which will be chained
 * with [Modifier.indication] if needed.
 */
internal inline fun Modifier.clickableWithIndicationIfNeeded(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    crossinline createClickable: (MutableInteractionSource?, IndicationNodeFactory?) -> Modifier,
): Modifier {
    return this.then(
        when {
            // Fast path - indication is managed internally
            indication is IndicationNodeFactory -> createClickable(interactionSource, indication)
            // Fast path - no need for indication
            indication == null -> createClickable(interactionSource, null)
            // Non-null Indication (not IndicationNodeFactory) with a non-null InteractionSource
            interactionSource != null ->
                Modifier.indication(interactionSource, indication)
                    .then(createClickable(interactionSource, null))
            // Non-null Indication (not IndicationNodeFactory) with a null InteractionSource, so we
            // need
            // to use composed to create an InteractionSource that can be shared. This should be a
            // rare
            // code path and can only be hit from new callers.
            else ->
                Modifier.composed {
                    val newInteractionSource = remember { MutableInteractionSource() }
                    Modifier.indication(newInteractionSource, indication)
                        .then(createClickable(newInteractionSource, null))
                }
        }
    )
}

/**
 * How long to wait before appearing 'pressed' (emitting [PressInteraction.Press]) - if a touch down
 * will quickly become a drag / scroll, this timeout means that we don't show a press effect.
 */
internal expect val TapIndicationDelay: Long

/**
 * Returns whether the root Compose layout node is hosted in a scrollable container outside of
 * Compose. On Android this will be whether the root View is in a scrollable ViewGroup, as even if
 * nothing in the Compose part of the hierarchy is scrollable, if the View itself is in a scrollable
 * container, we still want to delay presses in case presses in Compose convert to a scroll outside
 * of Compose.
 *
 * Combine this with [hasScrollableContainer], which returns whether a [Modifier] is within a
 * scrollable Compose layout, to calculate whether this modifier is within some form of scrollable
 * container, and hence should delay presses.
 */
internal expect fun DelegatableNode.isComposeRootInScrollableContainer(): Boolean

/**
 * Whether the specified [KeyEvent] should trigger a press for a clickable component, i.e. whether
 * it is associated with a press of an enter key or dpad centre.
 */
private val KeyEvent.isPress: Boolean
    get() = type == KeyDown && isEnter

/**
 * Whether the specified [KeyEvent] should trigger a click for a clickable component, i.e. whether
 * it is associated with a release of an enter key or dpad centre.
 */
private val KeyEvent.isClick: Boolean
    get() = type == KeyUp && isEnter

private val KeyEvent.isEnter: Boolean
    get() =
        when (key) {
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter,
            Key.Spacebar -> true

            else -> false
        }

private class ClickableElement(
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val useLocalIndication: Boolean,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role?,
    private val onClick: () -> Unit,
) : ModifierNodeElement<ClickableNode>() {
    override fun create() =
        ClickableNode(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )

    override fun update(node: ClickableNode) {
        node.update(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClick"] = onClick
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indicationNodeFactory"] = indicationNodeFactory
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as ClickableElement

        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (useLocalIndication != other.useLocalIndication) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick !== other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + useLocalIndication.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class CombinedClickableElement(
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val useLocalIndication: Boolean,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role?,
    private val onClick: () -> Unit,
    private val onLongClickLabel: String?,
    private val onLongClick: (() -> Unit)?,
    private val onDoubleClick: (() -> Unit)?,
    private val hapticFeedbackEnabled: Boolean,
) : ModifierNodeElement<CombinedClickableNode>() {
    override fun create() =
        CombinedClickableNode(
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        )

    override fun update(node: CombinedClickableNode) {
        node.hapticFeedbackEnabled = hapticFeedbackEnabled
        node.update(
            onClick,
            onLongClickLabel,
            onLongClick,
            onDoubleClick,
            interactionSource,
            indicationNodeFactory,
            useLocalIndication,
            enabled,
            onClickLabel,
            role,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "combinedClickable"
        properties["indicationNodeFactory"] = indicationNodeFactory
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onLongClickLabel"] = onLongClickLabel
        properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as CombinedClickableElement

        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (useLocalIndication != other.useLocalIndication) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick !== other.onClick) return false
        if (onLongClickLabel != other.onLongClickLabel) return false
        if (onLongClick !== other.onLongClick) return false
        if (onDoubleClick !== other.onDoubleClick) return false
        if (hapticFeedbackEnabled != other.hapticFeedbackEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + useLocalIndication.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        result = 31 * result + (onLongClickLabel?.hashCode() ?: 0)
        result = 31 * result + (onLongClick?.hashCode() ?: 0)
        result = 31 * result + (onDoubleClick?.hashCode() ?: 0)
        result = 31 * result + hapticFeedbackEnabled.hashCode()
        return result
    }
}

internal open class ClickableNode(
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    useLocalIndication: Boolean,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit,
) :
    AbstractClickableNode(
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        useLocalIndication = useLocalIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    ) {

    private var downEvent: PointerInputChange? = null
    private var indirectDownEvent: IndirectPointerInputChange? = null

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        super.onPointerEvent(pointerEvent, pass, bounds)
        if (pass == PointerEventPass.Main) {
            if (downEvent == null) {
                if (pointerEvent.isChangedToDown(requireUnconsumed = true)) {
                    handleDownEvent(pointerEvent.changes[0])
                }
            } else {
                if (pointerEvent.changes.fastAll { it.changedToUp() }) {
                    // All pointers are up
                    handleUpEvent(pointerEvent.changes[0])
                } else {
                    // Other events need to be checked for consumption / bounds related
                    // cancellation.
                    handleNonUpEventIfNeeded(pointerEvent, bounds)
                }
            }
        } else if (pass == PointerEventPass.Final) {
            checkForCancellation(pointerEvent)
        }
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        super.onIndirectPointerEvent(event, pass)
        if (pass == PointerEventPass.Main) {
            if (indirectDownEvent == null) {
                if (event.changes.fastAny { it.changedToDownIgnoreConsumed() }) {
                    handleDownEvent(event.changes[0])
                }
            } else {
                if (event.changes.fastAll { it.changedToUp() }) {
                    // All pointers are up
                    handleUpEvent(event.changes[0])
                } else {
                    // Other events need to be checked for consumption / exceeding touch slop
                    handleNonUpEventIfNeeded(event)
                }
            }
        } else if (pass == PointerEventPass.Final) {
            checkForCancellation(event)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun handleDownEvent(down: PointerInputChange) {
        down.consume()
        this.downEvent = down
        if (enabled) {
            if (isDelayPressesUsingGestureConsumptionEnabled) {
                handlePressInteractionStart(down)
            } else {
                handlePressInteractionStart(down.position, false)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun handleDownEvent(down: IndirectPointerInputChange) {
        down.consume()
        this.indirectDownEvent = down
        if (enabled) {
            if (isDelayPressesUsingGestureConsumptionEnabled) {
                handlePressInteractionStart(down)
            } else {
                handlePressInteractionStart(down.position, true)
            }
        }
    }

    private fun handleUpEvent(up: PointerInputChange) {
        up.consume()
        if (enabled) {
            handlePressInteractionRelease(downEvent!!.position, indirectPointer = false)
            onClick()
        }
        this.downEvent = null
    }

    private fun handleUpEvent(up: IndirectPointerInputChange) {
        up.consume()
        if (enabled) {
            handlePressInteractionRelease(indirectDownEvent!!.position, indirectPointer = true)
            onClick()
        }
        this.indirectDownEvent = null
    }

    private fun handleNonUpEventIfNeeded(pointerEvent: PointerEvent, bounds: IntSize) {
        val touchPadding = getExtendedTouchPadding(bounds)
        if (
            pointerEvent.changes.fastAny { it.isConsumed || it.isOutOfBounds(bounds, touchPadding) }
        ) {
            cancelInput(indirectPointer = false)
        }
    }

    private fun handleNonUpEventIfNeeded(indirectPointerEvent: IndirectPointerEvent) {
        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        if (
            indirectPointerEvent.changes.fastAny {
                val distanceFromPress = it.position - indirectDownEvent!!.position
                val isOutOfBounds = abs(distanceFromPress.getDistance()) > touchSlop
                it.isConsumed || isOutOfBounds
            }
        ) {
            cancelInput(indirectPointer = true)
        }
    }

    private fun checkForCancellation(pointerEvent: PointerEvent) {
        if (downEvent != null) {
            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the pass we checked above.
            if (pointerEvent.changes.fastAny { it.isConsumed && it != downEvent }) {
                cancelInput(indirectPointer = false)
            }
        }
    }

    private fun checkForCancellation(indirectPointerEvent: IndirectPointerEvent) {
        if (indirectDownEvent != null) {
            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the pass we checked above.
            if (indirectPointerEvent.changes.fastAny { it.isConsumed && it != indirectDownEvent }) {
                cancelInput(indirectPointer = true)
            }
        }
    }

    override fun onCancelPointerInput() {
        super.onCancelPointerInput()
        cancelInput(indirectPointer = false)
    }

    override fun onCancelIndirectPointerInput() {
        cancelInput(indirectPointer = true)
    }

    private fun cancelInput(indirectPointer: Boolean) {
        // Don't cancel pointer events when cancelling indirect events (because of losing focus for
        // example), and vice versa.
        if (indirectPointer) {
            indirectDownEvent = null
        } else {
            downEvent = null
        }
        handlePressInteractionCancel(indirectPointer = indirectPointer)
    }

    fun update(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
    ) {
        // enabled and onClick are captured inside callbacks, not as an input to detectTapGestures,
        // so no need to reset pointer input handling when they change
        updateCommon(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

    final override fun onClickKeyDownEvent(event: KeyEvent) = false

    final override fun onClickKeyUpEvent(event: KeyEvent): Boolean {
        onClick()
        return true
    }
}

private class CombinedClickableNode(
    onClick: () -> Unit,
    private var onLongClickLabel: String?,
    private var onLongClick: (() -> Unit)?,
    private var onDoubleClick: (() -> Unit)?,
    var hapticFeedbackEnabled: Boolean,
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    useLocalIndication: Boolean,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
) :
    CompositionLocalConsumerModifierNode,
    AbstractClickableNode(
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        useLocalIndication = useLocalIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    ) {
    class DoubleKeyClickState(val job: Job) {
        var doubleTapMinTimeMillisElapsed: Boolean = false
    }

    private val longKeyPressJobs = mutableLongObjectMapOf<Job>()
    private val doubleKeyClickStates = mutableLongObjectMapOf<DoubleKeyClickState>()
    @OptIn(ExperimentalFoundationApi::class)
    private val isSuspendingPointerInputEnabled =
        !ComposeFoundationFlags.isNonSuspendingPointerInputInCombinedClickableEnabled
    private var downEvent: PointerInputChange? = null
    private var longPressJob: Job? = null
    private var tapJob: Job? = null
    private var isSecondTap = false
    private var longPressTriggered = false
    private var firstTapUpTime = -1L
    private var ignoreNextUp = false

    private var indirectDownEvent: IndirectPointerInputChange? = null
    private var indirectLongPressJob: Job? = null
    private var indirectTapJob: Job? = null
    private var indirectIsSecondTap = false
    private var indirectLongPressTriggered = false
    private var indirectFirstTapUpTime = -1L
    private var indirectIgnoreNextUp = false

    override fun createPointerInputNodeIfNeeded(): SuspendingPointerInputModifierNode? {
        if (isSuspendingPointerInputEnabled) {
            return SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onDoubleTap =
                        if (enabled && onDoubleClick != null) {
                            { onDoubleClick?.invoke() }
                        } else null,
                    onLongPress =
                        if (enabled && onLongClick != null) {
                            {
                                onLongClick?.invoke()
                                if (hapticFeedbackEnabled) {
                                    currentValueOf(LocalHapticFeedback)
                                        .performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        } else null,
                    onPress = { offset ->
                        if (enabled) {
                            handlePressInteraction(offset)
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClick()
                        }
                    },
                )
            }
        }
        return null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        super.onPointerEvent(pointerEvent, pass, bounds)
        if (isSuspendingPointerInputEnabled) return

        if (pass == PointerEventPass.Main) {
            if (downEvent == null) {
                if (pointerEvent.isChangedToDown(requireUnconsumed = true)) {
                    handleDownEvent(pointerEvent.changes[0])
                }
            } else {
                if (pointerEvent.isDeepPress) {
                    handleDeepPress()
                }

                if (longPressTriggered) {
                    // This branch specifically handles the case where the long press callback has
                    // already been invoked.
                    if (pointerEvent.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                        // A long press already fired its callback and all the pointers are up. We
                        // must reset our state even if the up event was already consumed by a
                        // child.
                        val up = pointerEvent.changes[0]
                        up.consume()
                        handleUpEvent(uptimeMillis = up.uptimeMillis, downChange = downEvent!!)
                    } else {
                        // Once a long press has triggered, consume every event until pointers are
                        // up.
                        pointerEvent.changes.fastForEach { it.consume() }
                    }
                    return
                }

                if (pointerEvent.changes.fastAll { it.changedToUp() }) {
                    // All pointers are up
                    val up = pointerEvent.changes[0]
                    up.consume()
                    handleUpEvent(uptimeMillis = up.uptimeMillis, downChange = downEvent!!)
                } else {
                    // Other events need to be checked for consumption / bounds related
                    // cancellation.
                    handleNonUpEventIfNeeded(pointerEvent, bounds)
                }
            }
        } else if (pass == PointerEventPass.Final) {
            checkForCancellation(pointerEvent)
        }
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        super.onIndirectPointerEvent(event, pass)
        if (pass == PointerEventPass.Main) {
            if (indirectDownEvent == null) {
                if (event.changes.fastAny { it.changedToDownIgnoreConsumed() }) {
                    handleDownEvent(event.changes[0])
                }
            } else {
                if (indirectLongPressTriggered) {
                    // This branch specifically handles the case where the long press callback has
                    // already been invoked.
                    if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                        // A long press already fired its callback and all the pointers are up. We
                        // must reset our state even if the up event was already consumed by a
                        // child.
                        val up = event.changes[0]
                        up.consume()
                        handleUpEvent(
                            uptimeMillis = up.uptimeMillis,
                            downChange = indirectDownEvent!!,
                        )
                    } else {
                        // Once a long press has triggered, consume every event until pointers are
                        // up
                        event.changes.fastForEach { it.consume() }
                    }
                    return
                }

                if (event.changes.fastAll { it.changedToUp() }) {
                    // All pointers are up
                    val up = event.changes[0]
                    up.consume()
                    handleUpEvent(uptimeMillis = up.uptimeMillis, downChange = indirectDownEvent!!)
                } else {
                    // Other events need to be checked for consumption / exceeding touch slop
                    handleNonUpEventIfNeeded(event)
                }
            }
        } else if (pass == PointerEventPass.Final) {
            checkForCancellation(event)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun handleDownEvent(down: PointerInputChange) {
        down.consume()
        this.downEvent = down

        if (enabled) {
            if (tapJob?.isActive == true) {
                val minTime = currentValueOf(LocalViewConfiguration).doubleTapMinTimeMillis
                if (down.uptimeMillis - firstTapUpTime < minTime) {
                    ignoreNextUp = true
                    // Ignore this down event, don't check for long press / emit press
                    // interactions
                    return
                } else {
                    isSecondTap = true
                    tapJob?.cancel()
                    tapJob = null
                }
            }
            longPressTriggered = false

            if (isDelayPressesUsingGestureConsumptionEnabled) {
                handlePressInteractionStart(down)
            } else {
                handlePressInteractionStart(down.position, false)
            }

            if (onLongClick != null) {
                longPressJob =
                    coroutineScope.launch {
                        delay(currentValueOf(LocalViewConfiguration).longPressTimeoutMillis)
                        onLongClick?.invoke()
                        if (hapticFeedbackEnabled) {
                            currentValueOf(LocalHapticFeedback)
                                .performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        longPressTriggered = true
                        tapJob?.cancel()
                        tapJob = null
                        longPressJob = null
                    }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun handleDownEvent(down: IndirectPointerInputChange) {
        down.consume()
        this.indirectDownEvent = down

        if (enabled) {
            if (indirectTapJob?.isActive == true) {
                val minTime = currentValueOf(LocalViewConfiguration).doubleTapMinTimeMillis
                if (down.uptimeMillis - indirectFirstTapUpTime < minTime) {
                    indirectIgnoreNextUp = true
                    // Ignore this down event, don't check for long press / emit press
                    // interactions
                    return
                } else {
                    indirectIsSecondTap = true
                    indirectTapJob?.cancel()
                    indirectTapJob = null
                }
            }
            indirectLongPressTriggered = false

            if (isDelayPressesUsingGestureConsumptionEnabled) {
                handlePressInteractionStart(down)
            } else {
                handlePressInteractionStart(down.position, true)
            }

            if (onLongClick != null) {
                indirectLongPressJob =
                    coroutineScope.launch {
                        delay(currentValueOf(LocalViewConfiguration).longPressTimeoutMillis)
                        onLongClick?.invoke()
                        if (hapticFeedbackEnabled) {
                            currentValueOf(LocalHapticFeedback)
                                .performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        indirectLongPressTriggered = true
                        indirectTapJob?.cancel()
                        indirectTapJob = null
                        indirectLongPressJob = null
                    }
            }
        }
    }

    private fun handleUpEvent(uptimeMillis: Long, downChange: PointerInputChange) {
        if (enabled && !ignoreNextUp) {
            handlePressInteractionRelease(downChange.position, indirectPointer = false)
            firstTapUpTime = uptimeMillis // store uptime for double tap check
            if (!longPressTriggered) {
                if (isSecondTap) {
                    onDoubleClick?.invoke()
                } else {
                    if (onDoubleClick != null) {
                        tapJob =
                            coroutineScope.launch {
                                delay(currentValueOf(LocalViewConfiguration).doubleTapTimeoutMillis)
                                onClick()
                                tapJob = null
                            }
                    } else {
                        onClick()
                    }
                }
            }
        }
        this.downEvent = null
        ignoreNextUp = false
        isSecondTap = false
        longPressJob?.cancel()
        longPressJob = null
        longPressTriggered = false
    }

    private fun handleUpEvent(uptimeMillis: Long, downChange: IndirectPointerInputChange) {
        if (enabled && !indirectIgnoreNextUp) {
            handlePressInteractionRelease(downChange.position, indirectPointer = true)
            indirectFirstTapUpTime = uptimeMillis // store uptime for double tap check
            if (!indirectLongPressTriggered) {
                if (indirectIsSecondTap) {
                    onDoubleClick?.invoke()
                } else {
                    if (onDoubleClick != null) {
                        indirectTapJob =
                            coroutineScope.launch {
                                delay(currentValueOf(LocalViewConfiguration).doubleTapTimeoutMillis)
                                onClick()
                                indirectTapJob = null
                            }
                    } else {
                        onClick()
                    }
                }
            }
        }
        this.indirectDownEvent = null
        indirectIgnoreNextUp = false
        indirectIsSecondTap = false
        indirectLongPressJob?.cancel()
        indirectLongPressJob = null
        indirectLongPressTriggered = false
    }

    private fun handleNonUpEventIfNeeded(pointerEvent: PointerEvent, bounds: IntSize) {
        val touchPadding = getExtendedTouchPadding(bounds)
        if (
            pointerEvent.changes.fastAny { change ->
                change.isConsumed || change.isOutOfBounds(bounds, touchPadding)
            }
        ) {
            cancelInput(indirectPointer = false)
        }
    }

    private fun handleNonUpEventIfNeeded(indirectPointerEvent: IndirectPointerEvent) {
        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        if (
            indirectPointerEvent.changes.fastAny { change ->
                val distanceFromPress = change.position - indirectDownEvent!!.position
                val isOutOfBounds = abs(distanceFromPress.getDistance()) > touchSlop
                change.isConsumed || isOutOfBounds
            }
        ) {
            cancelInput(indirectPointer = true)
        }
    }

    private fun handleDeepPress() {
        if (!longPressTriggered && enabled && onLongClick != null) {
            longPressJob?.cancel()
            longPressJob = null
            onLongClick?.invoke()
            if (hapticFeedbackEnabled) {
                currentValueOf(LocalHapticFeedback)
                    .performHapticFeedback(HapticFeedbackType.LongPress)
            }
            longPressTriggered = true
        }
    }

    private fun checkForCancellation(pointerEvent: PointerEvent) {
        if (downEvent != null && !longPressTriggered) {
            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the pass we checked above. We ignore
            // cases where the long press has already triggered, as in this case we will consume
            // events ourselves until the pointer is released.
            if (pointerEvent.changes.fastAny { it.isConsumed && it != downEvent }) {
                // Canceled
                cancelInput(indirectPointer = false)
            }
        }
    }

    private fun checkForCancellation(indirectPointerEvent: IndirectPointerEvent) {
        if (indirectDownEvent != null && !indirectLongPressTriggered) {
            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the pass we checked above. We ignore
            // cases where the long press has already triggered, as in this case we will consume
            // events ourselves until the pointer is released.
            if (indirectPointerEvent.changes.fastAny { it.isConsumed && it != indirectDownEvent }) {
                // Canceled
                cancelInput(indirectPointer = true)
            }
        }
    }

    override fun onCancelPointerInput() {
        super.onCancelPointerInput()
        cancelInput(indirectPointer = false)
    }

    override fun onCancelIndirectPointerInput() {
        cancelInput(indirectPointer = true)
    }

    private fun cancelInput(indirectPointer: Boolean) {
        if (indirectPointer) {
            indirectDownEvent = null
            indirectLongPressJob?.cancel()
            indirectLongPressJob = null
            indirectTapJob?.cancel()
            indirectTapJob = null
            indirectIsSecondTap = false
            indirectLongPressTriggered = false
            indirectFirstTapUpTime = -1L
            indirectIgnoreNextUp = false
        } else {
            downEvent = null
            longPressJob?.cancel()
            longPressJob = null
            tapJob?.cancel()
            tapJob = null
            isSecondTap = false
            longPressTriggered = false
            firstTapUpTime = -1L
            ignoreNextUp = false
        }
        handlePressInteractionCancel(indirectPointer)
    }

    fun update(
        onClick: () -> Unit,
        onLongClickLabel: String?,
        onLongClick: (() -> Unit)?,
        onDoubleClick: (() -> Unit)?,
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
    ) {
        var resetPointerInputHandling = false

        // onClick is captured inside a callback, not as an input to detectTapGestures,
        // so no need to reset pointer input handling

        if (this.onLongClickLabel != onLongClickLabel) {
            this.onLongClickLabel = onLongClickLabel
            invalidateSemantics()
        }

        // We capture onLongClick and onDoubleClick inside the callback, so if the lambda changes
        // value we don't want to reset input handling - only reset if they go from not-defined to
        // defined, and vice versa, as that is what is captured in the parameter to
        // detectTapGestures.
        if ((this.onLongClick == null) != (onLongClick == null)) {
            // Adding or removing longClick should cancel any existing press interactions
            disposeInteractions()
            // Adding or removing longClick should add / remove the corresponding property
            invalidateSemantics()
            resetPointerInputHandling = true
        }

        this.onLongClick = onLongClick

        if ((this.onDoubleClick == null) != (onDoubleClick == null)) {
            resetPointerInputHandling = true
        }
        this.onDoubleClick = onDoubleClick

        // enabled is captured as a parameter to detectTapGestures, so we need to restart detecting
        // gestures if it changes.
        if (this.enabled != enabled) {
            resetPointerInputHandling = true
            // Updating is handled inside updateCommon
        }

        updateCommon(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )

        if (resetPointerInputHandling) {
            resetPointerInputHandler()
            cancelInput(indirectPointer = false)
            cancelInput(indirectPointer = true)
        }
    }

    override fun SemanticsPropertyReceiver.applyAdditionalSemantics() {
        if (onLongClick != null) {
            onLongClick(
                action = {
                    onLongClick?.invoke()
                    true
                },
                label = onLongClickLabel,
            )
        }
    }

    override fun onClickKeyDownEvent(event: KeyEvent): Boolean {
        val keyCode = event.key.keyCode
        var handledByLongClick = false
        if (onLongClick != null) {
            if (longKeyPressJobs[keyCode] == null) {
                longKeyPressJobs[keyCode] =
                    coroutineScope.launch {
                        delay(currentValueOf(LocalViewConfiguration).longPressTimeoutMillis)
                        onLongClick?.invoke()
                    }
                handledByLongClick = true
            }
        }
        val doubleClickState = doubleKeyClickStates[keyCode]
        // This is the second down event, so it might be a double click
        if (doubleClickState != null) {
            // Within the allowed timeout, so check if this is above the minimum time needed for
            // a double click
            if (doubleClickState.job.isActive) {
                doubleClickState.job.cancel()
                // If the second down was before the minimum double tap time, don't track this as
                // a double click. Instead, we need to invoke onClick for the previous click, since
                // that is now counted as a standalone click instead of the first of a double click.
                if (!doubleClickState.doubleTapMinTimeMillisElapsed) {
                    onClick()
                    doubleKeyClickStates.remove(keyCode)
                }
            } else {
                // We already invoked onClick because we passed the timeout, so stop tracking this
                // as a double click
                doubleKeyClickStates.remove(keyCode)
            }
        }
        return handledByLongClick
    }

    override fun onClickKeyUpEvent(event: KeyEvent): Boolean {
        val keyCode = event.key.keyCode
        var longClickInvoked = false
        if (longKeyPressJobs[keyCode] != null) {
            longKeyPressJobs[keyCode]?.let {
                if (it.isActive) {
                    it.cancel()
                } else {
                    // If we already passed the timeout, we invoked long click already, and so
                    // we shouldn't invoke onClick in this case
                    longClickInvoked = true
                }
            }
            longKeyPressJobs.remove(keyCode)
        }
        if (onDoubleClick != null) {
            when {
                // First click
                doubleKeyClickStates[keyCode] == null -> {
                    // We only track the second click if the first click was not a long click
                    if (!longClickInvoked) {
                        doubleKeyClickStates[keyCode] =
                            DoubleKeyClickState(
                                coroutineScope.launch {
                                    val configuration = currentValueOf(LocalViewConfiguration)
                                    val minTime = configuration.doubleTapMinTimeMillis
                                    val timeout = configuration.doubleTapTimeoutMillis
                                    delay(minTime)
                                    doubleKeyClickStates[keyCode]?.doubleTapMinTimeMillisElapsed =
                                        true
                                    // Delay the remainder until we are at timeout
                                    delay(timeout - minTime)
                                    // If there was no second key press after the timeout, invoke
                                    // onClick as normal
                                    onClick()
                                }
                            )
                    }
                }
                // Second click
                else -> {
                    // Invoke onDoubleClick if the second click was not a long click
                    if (!longClickInvoked) {
                        onDoubleClick?.invoke()
                    }
                    doubleKeyClickStates.remove(keyCode)
                }
            }
        } else {
            if (!longClickInvoked) {
                onClick()
            }
        }
        return true
    }

    override fun onCancelKeyInput() {
        resetKeyPressState()
    }

    override fun onReset() {
        super.onReset()
        resetKeyPressState()
    }

    private fun resetKeyPressState() {
        longKeyPressJobs.apply {
            forEachValue { it.cancel() }
            clear()
        }
        doubleKeyClickStates.apply {
            forEachValue { it.job.cancel() }
            clear()
        }
    }
}

internal abstract class AbstractClickableNode(
    private var interactionSource: MutableInteractionSource?,
    private var indicationNodeFactory: IndicationNodeFactory?,
    private var useLocalIndication: Boolean,
    enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    onClick: () -> Unit,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    KeyInputModifierNode,
    SemanticsModifierNode,
    TraversableNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    IndirectPointerInputModifierNode,
    GestureConnection {
    protected var enabled = enabled
        private set

    protected var onClick = onClick
        private set

    final override val shouldAutoInvalidate: Boolean = false

    private val focusableNode: FocusableNode =
        FocusableNode(
            interactionSource,
            focusability = Focusability.SystemDefined,
            onFocusChange = ::onFocusChange,
        )

    private var localIndicationNodeFactory: IndicationNodeFactory? = null

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null
    private var gestureNode: DelegatableNode? = null
    private var indicationNode: DelegatableNode? = null

    private var pressInteraction: PressInteraction.Press? = null
    private var hoverInteraction: HoverInteraction.Enter? = null
    private val currentKeyPressInteractions = mutableLongObjectMapOf<PressInteraction.Press>()
    private var centerOffset: Offset = Offset.Zero

    private var indirectPointerPressInteraction: PressInteraction.Press? = null
    private var indirectPointerEventPressPosition: Offset? = null

    // Track separately from interactionSource, as we will create our own internal
    // InteractionSource if needed
    private var userProvidedInteractionSource: MutableInteractionSource? = interactionSource

    private var lazilyCreateIndication = shouldLazilyCreateIndication()

    private fun shouldLazilyCreateIndication() = userProvidedInteractionSource == null

    /**
     * Handles subclass-specific click related pointer input logic. Hover is already handled
     * elsewhere, so this should only handle clicks.
     *
     * TODO(b/477836055) Migrate to non-suspending API.
     */
    open fun createPointerInputNodeIfNeeded(): SuspendingPointerInputModifierNode? = null

    open fun SemanticsPropertyReceiver.applyAdditionalSemantics() {}

    protected fun updateCommon(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
    ) {
        var isIndicationNodeDirty = false
        // Compare against userProvidedInteractionSource, as we will create a new InteractionSource
        // lazily if the userProvidedInteractionSource is null, and assign it to interactionSource
        if (userProvidedInteractionSource != interactionSource) {
            disposeInteractions()
            userProvidedInteractionSource = interactionSource
            this.interactionSource = interactionSource
            isIndicationNodeDirty = true
        }
        if (this.indicationNodeFactory != indicationNodeFactory) {
            this.indicationNodeFactory = indicationNodeFactory
            isIndicationNodeDirty = true
        }
        if (this.useLocalIndication != useLocalIndication) {
            this.useLocalIndication = useLocalIndication
            if (useLocalIndication) {
                // Need to update localIndicationNodeFactory, and start observing changes
                onObservedReadsChanged()
            }
            isIndicationNodeDirty = true
        }
        if (this.enabled != enabled) {
            if (enabled) {
                delegate(focusableNode)
            } else {
                // TODO: Should we remove indicationNode? Previously we always emitted indication
                undelegate(focusableNode)
                disposeInteractions()
            }
            invalidateSemantics()
            this.enabled = enabled
        }
        if (this.onClickLabel != onClickLabel) {
            this.onClickLabel = onClickLabel
            invalidateSemantics()
        }
        if (this.role != role) {
            this.role = role
            invalidateSemantics()
        }
        this.onClick = onClick
        if (lazilyCreateIndication != shouldLazilyCreateIndication()) {
            lazilyCreateIndication = shouldLazilyCreateIndication()
            // If we are no longer lazily creating the node, and we haven't created the node yet,
            // create it
            if (!lazilyCreateIndication && indicationNode == null) isIndicationNodeDirty = true
        }
        // Create / recreate indication node
        if (isIndicationNodeDirty) {
            recreateIndicationIfNeeded()
        }
        focusableNode.update(this.interactionSource)
    }

    protected fun getExtendedTouchPadding(size: IntSize): Size {
        // copied from SuspendingPointerInputModifierNodeImpl.extendedTouchPadding:
        // TODO expose this as a new public api available outside of suspending apis b/422396609
        val minimumTouchTargetSizeDp = currentValueOf(LocalViewConfiguration).minimumTouchTargetSize
        val minimumTouchTargetSize = with(requireDensity()) { minimumTouchTargetSizeDp.toSize() }
        val horizontal = max(0f, minimumTouchTargetSize.width - size.width) / 2f
        val vertical = max(0f, minimumTouchTargetSize.height - size.height) / 2f
        return Size(horizontal, vertical)
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        initializeIndicationAndInteractionSourceIfNeeded()
        if (enabled) {
            initializeGestureCoordination()
        }
    }

    final override fun onAttach() {
        onObservedReadsChanged()
        if (!lazilyCreateIndication) {
            initializeIndicationAndInteractionSourceIfNeeded()
        }
        if (enabled) {
            delegate(focusableNode)
        }
    }

    override fun onObservedReadsChanged() {
        if (useLocalIndication) {
            observeReads {
                val indication = currentValueOf(LocalIndication)
                requirePrecondition(indication is IndicationNodeFactory) {
                    unsupportedIndicationExceptionMessage(indication)
                }
                val previousFactory = localIndicationNodeFactory
                localIndicationNodeFactory = indication
                // If we are changing from a non-null factory to a different factory, recreate
                // indication if needed
                if (previousFactory != null && localIndicationNodeFactory != previousFactory) {
                    recreateIndicationIfNeeded()
                }
            }
        }
    }

    final override fun onDetach() {
        disposeInteractions()
        // If we lazily created an interaction source, reset it in case we are reused / moved. Note
        // that we need to do it here instead of onReset() - since onReset won't be called in the
        // movableContent case but we still want to dispose for that case
        if (userProvidedInteractionSource == null) {
            interactionSource = null
        }
        // Remove indication in case we are reused / moved - we will create a new node when needed
        indicationNode?.let { undelegate(it) }
        indicationNode = null

        gestureNode?.let { undelegate(it) }
        gestureNode = null
    }

    protected fun disposeInteractions() {
        interactionSource?.let { interactionSource ->
            pressInteraction?.let { oldValue ->
                val interaction = PressInteraction.Cancel(oldValue)
                interactionSource.tryEmit(interaction)
            }
            indirectPointerPressInteraction?.let { oldValue ->
                val interaction = PressInteraction.Cancel(oldValue)
                interactionSource.tryEmit(interaction)
            }
            hoverInteraction?.let { oldValue ->
                val interaction = HoverInteraction.Exit(oldValue)
                interactionSource.tryEmit(interaction)
            }
            currentKeyPressInteractions.forEachValue {
                interactionSource.tryEmit(PressInteraction.Cancel(it))
            }
        }
        pressInteraction = null
        indirectPointerPressInteraction = null
        indirectPointerEventPressPosition = null
        hoverInteraction = null
        currentKeyPressInteractions.clear()
    }

    private fun onFocusChange(isFocused: Boolean) {
        if (isFocused) {
            initializeIndicationAndInteractionSourceIfNeeded()
        } else {
            // If we are no longer focused while we are tracking existing key presses, we need to
            // clear them and cancel the presses.
            if (interactionSource != null) {
                currentKeyPressInteractions.forEachValue {
                    coroutineScope.launch { interactionSource?.emit(PressInteraction.Cancel(it)) }
                }
                indirectPointerPressInteraction?.let {
                    coroutineScope.launch { interactionSource?.emit(PressInteraction.Cancel(it)) }
                }
            }
            currentKeyPressInteractions.clear()
            indirectPointerPressInteraction = null
            onCancelKeyInput()
        }
    }

    private fun recreateIndicationIfNeeded() {
        // If we already created a node lazily, or we are not lazily creating the node, create
        if (indicationNode != null || !lazilyCreateIndication) {
            indicationNode?.let { undelegate(it) }
            indicationNode = null
            initializeIndicationAndInteractionSourceIfNeeded()
        }
    }

    private fun initializeIndicationAndInteractionSourceIfNeeded() {
        // We have already created the node, no need to do any work
        if (indicationNode != null) return
        val indicationFactory =
            if (useLocalIndication) localIndicationNodeFactory else indicationNodeFactory
        indicationFactory?.let { factory ->
            if (interactionSource == null) {
                interactionSource = MutableInteractionSource()
            }
            focusableNode.update(interactionSource)
            val node = factory.create(interactionSource!!)
            delegate(node)
            indicationNode = node
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun initializeGestureCoordination() {
        if (!isDelayPressesUsingGestureConsumptionEnabled) return
        if (gestureNode == null) {
            gestureNode = delegate(gestureNode(this))
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        centerOffset = bounds.center.toOffset()
        initializeIndicationAndInteractionSourceIfNeeded()
        if (enabled) {
            initializeGestureCoordination()
            if (pass == PointerEventPass.Main) {
                when (pointerEvent.type) {
                    PointerEventType.Enter -> coroutineScope.launch { emitHoverEnter() }
                    PointerEventType.Exit -> coroutineScope.launch { emitHoverExit() }
                }
            }
        }
        if (pointerInputNode == null) {
            val node = createPointerInputNodeIfNeeded()
            if (node != null) {
                pointerInputNode = delegate(node)
            }
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        // Press cancellation is handled as part of detecting presses
        interactionSource?.let { interactionSource ->
            hoverInteraction?.let { oldValue ->
                val interaction = HoverInteraction.Exit(oldValue)
                interactionSource.tryEmit(interaction)
            }
        }
        hoverInteraction = null
        pointerInputNode?.onCancelPointerInput()
    }

    final override fun onKeyEvent(event: KeyEvent): Boolean {
        // Key events usually require focus, but if a focused child does not handle the KeyEvent,
        // the event can bubble up without this clickable ever being focused, and hence without
        // this being initialized through the focus path
        initializeIndicationAndInteractionSourceIfNeeded()
        val keyCode = event.key.keyCode
        return when {
            enabled && event.isPress -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                var wasInteractionHandled = false
                if (!currentKeyPressInteractions.containsKey(keyCode)) {
                    val press = PressInteraction.Press(centerOffset)
                    currentKeyPressInteractions[keyCode] = press
                    // Even if the interactionSource is null, we still want to intercept the presses
                    // so we always track them above, and return true
                    if (interactionSource != null) {
                        coroutineScope.launch { interactionSource?.emit(press) }
                    }
                    wasInteractionHandled = true
                }
                onClickKeyDownEvent(event) || wasInteractionHandled
            }

            enabled && event.isClick -> {
                val press = currentKeyPressInteractions.remove(keyCode)
                if (press != null) {
                    if (interactionSource != null) {
                        coroutineScope.launch {
                            interactionSource?.emit(PressInteraction.Release(press))
                        }
                    }
                    // Don't invoke onClick if we were not pressed - this could happen if we became
                    // focused after the down event, or if the node was reused after the down event.
                    onClickKeyUpEvent(event)
                }
                // Only consume if we were previously pressed for this key event
                press != null
            }

            else -> false
        }
    }

    protected abstract fun onClickKeyDownEvent(event: KeyEvent): Boolean

    protected abstract fun onClickKeyUpEvent(event: KeyEvent): Boolean

    /**
     * Called when focus is lost, to allow cleaning up and resetting the state for ongoing key
     * presses
     */
    protected open fun onCancelKeyInput() {}

    final override fun onPreKeyEvent(event: KeyEvent) = false

    final override val shouldMergeDescendantSemantics: Boolean
        get() = true

    final override fun SemanticsPropertyReceiver.applySemantics() {
        if (this@AbstractClickableNode.role != null) {
            role = this@AbstractClickableNode.role!!
        }
        onClick(
            action = {
                onClick()
                true
            },
            label = onClickLabel,
        )
        if (enabled) {
            with(focusableNode) { applySemantics() }
        } else {
            disabled()
        }
        applyAdditionalSemantics()
    }

    protected fun resetPointerInputHandler() = pointerInputNode?.resetPointerInputHandler()

    private var delayJob: Job? = null

    /** Handles emitting a [PressInteraction.Press]. */
    protected fun handlePressInteractionStart(event: IndirectPointerInputChange) {
        interactionSource?.let { interactionSource ->
            val press = PressInteraction.Press(event.position)
            if (delayPressInteraction(event)) {
                delayJob =
                    coroutineScope.launch {
                        delay(TapIndicationDelay)
                        interactionSource.emit(press)
                        indirectPointerPressInteraction = press
                    }
            } else {
                indirectPointerPressInteraction = press
                coroutineScope.launch { interactionSource.emit(press) }
            }
        }
    }

    protected fun handlePressInteractionStart(event: PointerInputChange) {
        interactionSource?.let { interactionSource ->
            val press = PressInteraction.Press(event.position)
            if (delayPressInteraction(event)) {
                delayJob =
                    coroutineScope.launch {
                        delay(TapIndicationDelay)
                        interactionSource.emit(press)
                        pressInteraction = press
                    }
            } else {
                pressInteraction = press
                coroutineScope.launch { interactionSource.emit(press) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    protected fun handlePressInteractionStart(offset: Offset, indirectPointer: Boolean) {
        interactionSource?.let { interactionSource ->
            val press = PressInteraction.Press(offset)
            val shouldDelayPress =
                if (isDelayPressesUsingGestureConsumptionEnabled) {
                    delayPressInteraction(null)
                } else {
                    delayPressInteraction()
                }
            if (shouldDelayPress) {
                delayJob =
                    coroutineScope.launch {
                        delay(TapIndicationDelay)
                        interactionSource.emit(press)
                        if (indirectPointer) {
                            indirectPointerPressInteraction = press
                        } else {
                            pressInteraction = press
                        }
                    }
            } else {
                if (indirectPointer) {
                    indirectPointerPressInteraction = press
                } else {
                    pressInteraction = press
                }
                coroutineScope.launch { interactionSource.emit(press) }
            }
        }
    }

    /**
     * Handles emitting a [PressInteraction.Release].
     *
     * @param offset offset of the press
     * @param indirectPointer whether the source of this press was indirect pointer. False for
     *   pointer input.
     */
    protected fun handlePressInteractionRelease(offset: Offset, indirectPointer: Boolean) {
        interactionSource?.let { interactionSource ->
            // To resolve b/414319919 it is important that we capture a reference to `delayJob`
            // outside the coroutine block - when the CPU is busy we can end up handling
            // press-release-press before the coroutine starts to execute, which means we can launch
            // two jobs and mutate delayJob twice. At the time this function is called, delayJob
            // points to the correct corresponding press event, so we just reference this instance
            // to make sure that there is no issue if coroutines are executed after the next set of
            // gestures have been processed.
            val job = delayJob
            if (job?.isActive == true) {
                // Immediately cancel the job to avoid a race condition from coroutine launching -
                // if we wait until inside the launch to cancel it could be executed after the job
                // is no longer active. An alternative approach would be to launch with
                // start = CoroutineStart.UNDISPATCHED, but it is more reasonable to cancel
                // outside the coroutine in any case.
                job.cancel()
                coroutineScope.launch {
                    // Wait for cancelling the job to finish if needed
                    job.join()
                    // The press released successfully, before the timeout duration - emit the press
                    // interaction instantly.
                    val press = PressInteraction.Press(offset)
                    val release = PressInteraction.Release(press)
                    interactionSource.emit(press)
                    interactionSource.emit(release)
                }
            } else {
                val interaction =
                    if (indirectPointer) indirectPointerPressInteraction else pressInteraction
                interaction?.let {
                    coroutineScope.launch {
                        // Important that we capture `interaction` outside the `launch`, rather than
                        // referring to it in here - the underlying fields are mutable and could
                        // change by the time this coroutine is executed
                        val endInteraction = PressInteraction.Release(it)
                        interactionSource.emit(endInteraction)
                    }
                }
            }
            if (indirectPointer) {
                indirectPointerPressInteraction = null
            } else {
                pressInteraction = null
            }
        }
    }

    /**
     * Handles emitting a [PressInteraction.Cancel].
     *
     * @param indirectPointer whether the source of this press was indirect pointer. False for
     *   pointer input.
     */
    protected fun handlePressInteractionCancel(indirectPointer: Boolean) {
        interactionSource?.let { interactionSource ->
            if (delayJob?.isActive == true) {
                // We didn't finish sending the press, and we are cancelled, so we don't emit
                // any interaction.
                delayJob?.cancel()
            } else {
                val interaction =
                    if (indirectPointer) indirectPointerPressInteraction else pressInteraction
                interaction?.let {
                    val endInteraction = PressInteraction.Cancel(it)
                    // If this is being called from inside onDetach(), we are still attached, but
                    // the scope will be cancelled soon after - so the launch {} might not even
                    // start before it is cancelled. We don't want to use
                    // CoroutineStart.UNDISPATCHED, or always call tryEmit() as this will break
                    // other timing / cause some events to be missed for other cases. Instead just
                    // make sure we call tryEmit if we cancel the scope, before we finish emitting.
                    val handler =
                        coroutineScope.coroutineContext[Job]?.invokeOnCompletion {
                            interactionSource.tryEmit(endInteraction)
                        }
                    coroutineScope.launch {
                        interactionSource.emit(endInteraction)
                        handler?.dispose()
                    }
                }
            }
            if (indirectPointer) {
                indirectPointerPressInteraction = null
            } else {
                pressInteraction = null
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    protected suspend fun PressGestureScope.handlePressInteraction(offset: Offset) {
        interactionSource?.let { interactionSource ->
            coroutineScope {
                val delayJob = launch {
                    val shouldDelayPress =
                        if (isDelayPressesUsingGestureConsumptionEnabled) {
                            delayPressInteraction(null)
                        } else {
                            delayPressInteraction()
                        }
                    if (shouldDelayPress) {
                        delay(TapIndicationDelay)
                    }
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    pressInteraction = press
                }
                val success = tryAwaitRelease()
                if (delayJob.isActive) {
                    delayJob.cancelAndJoin()
                    // The press released successfully, before the timeout duration - emit the press
                    // interaction instantly. No else branch - if the press was cancelled before the
                    // timeout, we don't want to emit a press interaction.
                    if (success) {
                        val press = PressInteraction.Press(offset)
                        val release = PressInteraction.Release(press)
                        interactionSource.emit(press)
                        interactionSource.emit(release)
                    }
                } else {
                    pressInteraction?.let { pressInteraction ->
                        val endInteraction =
                            if (success) {
                                PressInteraction.Release(pressInteraction)
                            } else {
                                PressInteraction.Cancel(pressInteraction)
                            }
                        interactionSource.emit(endInteraction)
                    }
                }
                pressInteraction = null
            }
        }
    }

    private fun delayPressInteraction(): Boolean =
        hasScrollableContainer() || isComposeRootInScrollableContainer()

    private fun delayPressInteraction(event: PointerInputChange?): Boolean {
        val hasInterestedParent =
            if (event == null) {
                parentGestureConnection != null
            } else {
                hasInterestedParent(event)
            }
        return hasInterestedParent || isComposeRootInScrollableContainer()
    }

    private fun delayPressInteraction(event: IndirectPointerInputChange): Boolean =
        hasInterestedParent(event) || isComposeRootInScrollableContainer()

    private fun emitHoverEnter() {
        if (hoverInteraction == null) {
            val interaction = HoverInteraction.Enter()
            interactionSource?.let { interactionSource ->
                coroutineScope.launch { interactionSource.emit(interaction) }
            }
            hoverInteraction = interaction
        }
    }

    private fun emitHoverExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource?.let { interactionSource ->
                coroutineScope.launch { interactionSource.emit(interaction) }
            }
            hoverInteraction = null
        }
    }

    override val traverseKey: Any = TraverseKey

    companion object TraverseKey
}

internal fun DelegatingNode.hasInterestedParent(event: IndirectPointerInputChange): Boolean {
    var hasInterestedParent = false
    traverseAncestorGestureConnections { coordinator ->
        val isCoordinatorInterested = coordinator.isInterested(event)
        hasInterestedParent = hasInterestedParent || isCoordinatorInterested
        !hasInterestedParent
    }
    return hasInterestedParent
}

internal fun DelegatingNode.hasInterestedParent(event: PointerInputChange): Boolean {
    var hasInterestedParent = false
    traverseAncestorGestureConnections { coordinator ->
        val isCoordinatorInterested = coordinator.isInterested(event)
        hasInterestedParent = hasInterestedParent || isCoordinatorInterested
        !hasInterestedParent
    }
    return hasInterestedParent
}

internal fun TraversableNode.hasScrollableContainer(): Boolean {
    var hasScrollable = false
    traverseAncestors(ScrollableContainerNode.TraverseKey) { node ->
        hasScrollable = hasScrollable || (node as ScrollableContainerNode).enabled
        !hasScrollable
    }
    return hasScrollable
}

private fun unsupportedIndicationExceptionMessage(indication: Indication): String {
    return "clickable only supports IndicationNodeFactory instances provided to LocalIndication, " +
        "but Indication was provided instead. Either migrate the Indication implementation to " +
        "implement IndicationNodeFactory, or use the other clickable overload that takes an " +
        "Indication parameter, and explicitly pass LocalIndication.current there. The Indication" +
        " instance provided here was: $indication"
}

private fun IndirectPointerInputChange.changedToUp() = !isConsumed && previousPressed && !pressed

private fun IndirectPointerInputChange.changedToUpIgnoreConsumed() = previousPressed && !pressed
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyList.kt
```kotlin
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.lazy.layout.StickyItemsPlacement
import androidx.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.scrollableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.trace
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyList(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier,
    /** State controlling the scroll position */
    state: LazyListState,
    /** The inner padding to be added for the whole content(not for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior,
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** The overscroll effect to render and dispatch events to */
    overscrollEffect: OverscrollEffect?,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int = defaultLazyListBeyondBoundsItemCount(),
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The content of the list */
    content: LazyListScope.() -> Unit,
) {
    val itemProviderLambda = rememberLazyListItemProviderLambda(state, content)

    val semanticState = rememberLazyListSemanticState(state, isVertical)
    val coroutineScope = rememberCoroutineScope()
    val graphicsContext = LocalGraphicsContext.current
    val stickyHeadersEnabled = !LocalScrollCaptureInProgress.current

    val measurePolicy =
        rememberLazyListMeasurePolicy(
            itemProviderLambda,
            state,
            contentPadding,
            reverseLayout,
            isVertical,
            beyondBoundsItemCount,
            horizontalAlignment,
            verticalAlignment,
            horizontalArrangement,
            verticalArrangement,
            coroutineScope,
            graphicsContext,
            if (stickyHeadersEnabled) StickyItemsPlacement.StickToTopPlacement else null,
        )

    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal

    val beyondBoundsModifier =
        if (userScrollEnabled) {
            Modifier.lazyLayoutBeyondBoundsModifier(
                state =
                    rememberLazyListBeyondBoundsState(
                        state = state,
                        beyondBoundsItemCount = beyondBoundsItemCount,
                    ),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                orientation = orientation,
            )
        } else {
            Modifier
        }

    LazyLayout(
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .then(state.awaitLayoutModifier)
                .lazyLayoutSemantics(
                    itemProviderLambda = itemProviderLambda,
                    state = semanticState,
                    orientation = orientation,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .then(beyondBoundsModifier)
                .then(state.itemAnimator.modifier)
                .scrollableArea(
                    state = state,
                    orientation = orientation,
                    enabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                    flingBehavior = flingBehavior,
                    interactionSource = state.internalInteractionSource,
                    overscrollEffect = overscrollEffect,
                ),
        prefetchState = state.prefetchState,
        measurePolicy = measurePolicy,
        itemProvider = itemProviderLambda,
    )
}

@ExperimentalFoundationApi
@Composable
private fun rememberLazyListMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> LazyListItemProvider,
    /** The state of the list. */
    state: LazyListState,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int,
    /** The alignment to align items horizontally */
    horizontalAlignment: Alignment.Horizontal?,
    /** The alignment to align items vertically */
    verticalAlignment: Alignment.Vertical?,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
    /** Scope for animations */
    coroutineScope: CoroutineScope,
    /** Used for creating graphics layers */
    graphicsContext: GraphicsContext,
    /** Scroll behavior for sticky items */
    stickyItemsPlacement: StickyItemsPlacement?,
) =
    remember(
        state,
        contentPadding,
        reverseLayout,
        isVertical,
        beyondBoundsItemCount,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement,
        graphicsContext,
        stickyItemsPlacement,
    ) {
        LazyLayoutMeasurePolicy { containerConstraints ->
            state.measurementScopeInvalidator.attachToScope()
            // Tracks if the lookahead pass has occurred
            val hasLookaheadOccurred = state.hasLookaheadOccurred || isLookingAhead
            checkScrollableContainerConstraints(
                containerConstraints,
                if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            )

            // resolve content paddings
            val startPadding =
                if (isVertical) {
                    contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateStartPadding(layoutDirection).roundToPx()
                }

            val endPadding =
                if (isVertical) {
                    contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateEndPadding(layoutDirection).roundToPx()
                }
            val topPadding = contentPadding.calculateTopPadding().roundToPx()
            val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
            val totalVerticalPadding = topPadding + bottomPadding
            val totalHorizontalPadding = startPadding + endPadding
            val totalMainAxisPadding =
                if (isVertical) totalVerticalPadding else totalHorizontalPadding
            val beforeContentPadding =
                when {
                    isVertical && !reverseLayout -> topPadding
                    isVertical && reverseLayout -> bottomPadding
                    !isVertical && !reverseLayout -> startPadding
                    else -> endPadding // !isVertical && reverseLayout
                }
            val afterContentPadding = totalMainAxisPadding - beforeContentPadding
            val contentConstraints =
                containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

            val itemProvider = itemProviderLambda()
            // this will update the scope used by the item composables
            itemProvider.itemScope.setMaxSize(
                width = contentConstraints.maxWidth,
                height = contentConstraints.maxHeight,
            )

            val spaceBetweenItemsDp =
                if (isVertical) {
                    requirePreconditionNotNull(verticalArrangement) {
                            "null verticalArrangement when isVertical == true"
                        }
                        .spacing
                } else {
                    requirePreconditionNotNull(horizontalArrangement) {
                            "null horizontalAlignment when isVertical == false"
                        }
                        .spacing
                }
            val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

            val itemsCount = itemProvider.itemCount

            // can be negative if the content padding is larger than the max size from constraints
            val mainAxisAvailableSize =
                if (isVertical) {
                    containerConstraints.maxHeight - totalVerticalPadding
                } else {
                    containerConstraints.maxWidth - totalHorizontalPadding
                }
            val visualItemOffset =
                if (!reverseLayout || mainAxisAvailableSize > 0) {
                    IntOffset(startPadding, topPadding)
                } else {
                    // When layout is reversed and paddings together take >100% of the available
                    // space,
                    // layout size is coerced to 0 when positioning. To take that space into
                    // account,
                    // we offset start padding by negative space between paddings.
                    IntOffset(
                        if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                        if (isVertical) topPadding + mainAxisAvailableSize else topPadding,
                    )
                }

            val measuredItemProvider =
                object :
                    LazyListMeasuredItemProvider(
                        contentConstraints,
                        isVertical,
                        itemProvider,
                        this,
                    ) {
                    override fun createItem(
                        index: Int,
                        key: Any,
                        contentType: Any?,
                        placeables: List<Placeable>,
                        constraints: Constraints,
                    ): LazyListMeasuredItem {
                        // we add spaceBetweenItems as an extra spacing for all items apart from the
                        // last one so
                        // the lazy list measuring logic will take it into account.
                        val spacing = if (index == itemsCount - 1) 0 else spaceBetweenItems
                        return LazyListMeasuredItem(
                            index = index,
                            placeables = placeables,
                            isVertical = isVertical,
                            horizontalAlignment = horizontalAlignment,
                            verticalAlignment = verticalAlignment,
                            layoutDirection = layoutDirection,
                            reverseLayout = reverseLayout,
                            beforeContentPadding = beforeContentPadding,
                            afterContentPadding = afterContentPadding,
                            spacing = spacing,
                            visualOffset = visualItemOffset,
                            key = key,
                            contentType = contentType,
                            animator = state.itemAnimator,
                            constraints = constraints,
                        )
                    }
                }

            val firstVisibleItemIndex: Int
            val firstVisibleScrollOffset: Int
            Snapshot.withoutReadObservation {
                firstVisibleItemIndex =
                    state.updateScrollPositionIfTheFirstItemWasMoved(
                        itemProvider,
                        state.firstVisibleItemIndex,
                    )
                firstVisibleScrollOffset = state.firstVisibleItemScrollOffset
            }

            val pinnedItems =
                itemProvider.calculateLazyLayoutPinnedIndices(
                    pinnedItemList = state.pinnedItems,
                    beyondBoundsInfo = state.beyondBoundsInfo,
                )

            val scrollToBeConsumed =
                if (isLookingAhead || !hasLookaheadOccurred) {
                    state.scrollToBeConsumed
                } else {
                    state.scrollDeltaBetweenPasses
                }

            // todo: wrap with snapshot when b/341782245 is resolved
            val measureResult =
                measureLazyList(
                    itemsCount = itemsCount,
                    measuredItemProvider = measuredItemProvider,
                    mainAxisAvailableSize = mainAxisAvailableSize,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    spaceBetweenItems = spaceBetweenItems,
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleScrollOffset,
                    scrollToBeConsumed = scrollToBeConsumed,
                    constraints = contentConstraints,
                    isVertical = isVertical,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement,
                    reverseLayout = reverseLayout,
                    density = this,
                    itemAnimator = state.itemAnimator,
                    beyondBoundsItemCount = beyondBoundsItemCount,
                    pinnedItems = pinnedItems,
                    hasLookaheadOccurred = hasLookaheadOccurred,
                    isLookingAhead = isLookingAhead,
                    coroutineScope = coroutineScope,
                    placementScopeInvalidator = state.placementScopeInvalidator,
                    graphicsContext = graphicsContext,
                    stickyItemsPlacement = stickyItemsPlacement,
                    layout = { width, height, placement ->
                        layout(
                            containerConstraints.constrainWidth(width + totalHorizontalPadding),
                            containerConstraints.constrainHeight(height + totalVerticalPadding),
                            emptyMap(),
                            placement,
                        )
                    },
                )

            state.applyMeasureResult(measureResult, isLookingAhead)
            // apply keep around after updating the strategy with measure result.
            (state.prefetchStrategy as? CacheWindowLogic)?.keepAroundItems(
                measureResult.visibleItemsInfo,
                measuredItemProvider,
            )
            measureResult
        }
    }

@OptIn(ExperimentalFoundationApi::class)
private fun CacheWindowLogic.keepAroundItems(
    visibleItemsList: List<LazyListMeasuredItem>,
    measuredItemProvider: LazyListMeasuredItemProvider,
) {
    trace("compose:lazy:cache_window:keepAroundItems") {
        // only run if window and new layout info is available
        if (hasValidBounds() && visibleItemsList.isNotEmpty()) {
            val firstVisibleItemIndex = visibleItemsList.first().index
            val lastVisibleItemIndex = visibleItemsList.last().index
            // we must send a message in case of changing directions for items
            // that were keep around and become prefetch forward
            for (item in prefetchWindowStartLine..<firstVisibleItemIndex) {
                measuredItemProvider.keepAround(item)
            }

            for (item in (lastVisibleItemIndex + 1)..prefetchWindowEndLine) {
                measuredItemProvider.keepAround(item)
            }
        }
    }
}

@Composable internal expect fun defaultLazyListBeyondBoundsItemCount(): Int
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyListState.kt
```kotlin
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

package androidx.compose.foundation.lazy

import androidx.annotation.IntRange as AndroidXIntRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.lazy.LazyListState.Companion.Saver
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollDeltaBetweenPasses
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.animateScrollToItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.traceValue
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyListState {
    return rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
}

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyListPrefetchStrategy] to use for prefetching content in this
 *   list
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    prefetchStrategy: LazyListPrefetchStrategy = remember { LazyListPrefetchStrategy() },
): LazyListState {
    return rememberSaveable(prefetchStrategy, saver = LazyListState.saver(prefetchStrategy)) {
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            prefetchStrategy,
        )
    }
}

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param cacheWindow specifies the size of the ahead and behind window to be used as per
 *   [LazyLayoutCacheWindow].
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyListState(
    cacheWindow: LazyLayoutCacheWindow,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyListState {
    return rememberSaveable(cacheWindow, saver = LazyListState.saver(cacheWindow)) {
        LazyListState(
            cacheWindow,
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberLazyListState].
 *
 * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 *   [LazyListState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyListPrefetchStrategy] to use for prefetching content in this
 *   list
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class LazyListState
@ExperimentalFoundationApi
constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    internal val prefetchStrategy: LazyListPrefetchStrategy = LazyListPrefetchStrategy(),
) : ScrollableState {

    /**
     * @param cacheWindow specifies the size of the ahead and behind window to be used as per
     *   [LazyLayoutCacheWindow].
     * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
     * @param firstVisibleItemScrollOffset the initial value for
     *   [LazyListState.firstVisibleItemScrollOffset]
     */
    @ExperimentalFoundationApi
    constructor(
        cacheWindow: LazyLayoutCacheWindow,
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
    ) : this(
        firstVisibleItemIndex,
        firstVisibleItemScrollOffset,
        LazyListCacheWindowStrategy(cacheWindow),
    )

    /**
     * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
     * @param firstVisibleItemScrollOffset the initial value for
     *   [LazyListState.firstVisibleItemScrollOffset]
     */
    constructor(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
    ) : this(firstVisibleItemIndex, firstVisibleItemScrollOffset, LazyListPrefetchStrategy())

    internal var hasLookaheadOccurred: Boolean = false
        private set

    internal var approachLayoutInfo: LazyListMeasureResult? = null
        private set

    // always execute requests in high priority
    private var executeRequestsInHighPriorityMode = false

    /** The holder class for the current scroll position. */
    private val scrollPosition =
        LazyListScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    /**
     * The index of the first item that is visible within the scrollable viewport area not including
     * items in the content padding region. For the first visible item that includes items in the
     * content padding please use [LazyListLayoutInfo.visibleItemsInfo].
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change causing potential performance issues.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingListScrollPositionForSideEffectSample
     *
     * If you need to use it in the composition then consider wrapping the calculation into a
     * derived state in order to only have recompositions when the derived value changes:
     *
     * @sample androidx.compose.foundation.samples.UsingListScrollPositionInCompositionSample
     */
    val firstVisibleItemIndex: Int
        @FrequentlyChangingValue get() = scrollPosition.index

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the amount
     * that the item is offset backwards.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     *
     * @see firstVisibleItemIndex for samples with the recommended usage patterns.
     */
    val firstVisibleItemScrollOffset: Int
        @FrequentlyChangingValue get() = scrollPosition.scrollOffset

    /** Backing state for [layoutInfo] */
    private val layoutInfoState = mutableStateOf(EmptyLazyListMeasureResult, neverEqualPolicy())

    /**
     * The object of [LazyListLayoutInfo] calculated during the last layout pass. For example, you
     * can use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure. If you
     * use it in the composable function it will be recomposed on every change causing potential
     * performance issues including infinity recomposition loop. Therefore, avoid using it in the
     * composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingListLayoutInfoForSideEffectSample
     */
    val layoutInfo: LazyListLayoutInfo
        @FrequentlyChangingValue get() = layoutInfoState.value

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * The amount of scroll to be consumed in the next layout pass. Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    internal var scrollToBeConsumed = 0f
        private set

    internal val density: Density
        get() = layoutInfoState.value.density

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once we
     * reached the end of the list.
     */
    private val scrollableState = ScrollableState { -onScroll(-it) }

    /** Only used for testing to confirm that we're not making too many measure passes */
    /*@VisibleForTesting*/
    internal var numMeasurePasses: Int = 0
        private set

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@LazyListState.remeasurement = remeasurement
            }
        }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    internal val itemAnimator = LazyLayoutItemAnimator<LazyListMeasuredItem>()

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    @Suppress("DEPRECATION") // b/420551535
    internal val prefetchState =
        LazyLayoutPrefetchState(prefetchStrategy.prefetchScheduler) {
            with(prefetchStrategy) {
                onNestedPrefetch(Snapshot.withoutReadObservation { firstVisibleItemIndex })
            }
        }

    private val prefetchScope: LazyListPrefetchScope =
        object : LazyListPrefetchScope {
            override fun schedulePrefetch(
                index: Int,
                onPrefetchFinished: (LazyListPrefetchResultScope.() -> Unit)?,
            ): LazyLayoutPrefetchState.PrefetchHandle {
                // Without read observation since this can be triggered from scroll - this will then
                // cause us to recompose when the measure result changes. We don't care since the
                // prefetch is best effort.
                val lastMeasureResult = Snapshot.withoutReadObservation { layoutInfoState.value }
                return prefetchState.schedulePrecompositionAndPremeasure(
                    index,
                    lastMeasureResult.childConstraints,
                    executeRequestsInHighPriorityMode,
                ) {
                    if (onPrefetchFinished != null) {
                        var mainAxisItemSize = 0
                        repeat(placeablesCount) {
                            mainAxisItemSize +=
                                if (lastMeasureResult.orientation == Orientation.Vertical) {
                                    getSize(it).height
                                } else {
                                    getSize(it).width
                                }
                        }

                        onPrefetchFinished.invoke(
                            LazyListPrefetchResultScopeImpl(index, mainAxisItemSize)
                        )
                    }
                }
            }
        }

    private val _scrollIndicatorState =
        object : ScrollIndicatorState {
            override val scrollOffset: Int
                get() = calculateScrollOffset()

            override val contentSize: Int
                get() = layoutInfo.calculateContentSize()

            override val viewportSize: Int
                get() = layoutInfo.singleAxisViewportSize
        }

    private fun calculateScrollOffset(): Int {
        return (layoutInfo.visibleItemsAverageSize() * firstVisibleItemIndex) +
            firstVisibleItemScrollOffset
    }

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    /**
     * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun scrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll { snapToItemIndexInternal(index, scrollOffset, forceRemeasure = true) }
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the item at [index] to be at the start of the viewport during the next remeasure,
     * offset by [scrollOffset], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the first visible item key (when a data set change will also be applied during the
     * next remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    fun requestScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            layoutInfoState.value.coroutineScope.launch { scroll {} }
        }

        snapToItemIndexInternal(index, scrollOffset, forceRemeasure = false)
    }

    /**
     * Snaps to the requested scroll position. Synchronously executes remeasure if [forceRemeasure]
     * is true, and schedules a remeasure if false.
     */
    internal fun snapToItemIndexInternal(index: Int, scrollOffset: Int, forceRemeasure: Boolean) {
        val positionChanged =
            scrollPosition.index != index || scrollPosition.scrollOffset != scrollOffset
        // sometimes this method is called not to scroll, but to stay on the same index when
        // the data changes, as by default we maintain the scroll position by key, not index.
        // when this happens we don't need to reset the animations as from the user perspective
        // we didn't scroll anywhere and if there is an offset change for an item, this change
        // should be animated.
        // however, when the request is to really scroll to a different position, we have to
        // reset previously known item positions as we don't want offset changes to be animated.
        // this offset should be considered as a scroll, not the placement change.
        if (positionChanged) {
            itemAnimator.reset()
            // we changed positions, cancel existing requests and wait for the next scroll to
            // refill the window
            (prefetchStrategy as? CacheWindowLogic)?.resetStrategy()
        }
        scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)

        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        } else {
            measurementScopeInvalidator.invalidateScope()
        }
    }

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        if (layoutInfoState.value === EmptyLazyListMeasureResult) {
            awaitLayoutModifier.waitForFirstLayout()
        }
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    override val scrollIndicatorState: ScrollIndicatorState?
        get() = _scrollIndicatorState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
    //  fine-grained control over scrolling
    /*@VisibleForTesting*/
    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        checkPrecondition(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll"
        }
        executeRequestsInHighPriorityMode = true
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            val intDelta = scrollToBeConsumed.fastRoundToInt()

            var scrolledLayoutInfo =
                layoutInfoState.value.copyWithScrollDeltaWithoutRemeasure(
                    delta = intDelta,
                    updateAnimations = !hasLookaheadOccurred,
                )
            if (scrolledLayoutInfo != null && this.approachLayoutInfo != null) {
                // if we were able to scroll the lookahead layout info without remeasure, lets
                // try to do the same for approach layout info (sometimes they diverge).
                val scrolledApproachLayoutInfo =
                    approachLayoutInfo?.copyWithScrollDeltaWithoutRemeasure(
                        delta = intDelta,
                        updateAnimations = true,
                    )
                if (scrolledApproachLayoutInfo != null) {
                    // we can apply scroll delta for both phases without remeasure
                    approachLayoutInfo = scrolledApproachLayoutInfo
                } else {
                    // we can't apply scroll delta for approach, so we have to remeasure
                    scrolledLayoutInfo = null
                }
            }

            if (scrolledLayoutInfo != null) {
                applyMeasureResult(
                    result = scrolledLayoutInfo,
                    isLookingAhead = hasLookaheadOccurred,
                    visibleItemsStayedTheSame = true,
                )
                // we don't need to remeasure, so we only trigger re-placement:
                placementScopeInvalidator.invalidateScope()

                notifyPrefetchOnScroll(
                    preScrollToBeConsumed - scrollToBeConsumed,
                    scrolledLayoutInfo,
                )
            } else {
                remeasurement?.forceRemeasure()
                notifyPrefetchOnScroll(preScrollToBeConsumed - scrollToBeConsumed, this.layoutInfo)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    private fun notifyPrefetchOnScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        if (prefetchingEnabled) {
            with(prefetchStrategy) { prefetchScope.onScroll(delta, layoutInfo) }
        }
    }

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun animateScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll {
            LazyLayoutScrollScope(this@LazyListState, this)
                .animateScrollToItem(index, scrollOffset, NumberOfItemsToTeleport, density)
        }
    }

    /** Updates the state with the new calculated scroll position and consumed scroll. */
    internal fun applyMeasureResult(
        result: LazyListMeasureResult,
        isLookingAhead: Boolean,
        visibleItemsStayedTheSame: Boolean = false,
    ) {
        // update the prefetch state with the number of nested prefetch items this layout
        // should use.
        prefetchState.idealNestedPrefetchCount = result.visibleItemsInfo.size

        if (!isLookingAhead && hasLookaheadOccurred) {
            // If there was already a lookahead pass, record this result as approach result
            approachLayoutInfo = result
            Snapshot.withoutReadObservation {
                if (
                    _lazyLayoutScrollDeltaBetweenPasses.isActive &&
                        result.firstVisibleItem?.index == scrollPosition.index &&
                        result.firstVisibleItemScrollOffset == scrollPosition.scrollOffset
                ) {
                    _lazyLayoutScrollDeltaBetweenPasses.stop()
                }
            }
        } else {
            if (isLookingAhead) {
                hasLookaheadOccurred = true
            }

            canScrollBackward = result.canScrollBackward
            canScrollForward = result.canScrollForward
            scrollToBeConsumed -= result.consumedScroll
            layoutInfoState.value = result

            if (visibleItemsStayedTheSame) {
                scrollPosition.updateScrollOffset(result.firstVisibleItemScrollOffset)
            } else {
                traceVisibleItems(result) // trace when visible window changed
                scrollPosition.updateFromMeasureResult(result)
                if (prefetchingEnabled) {
                    with(prefetchStrategy) { prefetchScope.onVisibleItemsUpdated(result) }
                }
            }

            if (isLookingAhead) {
                _lazyLayoutScrollDeltaBetweenPasses.updateScrollDeltaForApproach(
                    result.scrollBackAmount,
                    result.density,
                    result.coroutineScope,
                )
            }
            numMeasurePasses++
        }
    }

    private fun traceVisibleItems(measureResult: LazyListMeasureResult) {
        val firstVisibleItem = measureResult.visibleItemsInfo.firstOrNull()
        val lastVisibleItem = measureResult.visibleItemsInfo.lastOrNull()
        traceValue("firstVisibleItem:index", firstVisibleItem?.index?.toLong() ?: -1L)
        traceValue("lastVisibleItem:index", lastVisibleItem?.index?.toLong() ?: -1L)
    }

    internal val scrollDeltaBetweenPasses: Float
        get() = _lazyLayoutScrollDeltaBetweenPasses.scrollDeltaBetweenPasses

    private val _lazyLayoutScrollDeltaBetweenPasses = LazyLayoutScrollDeltaBetweenPasses()

    /**
     * When the user provided custom keys for the items we can try to detect when there were items
     * added or removed before our current first visible item and keep this item as the first
     * visible one even given that its index has been changed. The scroll position will not be
     * updated if [requestScrollToItem] was called since the last time this method was called.
     */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: LazyListItemProvider,
        firstItemIndex: Int,
    ): Int = scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

    companion object {
        /** The default [Saver] implementation for [LazyListState]. */
        val Saver: Saver<LazyListState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyListState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                    )
                },
            )

        /**
         * A [Saver] implementation for [LazyListState] that handles setting a custom
         * [LazyListPrefetchStrategy].
         */
        internal fun saver(prefetchStrategy: LazyListPrefetchStrategy): Saver<LazyListState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyListState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                        prefetchStrategy,
                    )
                },
            )

        /**
         * A [Saver] implementation for [LazyListState] that handles setting a custom
         * [LazyLayoutCacheWindow].
         */
        internal fun saver(cacheWindow: LazyLayoutCacheWindow): Saver<LazyListState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyListState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                        cacheWindow = cacheWindow,
                    )
                },
            )
    }
}

private val EmptyLazyListMeasureResult =
    LazyListMeasureResult(
        firstVisibleItem = null,
        firstVisibleItemScrollOffset = 0,
        canScrollForward = false,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        scrollBackAmount = 0f,
        visibleItemsInfo = emptyList(),
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        totalItemsCount = 0,
        reverseLayout = false,
        orientation = Orientation.Vertical,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        remeasureNeeded = false,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        density = Density(1f),
        childConstraints = Constraints(),
    )

private const val NumberOfItemsToTeleport = 100
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyDsl.kt
```kotlin
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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Receiver scope which is used by [LazyColumn] and [LazyRow]. */
@LazyScopeMarker
@JvmDefaultWithCompatibility
interface LazyListScope {
    /**
     * Adds a single item.
     *
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the list is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the list will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one. This can be overridden by calling 'requestScrollToItem' on the
     *   'LazyListState'.
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the item
     */
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        error("The method is not implemented")
    }

    @Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit) {
        item(key, null, content)
    }

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key for
     *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
     *   Android. If null is passed the position in the list will represent the key. When you
     *   specify the key the scroll position will be maintained based on the key, which means if you
     *   add/remove items before the current visible item the item with the given key will be kept
     *   as the first visible one. This can be overridden by calling 'requestScrollToItem' on the
     *   'LazyListState'.
     * @param contentType a factory of the content types for the item. The item compositions of the
     *   same type could be reused more efficiently. Note that null is a valid type and items of
     *   such type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        error("The method is not implemented")
    }

    @Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        items(count, key, { null }, itemContent)
    }

    /**
     * Adds a sticky header item, which will remain pinned even when scrolling after it. The header
     * will remain pinned until the next header will take its place.
     *
     * @sample androidx.compose.foundation.samples.StickyHeaderListSample
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the list is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the list will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one. This can be overridden by calling 'requestScrollToItem' on the
     *   'LazyListState'.
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the header
     */
    @Deprecated(
        "Please use the overload with indexing capabilities.",
        level = DeprecationLevel.HIDDEN,
        replaceWith = ReplaceWith("stickyHeader(key, contentType, { _ -> content() })"),
    )
    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit,
    ) = stickyHeader(key, contentType) { _ -> content() }

    /**
     * Adds a sticky header item, which will remain pinned even when scrolling after it. The header
     * will remain pinned until the next header will take its place.
     *
     * @sample androidx.compose.foundation.samples.StickyHeaderListSample
     * @sample androidx.compose.foundation.samples.StickyHeaderHeaderIndexSample
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the list is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the list will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one. This can be overridden by calling 'requestScrollToItem' on the
     *   'LazyListState'.
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the header, the header index is provided, this is the item
     *   position within the total set of items in this lazy list (the global index).
     */
    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.(Int) -> Unit,
    ) {
        item(key, contentType) { content.invoke(this, 0) }
    }
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling 'requestScrollToItem' on the 'LazyListState'.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) },
    ) {
        itemContent(items[it])
    }

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
inline fun <T> LazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) = items(items, key, itemContent = itemContent)

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling 'requestScrollToItem' on the 'LazyListState'.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
inline fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) = itemsIndexed(items, key, itemContent = itemContent)

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling 'requestScrollToItem' on the 'LazyListState'.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) },
    ) {
        itemContent(items[it])
    }

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
inline fun <T> LazyListScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) = items(items, key, itemContent = itemContent)

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the list is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the list will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling 'requestScrollToItem' on the 'LazyListState'.
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyListScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        contentType = { index -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
inline fun <T> LazyListScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) = itemsIndexed(items, key, itemContent = itemContent)

/**
 * The horizontally scrolling list that only composes and lays out the currently visible items. The
 * [content] block defines a DSL which allows you to emit items of different types. For example you
 * can use [LazyListScope.item] to add a single item and [LazyListScope.items] to add a list of
 * items.
 *
 * @sample androidx.compose.foundation.samples.LazyRowSample
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first item or after the last one. If you want to add a spacing between each
 *   item use [horizontalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are laid
 *   out in the reverse order and [LazyListState.firstVisibleItemIndex] == 0 means that row is
 *   scrolled to the end. Note that [reverseLayout] does not change the behavior of
 *   [horizontalArrangement], e.g. with [Arrangement.Start] [123###] becomes [321###].
 * @param horizontalArrangement The horizontal arrangement of the layout's children. This allows to
 *   add a spacing between items and specify the arrangement of the items when we have not enough of
 *   them to fill the whole minimum size.
 * @param verticalAlignment the vertical alignment applied to the items
 * @param flingBehavior logic describing fling behavior.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content a block which describes the content. Inside this block you can use methods like
 *   [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: LazyListScope.() -> Unit,
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
        isVertical = false,
        flingBehavior = flingBehavior,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

/**
 * The vertically scrolling list that only composes and lays out the currently visible items. The
 * [content] block defines a DSL which allows you to emit items of different types. For example you
 * can use [LazyListScope.item] to add a single item and [LazyListScope.items] to add a list of
 * items.
 *
 * @sample androidx.compose.foundation.samples.LazyColumnSample
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the list's state.
 * @param contentPadding a padding around the whole content. This will add padding for the. content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first item or after the last one. If you want to add a spacing between each
 *   item use [verticalArrangement].
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are laid
 *   out in the reverse order and [LazyListState.firstVisibleItemIndex] == 0 means that column is
 *   scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 *   [verticalArrangement], e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321###
 *   (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows to add
 *   a spacing between items and specify the arrangement of the items when we have not enough of
 *   them to fill the whole minimum size.
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param flingBehavior logic describing fling behavior.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using the state even when it is disabled
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content a block which describes the content. Inside this block you can use methods like
 *   [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items.
 */
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: LazyListScope.() -> Unit,
) {
    LazyList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        flingBehavior = flingBehavior,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        isVertical = true,
        reverseLayout = reverseLayout,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = rememberOverscrollEffect(),
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = true,
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = rememberOverscrollEffect(),
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = true,
        content = content,
    )
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyListItemProvider.kt
```kotlin
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.collection.IntList
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

internal interface LazyListItemProvider : LazyLayoutItemProvider {
    val keyIndexMap: LazyLayoutKeyIndexMap
    /** The list of indexes of the sticky header items */
    val headerIndexes: IntList
    /** The scope used by the item content lambdas */
    val itemScope: LazyItemScopeImpl
}

@Composable
internal fun rememberLazyListItemProviderLambda(
    state: LazyListState,
    content: LazyListScope.() -> Unit,
): () -> LazyListItemProvider {
    val latestContent = rememberUpdatedState(content)
    return remember(state) {
        val scope = LazyItemScopeImpl()
        val intervalContentState =
            derivedStateOf(referentialEqualityPolicy()) {
                LazyListIntervalContent(latestContent.value)
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
                LazyListItemProviderImpl(
                    state = state,
                    intervalContent = intervalContent,
                    itemScope = scope,
                    keyIndexMap = map,
                )
            }
        itemProviderState::value
    }
}

private class LazyListItemProviderImpl
constructor(
    private val state: LazyListState,
    private val intervalContent: LazyListIntervalContent,
    override val itemScope: LazyItemScopeImpl,
    override val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyListItemProvider {

    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(itemScope, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override val headerIndexes: IntList
        get() = intervalContent.headerIndexes

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyListItemProviderImpl) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/grid/LazyGrid.kt
```kotlin
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.internal.requirePreconditionNotNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.foundation.lazy.layout.StickyItemsPlacement
import androidx.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.scrollableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyGrid(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** State controlling the scroll position */
    state: LazyGridState,
    /** Prefix sums of cross axis sizes of slots per line, e.g. the columns for vertical grid. */
    slots: LazyGridSlotsProvider,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
    /** The layout orientation of the grid */
    isVertical: Boolean,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** The overscroll effect to render and dispatch events to */
    overscrollEffect: OverscrollEffect?,
    /** The vertical arrangement for items/lines. */
    verticalArrangement: Arrangement.Vertical,
    /** The horizontal arrangement for items/lines. */
    horizontalArrangement: Arrangement.Horizontal,
    /** The content of the grid */
    content: LazyGridScope.() -> Unit,
) {
    val itemProviderLambda = rememberLazyGridItemProviderLambda(state, content)

    val semanticState = rememberLazyGridSemanticState(state, reverseLayout)

    val coroutineScope = rememberCoroutineScope()
    val graphicsContext = LocalGraphicsContext.current
    val stickyHeadersEnabled = !LocalScrollCaptureInProgress.current

    val measurePolicy =
        rememberLazyGridMeasurePolicy(
            itemProviderLambda,
            state,
            slots,
            contentPadding,
            reverseLayout,
            isVertical,
            horizontalArrangement,
            verticalArrangement,
            coroutineScope,
            graphicsContext,
            if (stickyHeadersEnabled) StickyItemsPlacement.StickToTopPlacement else null,
        )

    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal

    val beyondBoundsModifier =
        if (userScrollEnabled) {
            Modifier.lazyLayoutBeyondBoundsModifier(
                state = rememberLazyGridBeyondBoundsState(state = state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                orientation = orientation,
            )
        } else {
            Modifier
        }

    LazyLayout(
        modifier =
            modifier
                .then(state.remeasurementModifier)
                .then(state.awaitLayoutModifier)
                .lazyLayoutSemantics(
                    itemProviderLambda = itemProviderLambda,
                    state = semanticState,
                    orientation = orientation,
                    userScrollEnabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                )
                .then(beyondBoundsModifier)
                .then(state.itemAnimator.modifier)
                .scrollableArea(
                    state = state,
                    orientation = orientation,
                    enabled = userScrollEnabled,
                    reverseScrolling = reverseLayout,
                    flingBehavior = flingBehavior,
                    interactionSource = state.internalInteractionSource,
                    overscrollEffect = overscrollEffect,
                ),
        prefetchState = state.prefetchState,
        measurePolicy = measurePolicy,
        itemProvider = itemProviderLambda,
    )
}

/** lazy grid slots configuration */
internal class LazyGridSlots(val sizes: IntArray, val positions: IntArray)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberLazyGridMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> LazyGridItemProvider,
    /** The state of the list. */
    state: LazyGridState,
    /** Prefix sums of cross axis sizes of slots of the grid. */
    slots: LazyGridSlotsProvider,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
    /** Coroutine scope for item animations */
    coroutineScope: CoroutineScope,
    /** Used for creating graphics layers */
    graphicsContext: GraphicsContext,
    /** Configures the placement of sticky items */
    stickyItemsScrollBehavior: StickyItemsPlacement?,
) =
    remember(
        state,
        slots,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalArrangement,
        verticalArrangement,
        graphicsContext,
    ) {
        LazyLayoutMeasurePolicy { containerConstraints ->
            state.measurementScopeInvalidator.attachToScope()
            // Tracks if the lookahead pass has occurred
            val isInLookaheadScope = state.hasLookaheadOccurred || isLookingAhead
            checkScrollableContainerConstraints(
                containerConstraints,
                if (isVertical) Orientation.Vertical else Orientation.Horizontal,
            )

            // resolve content paddings
            val startPadding =
                if (isVertical) {
                    contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateStartPadding(layoutDirection).roundToPx()
                }

            val endPadding =
                if (isVertical) {
                    contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateEndPadding(layoutDirection).roundToPx()
                }
            val topPadding = contentPadding.calculateTopPadding().roundToPx()
            val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
            val totalVerticalPadding = topPadding + bottomPadding
            val totalHorizontalPadding = startPadding + endPadding
            val totalMainAxisPadding =
                if (isVertical) totalVerticalPadding else totalHorizontalPadding
            val beforeContentPadding =
                when {
                    isVertical && !reverseLayout -> topPadding
                    isVertical && reverseLayout -> bottomPadding
                    !isVertical && !reverseLayout -> startPadding
                    else -> endPadding // !isVertical && reverseLayout
                }
            val afterContentPadding = totalMainAxisPadding - beforeContentPadding
            val contentConstraints =
                containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

            val itemProvider = itemProviderLambda()
            val spanLayoutProvider = itemProvider.spanLayoutProvider
            val resolvedSlots = slots.invoke(density = this, constraints = contentConstraints)
            val slotsPerLine = resolvedSlots.sizes.size
            spanLayoutProvider.slotsPerLine = slotsPerLine

            val spaceBetweenLinesDp =
                if (isVertical) {
                    requirePreconditionNotNull(verticalArrangement) {
                            "null verticalArrangement when isVertical == true"
                        }
                        .spacing
                } else {
                    requirePreconditionNotNull(horizontalArrangement) {
                            "null horizontalArrangement when isVertical == false"
                        }
                        .spacing
                }
            val spaceBetweenLines = spaceBetweenLinesDp.roundToPx()
            val itemsCount = itemProvider.itemCount

            // can be negative if the content padding is larger than the max size from constraints
            val mainAxisAvailableSize =
                if (isVertical) {
                    containerConstraints.maxHeight - totalVerticalPadding
                } else {
                    containerConstraints.maxWidth - totalHorizontalPadding
                }
            val visualItemOffset =
                if (!reverseLayout || mainAxisAvailableSize > 0) {
                    IntOffset(startPadding, topPadding)
                } else {
                    // When layout is reversed and paddings together take >100% of the available
                    // space,
                    // layout size is coerced to 0 when positioning. To take that space into
                    // account,
                    // we offset start padding by negative space between paddings.
                    IntOffset(
                        if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                        if (isVertical) topPadding + mainAxisAvailableSize else topPadding,
                    )
                }

            val measuredItemProvider =
                object : LazyGridMeasuredItemProvider(itemProvider, this, spaceBetweenLines) {
                    override fun createItem(
                        index: Int,
                        key: Any,
                        contentType: Any?,
                        crossAxisSize: Int,
                        mainAxisSpacing: Int,
                        placeables: List<Placeable>,
                        constraints: Constraints,
                        lane: Int,
                        span: Int,
                    ) =
                        LazyGridMeasuredItem(
                            index = index,
                            key = key,
                            isVertical = isVertical,
                            crossAxisSize = crossAxisSize,
                            mainAxisSpacing = mainAxisSpacing,
                            reverseLayout = reverseLayout,
                            layoutDirection = layoutDirection,
                            beforeContentPadding = beforeContentPadding,
                            afterContentPadding = afterContentPadding,
                            visualOffset = visualItemOffset,
                            placeables = placeables,
                            contentType = contentType,
                            animator = state.itemAnimator,
                            constraints = constraints,
                            lane = lane,
                            span = span,
                        )
                }
            val measuredLineProvider =
                object :
                    LazyGridMeasuredLineProvider(
                        isVertical = isVertical,
                        slots = resolvedSlots,
                        gridItemsCount = itemsCount,
                        spaceBetweenLines = spaceBetweenLines,
                        measuredItemProvider = measuredItemProvider,
                        spanLayoutProvider = spanLayoutProvider,
                    ) {
                    override fun createLine(
                        index: Int,
                        items: Array<LazyGridMeasuredItem>,
                        spans: List<GridItemSpan>,
                        mainAxisSpacing: Int,
                    ) =
                        LazyGridMeasuredLine(
                            index = index,
                            items = items,
                            spans = spans,
                            slots = resolvedSlots,
                            isVertical = isVertical,
                            mainAxisSpacing = mainAxisSpacing,
                        )
                }
            val prefetchInfoRetriever: (line: Int) -> List<Pair<Int, Constraints>> = { line ->
                val lineConfiguration = spanLayoutProvider.getLineConfiguration(line)
                var index = lineConfiguration.firstItemIndex
                var slot = 0
                val result = ArrayList<Pair<Int, Constraints>>(lineConfiguration.spans.size)
                lineConfiguration.spans.fastForEach {
                    val span = it.currentLineSpan
                    result.add(index to measuredLineProvider.childConstraints(slot, span))
                    ++index
                    slot += span
                }
                result
            }

            val lineIndexProvider: (itemIndex: Int) -> Int = { itemIndex ->
                spanLayoutProvider.getLineIndexOfItem(itemIndex)
            }

            val firstVisibleLineIndex: Int
            val firstVisibleLineScrollOffset: Int

            Snapshot.withoutReadObservation {
                val index =
                    state.updateScrollPositionIfTheFirstItemWasMoved(
                        itemProvider,
                        state.firstVisibleItemIndex,
                    )
                if (index < itemsCount || itemsCount <= 0) {
                    firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(index)
                    firstVisibleLineScrollOffset = state.firstVisibleItemScrollOffset
                } else {
                    // the data set has been updated and now we have less items that we were
                    // scrolled to before
                    firstVisibleLineIndex = spanLayoutProvider.getLineIndexOfItem(itemsCount - 1)
                    firstVisibleLineScrollOffset = 0
                }
            }

            val pinnedItems =
                itemProvider.calculateLazyLayoutPinnedIndices(
                    state.pinnedItems,
                    state.beyondBoundsInfo,
                )

            val scrollToBeConsumed =
                if (isLookingAhead || !isInLookaheadScope) {
                    state.scrollToBeConsumed
                } else {
                    state.scrollDeltaBetweenPasses
                }

            // todo: wrap with snapshot when b/341782245 is resolved
            val measureResult =
                measureLazyGrid(
                    itemsCount = itemsCount,
                    measuredLineProvider = measuredLineProvider,
                    measuredItemProvider = measuredItemProvider,
                    mainAxisAvailableSize = mainAxisAvailableSize,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    spaceBetweenLines = spaceBetweenLines,
                    firstVisibleLineIndex = firstVisibleLineIndex,
                    firstVisibleLineScrollOffset = firstVisibleLineScrollOffset,
                    scrollToBeConsumed = scrollToBeConsumed,
                    constraints = contentConstraints,
                    isVertical = isVertical,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement,
                    reverseLayout = reverseLayout,
                    density = this,
                    itemAnimator = state.itemAnimator,
                    slotsPerLine = slotsPerLine,
                    pinnedItems = pinnedItems,
                    isInLookaheadScope = isInLookaheadScope,
                    isLookingAhead = isLookingAhead,
                    approachLayoutInfo = state.approachLayoutInfo,
                    coroutineScope = coroutineScope,
                    placementScopeInvalidator = state.placementScopeInvalidator,
                    prefetchInfoRetriever = prefetchInfoRetriever,
                    lineIndexProvider = lineIndexProvider,
                    graphicsContext = graphicsContext,
                    stickyItemsScrollBehavior = stickyItemsScrollBehavior,
                    layout = { width, height, placement ->
                        layout(
                            containerConstraints.constrainWidth(width + totalHorizontalPadding),
                            containerConstraints.constrainHeight(height + totalVerticalPadding),
                            emptyMap(),
                            placement,
                        )
                    },
                )
            state.applyMeasureResult(measureResult, isLookingAhead = isLookingAhead)
            // apply keep around after updating the strategy with measure result.
            (state.prefetchStrategy as? CacheWindowLogic)?.keepAroundItems(
                measureResult.orientation,
                measureResult.visibleItemsInfo,
                measuredLineProvider,
            )
            measureResult
        }
    }

@OptIn(ExperimentalFoundationApi::class)
private fun CacheWindowLogic.keepAroundItems(
    orientation: Orientation,
    visibleItemsList: List<LazyGridMeasuredItem>,
    measuredLineProvider: LazyGridMeasuredLineProvider,
) {
    trace("compose:lazy:cache_window:keepAroundItems") {
        // only run if window and new layout info is available
        if (hasValidBounds() && visibleItemsList.isNotEmpty()) {
            val firstVisibleItemIndex = visibleItemsList.first().lineIndex(orientation)
            val lastVisibleItemIndex = visibleItemsList.last().lineIndex(orientation)
            // we must send a message in case of changing directions for items
            // that were keep around and become prefetch forward
            for (line in prefetchWindowStartLine..<firstVisibleItemIndex) {
                measuredLineProvider.keepAround(line)
            }

            for (line in (lastVisibleItemIndex + 1)..prefetchWindowEndLine) {
                measuredLineProvider.keepAround(line)
            }
        }
    }
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/grid/LazyGridDsl.kt
```kotlin
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A lazy vertical grid layout. It composes only visible rows of the grid.
 *
 * Sample:
 *
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSample
 *
 * Sample with custom item spans:
 *
 * @sample androidx.compose.foundation.samples.LazyVerticalGridSpanSample
 * @param columns describes the count and the size of the grid's columns, see [GridCells] doc for
 *   more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items will be
 *   laid out in the reverse order and [LazyGridState.firstVisibleItemIndex] == 0 means that grid is
 *   scrolled to the bottom. Note that [reverseLayout] does not change the behavior of
 *   [verticalArrangement], e.g. with [Arrangement.Top] (top) 123### (bottom) becomes (top) 321###
 *   (bottom).
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content the [LazyGridScope] which describes the content
 */
@Composable
fun LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: LazyGridScope.() -> Unit,
) {
    LazyGrid(
        slots = rememberColumnWidthSums(columns, horizontalArrangement),
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = true,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = rememberOverscrollEffect(),
        content = content,
    )
}

/**
 * A lazy horizontal grid layout. It composes only visible columns of the grid.
 *
 * Sample:
 *
 * @sample androidx.compose.foundation.samples.LazyHorizontalGridSample
 *
 * Sample with custom item spans:
 *
 * @sample androidx.compose.foundation.samples.LazyHorizontalGridSpanSample
 * @param rows a class describing how cells form rows, see [GridCells] doc for more information
 * @param modifier the modifier to apply to this layout
 * @param state the state object to be used to control or observe the list's state
 * @param contentPadding specify a padding around the whole content
 * @param reverseLayout reverse the direction of scrolling and layout. When `true`, items are laid
 *   out in the reverse order and [LazyGridState.firstVisibleItemIndex] == 0 means that grid is
 *   scrolled to the end. Note that [reverseLayout] does not change the behavior of
 *   [horizontalArrangement], e.g. with [Arrangement.Start] [123###] becomes [321###].
 * @param verticalArrangement The vertical arrangement of the layout's children
 * @param horizontalArrangement The horizontal arrangement of the layout's children
 * @param flingBehavior logic describing fling behavior
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using the state even when it is disabled.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param content the [LazyGridScope] which describes the content
 */
@Composable
fun LazyHorizontalGrid(
    rows: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: LazyGridScope.() -> Unit,
) {
    LazyGrid(
        slots = rememberRowHeightSums(rows, verticalArrangement),
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        isVertical = false,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        content = content,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun LazyHorizontalGrid(
    rows: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    LazyHorizontalGrid(
        rows = rows,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = rememberOverscrollEffect(),
        content = content,
    )
}

/** Returns prefix sums of column widths. */
@Composable
private fun rememberColumnWidthSums(
    columns: GridCells,
    horizontalArrangement: Arrangement.Horizontal,
) =
    remember<LazyGridSlotsProvider>(columns, horizontalArrangement) {
        GridSlotCache { constraints ->
            requirePrecondition(constraints.maxWidth != Constraints.Infinity) {
                "LazyVerticalGrid's width should be bound by parent."
            }

            val gridWidth = constraints.maxWidth
            with(columns) {
                calculateCrossAxisCellSizes(gridWidth, horizontalArrangement.spacing.roundToPx())
                    .toIntArray()
                    .let { sizes ->
                        val positions = IntArray(sizes.size)
                        with(horizontalArrangement) {
                            arrange(gridWidth, sizes, LayoutDirection.Ltr, positions)
                        }
                        LazyGridSlots(sizes, positions)
                    }
            }
        }
    }

/** Returns prefix sums of row heights. */
@Composable
private fun rememberRowHeightSums(rows: GridCells, verticalArrangement: Arrangement.Vertical) =
    remember<LazyGridSlotsProvider>(rows, verticalArrangement) {
        GridSlotCache { constraints ->
            requirePrecondition(constraints.maxHeight != Constraints.Infinity) {
                "LazyHorizontalGrid's height should be bound by parent."
            }

            val gridHeight = constraints.maxHeight
            with(rows) {
                calculateCrossAxisCellSizes(gridHeight, verticalArrangement.spacing.roundToPx())
                    .toIntArray()
                    .let { sizes ->
                        val positions = IntArray(sizes.size)
                        with(verticalArrangement) { arrange(gridHeight, sizes, positions) }
                        LazyGridSlots(sizes, positions)
                    }
            }
        }
    }

// Note: Implementing function interface is prohibited in K/JS (class A: () -> Unit)
// therefore we workaround this limitation by inheriting a fun interface instead
internal fun interface LazyGridSlotsProvider {
    fun invoke(density: Density, constraints: Constraints): LazyGridSlots
}

/** measurement cache to avoid recalculating row/column sizes on each scroll. */
private class GridSlotCache(private val calculation: Density.(Constraints) -> LazyGridSlots) :
    LazyGridSlotsProvider {
    private var cachedConstraints = Constraints()
    private var cachedDensity: Float = 0f
    private var cachedSizes: LazyGridSlots? = null

    override fun invoke(density: Density, constraints: Constraints): LazyGridSlots {
        with(density) {
            if (
                cachedSizes != null &&
                    cachedConstraints == constraints &&
                    cachedDensity == this.density
            ) {
                return cachedSizes!!
            }

            cachedConstraints = constraints
            cachedDensity = this.density
            return calculation(constraints).also { cachedSizes = it }
        }
    }
}

/**
 * This class describes the count and the sizes of columns in vertical grids, or rows in horizontal
 * grids.
 */
@Stable
interface GridCells {
    /**
     * Calculates the number of cells and their cross axis size based on [availableSize] and
     * [spacing].
     *
     * For example, in vertical grids, [spacing] is passed from the grid's [Arrangement.Horizontal].
     * The [Arrangement.Horizontal] will also be used to arrange items in a row if the grid is wider
     * than the calculated sum of columns.
     *
     * Note that the calculated cross axis sizes will be considered in an RTL-aware manner -- if the
     * grid is vertical and the layout direction is RTL, the first width in the returned list will
     * correspond to the rightmost column.
     *
     * @param availableSize available size on cross axis, e.g. width of [LazyVerticalGrid].
     * @param spacing cross axis spacing, e.g. horizontal spacing for [LazyVerticalGrid]. The
     *   spacing is passed from the corresponding [Arrangement] param of the lazy grid.
     */
    fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int>

    /**
     * Defines a grid with fixed number of rows or columns.
     *
     * For example, for the vertical [LazyVerticalGrid] Fixed(3) would mean that there are 3 columns
     * 1/3 of the parent width.
     */
    class Fixed(private val count: Int) : GridCells {
        init {
            requirePrecondition(count > 0) { "Provided count should be larger than zero" }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int,
        ): List<Int> {
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return -count // Different sign from Adaptive.
        }

        override fun equals(other: Any?): Boolean {
            return other is Fixed && count == other.count
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that every cell has
     * at least [minSize] space and all extra space distributed evenly.
     *
     * For example, for the vertical [LazyVerticalGrid] Adaptive(20.dp) would mean that there will
     * be as many columns as possible and every column will be at least 20.dp and all the columns
     * will have equal width. If the screen is 88.dp wide then there will be 4 columns 22.dp each.
     */
    class Adaptive(private val minSize: Dp) : GridCells {
        init {
            requirePrecondition(minSize > 0.dp) { "Provided min size should be larger than zero." }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int,
        ): List<Int> {
            val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return minSize.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is Adaptive && minSize == other.minSize
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that every cell
     * takes exactly [size] space. The remaining space will be arranged through [LazyGrid]
     * arrangements on corresponding axis. If [size] is larger than container size, the cell will be
     * size to match the container.
     *
     * For example, for the vertical [LazyGrid] FixedSize(20.dp) would mean that there will be as
     * many columns as possible and every column will be exactly 20.dp. If the screen is 88.dp wide
     * tne there will be 4 columns 20.dp each with remaining 8.dp distributed through
     * [Arrangement.Horizontal].
     */
    class FixedSize(private val size: Dp) : GridCells {
        init {
            requirePrecondition(size > 0.dp) { "Provided size should be larger than zero." }
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int,
        ): List<Int> {
            val cellSize = size.roundToPx()
            return if (cellSize + spacing < availableSize + spacing) {
                val cellCount = (availableSize + spacing) / (cellSize + spacing)
                List(cellCount) { cellSize }
            } else {
                List(1) { availableSize }
            }
        }

        override fun hashCode(): Int {
            return size.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is FixedSize && size == other.size
        }
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int,
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) { slotSize + if (it < remainingPixels) 1 else 0 }
}

/** Receiver scope which is used by [LazyVerticalGrid]. */
@LazyGridScopeMarker
sealed interface LazyGridScope {
    /**
     * Adds a single item to the scope.
     *
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the grid is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the grid will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one. This can be overridden by calling [LazyGridState.requestScrollToItem].
     * @param span the span of the item. Default is 1x1. It is good practice to leave it `null` when
     *   this matches the intended behavior, as providing a custom implementation impacts
     *   performance
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the item
     */
    fun item(
        key: Any? = null,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.() -> Unit,
    )

    /**
     * Adds a [count] of items.
     *
     * @param count the items count
     * @param key a factory of stable and unique keys representing the item. Using the same key for
     *   multiple items in the grid is not allowed. Type of the key should be saveable via Bundle on
     *   Android. If null is passed the position in the grid will represent the key. When you
     *   specify the key the scroll position will be maintained based on the key, which means if you
     *   add/remove items before the current visible item the item with the given key will be kept
     *   as the first visible one.This can be overridden by calling
     *   [LazyGridState.requestScrollToItem].
     * @param span define custom spans for the items. Default is 1x1. It is good practice to leave
     *   it `null` when this matches the intended behavior, as providing a custom implementation
     *   impacts performance
     * @param contentType a factory of the content types for the item. The item compositions of the
     *   same type could be reused more efficiently. Note that null is a valid type and items of
     *   such type will be considered compatible.
     * @param itemContent the content displayed by a single item
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit,
    )

    /**
     * Adds a sticky header item, which will remain pinned even when scrolling after it. The header
     * will remain pinned until the next header will take its place. Sticky Headers are full span
     * items, that is, they will occupy [LazyGridItemSpanScope.maxLineSpan].
     *
     * @sample androidx.compose.foundation.samples.StickyHeaderGridSample
     * @param key a stable and unique key representing the item. Using the same key for multiple
     *   items in the list is not allowed. Type of the key should be saveable via Bundle on Android.
     *   If null is passed the position in the list will represent the key. When you specify the key
     *   the scroll position will be maintained based on the key, which means if you add/remove
     *   items before the current visible item the item with the given key will be kept as the first
     *   visible one. This can be overridden by calling 'requestScrollToItem' on the
     *   'LazyGridState'.
     * @param contentType the type of the content of this item. The item compositions of the same
     *   type could be reused more efficiently. Note that null is a valid type and items of such
     *   type will be considered compatible.
     * @param content the content of the header. The header index is provided, this is the item
     *   position within the total set of items in this lazy list (the global index).
     */
    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyGridItemScope.(Int) -> Unit,
    )
}

/**
 * Adds a list of items.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the grid is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the grid will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling [LazyGridState.requestScrollToItem].
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave it
 *   `null` when this matches the intended behavior, as providing a custom implementation impacts
 *   performance
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        span =
            if (span != null) {
                { span(items[it]) }
            } else null,
        contentType = { index: Int -> contentType(items[index]) },
    ) {
        itemContent(items[it])
    }

/**
 * Adds a list of items where the content of an item is aware of its index.
 *
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the grid is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the grid will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling [LazyGridState.requestScrollToItem].
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave it
 *   `null` when this matches the intended behavior, as providing a custom implementation impacts
 *   performance
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        span =
            if (span != null) {
                { span(it, items[it]) }
            } else null,
        contentType = { index -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }

/**
 * Adds an array of items.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the grid is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the grid will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one.This can be overridden by calling [LazyGridState.requestScrollToItem].
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave it
 *   `null` when this matches the intended behavior, as providing a custom implementation impacts
 *   performance
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.items(
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        span =
            if (span != null) {
                { span(items[it]) }
            } else null,
        contentType = { index: Int -> contentType(items[index]) },
    ) {
        itemContent(items[it])
    }

/**
 * Adds an array of items where the content of an item is aware of its index.
 *
 * @param items the data array
 * @param key a factory of stable and unique keys representing the item. Using the same key for
 *   multiple items in the grid is not allowed. Type of the key should be saveable via Bundle on
 *   Android. If null is passed the position in the grid will represent the key. When you specify
 *   the key the scroll position will be maintained based on the key, which means if you add/remove
 *   items before the current visible item the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling [LazyGridState.requestScrollToItem].
 * @param span define custom spans for the items. Default is 1x1. It is good practice to leave it
 *   `null` when this matches the intended behavior, as providing a custom implementation impacts
 *   performance
 * @param contentType a factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param itemContent the content displayed by a single item
 */
inline fun <T> LazyGridScope.itemsIndexed(
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit,
) =
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        span =
            if (span != null) {
                { span(it, items[it]) }
            } else null,
        contentType = { index -> contentType(index, items[index]) },
    ) {
        itemContent(it, items[it])
    }
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/grid/LazyGridState.kt
```kotlin
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

package androidx.compose.foundation.lazy.grid

import androidx.annotation.IntRange as AndroidXIntRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.lazy.grid.LazyGridState.Companion.Saver
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollDeltaBetweenPasses
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.animateScrollToItem
import androidx.compose.foundation.lazy.singleAxisViewportSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates a [LazyGridState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyGridState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberLazyGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyGridState {
    return rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
}

/**
 * Creates a [LazyGridState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyGridState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyGridPrefetchStrategy] to use for prefetching content in this
 *   grid
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    prefetchStrategy: LazyGridPrefetchStrategy = remember { LazyGridPrefetchStrategy() },
): LazyGridState {
    return rememberSaveable(prefetchStrategy, saver = LazyGridState.saver(prefetchStrategy)) {
        LazyGridState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
            prefetchStrategy,
        )
    }
}

/**
 * Creates a [LazyGridState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param cacheWindow specifies the size of the ahead and behind window to be used as per
 *   [LazyLayoutCacheWindow].
 * @param initialFirstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [LazyGridState.firstVisibleItemScrollOffset]
 */
@ExperimentalFoundationApi
@Composable
fun rememberLazyGridState(
    cacheWindow: LazyLayoutCacheWindow,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyGridState {
    return rememberSaveable(cacheWindow, saver = LazyGridState.saver(cacheWindow)) {
        LazyGridState(
            cacheWindow,
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberLazyGridState].
 *
 * @param firstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 *   [LazyGridState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyGridPrefetchStrategy] to use for prefetching content in this
 *   grid
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class LazyGridState
@ExperimentalFoundationApi
constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    internal val prefetchStrategy: LazyGridPrefetchStrategy = LazyGridPrefetchStrategy(),
) : ScrollableState {

    /**
     * @param cacheWindow specifies the size of the ahead and behind window to be used as per
     *   [LazyLayoutCacheWindow].
     * @param firstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
     * @param firstVisibleItemScrollOffset the initial value for
     *   [LazyGridState.firstVisibleItemScrollOffset]
     */
    @ExperimentalFoundationApi
    constructor(
        cacheWindow: LazyLayoutCacheWindow,
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
    ) : this(
        firstVisibleItemIndex,
        firstVisibleItemScrollOffset,
        LazyGridCacheWindowPrefetchStrategy(cacheWindow),
    )

    /**
     * @param firstVisibleItemIndex the initial value for [LazyGridState.firstVisibleItemIndex]
     * @param firstVisibleItemScrollOffset the initial value for
     *   [LazyGridState.firstVisibleItemScrollOffset]
     */
    constructor(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
    ) : this(firstVisibleItemIndex, firstVisibleItemScrollOffset, LazyGridPrefetchStrategy())

    internal var hasLookaheadOccurred: Boolean = false
        private set

    internal var approachLayoutInfo: LazyGridMeasureResult? = null
        private set

    // always execute requests in high priority
    private var executeRequestsInHighPriorityMode = false

    /** The holder class for the current scroll position. */
    private val scrollPosition =
        LazyGridScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    /**
     * The index of the first item that is visible within the scrollable viewport area, this means,
     * not including items in the content padding region. For the first visible item that includes
     * items in the content padding please use [LazyGridLayoutInfo.visibleItemsInfo].
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change causing potential performance issues.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingGridScrollPositionForSideEffectSample
     *
     * If you need to use it in the composition then consider wrapping the calculation into a
     * derived state in order to only have recompositions when the derived value changes:
     *
     * @sample androidx.compose.foundation.samples.UsingGridScrollPositionInCompositionSample
     */
    val firstVisibleItemIndex: Int
        @FrequentlyChangingValue get() = scrollPosition.index

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the amount
     * that the item is offset backwards
     */
    val firstVisibleItemScrollOffset: Int
        @FrequentlyChangingValue get() = scrollPosition.scrollOffset

    /** Backing state for [layoutInfo] */
    private val layoutInfoState = mutableStateOf(EmptyLazyGridLayoutInfo, neverEqualPolicy())

    /**
     * The object of [LazyGridLayoutInfo] calculated during the last layout pass. For example, you
     * can use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure. If you
     * use it in the composable function it will be recomposed on every change causing potential
     * performance issues including infinity recomposition loop. Therefore, avoid using it in the
     * composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingGridLayoutInfoForSideEffectSample
     */
    val layoutInfo: LazyGridLayoutInfo
        @FrequentlyChangingValue get() = layoutInfoState.value

    /**
     * [InteractionSource] that will be used to dispatch drag events when this grid is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * The amount of scroll to be consumed in the next layout pass. Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    internal var scrollToBeConsumed = 0f
        private set

    internal val slotsPerLine: Int
        get() = layoutInfoState.value.slotsPerLine

    internal val density: Density
        get() = layoutInfoState.value.density

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once we
     * reached the end of the grid.
     */
    private val scrollableState = ScrollableState { -onScroll(-it) }

    /** Only used for testing to confirm that we're not making too many measure passes */
    /*@VisibleForTesting*/
    internal var numMeasurePasses: Int = 0
        private set

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@LazyGridState.remeasurement = remeasurement
            }
        }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    internal val itemAnimator = LazyLayoutItemAnimator<LazyGridMeasuredItem>()

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    @Suppress("DEPRECATION") // b/420551535
    internal val prefetchState =
        LazyLayoutPrefetchState(prefetchStrategy.prefetchScheduler) {
            with(prefetchStrategy) {
                onNestedPrefetch(Snapshot.withoutReadObservation { firstVisibleItemIndex })
            }
        }

    private val prefetchScope: LazyGridPrefetchScope =
        object : LazyGridPrefetchScope {
            override fun scheduleLinePrefetch(
                lineIndex: Int
            ): List<LazyLayoutPrefetchState.PrefetchHandle> {
                return scheduleLinePrefetch(lineIndex, null)
            }

            @Suppress("PrimitiveInCollection")
            override fun scheduleLinePrefetch(
                lineIndex: Int,
                onPrefetchFinished: (LazyGridPrefetchResultScope.() -> Unit)?,
            ): List<LazyLayoutPrefetchState.PrefetchHandle> {
                // Without read observation since this can be triggered from scroll - this will then
                // cause us to recompose when the measure result changes. We don't care since the
                // prefetch is best effort.
                val prefetchHandles = mutableListOf<LazyLayoutPrefetchState.PrefetchHandle>()
                val itemSizes: MutableList<Int>? =
                    if (onPrefetchFinished == null) null else mutableListOf()

                Snapshot.withoutReadObservation {
                    val layoutInfo =
                        if (hasLookaheadOccurred) {
                            approachLayoutInfo
                        } else {
                            layoutInfoState.value
                        }

                    layoutInfo?.let { measureResult ->
                        var completedCount = 1
                        val itemsInLineInfo = measureResult.prefetchInfoRetriever(lineIndex)
                        itemsInLineInfo.fastForEach { lineInfo ->
                            prefetchHandles.add(
                                prefetchState.schedulePrecompositionAndPremeasure(
                                    lineInfo.first,
                                    lineInfo.second,
                                    executeRequestsInHighPriorityMode,
                                ) {
                                    var itemMainAxisItemSize = 0
                                    repeat(placeablesCount) {
                                        itemMainAxisItemSize +=
                                            if (measureResult.orientation == Orientation.Vertical) {
                                                getSize(it).height
                                            } else {
                                                getSize(it).width
                                            }
                                    }

                                    itemSizes?.add(itemMainAxisItemSize)
                                    // all items in this line were prefetched, report the size
                                    if (completedCount == itemsInLineInfo.size) {
                                        if (onPrefetchFinished != null && itemSizes != null) {
                                            onPrefetchFinished.invoke(
                                                LazyGridPrefetchResultScopeImpl(
                                                    lineIndex,
                                                    itemSizes,
                                                )
                                            )
                                        }
                                    } else {
                                        completedCount++
                                    }
                                }
                            )
                        }
                    }
                }
                return prefetchHandles
            }
        }

    private val _scrollIndicatorState =
        object : ScrollIndicatorState {
            override val scrollOffset: Int
                get() = calculateScrollOffset()

            override val contentSize: Int
                get() = layoutInfo.calculateContentSize()

            override val viewportSize: Int
                get() = layoutInfo.singleAxisViewportSize
        }

    private fun calculateScrollOffset(): Int {
        val info = layoutInfo
        return (info.visibleLinesAverageMainAxisSize() * info.firstVisibleItemLineIndex) +
            firstVisibleItemScrollOffset
    }

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun scrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll { snapToItemIndexInternal(index, scrollOffset, forceRemeasure = true) }
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the item at [index] to be at the start of the viewport during the next remeasure,
     * offset by [scrollOffset], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the first visible item key (when a data set change will also be applied during the
     * next remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    fun requestScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            layoutInfoState.value.coroutineScope.launch { stopScroll() }
        }

        snapToItemIndexInternal(index, scrollOffset, forceRemeasure = false)
    }

    internal fun snapToItemIndexInternal(index: Int, scrollOffset: Int, forceRemeasure: Boolean) {
        val positionChanged =
            scrollPosition.index != index || scrollPosition.scrollOffset != scrollOffset
        // sometimes this method is called not to scroll, but to stay on the same index when
        // the data changes, as by default we maintain the scroll position by key, not index.
        // when this happens we don't need to reset the animations as from the user perspective
        // we didn't scroll anywhere and if there is an offset change for an item, this change
        // should be animated.
        // however, when the request is to really scroll to a different position, we have to
        // reset previously known item positions as we don't want offset changes to be animated.
        // this offset should be considered as a scroll, not the placement change.
        if (positionChanged) {
            itemAnimator.reset()
            // we changed positions, cancel existing requests and wait for the next scroll to
            // refill the window
            (prefetchStrategy as? CacheWindowLogic)?.resetStrategy()
        }
        scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)
        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        } else {
            measurementScopeInvalidator.invalidateScope()
        }
    }

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        if (layoutInfoState.value === EmptyLazyGridLayoutInfo) {
            awaitLayoutModifier.waitForFirstLayout()
        }
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    override val scrollIndicatorState: ScrollIndicatorState?
        get() = _scrollIndicatorState

    // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
    //  fine-grained control over scrolling
    /*@VisibleForTesting*/
    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        checkPrecondition(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            val intDelta = scrollToBeConsumed.roundToInt()
            var scrolledLayoutInfo =
                layoutInfoState.value.copyWithScrollDeltaWithoutRemeasure(
                    delta = intDelta,
                    updateAnimations = !hasLookaheadOccurred,
                )
            if (scrolledLayoutInfo != null && this.approachLayoutInfo != null) {
                // if we were able to scroll the lookahead layout info without remeasure, lets
                // try to do the same for post lookahead layout info (sometimes they diverge).
                val scrolledApproachLayoutInfo =
                    approachLayoutInfo?.copyWithScrollDeltaWithoutRemeasure(
                        delta = intDelta,
                        updateAnimations = true,
                    )
                if (scrolledApproachLayoutInfo != null) {
                    // we can apply scroll delta for both phases without remeasure
                    approachLayoutInfo = scrolledApproachLayoutInfo
                } else {
                    // we can't apply scroll delta for post lookahead, so we have to remeasure
                    scrolledLayoutInfo = null
                }
            }
            if (scrolledLayoutInfo != null) {
                applyMeasureResult(
                    result = scrolledLayoutInfo,
                    isLookingAhead = hasLookaheadOccurred,
                    visibleItemsStayedTheSame = true,
                )
                // we don't need to remeasure, so we only trigger re-placement:
                placementScopeInvalidator.invalidateScope()

                notifyPrefetchOnScroll(
                    preScrollToBeConsumed - scrollToBeConsumed,
                    scrolledLayoutInfo,
                )
            } else {
                remeasurement?.forceRemeasure()
                notifyPrefetchOnScroll(preScrollToBeConsumed - scrollToBeConsumed, this.layoutInfo)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    private fun notifyPrefetchOnScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
        if (prefetchingEnabled) {
            with(prefetchStrategy) { prefetchScope.onScroll(delta, layoutInfo) }
        }
    }

    private val numOfItemsToTeleport: Int
        get() = 100 * slotsPerLine

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun animateScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll {
            LazyLayoutScrollScope(this@LazyGridState, this)
                .animateScrollToItem(index, scrollOffset, numOfItemsToTeleport, density)
        }
    }

    /** Updates the state with the new calculated scroll position and consumed scroll. */
    internal fun applyMeasureResult(
        result: LazyGridMeasureResult,
        isLookingAhead: Boolean,
        visibleItemsStayedTheSame: Boolean = false,
    ) {
        // update the prefetch state with the number of nested prefetch items this layout
        // should use.
        prefetchState.idealNestedPrefetchCount = result.visibleItemsInfo.size

        if (!isLookingAhead && hasLookaheadOccurred) {
            // If there was already a lookahead pass, record this result as Approach result
            approachLayoutInfo = result
        } else {
            if (isLookingAhead) {
                hasLookaheadOccurred = true
            }
            scrollToBeConsumed -= result.consumedScroll
            layoutInfoState.value = result

            canScrollBackward = result.canScrollBackward
            canScrollForward = result.canScrollForward

            if (visibleItemsStayedTheSame) {
                scrollPosition.updateScrollOffset(result.firstVisibleLineScrollOffset)
            } else {
                scrollPosition.updateFromMeasureResult(result)
                if (prefetchingEnabled) {
                    with(prefetchStrategy) { prefetchScope.onVisibleItemsUpdated(result) }
                }
            }

            if (isLookingAhead) {
                _lazyLayoutScrollDeltaBetweenPasses.updateScrollDeltaForApproach(
                    result.scrollBackAmount,
                    result.density,
                    result.coroutineScope,
                )
            }
            numMeasurePasses++
        }
    }

    private val _lazyLayoutScrollDeltaBetweenPasses = LazyLayoutScrollDeltaBetweenPasses()

    internal val scrollDeltaBetweenPasses
        get() = _lazyLayoutScrollDeltaBetweenPasses.scrollDeltaBetweenPasses

    /**
     * When the user provided custom keys for the items we can try to detect when there were items
     * added or removed before our current first visible item and keep this item as the first
     * visible one even given that its index has been changed.
     */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: LazyGridItemProvider,
        firstItemIndex: Int,
    ): Int = scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

    companion object {
        /** The default [Saver] implementation for [LazyGridState]. */
        val Saver: Saver<LazyGridState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyGridState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                    )
                },
            )

        /**
         * A [Saver] implementation for [LazyGridState] that handles setting a custom
         * [LazyGridPrefetchStrategy].
         */
        @ExperimentalFoundationApi
        internal fun saver(prefetchStrategy: LazyGridPrefetchStrategy): Saver<LazyGridState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyGridState(
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                        prefetchStrategy,
                    )
                },
            )

        /**
         * A [Saver] implementation for [LazyGridState] that handles setting a custom
         * [LazyLayoutCacheWindow].
         */
        @ExperimentalFoundationApi
        internal fun saver(cacheWindow: LazyLayoutCacheWindow): Saver<LazyGridState, *> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    LazyGridState(
                        cacheWindow = cacheWindow,
                        firstVisibleItemIndex = it[0],
                        firstVisibleItemScrollOffset = it[1],
                    )
                },
            )
    }
}

private val EmptyLazyGridLayoutInfo =
    LazyGridMeasureResult(
        firstVisibleLine = null,
        firstVisibleLineScrollOffset = 0,
        canScrollForward = false,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        scrollBackAmount = 0f,
        visibleItemsInfo = emptyList(),
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        totalItemsCount = 0,
        reverseLayout = false,
        orientation = Orientation.Vertical,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        remeasureNeeded = false,
        density = Density(1f),
        slotsPerLine = 0,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        prefetchInfoRetriever = { emptyList() },
        lineIndexProvider = { -1 },
    )
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/BasicTextField.kt
```kotlin
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.contextmenu.modifier.ToolbarRequesterImpl
import androidx.compose.foundation.text.handwriting.stylusHandwriting
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.CodepointTransformation
import androidx.compose.foundation.text.input.internal.SingleLineCodepointTransformation
import androidx.compose.foundation.text.input.internal.TextFieldCoreModifier
import androidx.compose.foundation.text.input.internal.TextFieldDecoratorModifier
import androidx.compose.foundation.text.input.internal.TextFieldTextLayoutModifier
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.collectIsDragAndDropHoveredAsState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState.InputType
import androidx.compose.foundation.text.input.internal.selection.TextToolbarHandler
import androidx.compose.foundation.text.input.internal.selection.TextToolbarState
import androidx.compose.foundation.text.input.internal.selection.addBasicTextFieldTextContextMenuComponents
import androidx.compose.foundation.text.input.internal.selection.menuItem
import androidx.compose.foundation.text.selection.SelectedTextType
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.foundation.text.selection.rememberPlatformSelectionBehaviors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private object BasicTextFieldDefaults {
    val CursorBrush = SolidColor(Color.Black)
}

/**
 * Basic text composable that provides an interactive box that accepts text input through software
 * or hardware keyboard, but provides no decorations like hint or placeholder.
 *
 * All the editing state of this composable is hoisted through [state]. Whenever the contents of
 * this composable change via user input or semantics, [TextFieldState.text] gets updated.
 * Similarly, all the programmatic updates made to [state] also reflect on this composable.
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decorator.
 *
 * In order to filter (e.g. only allow digits, limit the number of characters), or change (e.g.
 * convert every character to uppercase) the input received from the user, use an
 * [InputTransformation].
 *
 * Limiting the height of the [BasicTextField] in terms of line count and choosing a scroll
 * direction can be achieved by using [TextFieldLineLimits].
 *
 * Scroll state of the composable is also hoisted to enable observation and manipulation of the
 * scroll behavior by the developer, e.g. bringing a searched keyword into view by scrolling to its
 * position without focusing, or changing selection.
 *
 * It's also possible to internally wrap around an existing TextFieldState and expose a more
 * lightweight state hoisting mechanism through a value that dictates the content of the TextField
 * and an onValueChange callback that communicates the changes to this value.
 *
 * @param state [TextFieldState] object that holds the internal editing state of [BasicTextField].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable.
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit.
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. the transformation will not immediately affect the
 *   current [state].
 * @param textStyle Typographic and graphic style configuration for text content that's displayed in
 *   the editor.
 * @param keyboardOptions Software keyboard options that contain configurations such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction Called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard if the [lineLimits] is configured as
 *   [TextFieldLineLimits.SingleLine]. By default this parameter is null, and would execute the
 *   default behavior for a received IME Action e.g., [ImeAction.Done] would close the keyboard,
 *   [ImeAction.Next] would switch the focus to the next focusable item on the screen.
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' '), ensuring that
 *   the contents of the text field are presented in a single line.
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 *   callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 *   or null if it cannot. The function reads the layout result from a snapshot state object, and
 *   will invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 *   paragraph information, size of the text, baselines and other details. The callback can be used
 *   to add additional decoration or functionality to the text. For example, to draw a cursor or
 *   selection around the text. [Density] scope is the one that was used while creating the given
 *   text layout.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this TextField. You can create and pass in your own remembered [MutableInteractionSource]
 *   if you want to observe [Interaction]s and customize the appearance / behavior of this TextField
 *   for different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, then no cursor will be drawn.
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 *   text field are presented.
 * @param decorator Allows to add decorations around text field, such as icon, placeholder, helper
 *   messages or similar, and automatically increase the hit target area of the text field.
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of TextField.
 *   If [lineLimits] is [SingleLine], this text field is treated as single line with horizontal
 *   scroll behavior. In other cases the text field becomes vertically scrollable.
 * @sample androidx.compose.foundation.samples.BasicTextFieldDecoratorSample
 * @sample androidx.compose.foundation.samples.BasicTextFieldCustomInputTransformationSample
 * @sample androidx.compose.foundation.samples.BasicTextFieldWithValueOnValueChangeSample
 */
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = BasicTextFieldDefaults.CursorBrush,
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    BasicTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        codepointTransformation = null,
        outputTransformation = outputTransformation,
        decorator = decorator,
        scrollState = scrollState,
    )
}

/**
 * Internal core text field that accepts a [CodepointTransformation].
 *
 * @param codepointTransformation Visual transformation interface that provides a 1-to-1 mapping of
 *   codepoints.
 */
// This takes a composable lambda, but it is not primarily a container.
@Suppress("ComposableLambdaParameterPosition")
@Composable
internal fun BasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = BasicTextFieldDefaults.CursorBrush,
    codepointTransformation: CodepointTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    decorator: TextFieldDecorator? = null,
    scrollState: ScrollState = rememberScrollState(),
    isPassword: Boolean = false,
    // Last parameter must not be a function unless it's intended to be commonly used as a trailing
    // lambda.
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val singleLine = lineLimits == SingleLine
    // We're using this to communicate focus state to cursor for now.
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val isDragHovered = interactionSource.collectIsDragAndDropHoveredAsState().value
    // Avoid reading LocalWindowInfo.current.isWindowFocused when the text field is not focused;
    // otherwise all text fields in a window will be recomposed when it becomes focused.
    val isWindowAndTextFieldFocused = isFocused && LocalWindowInfo.current.isWindowFocused
    val stylusHandwritingTrigger = remember {
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    }

    val transformedState =
        remember(state, codepointTransformation, outputTransformation) {
            // First prefer provided codepointTransformation if not null, e.g. BasicSecureTextField
            // would send PasswordTransformation. Second, apply a SingleLineCodepointTransformation
            // if
            // text field is configured to be single line. Else, don't apply any visual
            // transformation.
            val appliedCodepointTransformation =
                codepointTransformation ?: SingleLineCodepointTransformation.takeIf { singleLine }
            TransformedTextFieldState(
                textFieldState = state,
                inputTransformation = inputTransformation,
                codepointTransformation = appliedCodepointTransformation,
                outputTransformation = outputTransformation,
            )
        }

    // Invalidate textLayoutState if TextFieldState itself has changed, since TextLayoutState
    // would be carrying an invalid TextFieldState in its nonMeasureInputs.
    val textLayoutState = remember(transformedState) { TextLayoutState() }

    // InputTransformation.keyboardOptions might be backed by Snapshot state.
    // Read in a restartable composable scope to make sure the resolved value is always up-to-date.
    val resolvedKeyboardOptions =
        keyboardOptions.fillUnspecifiedValuesWith(inputTransformation?.keyboardOptions)

    val coroutineScope = rememberCoroutineScope()
    @OptIn(ExperimentalFoundationApi::class)
    val platformSelectionBehaviors =
        if (ComposeFoundationFlags.isSmartSelectionEnabled) {
            val resolvedLocaleList = textStyle.localeList ?: LocaleList.current
            rememberPlatformSelectionBehaviors(SelectedTextType.EditableText, resolvedLocaleList)
        } else {
            null
        }
    val toolbarRequester = remember { ToolbarRequesterImpl() }
    val currentClipboard = LocalClipboard.current
    val textFieldSelectionState =
        remember(transformedState) {
            TextFieldSelectionState(
                textFieldState = transformedState,
                textLayoutState = textLayoutState,
                density = density,
                enabled = enabled,
                readOnly = readOnly,
                isFocused = isWindowAndTextFieldFocused,
                isPassword = isPassword,
                toolbarRequester = toolbarRequester,
                coroutineScope = coroutineScope,
                platformSelectionBehaviors = platformSelectionBehaviors,
                clipboard = currentClipboard,
            )
        }
    val currentHapticFeedback = LocalHapticFeedback.current
    val currentTextToolbar = LocalTextToolbar.current

    val textToolbarHandler =
        remember(coroutineScope, currentTextToolbar) {
            object : TextToolbarHandler {
                override suspend fun showTextToolbar(
                    selectionState: TextFieldSelectionState,
                    rect: Rect,
                ) =
                    with(selectionState) {
                        selectionState.updateClipboardEntry()
                        currentTextToolbar.showMenu(
                            rect = rect,
                            onCopyRequested =
                                menuItem(canShowCopyMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        copy()
                                    }
                                },
                            onPasteRequested =
                                menuItem(canShowPasteMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        paste()
                                    }
                                },
                            onCutRequested =
                                menuItem(canShowCutMenuItem(), TextToolbarState.None) {
                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        cut()
                                    }
                                },
                            onSelectAllRequested =
                                menuItem(canShowSelectAllMenuItem(), TextToolbarState.Selection) {
                                    selectAll()
                                },
                            onAutofillRequested =
                                menuItem(canShowAutofillMenuItem(), TextToolbarState.None) {
                                    autofill()
                                },
                        )
                    }

                override fun hideTextToolbar() {
                    if (currentTextToolbar.status == TextToolbarStatus.Shown) {
                        currentTextToolbar.hide()
                    }
                }
            }
        }

    rememberClipboardEventsHandler(
        isEnabled = isFocused,
        onPaste = { textFieldSelectionState.onPasteEvent(it) },
        onCopy = { textFieldSelectionState.copyWithResult() },
        onCut = { textFieldSelectionState.cutWithResult() },
    )

    SideEffect {
        // These properties are not backed by snapshot state, so they can't be updated directly in
        // composition.
        transformedState.update(inputTransformation)

        textFieldSelectionState.update(
            hapticFeedBack = currentHapticFeedback,
            clipboard = currentClipboard,
            density = density,
            enabled = enabled,
            readOnly = readOnly,
            isPassword = isPassword,
            showTextToolbar = textToolbarHandler,
        )
    }

    DisposableEffect(textFieldSelectionState) { onDispose { textFieldSelectionState.dispose() } }

    val overscrollEffect = rememberTextFieldOverscrollEffect()

    val handwritingEnabled =
        !isPassword &&
            keyboardOptions.keyboardType != KeyboardType.Password &&
            keyboardOptions.keyboardType != KeyboardType.NumberPassword
    val decorationModifiers =
        modifier
            .stylusHandwriting(enabled, handwritingEnabled) {
                // If this is a password field, we can't trigger handwriting.
                // The expected behavior is 1) request focus 2) show software keyboard.
                // Note: TextField will show software keyboard automatically when it
                // gain focus. 3) show a toast message telling that handwriting is not
                // supported for password fields. TODO(b/335294152)
                if (handwritingEnabled) {
                    // Send the handwriting start signal to platform.
                    // The editor should send the signal when it is focused or is about
                    // to gain focus, Here are more details:
                    //   1) if the editor already has an active input session, the
                    //   platform handwriting service should already listen to this flow
                    //   and it'll start handwriting right away.
                    //
                    //   2) if the editor is not focused, but it'll be focused and
                    //   create a new input session, one handwriting signal will be
                    //   replayed when the platform collect this flow. And the platform
                    //   should trigger handwriting accordingly.
                    stylusHandwritingTrigger.tryEmit(Unit)
                }
            }
            .then(
                // semantics + some focus + input session + touch to focus
                TextFieldDecoratorModifier(
                    textFieldState = transformedState,
                    textLayoutState = textLayoutState,
                    textFieldSelectionState = textFieldSelectionState,
                    filter = inputTransformation,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = resolvedKeyboardOptions,
                    keyboardActionHandler = onKeyboardAction,
                    singleLine = singleLine,
                    interactionSource = interactionSource,
                    isPassword = isPassword,
                    stylusHandwritingTrigger = stylusHandwritingTrigger,
                )
            )
            .scrollable(
                state = scrollState,
                orientation = orientation,
                // Disable scrolling when textField is disabled or another dragging gesture is
                // taking place
                enabled =
                    enabled && textFieldSelectionState.directDragGestureInitiator == InputType.None,
                reverseDirection =
                    ScrollableDefaults.reverseDirection(
                        layoutDirection = layoutDirection,
                        orientation = orientation,
                        reverseScrolling = false,
                    ),
                interactionSource = interactionSource,
                overscrollEffect = overscrollEffect,
            )
            .pointerHoverIcon(PointerIcon.Text)
            .addContextMenuComponents(textFieldSelectionState, coroutineScope)

    Box(decorationModifiers, propagateMinConstraints = true) {
        ContextMenuArea(textFieldSelectionState, enabled) {
            val nonNullDecorator = decorator ?: DefaultTextFieldDecorator
            nonNullDecorator.Decoration {
                val minLines: Int
                val maxLines: Int
                if (lineLimits is MultiLine) {
                    minLines = lineLimits.minHeightInLines
                    maxLines = lineLimits.maxHeightInLines
                } else {
                    minLines = 1
                    maxLines = 1
                }

                Box(
                    propagateMinConstraints = true,
                    modifier =
                        Modifier.minHeightForSingleLineField(textLayoutState)
                            .heightInLines(
                                textStyle = textStyle,
                                minLines = minLines,
                                maxLines = maxLines,
                            )
                            .textFieldMinSize(textStyle)
                            .clipToBounds()
                            .then(
                                TextFieldCoreModifier(
                                    isFocused = isWindowAndTextFieldFocused,
                                    isDragHovered = isDragHovered,
                                    textLayoutState = textLayoutState,
                                    textFieldState = transformedState,
                                    textFieldSelectionState = textFieldSelectionState,
                                    cursorBrush = cursorBrush,
                                    writeable = enabled && !readOnly,
                                    scrollState = scrollState,
                                    orientation = orientation,
                                    toolbarRequester = toolbarRequester,
                                    platformSelectionBehaviors = platformSelectionBehaviors,
                                )
                            ),
                ) {
                    Box(
                        modifier =
                            TextFieldTextLayoutModifier(
                                textLayoutState = textLayoutState,
                                textFieldState = transformedState,
                                textStyle = textStyle,
                                singleLine = singleLine,
                                onTextLayout = onTextLayout,
                                keyboardOptions = resolvedKeyboardOptions,
                            )
                    )

                    if (
                        enabled &&
                            isWindowAndTextFieldFocused &&
                            textFieldSelectionState.isInTouchMode
                    ) {
                        TextFieldSelectionHandles(selectionState = textFieldSelectionState)
                        if (!readOnly) {
                            TextFieldCursorHandle(selectionState = textFieldSelectionState)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.minHeightForSingleLineField(textLayoutState: TextLayoutState) =
    if (ComposeFoundationFlags.isBasicTextFieldMinSizeOptimizationEnabled) {
        layout { measurable, constraints ->
            val wrappedConstraints =
                constraints.constrain(
                    Constraints(
                        minWidth = 0,
                        maxWidth = Constraints.Infinity,
                        minHeight = textLayoutState.minHeightForSingleLineField.roundToPx(),
                        maxHeight = Constraints.Infinity,
                    )
                )
            val placeable = measurable.measure(wrappedConstraints)
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
    } else {
        heightIn(min = textLayoutState.minHeightForSingleLineField)
    }

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.addContextMenuComponents(
    textFieldSelectionState: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier =
    if (ComposeFoundationFlags.isNewContextMenuEnabled)
        addBasicTextFieldTextContextMenuComponents(textFieldSelectionState, coroutineScope)
    else this

@Composable
internal fun TextFieldCursorHandle(selectionState: TextFieldSelectionState) {
    // Does not recompose if only position of the handle changes.
    val cursorHandleVisible by
        remember(selectionState) {
            derivedStateOf { selectionState.getCursorHandleState(includePosition = false).visible }
        }
    if (cursorHandleVisible) {
        CursorHandle(
            offsetProvider = {
                selectionState.getCursorHandleState(includePosition = true).position
            },
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { cursorHandleGestures() }
                },
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

@Composable
internal fun TextFieldSelectionHandles(selectionState: TextFieldSelectionState) {
    // Does not recompose if only position of the handle changes.
    val startHandleState by
        remember(selectionState) {
            derivedStateOf {
                selectionState.getSelectionHandleState(
                    isStartHandle = true,
                    includePosition = false,
                )
            }
        }
    // Read once here to avoid repeating derived state reads
    val startHandle = startHandleState
    if (startHandle.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = true, includePosition = true)
                    .position
            },
            isStartHandle = true,
            direction = startHandle.direction,
            handlesCrossed = startHandle.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(true) }
                },
            lineHeight = startHandle.lineHeight,
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }

    // Does not recompose if only position of the handle changes.
    val endHandleState by
        remember(selectionState) {
            derivedStateOf {
                selectionState.getSelectionHandleState(
                    isStartHandle = false,
                    includePosition = false,
                )
            }
        }
    // Read once here to avoid repeating derived state reads
    val endHandle = endHandleState
    if (endHandle.visible) {
        SelectionHandle(
            offsetProvider = {
                selectionState
                    .getSelectionHandleState(isStartHandle = false, includePosition = true)
                    .position
            },
            isStartHandle = false,
            direction = endHandle.direction,
            handlesCrossed = endHandle.handlesCrossed,
            modifier =
                Modifier.pointerInput(selectionState) {
                    with(selectionState) { selectionHandleGestures(false) }
                },
            lineHeight = endHandle.lineHeight,
            minTouchTargetSize = MinTouchTargetSizeForHandles,
        )
    }
}

private val DefaultTextFieldDecorator = TextFieldDecorator { it() }

/**
 * Defines a minimum touch target area size for Selection and Cursor handles.
 *
 * Although BasicTextField is not part of Material spec, this accessibility feature is important
 * enough to be included at foundation layer, and also TextField cannot change selection handles
 * provided by BasicTextField to somehow achieve this accessibility requirement.
 *
 * This value is adopted from Android platform's TextView implementation.
 */
private val MinTouchTargetSizeForHandles = DpSize(40.dp, 40.dp)

/**
 * Basic composable that enables users to edit text via hardware or software keyboard, but provides
 * no decorations like hint or placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [String] with which developer is expected to update their state.
 *
 * Unlike [TextFieldValue] overload, this composable does not let the developer control selection,
 * cursor and text composition information. Please check [TextFieldValue] and corresponding
 * [BasicTextField] overload for more information.
 *
 * It is crucial that the value provided to the [onValueChange] is fed back into [BasicTextField] in
 * order to actually display and continue to edit that text in the field. The value you feed back
 * into the field may be different than the one provided to the [onValueChange] callback, however
 * the following caveats apply:
 * - The new value must be provided to [BasicTextField] immediately (i.e. by the next frame), or the
 *   text field may appear to glitch, e.g. the cursor may jump around. For more information about
 *   this requirement, see
 *   [this article](https://developer.android.com/jetpack/compose/text/user-input#state-practices).
 * - The value fed back into the field may be different from the one passed to [onValueChange],
 *   although this may result in the input connection being restarted, which can make the keyboard
 *   flicker for the user. This is acceptable when you're using the callback to, for example, filter
 *   out certain types of input, but should probably not be done on every update when entering
 *   freeform text.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldWithStringSample
 *
 * For example, if you need to include a placeholder in your TextField, you can write a composable
 * using the decoration box like this:
 *
 * @sample androidx.compose.foundation.samples.PlaceholderBasicTextFieldSample
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decoration box:
 *
 * @sample androidx.compose.foundation.samples.TextFieldWithIconSample
 *
 * In order to create formatted text field, for example for entering a phone number or a social
 * security number, use a [visualTransformation] parameter. Below is the example of the text field
 * for entering a credit card number:
 *
 * @sample androidx.compose.foundation.samples.CreditCardSample
 *
 * Note: This overload does not support [KeyboardOptions.showKeyboardOnFocus].
 *
 * @param value the input [String] text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are automatically
 *   set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param visualTransformation The visual transformation filter for changing the visual
 *   representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such as
 *   icon, placeholder, helper messages or similar, and automatically increase the hit target area
 *   of the text field. To allow you to control the placement of the inner text field relative to
 *   your decorations, the text field implementation will pass in a framework-controlled composable
 *   parameter "innerTextField" to the decorationBox lambda you provide. You must call
 *   innerTextField exactly once.
 */
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the CoreTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(text = value)

    SideEffect {
        if (
            textFieldValue.selection != textFieldValueState.selection ||
                textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // CoreTextField's onValueChange is called multiple times without recomposition in between.
    var lastTextValue by remember(value) { mutableStateOf(value) }

    CoreTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        imeOptions = keyboardOptions.toImeOptions(singleLine = singleLine),
        keyboardActions = keyboardActions,
        softWrap = !singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        decorationBox = decorationBox,
        enabled = enabled,
        readOnly = readOnly,
    )
}

/**
 * Basic composable that enables users to edit text via hardware or software keyboard, but provides
 * no decorations like hint or placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well as
 * selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided to the [onValueChange] is fed back into [BasicTextField] in
 * order to actually display and continue to edit that text in the field. The value you feed back
 * into the field may be different than the one provided to the [onValueChange] callback, however
 * the following caveats apply:
 * - The new value must be provided to [BasicTextField] immediately (i.e. by the next frame), or the
 *   text field may appear to glitch, e.g. the cursor may jump around. For more information about
 *   this requirement, see
 *   [this article](https://developer.android.com/jetpack/compose/text/user-input#state-practices).
 * - The value fed back into the field may be different from the one passed to [onValueChange],
 *   although this may result in the input connection being restarted, which can make the keyboard
 *   flicker for the user. This is acceptable when you're using the callback to, for example, filter
 *   out certain types of input, but should probably not be done on every update when entering
 *   freeform text.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldSample
 *
 * For example, if you need to include a placeholder in your TextField, you can write a composable
 * using the decoration box like this:
 *
 * @sample androidx.compose.foundation.samples.PlaceholderBasicTextFieldSample
 *
 * If you want to add decorations to your text field, such as icon or similar, and increase the hit
 * target area, use the decoration box:
 *
 * @sample androidx.compose.foundation.samples.TextFieldWithIconSample
 *
 * Note: This overload does not support [KeyboardOptions.showKeyboardOnFocus].
 *
 * @param value The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the
 *   [BasicTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicTextField]. When `false`, the text field
 *   will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicTextField]. When `true`, the text field
 *   can not be modified, however, a user can focus it and copy text from it. Read-only text fields
 *   are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are automatically
 *   set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param visualTransformation The visual transformation filter for changing the visual
 *   representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 *   provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such as
 *   icon, placeholder, helper messages or similar, and automatically increase the hit target area
 *   of the text field. To allow you to control the placement of the inner text field relative to
 *   your decorations, the text field implementation will pass in a framework-controlled composable
 *   parameter "innerTextField" to the decorationBox lambda you provide. You must call
 *   innerTextField exactly once.
 */
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    CoreTextField(
        value = value,
        onValueChange = {
            if (value != it) {
                onValueChange(it)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        imeOptions = keyboardOptions.toImeOptions(singleLine = singleLine),
        keyboardActions = keyboardActions,
        softWrap = !singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines,
        decorationBox = decorationBox,
        enabled = enabled,
        readOnly = readOnly,
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = 1,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = 1,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/BasicText.kt
```kotlin
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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.modifiers.SelectableTextAnnotatedStringElement
import androidx.compose.foundation.text.modifiers.SelectionController
import androidx.compose.foundation.text.modifiers.TextAnnotatedStringElement
import androidx.compose.foundation.text.modifiers.TextAnnotatedStringNode
import androidx.compose.foundation.text.modifiers.TextStringSimpleElement
import androidx.compose.foundation.text.modifiers.hasLinks
import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Constraints.Companion.fitPrioritizingWidth
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapIndexedNotNull
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.floor

/**
 * Basic element that displays text and provides semantics / accessibility information. Typically
 * you will instead want to use [androidx.compose.material.Text], which is a higher level Text
 * element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param color Overrides the text color provided in [style]
 * @param autoSize Enable auto sizing for this text composable. Finds the biggest font size that
 *   fits in the available space and lays the text out with this size. This performs multiple layout
 *   passes and can be slower than using a fixed font size. This takes precedence over sizes defined
 *   through [style]. See [TextAutoSize] and
 *   [androidx.compose.foundation.samples.TextAutoSizeBasicTextSample].
 */
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    validateMinMaxLines(minLines = minLines, maxLines = maxLines)
    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController =
        if (selectionRegistrar != null) {
            val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
            val selectableId =
                rememberSaveable(selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
                    selectionRegistrar.nextSelectableId()
                }
            remember(selectableId, selectionRegistrar, backgroundSelectionColor) {
                SelectionController(selectableId, selectionRegistrar, backgroundSelectionColor)
            }
        } else {
            null
        }

    val fontFamilyResolver = LocalFontFamilyResolver.current

    BackgroundTextMeasurement(text = text, style = style, fontFamilyResolver = fontFamilyResolver)

    val finalModifier =
        if (selectionController != null || onTextLayout != null || autoSize != null) {
            modifier.textModifier(
                AnnotatedString(text = text),
                style = style,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                fontFamilyResolver = LocalFontFamilyResolver.current,
                placeholders = null,
                onPlaceholderLayout = null,
                selectionController = selectionController,
                color = color,
                onShowTranslation = null,
                autoSize = autoSize,
            )
        } else {
            modifier then
                TextStringSimpleElement(
                    text = text,
                    style = style,
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    color = color,
                )
        }
    Layout(finalModifier, EmptyMeasurePolicy)
}

/**
 * Basic element that displays text and provides semantics / accessibility information. Typically
 * you will instead want to use [androidx.compose.material.Text], which is a higher level Text
 * element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's used
 *   to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param color Overrides the text color provided in [style]
 * @param autoSize Enable auto sizing for this text composable. Finds the biggest font size that
 *   fits in the available space and lays the text out with this size. This performs multiple layout
 *   passes and can be slower than using a fixed font size. This takes precedence over sizes defined
 *   through [style]. See [TextAutoSize] and
 *   [androidx.compose.foundation.samples.TextAutoSizeBasicTextSample].
 */
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    validateMinMaxLines(minLines = minLines, maxLines = maxLines)
    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController =
        if (selectionRegistrar != null) {
            val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
            val selectableId =
                rememberSaveable(selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
                    selectionRegistrar.nextSelectableId()
                }
            remember(selectableId, selectionRegistrar, backgroundSelectionColor) {
                SelectionController(selectableId, selectionRegistrar, backgroundSelectionColor)
            }
        } else {
            null
        }
    val hasInlineContent = text.hasInlineContent()
    val hasLinks = text.hasLinks()

    val fontFamilyResolver = LocalFontFamilyResolver.current

    if (!hasInlineContent && !hasLinks) {
        BackgroundTextMeasurement(
            text = text,
            style = style,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = null,
        )

        // this is the same as text: String, use all the early exits
        Layout(
            modifier =
                modifier.textModifier(
                    text = text,
                    style = style,
                    onTextLayout = onTextLayout,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    fontFamilyResolver = fontFamilyResolver,
                    placeholders = null,
                    onPlaceholderLayout = null,
                    selectionController = selectionController,
                    color = color,
                    onShowTranslation = null,
                    autoSize = autoSize,
                ),
            EmptyMeasurePolicy,
        )
    } else {
        // takes into account text substitution (for translation) that is happening inside the
        // TextAnnotatedStringNode
        var displayedText by remember(text) { mutableStateOf(text) }

        LayoutWithLinksAndInlineContent(
            modifier = modifier,
            text = displayedText,
            onTextLayout = onTextLayout,
            hasInlineContent = hasInlineContent,
            inlineContent = inlineContent,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            fontFamilyResolver = fontFamilyResolver,
            selectionController = selectionController,
            color = color,
            onShowTranslation = { substitutionValue ->
                displayedText =
                    if (substitutionValue.isShowingSubstitution) {
                        substitutionValue.substitution
                    } else {
                        substitutionValue.original
                    }
            },
            autoSize = autoSize,
        )
    }
}

/**
 * Basic element that displays text and provides semantics / accessibility information. Typically
 * you will instead want to use [androidx.compose.material.Text], which is a higher level Text
 * element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param color Overrides the text color provided in [style]
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
) {
    BasicText(text, modifier, style, onTextLayout, overflow, softWrap, maxLines, minLines, color)
}

/**
 * Basic element that displays text and provides semantics / accessibility information. Typically
 * you will instead want to use [androidx.compose.material.Text], which is a higher level Text
 * element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's used
 *   to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param color Overrides the text color provided in [style]
 */
@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null,
) {
    BasicText(
        text,
        modifier,
        style,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        inlineContent,
        color,
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        minLines = 1,
        maxLines = maxLines,
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = 1,
        inlineContent = inlineContent,
    )
}

@Deprecated("Maintained for binary compat", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) = BasicText(text, modifier, style, onTextLayout, overflow, softWrap, maxLines, minLines)

@Deprecated("Maintained for binary compat", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) =
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
    )

/** A custom saver that won't save if no selection is active. */
private fun selectionIdSaver(selectionRegistrar: SelectionRegistrar?) =
    Saver<Long, Long>(
        save = { if (selectionRegistrar.hasSelection(it)) it else null },
        restore = { it },
    )

private object EmptyMeasurePolicy : MeasurePolicy {
    private val placementBlock: Placeable.PlacementScope.() -> Unit = {}

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight, placementBlock = placementBlock)
    }
}

/** Measure policy for inline content and links */
private class TextMeasurePolicy(
    private val shouldMeasureLinks: () -> Boolean,
    private val placements: () -> List<Rect?>?,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        // inline content
        val inlineContentMeasurables =
            measurables.fastFilter { it.parentData !is TextRangeLayoutModifier }
        val inlineContentToPlace =
            placements()?.fastMapIndexedNotNull { index, rect ->
                // PlaceholderRect will be null if it's ellipsized. In that case, the corresponding
                // inline children won't be measured or placed.
                rect?.let {
                    Pair(
                        inlineContentMeasurables[index].measure(
                            Constraints(
                                maxWidth = floor(it.width).toInt(),
                                maxHeight = floor(it.height).toInt(),
                            )
                        ),
                        IntOffset(it.left.fastRoundToInt(), it.top.fastRoundToInt()),
                    )
                }
            }

        // links
        val linksMeasurables = measurables.fastFilter { it.parentData is TextRangeLayoutModifier }
        val linksToPlace =
            measureWithTextRangeMeasureConstraints(
                measurables = linksMeasurables,
                shouldMeasureLinks = shouldMeasureLinks,
            )

        return layout(constraints.maxWidth, constraints.maxHeight) {
            // inline content
            inlineContentToPlace?.fastForEach { (placeable, position) -> placeable.place(position) }
            // links
            linksToPlace?.fastForEach { (placeable, measureResult) ->
                placeable.place(measureResult?.invoke() ?: IntOffset.Zero)
            }
        }
    }
}

/** Measure policy for links only */
private class LinksTextMeasurePolicy(private val shouldMeasureLinks: () -> Boolean) :
    MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight) {
            val linksToPlace =
                measureWithTextRangeMeasureConstraints(
                    measurables = measurables,
                    shouldMeasureLinks = shouldMeasureLinks,
                )
            linksToPlace?.fastForEach { (placeable, measureResult) ->
                placeable.place(measureResult?.invoke() ?: IntOffset.Zero)
            }
        }
    }
}

private fun measureWithTextRangeMeasureConstraints(
    measurables: List<Measurable>,
    shouldMeasureLinks: () -> Boolean,
): List<Pair<Placeable, (() -> IntOffset)?>>? {
    return if (shouldMeasureLinks()) {
        val textRangeLayoutMeasureScope = TextRangeLayoutMeasureScope()
        measurables.fastMapIndexedNotNull { _, measurable ->
            val rangeMeasurePolicy =
                (measurable.parentData as TextRangeLayoutModifier).measurePolicy
            val rangeMeasureResult =
                with(rangeMeasurePolicy) { textRangeLayoutMeasureScope.measure() }
            val placeable =
                measurable.measure(
                    fitPrioritizingWidth(
                        minWidth = rangeMeasureResult.width,
                        maxWidth = rangeMeasureResult.width,
                        minHeight = rangeMeasureResult.height,
                        maxHeight = rangeMeasureResult.height,
                    )
                )
            Pair(placeable, rangeMeasureResult.place)
        }
    } else {
        null
    }
}

private fun Modifier.textModifier(
    text: AnnotatedString,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
    onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
    selectionController: SelectionController?,
    color: ColorProducer?,
    onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)?,
    autoSize: TextAutoSize?,
): Modifier {
    if (selectionController == null) {
        val staticTextModifier =
            TextAnnotatedStringElement(
                text,
                style,
                fontFamilyResolver,
                onTextLayout,
                overflow,
                softWrap,
                maxLines,
                minLines,
                placeholders,
                onPlaceholderLayout,
                null,
                color,
                autoSize,
                onShowTranslation,
            )
        return this then Modifier /* selection position */ then staticTextModifier
    } else {
        val selectableTextModifier =
            SelectableTextAnnotatedStringElement(
                text,
                style,
                fontFamilyResolver,
                onTextLayout,
                overflow,
                softWrap,
                maxLines,
                minLines,
                placeholders,
                onPlaceholderLayout,
                selectionController,
                color,
                autoSize,
            )
        return this then selectionController.modifier then selectableTextModifier
    }
}

@Composable
private fun LayoutWithLinksAndInlineContent(
    modifier: Modifier,
    text: AnnotatedString,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    hasInlineContent: Boolean,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    style: TextStyle,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    fontFamilyResolver: FontFamily.Resolver,
    selectionController: SelectionController?,
    color: ColorProducer?,
    onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)?,
    autoSize: TextAutoSize?,
) {

    val textScope =
        if (text.hasLinks()) {
            remember(text) { TextLinkScope(text) }
        } else null

    // only adds additional span styles to the existing link annotations, doesn't semantically
    // change the text
    val styledText: () -> AnnotatedString =
        if (text.hasLinks()) {
            remember(text, textScope) { { textScope?.applyAnnotators() ?: text } }
        } else {
            { text }
        }

    // do the inline content allocs
    val (placeholders, inlineComposables) =
        if (hasInlineContent) {
            text.resolveInlineContent(inlineContent = inlineContent)
        } else Pair(null, null)

    val measuredPlaceholderPositions =
        if (hasInlineContent) {
            remember<MutableState<List<Rect?>?>> { mutableStateOf(null) }
        } else null

    val onPlaceholderLayout: ((List<Rect?>) -> Unit)? =
        if (hasInlineContent) {
            { measuredPlaceholderPositions?.value = it }
        } else null

    BackgroundTextMeasurement(
        text = text,
        style = style,
        fontFamilyResolver = fontFamilyResolver,
        placeholders = placeholders,
    )

    Layout(
        content = {
            textScope?.LinksComposables()
            inlineComposables?.let { InlineChildren(text = text, inlineContents = it) }
        },
        modifier =
            modifier.textModifier(
                text = styledText(),
                style = style,
                onTextLayout = {
                    textScope?.textLayoutResult = it
                    onTextLayout?.invoke(it)
                },
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = placeholders,
                onPlaceholderLayout = onPlaceholderLayout,
                selectionController = selectionController,
                color = color,
                onShowTranslation = onShowTranslation,
                autoSize = autoSize,
            ),
        measurePolicy =
            if (!hasInlineContent) {
                LinksTextMeasurePolicy(
                    shouldMeasureLinks = { textScope?.let { it.shouldMeasureLinks() } ?: false }
                )
            } else {
                TextMeasurePolicy(
                    shouldMeasureLinks = { textScope?.let { it.shouldMeasureLinks() } ?: false },
                    placements = { measuredPlaceholderPositions?.value },
                )
            },
    )
}

/**
 * This function pre-measures the text on Android platform to warm the platform text layout cache in
 * a background thread before the actual text layout begins.
 */
@Composable
@NonRestartableComposable
internal expect fun BackgroundTextMeasurement(
    text: String,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
)

/**
 * This function pre-measures the text on Android platform to warm the platform text layout cache in
 * a background thread before the actual text layout begins.
 */
@Composable
@NonRestartableComposable
internal expect fun BackgroundTextMeasurement(
    text: AnnotatedString,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
)
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/Draggable.kt
```kotlin
/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ComposeFoundationFlags.isDelayPressesUsingGestureConsumptionEnabled
import androidx.compose.foundation.ComposeFoundationFlags.isNestedDraggablesTouchConflictFixEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.GestureConnection
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestureNode
import androidx.compose.foundation.gestures.DragEvent.DragCancelled
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.gestures.DragEvent.DragStarted
import androidx.compose.foundation.gestures.DragEvent.DragStopped
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.parentGestureConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * State of [draggable]. Allows for a granular control of how deltas are consumed by the user as
 * well as to write custom drag methods using [drag] suspend function.
 */
@JvmDefaultWithCompatibility
interface DraggableState {
    /**
     * Call this function to take control of drag logic.
     *
     * All actions that change the logical drag position must be performed within a [drag] block
     * (even if they don't call any other methods on this object) in order to guarantee that mutual
     * exclusion is enforced.
     *
     * If [drag] is called from elsewhere with the [dragPriority] higher or equal to ongoing drag,
     * ongoing drag will be canceled.
     *
     * @param dragPriority of the drag operation
     * @param block to perform drag in
     */
    suspend fun drag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend DragScope.() -> Unit,
    )

    /**
     * Dispatch drag delta in pixels avoiding all drag related priority mechanisms.
     *
     * **NOTE:** unlike [drag], dispatching any delta with this method will bypass scrolling of any
     * priority. This method will also ignore `reverseDirection` and other parameters set in
     * [draggable].
     *
     * This method is used internally for low level operations, allowing implementers of
     * [DraggableState] influence the consumption as suits them, e.g. introduce nested scrolling.
     * Manually dispatching delta via this method will likely result in a bad user experience, you
     * must prefer [drag] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested drag process
     */
    fun dispatchRawDelta(delta: Float)
}

/** Scope used for suspending drag blocks */
interface DragScope {
    /** Attempts to drag by [pixels] px. */
    fun dragBy(pixels: Float)
}

/**
 * Default implementation of [DraggableState] interface that allows to pass a simple action that
 * will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [DraggableState.drag] call) with the delta in pixels.
 *
 * If you are creating [DraggableState] in composition, consider using [rememberDraggableState].
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
fun DraggableState(onDelta: (Float) -> Unit): DraggableState = DefaultDraggableState(onDelta)

/**
 * Create and remember default implementation of [DraggableState] interface that allows to pass a
 * simple action that will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [DraggableState.drag] call) with the delta in pixels.
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Composable
fun rememberDraggableState(onDelta: (Float) -> Unit): DraggableState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { DraggableState { onDeltaState.value.invoke(it) } }
}

/**
 * Configure touch dragging for the UI element in a single [Orientation]. The drag distance reported
 * to [DraggableState], allowing users to react on the drag delta and update their state.
 *
 * The common usecase for this component is when you need to be able to drag something inside the
 * component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [pointerInput] instead with the
 * helper functions like [detectDragGestures].
 *
 * If you want to enable dragging in 2 dimensions, consider using [draggable2D].
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.compose.foundation.samples.DraggableSample
 * @param state [DraggableState] state of the draggable. Defines how drag events will be interpreted
 *   by the user land logic.
 * @param orientation orientation of the drag
 * @param enabled whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [DragInteraction.Start] when this draggable is being dragged.
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 *   prevent other gesture detectors from reacting to "down" events (in order to block composed
 *   press-based gestures). This is intended to allow end users to "catch" an animating widget by
 *   pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag is about to start at the starting
 *   position, allowing user to suspend and perform preparation for drag, if desired. This suspend
 *   function is invoked with the draggable scope, allowing for async processing, if desired. Note
 *   that the scope used here is the one provided by the draggable node, for long running work that
 *   needs to outlast the modifier being in the composition you should use a scope that fits the
 *   lifecycle needed.
 * @param onDragStopped callback that will be invoked when drag is finished, allowing the user to
 *   react on velocity and process it. This suspend function is invoked with the draggable scope,
 *   allowing for async processing, if desired. Note that the scope used here is the one provided by
 *   the draggable node, for long running work that needs to outlast the modifier being in the
 *   composition you should use a scope that fits the lifecycle needed.
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 */
@Stable
fun Modifier.draggable(
    state: DraggableState,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = NoOpOnDragStarted,
    onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = NoOpOnDragStopped,
    reverseDirection: Boolean = false,
): Modifier =
    this then
        DraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = onDragStarted,
            onDragStopped = onDragStopped,
            reverseDirection = reverseDirection,
        )

internal class DraggableElement(
    private val state: DraggableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean,
    private val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private val reverseDirection: Boolean,
) : ModifierNodeElement<DraggableNode>() {
    override fun create(): DraggableNode =
        DraggableNode(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            startDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection,
        )

    override fun update(node: DraggableNode) {
        node.update(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            startDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as DraggableElement

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (onDragStarted != other.onDragStarted) return false
        if (onDragStopped != other.onDragStopped) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + onDragStarted.hashCode()
        result = 31 * result + onDragStopped.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "draggable"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["state"] = state
    }

    companion object {
        val CanDrag: (PointerType) -> Boolean = { true }
    }
}

internal class DraggableNode(
    private var state: DraggableState,
    canDrag: (PointerType) -> Boolean,
    private var orientation: Orientation,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    private var startDragImmediately: Boolean,
    private var onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private var onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private var reverseDirection: Boolean,
) :
    DragGestureNode(
        canDrag = canDrag,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = orientation,
    ) {

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.drag(MutatePriority.UserInput) {
            forEachDelta { dragDelta ->
                dragBy(dragDelta.delta.reverseIfNeeded().toFloat(orientation))
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {
        if (!isAttached || onDragStarted == NoOpOnDragStarted) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStarted(this, startedPosition)
        }
    }

    override fun onDragStopped(event: DragStopped) {
        if (!isAttached || onDragStopped == NoOpOnDragStopped) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStopped(
                this,
                event.velocity.reverseIfNeeded().toFloat(orientation),
            )
        }
    }

    override fun startDragImmediately(): Boolean = startDragImmediately

    fun update(
        state: DraggableState,
        canDrag: (PointerType) -> Boolean,
        orientation: Orientation,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        startDragImmediately: Boolean,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
        reverseDirection: Boolean,
    ) {
        var resetPointerInputHandling = false
        if (this.state != state) {
            this.state = state
            resetPointerInputHandling = true
        }
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }

        this.onDragStarted = onDragStarted
        this.onDragStopped = onDragStopped
        this.startDragImmediately = startDragImmediately

        update(canDrag, enabled, interactionSource, orientation, resetPointerInputHandling)
    }

    private fun Velocity.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f

    private fun Offset.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f
}

/** A node that performs drag gesture recognition and event propagation. */
@OptIn(ExperimentalFoundationApi::class)
internal abstract class DragGestureNode(
    canDrag: (PointerType) -> Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    var orientationLock: Orientation?,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    IndirectPointerInputModifierNode,
    CompositionLocalConsumerModifierNode,
    GestureConnection {

    var canDrag = canDrag
        private set

    protected var enabled = enabled
        private set

    protected var interactionSource = interactionSource
        private set

    private var gestureNode: DelegatableNode? = null

    // Use wrapper lambdas here to make sure that if these properties are updated while we suspend,
    // we point to the new reference when we invoke them. startDragImmediately is a lambda since we
    // need the most recent value passed to it from Scrollable.
    private val _canDrag: (PointerType) -> Boolean = { this.canDrag(it) }
    private var channel: Channel<DragEvent>? = null
    private var dragInteraction: DragInteraction.Start? = null
    internal var isListeningForEvents = false
    internal var isListeningForPointerInputEvents = false

    /** Store non-initialized states for re-use */
    private var _awaitDownState: DragDetectionState.AwaitDown? = null
    private val awaitDownState: DragDetectionState.AwaitDown
        get() = _awaitDownState ?: DragDetectionState.AwaitDown().also { _awaitDownState = it }

    private var _draggingState: DragDetectionState.Dragging? = null
    private val draggingState: DragDetectionState.Dragging
        get() = _draggingState ?: DragDetectionState.Dragging().also { _draggingState = it }

    private var _awaitTouchSlopState: DragDetectionState.AwaitTouchSlop? = null
    private val awaitTouchSlopState: DragDetectionState.AwaitTouchSlop
        get() =
            _awaitTouchSlopState
                ?: DragDetectionState.AwaitTouchSlop().also { _awaitTouchSlopState = it }

    private var _awaitGesturePickupState: DragDetectionState.AwaitGesturePickup? = null
    private val awaitGesturePickupState: DragDetectionState.AwaitGesturePickup
        get() =
            _awaitGesturePickupState
                ?: DragDetectionState.AwaitGesturePickup().also { _awaitGesturePickupState = it }

    private var currentDragState: DragDetectionState? = null
    private var velocityTracker: VelocityTracker? = null
    private var previousPositionOnScreen = Offset.Unspecified
    private var touchSlopDetector: TouchSlopDetector? = null
    private var indirectPointerInputDragCycleDetector: IndirectPointerInputDragCycleDetector? = null

    /**
     * Accumulated position offset of this [Modifier.Node] that happened during a drag cycle. This
     * is used to correct the pointer input events that are added to the Velocity Tracker. If this
     * Node is static during the drag cycle, nothing will happen. On the other hand, if the position
     * of this node changes during the drag cycle, we need to correct the Pointer Input used for the
     * drag events, this is because Velocity Tracker doesn't have the knowledge about changes in the
     * position of the container that uses it, and because each Pointer Input event is related to
     * the container's root.
     */
    private var nodeOffset = Offset.Zero

    /**
     * Responsible for the dragging behavior between the start and the end of the drag. It
     * continually invokes `forEachDelta` to process incoming events. In return, `forEachDelta`
     * calls `dragBy` method to process each individual delta.
     */
    abstract suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit)

    /**
     * Passes the action needed when a drag starts. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStarted(startedPosition: Offset)

    /**
     * Passes the action needed when a drag stops. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStopped(event: DragStopped)

    /**
     * If touch slop recognition should be skipped. If this is true, this node will start
     * recognizing drag events immediately without waiting for touch slop.
     */
    abstract fun startDragImmediately(): Boolean

    private fun requireVelocityTracker(): VelocityTracker =
        requireNotNull(velocityTracker) { "Velocity Tracker not initialized." }

    private fun requireChannel(): Channel<DragEvent> =
        requireNotNull(channel) { "Events channel not initialized." }

    private fun requireTouchSlopDetector(): TouchSlopDetector =
        requireNotNull(touchSlopDetector) { "Touch slop detector not initialized." }

    @OptIn(ExperimentalFoundationApi::class)
    private fun startListeningForEvents() {
        isListeningForEvents = true

        if (channel == null) {
            channel = Channel(capacity = Channel.UNLIMITED)
        }

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of
         * using the one provided by the pointer input modifier, this is to ensure that even when
         * the pointer input scope is reset we will continue any coroutine scope scope that we
         * started from these methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel?.receive()
                if (event !is DragStarted) continue
                processDragStart(event)
                try {
                    drag { processDelta ->
                        while (event !is DragStopped && event !is DragCancelled) {
                            (event as? DragDelta)?.let(processDelta)
                            event = channel?.receive()
                        }
                    }
                    if (event is DragStopped) {
                        processDragStop(event as DragStopped)
                    } else if (event is DragCancelled) {
                        processDragCancel()
                    }
                } catch (c: CancellationException) {
                    processDragCancel()
                }
            }
        }
    }

    override fun onDetach() {
        isListeningForEvents = false
        disposeInteractionSource()
        nodeOffset = Offset.Zero

        gestureNode?.let { undelegate(it) }
        gestureNode = null
    }

    protected fun initializeGestureCoordination() {
        if (!isDelayPressesUsingGestureConsumptionEnabled) return
        if (gestureNode == null) {
            gestureNode = delegate(gestureNode(this))
        }
    }

    @OptIn(ExperimentalIndirectPointerApi::class)
    override fun isInterested(event: IndirectPointerInputChange): Boolean {
        // for now, if this is a down event it may become a drag so we're
        // interested.
        return event.changedToDownIgnoreConsumed() && enabled
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        isListeningForPointerInputEvents = true
        initializeGestureCoordination()
        if (enabled) {
            // initialize current state
            if (currentDragState == null) currentDragState = awaitDownState
            processRawPointerEvent(pointerEvent, pass)
        }
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        initializeGestureCoordination()
        if (enabled) {
            if (indirectPointerInputDragCycleDetector == null) {
                indirectPointerInputDragCycleDetector = IndirectPointerInputDragCycleDetector(this)
            }
            indirectPointerInputDragCycleDetector?.processIndirectPointerInputEvent(event, pass)
        }
    }

    override fun onCancelIndirectPointerInput() {
        indirectPointerInputDragCycleDetector?.resetDragDetectionState()
    }

    /**
     * Draggable containers will be interested in the following events:
     * 1) DOWN events. They may become a drag gesture later.
     * 2) The touch slop trigger event if the preceding deltas form an angle of interest. The touch
     *    slop trigger event is when, effectively, draggables will start consuming. So at this
     *    point, we look at the collected deltas since the first down event, and we decide if we're
     *    interested based on the angle that those deltas form. We will favor vertical drags over
     *    horizontal drags more because UX-wise there's more freedom and uncertainty when a user
     *    performs a vertical gesture vs. a horizontal gesture.
     */
    override fun isInterested(event: PointerInputChange): Boolean {
        if (event.changedToDownIgnoreConsumed()) return enabled
        if (!isNestedDraggablesTouchConflictFixEnabled) return false
        if (event.changedToUpIgnoreConsumed()) return false

        if (touchSlopDetector == null) {
            touchSlopDetector = TouchSlopDetector(orientationLock)
        }

        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        val positionChange = event.positionChange()

        return with(requireTouchSlopDetector()) {
            getPostSlopOffset(positionChange, touchSlop, false) != Offset.Unspecified &&
                isDeltaAtAngleOfInterest(positionChange)
        }
    }

    override fun onCancelPointerInput() {
        if (isListeningForPointerInputEvents) resetDragDetectionState()
        isListeningForPointerInputEvents = false
    }

    private suspend fun processDragStart(event: DragStarted) {
        dragInteraction?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragInteraction = interaction
        onDragStarted(event.startPoint)
    }

    private suspend fun processDragStop(event: DragStopped) {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragInteraction = null
        }
        onDragStopped(event)
    }

    private suspend fun processDragCancel() {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
        onDragStopped(DragStopped(Velocity.Zero, isIndirectPointerEvent = false))
    }

    fun disposeInteractionSource() {
        dragInteraction?.let { interaction ->
            interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
    }

    fun update(
        canDrag: (PointerType) -> Boolean = this.canDrag,
        enabled: Boolean = this.enabled,
        interactionSource: MutableInteractionSource? = this.interactionSource,
        orientationLock: Orientation? = this.orientationLock,
        shouldResetPointerInputHandling: Boolean = false,
    ) {
        var resetPointerInputHandling = shouldResetPointerInputHandling

        this.canDrag = canDrag
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                disposeInteractionSource()
                indirectPointerInputDragCycleDetector = null
            }
            resetPointerInputHandling = true
        }
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }

        if (this.orientationLock != orientationLock) {
            this.orientationLock = orientationLock
            resetPointerInputHandling = true
        }

        if (resetPointerInputHandling) {
            if (isListeningForPointerInputEvents) resetDragDetectionState()
            indirectPointerInputDragCycleDetector?.resetDragDetectionState()
        }
    }

    private fun processRawPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass) {
        when (
            val state = requireNotNull(currentDragState) { "currentDragState should not be null" }
        ) {
            is DragDetectionState.AwaitDown -> processInitialDownState(pointerEvent, pass, state)
            is DragDetectionState.AwaitTouchSlop -> processAwaitTouchSlop(pointerEvent, pass, state)
            is DragDetectionState.AwaitGesturePickup ->
                processAwaitGesturePickup(pointerEvent, pass, state)

            is DragDetectionState.Dragging -> processDraggingState(pointerEvent, pass, state)
        }
    }

    private fun resetDragDetectionState() {
        moveToAwaitDownState()
        if (isListeningForEvents) sendDragCancelled()
        velocityTracker = null
    }

    private fun moveToAwaitTouchSlopState(
        initialDown: PointerInputChange,
        pointerId: PointerId,
        initialTouchSlopPositionChange: Offset = Offset.Zero,
        verifyConsumptionInFinalPass: Boolean = false,
    ) {
        currentDragState =
            awaitTouchSlopState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                if (touchSlopDetector == null) {
                    touchSlopDetector = TouchSlopDetector(orientationLock)
                } else {
                    touchSlopDetector?.orientation = orientationLock
                    touchSlopDetector?.reset(initialTouchSlopPositionChange)
                }
                this.verifyConsumptionInFinalPass = verifyConsumptionInFinalPass
            }
    }

    private fun moveToDraggingState(pointerId: PointerId) {
        currentDragState = draggingState.apply { this.pointerId = pointerId }
    }

    private fun moveToAwaitDownState() {
        currentDragState =
            awaitDownState.apply {
                awaitTouchSlop = DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized
                consumedOnInitial = false
            }
    }

    private fun moveToAwaitGesturePickupState(
        initialDown: PointerInputChange,
        pointerId: PointerId,
        touchSlopDetector: TouchSlopDetector,
    ) {
        currentDragState =
            awaitGesturePickupState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                this.touchSlopDetector = touchSlopDetector.also { it.reset() }
            }
    }

    private fun processInitialDownState(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitDown,
    ) {
        /** Wait for a down event in any pass. */
        if (pointerEvent.changes.isEmpty()) return
        if (!pointerEvent.isChangedToDown(requireUnconsumed = false)) return

        val firstDown = pointerEvent.changes.first()
        val awaitTouchSlop =
            when (state.awaitTouchSlop) {
                DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized -> {
                    if (!startDragImmediately()) {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.Yes
                    } else {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.No
                    }
                }

                else -> state.awaitTouchSlop
            }

        // update the touch slop in the current state
        state.awaitTouchSlop = awaitTouchSlop

        if (pass == PointerEventPass.Initial) {
            // If we shouldn't await touch slop, we consume the event immediately.
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.No) {
                firstDown.consume()

                // Change state properties so we dispatch only later, this aligns with the previous
                // behavior where dispatching only happened during the main pass
                state.consumedOnInitial = true
            }
        }

        if (pass == PointerEventPass.Main) {
            /**
             * At this point we detected a Down event, if we should await the slop we move to the
             * next state. If we shouldn't await the slop and we already consumed the event we
             * dispatch the drag start events and start the dragging state.
             */
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.Yes) {
                moveToAwaitTouchSlopState(firstDown, firstDown.id)
            } else if (state.consumedOnInitial) {
                sendDragStart(firstDown, firstDown, Offset.Zero)
                sendDragEvent(firstDown, Offset.Zero)
                moveToDraggingState(firstDown.id)
            }
        }
    }

    private fun processAwaitTouchSlop(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitTouchSlop,
    ) {
        /** Slop detection only cares about the main and final passes */
        if (pass == PointerEventPass.Initial) return
        val eventFromPointerId = pointerEvent.changes.fastFirstOrNull { it.id == state.pointerId }

        /**
         * We lost this pointer, try to replace it. This is to cover the case where multiple
         * pointers were down, but the original one we tracked (state.pointerId) is no longer down,
         * try to move tracking to a different pointer
         */
        val dragEvent =
            if (eventFromPointerId == null) {
                val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
                if (otherDown == null) {
                    // There are no other pointers down, reset the state
                    moveToAwaitDownState()
                    return
                } else {
                    // a new pointer was found, update the current state.
                    state.pointerId = otherDown.id
                }
                otherDown
            } else {
                eventFromPointerId
            }

        /**
         * Slop detection routines happens during the Main pass. Do we have unconsumed events for
         * this pointer?
         */
        if (pass == PointerEventPass.Main) {
            if (!dragEvent.isConsumed) {
                if (dragEvent.changedToUpIgnoreConsumed()) {
                    /** The pointer lifted, look for another pointer */
                    val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
                    if (otherDown == null) {
                        // There are no other pointers down, reset the state
                        moveToAwaitDownState()
                    } else {
                        // a new pointer was found, update the current state.
                        state.pointerId = otherDown.id
                    }
                } else {
                    // this is a regular event (MOVE)
                    val touchSlop =
                        currentValueOf(LocalViewConfiguration).pointerSlop(dragEvent.type)

                    // add data to the slop detector
                    val postSlopOffset =
                        requireTouchSlopDetector()
                            .getPostSlopOffset(dragEvent.positionChangeIgnoreConsumed(), touchSlop)

                    /**
                     * Here we use the [gestureNode] and [GestureConnection] APIs to make a
                     * decision. About this gesture. At this point we have all the triggers to start
                     * a recognizing a gesture in this current
                     * [androidx.compose.foundation.gestures.DragGestureNode]. This is the moment
                     * that touch slop is recognized here in this node. During this time, before we
                     * start consuming drag events we check the interested of the parent and our
                     * self-interest. If the parent is interested and we're not (for this specific
                     * event), we will give the parent a chance to do something by postponing the
                     * remaining consumption to the final pass.
                     */
                    if (isNestedDraggablesTouchConflictFixEnabled) {
                        if (postSlopOffset.isSpecified) {
                            val isSelfInterested = isInterested(dragEvent)
                            val isParentInterested =
                                parentGestureConnection?.isInterested(dragEvent) == true
                            if (!isSelfInterested && isParentInterested) {
                                state.verifyConsumptionInFinalPass = true
                            } else {
                                dragEvent.consume()
                                sendDragStart(state.initialDown!!, dragEvent, postSlopOffset)
                                sendDragEvent(dragEvent, postSlopOffset)
                                moveToDraggingState(dragEvent.id)
                            }
                        } else {
                            state.verifyConsumptionInFinalPass = true
                        }
                    } else {
                        if (postSlopOffset.isSpecified) {
                            dragEvent.consume()
                            sendDragStart(state.initialDown!!, dragEvent, postSlopOffset)
                            sendDragEvent(dragEvent, postSlopOffset)
                            moveToDraggingState(dragEvent.id)
                        } else {
                            state.verifyConsumptionInFinalPass = true
                        }
                    }
                }
            } else {
                // This draggable "lost" the event as it was consumed by someone else, enter the
                // gesture pickup state if the feature is enabled.
                // Someone consumed this gesture, move this to the await pickup state.
                moveToAwaitGesturePickupState(
                    requireNotNull(state.initialDown) {
                        "AwaitTouchSlop.initialDown was not initialized"
                    },
                    state.pointerId,
                    requireNotNull(touchSlopDetector) {
                        "AwaitTouchSlop.touchSlopDetector was not initialized"
                    },
                )
            }
        }

        /**
         * This checks 2 cases: 1) A parent consumed in the main pass and this child can only see
         * that consumption during the final pass. 2) The parent actually consumed during the final
         * pass.
         */
        if (pass == PointerEventPass.Final && state.verifyConsumptionInFinalPass) {
            if (dragEvent.isConsumed) {
                // This draggable "lost" the event as it was consumed by someone else, enter the
                // gesture pickup state if the feature is enabled.
                // Someone consumed this gesture, move this to the await pickup state.
                moveToAwaitGesturePickupState(
                    requireNotNull(state.initialDown) {
                        "AwaitTouchSlop.initialDown was not initialized"
                    },
                    state.pointerId,
                    requireNotNull(touchSlopDetector) {
                        "AwaitTouchSlop.touchSlopDetector was not initialized"
                    },
                )
            } else {
                /**
                 * Self and nobody consumed dragEvent. We will only get here if self didn't consume
                 * in the main pass OR if self wasn't interested during the main pass. In this case
                 * we remain in the awaitTouchSlop state and wait for more information (events).
                 */
                state.verifyConsumptionInFinalPass = false
            }
        }
    }

    private fun processAwaitGesturePickup(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitGesturePickup,
    ) {
        /**
         * Drag pickup only happens during the final pass so we're sure nobody else was interested
         * in this gesture.
         */
        if (pass != PointerEventPass.Final) return
        val hasUnconsumedDrag = pointerEvent.changes.fastAll { !it.isConsumed }
        val hasDownPointers = pointerEvent.changes.fastAny { it.pressed }
        // all pointers are up, reset
        if (!hasDownPointers || pointerEvent.changes.isEmpty()) {
            moveToAwaitDownState()
        } else if (hasUnconsumedDrag) {
            // has pointers down with unconsumed events, a chance to pick up this gesture,
            // move to the touch slop detection phase
            val initialPositionChange =
                pointerEvent.changes.first().position - state.initialDown!!.position

            // await touch slop again, using the initial down as starting point.
            // For most cases this should return immediately since we probably moved
            // far enough from the initial down event.
            moveToAwaitTouchSlopState(
                requireNotNull(state.initialDown) {
                    "AwaitGesturePickup.initialDown was not initialized."
                },
                state.pointerId,
                initialPositionChange,
            )
        }
    }

    private fun processDraggingState(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.Dragging,
    ) {
        if (pass != PointerEventPass.Main) return

        val pointer = state.pointerId
        val dragEvent = pointerEvent.changes.fastFirstOrNull { it.id == pointer } ?: return
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                if (!dragEvent.isConsumed && dragEvent.changedToUpIgnoreConsumed()) {
                    sendDragStopped(dragEvent)
                } else {
                    sendDragCancelled()
                }
                moveToAwaitDownState()
            } else {
                state.pointerId = otherDown.id
            }
        } else {
            if (dragEvent.isConsumed) {
                sendDragCancelled()
            } else {
                val positionChange = dragEvent.positionChangeIgnoreConsumed()

                /**
                 * During the gesture pickup we can pickup events at any direction so disable the
                 * orientation lock.
                 */
                val motionChange = positionChange.getDistance()
                if (motionChange != 0.0f) {
                    val positionChange = dragEvent.positionChange()
                    sendDragEvent(dragEvent, positionChange)
                    dragEvent.consume()
                }
            }
        }
    }

    private fun sendDragStart(
        down: PointerInputChange,
        slopTriggerChange: PointerInputChange,
        overSlopOffset: Offset,
    ) {
        if (velocityTracker == null) velocityTracker = VelocityTracker()
        requireVelocityTracker().addPointerInputChange(down)
        val dragStartedOffset = slopTriggerChange.position - overSlopOffset
        // the drag start event offset is the down event + touch slop value
        // or in this case the event that triggered the touch slop minus
        // the post slop offset
        nodeOffset = Offset.Zero // restart node offset
        if (canDrag(down.type)) {
            if (!isListeningForEvents) {
                if (channel == null) {
                    channel = Channel(capacity = Channel.UNLIMITED)
                }
                startListeningForEvents()
            }
            previousPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
            requireChannel().trySend(DragStarted(dragStartedOffset))
        }
    }

    private fun sendDragEvent(change: PointerInputChange, dragAmount: Offset) {
        val currentPositionOnScreen = node.requireLayoutCoordinates().positionOnScreen()
        // container changed positions
        if (
            previousPositionOnScreen != Offset.Unspecified &&
                currentPositionOnScreen != previousPositionOnScreen
        ) {
            val delta = currentPositionOnScreen - previousPositionOnScreen
            nodeOffset += delta
        }
        previousPositionOnScreen = currentPositionOnScreen
        requireVelocityTracker().addPointerInputChange(event = change, offset = nodeOffset)
        requireChannel().trySend(DragDelta(dragAmount, false))
    }

    private fun sendDragStopped(change: PointerInputChange) {
        requireVelocityTracker().addPointerInputChange(change)
        val maximumVelocity = currentValueOf(LocalViewConfiguration).maximumFlingVelocity
        val velocity =
            requireVelocityTracker().calculateVelocity(Velocity(maximumVelocity, maximumVelocity))
        requireVelocityTracker().resetTracking()
        requireChannel().trySend(DragStopped(velocity.toValidVelocity(), false))
        isListeningForPointerInputEvents = false
    }

    private fun sendDragCancelled() {
        requireChannel().trySend(DragCancelled)
    }

    fun onDragEvent(event: DragEvent) {
        if (event is DragStarted && !isListeningForEvents) {
            isListeningForEvents = true
            startListeningForEvents()
        }
        requireChannel().trySend(event)
    }
}

private class DefaultDraggableState(val onDelta: (Float) -> Unit) : DraggableState {

    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = onDelta(pixels)
        }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ): Unit = coroutineScope { scrollMutex.mutateWith(dragScope, dragPriority, block) }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

internal sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()

    class DragStopped(val velocity: Velocity, val isIndirectPointerEvent: Boolean) : DragEvent()

    object DragCancelled : DragEvent()

    class DragDelta(val delta: Offset, val isIndirectPointerEvent: Boolean) : DragEvent()
}

internal fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

internal fun Velocity.toValidVelocity() =
    Velocity(if (this.x.isNaN()) 0f else this.x, if (this.y.isNaN()) 0f else this.y)

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}

private sealed class DragDetectionState {
    /**
     * Starter state for any drag gesture cycle. At this state we're waiting for a Down event to
     * indicate that a drag gesture may start. Since drag gesture start at the initial pass we have
     * the option to indicate if we consumed the event during the initial pass using
     * [consumedOnInitial]. We also save the [awaitTouchSlop] between passes so we don't call the
     * [DragGestureNode.startDragImmediately] as often.
     */
    class AwaitDown(
        var awaitTouchSlop: AwaitTouchSlop = AwaitTouchSlop.NotInitialized,
        var consumedOnInitial: Boolean = false,
    ) : DragDetectionState() {

        enum class AwaitTouchSlop {
            Yes,
            No,
            NotInitialized,
        }
    }

    /**
     * If drag should wait for touch slop, after the initial down recognition we move to this state.
     * Here we will collect drag events until touch slop is crossed.
     */
    class AwaitTouchSlop(
        var initialDown: PointerInputChange? = null,
        var pointerId: PointerId = PointerId(Long.MAX_VALUE),
        var verifyConsumptionInFinalPass: Boolean = false,
    ) : DragDetectionState()

    /**
     * Alternative state that implements the gesture pick up feature. If a draggable loses an event
     * because someone else consumed it, it can still pick it up later if the consumer "gives up" on
     * that gesture. Once a gesture is lost the draggable will pass on to this state until all
     * fingers are up.
     */
    class AwaitGesturePickup(
        var initialDown: PointerInputChange? = null,
        var pointerId: PointerId = PointerId(Long.MAX_VALUE),
        var touchSlopDetector: TouchSlopDetector? = null,
    ) : DragDetectionState()

    /** State where dragging is happening. */
    class Dragging(var pointerId: PointerId = PointerId(Long.MAX_VALUE)) : DragDetectionState()
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/Scrollable.kt
```kotlin
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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ComposeFoundationFlags.isDelayPressesUsingGestureConsumptionEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.PlatformOptimizedCancellationException
import androidx.compose.foundation.relocation.BringIntoViewResponderNode
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberPlatformOverscrollEffect
import androidx.compose.foundation.scrollableArea
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.getFocusedRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.dispatchOnScrollChanged
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * `scrollable` is a low level modifier that handles low level scrolling input gestures, without
 * other behaviors commonly used for scrollable containers. For building scrollable containers, see
 * [androidx.compose.foundation.scrollableArea]. `scrollableArea` clips its content to its bounds,
 * renders overscroll, and adjusts the direction of scroll gestures to ensure that the content moves
 * with the user's gestures. See also [androidx.compose.foundation.verticalScroll] and
 * [androidx.compose.foundation.horizontalScroll] for high level scrollable containers that handle
 * layout and move the content as the user scrolls.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit drag events when
 *   this scrollable is being dragged.
 */
@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier =
    scrollable(
        state = state,
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior,
        interactionSource = interactionSource,
        overscrollEffect = null,
    )

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * `scrollable` is a low level modifier that handles low level scrolling input gestures, without
 * other behaviors commonly used for scrollable containers. For building scrollable containers, see
 * [androidx.compose.foundation.scrollableArea]. `scrollableArea` clips its content to its bounds,
 * renders overscroll, and adjusts the direction of scroll gestures to ensure that the content moves
 * with the user's gestures. See also [androidx.compose.foundation.verticalScroll] and
 * [androidx.compose.foundation.horizontalScroll] for high level scrollable containers that handle
 * layout and move the content as the user scrolls.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the over
 * scrolling logic. Use [androidx.compose.foundation.rememberOverscrollEffect] to create an instance
 * of the current provided overscroll implementation. Note: compared to other APIs that accept
 * [overscrollEffect] such as [scrollableArea] and [verticalScroll], `scrollable` does not render
 * the overscroll, it only provides events. Manually add [androidx.compose.foundation.overscroll] to
 * render the overscroll or use other APIs.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have some
 *   scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should also
 *   apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit drag events when
 *   this scrollable is being dragged.
 * @param bringIntoViewSpec The configuration that this scrollable should use to perform scrolling
 *   when scroll requests are received from the focus system. If null is provided the system will
 *   use the behavior provided by [LocalBringIntoViewSpec] which by default has a platform dependent
 *   implementation.
 */
@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
) =
    this then
        ScrollableElement(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )

private class ScrollableElement(
    val state: ScrollableState,
    val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec?,
) : ModifierNodeElement<ScrollableNode>() {
    override fun create(): ScrollableNode {
        return ScrollableNode(
            state,
            overscrollEffect,
            flingBehavior,
            orientation,
            enabled,
            reverseDirection,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun update(node: ScrollableNode) {
        node.update(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + bringIntoViewSpec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is ScrollableElement) return false

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class ScrollableNode(
    state: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior?,
    orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec?,
) :
    DragGestureNode(
        canDrag = CanDragCalculation,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = orientation,
    ),
    KeyInputModifierNode,
    SemanticsModifierNode,
    OnScrollChangedDispatcher {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = platformScrollableDefaultFlingBehavior()

    private val scrollingLogic =
        ScrollingLogic(
            scrollableState = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = flingBehavior ?: defaultFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher,
            onScrollChangedDispatcher = this,
            isScrollableNodeAttached = { isAttached },
        )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))

    private val contentInViewNode =
        delegate(
            ContentInViewNode(
                orientation = orientation,
                scrollingLogic = scrollingLogic,
                reverseDirection = reverseDirection,
                bringIntoViewSpec = bringIntoViewSpec,
                getFocusedRect = { focusTargetModifierNode.getFocusedRect() },
            )
        )

    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    private var mouseWheelScrollingLogic: MouseWheelScrollingLogic? = null
    private var trackpadScrollingLogic: TrackpadScrollingLogic? = null

    private var scrollableContainerNode: ScrollableContainerNode? = null

    init {
        /** Nested scrolling */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /** Focus scrolling */
        delegate(BringIntoViewResponderNode(contentInViewNode))

        if (!isDelayPressesUsingGestureConsumptionEnabled) {
            scrollableContainerNode = delegate(ScrollableContainerNode(enabled))
        }
    }

    override fun dispatchScrollDeltaInfo(delta: Offset) {
        if (!isAttached) return
        dispatchOnScrollChanged(delta)
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta {
                    // Indirect pointer Events should be reverted to account for the reverse we
                    // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
                    // that shouldn't happen for indirect pointer events, so we cancel the reverse
                    // here.
                    val invertIndirectPointer = if (it.isIndirectPointerEvent) -1f else 1f
                    scrollByWithOverscroll(
                        it.delta.singleAxisOffset() * invertIndirectPointer,
                        source = UserInput,
                    )
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(event: DragEvent.DragStopped) {
        nestedScrollDispatcher.coroutineScope.launch {
            // Indirect pointer Events should be reverted to account for the reverse we
            // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
            // that shouldn't happen for indirect pointer events, so we cancel the reverse
            // here.
            val invertIndirectPointer = if (event.isIndirectPointerEvent) -1f else 1f
            scrollingLogic.onScrollStopped(
                event.velocity * invertIndirectPointer,
                isMouseWheel = false,
            )
        }
    }

    private fun onWheelScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = true)
        }
    }

    private fun onTrackpadScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(velocity, isMouseWheel = false)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    private fun ensureMouseWheelScrollingLogicInitialized() {
        if (mouseWheelScrollingLogic == null) {
            mouseWheelScrollingLogic =
                MouseWheelScrollingLogic(
                    scrollingLogic = scrollingLogic,
                    mouseWheelScrollConfig = platformScrollConfig(),
                    onScrollStopped = ::onWheelScrollStopped,
                    density = requireDensity(),
                )
        }

        mouseWheelScrollingLogic?.startReceivingEvents(coroutineScope)
    }

    private fun ensureTrackpadScrollingLogicInitialized() {
        if (trackpadScrollingLogic == null) {
            trackpadScrollingLogic =
                TrackpadScrollingLogic(
                    scrollingLogic = scrollingLogic,
                    onScrollStopped = ::onTrackpadScrollStopped,
                    density = requireDensity(),
                )
        }

        trackpadScrollingLogic?.startReceivingEvents(coroutineScope)
    }

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?,
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode?.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling =
            scrollingLogic.update(
                scrollableState = state,
                orientation = orientation,
                overscrollEffect = overscrollEffect,
                reverseDirection = reverseDirection,
                flingBehavior = resolvedFlingBehavior,
                nestedScrollDispatcher = nestedScrollDispatcher,
            )
        contentInViewNode.update(orientation, reverseDirection, bringIntoViewSpec)

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = if (scrollingLogic.isVertical()) Vertical else Horizontal,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
        trackpadScrollingLogic?.updateDensity(requireDensity())
    }

    private fun updateDefaultFlingBehavior() {
        if (!isAttached) return
        val density = requireDensity()
        defaultFlingBehavior.updateDensity(density)
    }

    override fun onDensityChange() {
        onCancelPointerInput()
        updateDefaultFlingBehavior()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
        trackpadScrollingLogic?.updateDensity(requireDensity())
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return if (
            enabled &&
                (event.key == Key.PageDown || event.key == Key.PageUp) &&
                (event.type == KeyEventType.KeyDown) &&
                (!event.isCtrlPressed)
        ) {

            val scrollAmount: Offset =
                if (scrollingLogic.isVertical()) {
                    val viewportHeight = contentInViewNode.viewportSizeOrZero.height

                    val yAmount =
                        if (event.key == Key.PageUp) {
                            viewportHeight.toFloat()
                        } else {
                            -viewportHeight.toFloat()
                        }

                    Offset(0f, yAmount)
                } else {
                    val viewportWidth = contentInViewNode.viewportSizeOrZero.width

                    val xAmount =
                        if (event.key == Key.PageUp) {
                            viewportWidth.toFloat()
                        } else {
                            -viewportWidth.toFloat()
                        }

                    Offset(xAmount, 0f)
                }

            // A coroutine is launched for every individual scroll event in the
            // larger scroll gesture. If we see degradation in the future (that is,
            // a fast scroll gesture on a slow device causes UI jank [not seen up to
            // this point), we can switch to a more efficient solution where we
            // lazily launch one coroutine (with the first event) and use a Channel
            // to communicate the scroll amount to the UI thread.
            coroutineScope.launch {
                scrollingLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(offset = scrollAmount, source = UserInput)
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false

    // Forward all PointerInputModifierNode method calls to `mmouseWheelScrollNode.pointerInputNode`
    // See explanation in `MouseWheelScrollNode.pointerInputNode`

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pointerEvent.changes.fastAny { canDrag.invoke(it.type) }) {
            super.onPointerEvent(pointerEvent, pass, bounds)
        }
        initializeGestureCoordination()
        if (enabled) {
            if (pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Scroll) {
                ensureMouseWheelScrollingLogicInitialized()
            }
            mouseWheelScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)

            if (
                pass == PointerEventPass.Initial &&
                    (pointerEvent.type == PointerEventType.PanStart ||
                        pointerEvent.type == PointerEventType.PanMove ||
                        pointerEvent.type == PointerEventType.PanEnd)
            ) {
                ensureTrackpadScrollingLogicInitialized()
            }
            trackpadScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let { scrollBy(action = it) }

        scrollByOffsetAction?.let { scrollByOffset(action = it) }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch { scrollingLogic.semanticsScrollBy(Offset(x, y)) }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }
}

/** Contains the default values used by [scrollable] */
object ScrollableDefaults {

    /** Create and remember default [FlingBehavior] that will represent natural fling curve. */
    @Composable fun flingBehavior(): FlingBehavior = rememberPlatformDefaultFlingBehavior()

    /**
     * Returns a remembered [OverscrollEffect] created from the current value of
     * [LocalOverscrollFactory].
     *
     * This API has been deprecated, and replaced with [rememberOverscrollEffect]
     */
    @Deprecated(
        "This API has been replaced with rememberOverscrollEffect, which queries theme provided OverscrollFactory values instead of the 'platform default' without customization.",
        replaceWith =
            ReplaceWith(
                "rememberOverscrollEffect()",
                "androidx.compose.foundation.rememberOverscrollEffect",
            ),
    )
    @Composable
    fun overscrollEffect(): OverscrollEffect {
        return rememberPlatformOverscrollEffect() ?: NoOpOverscrollEffect
    }

    private object NoOpOverscrollEffect : OverscrollEffect {
        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset = performScroll(delta)

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            performFling(velocity)
        }

        override val isInProgress: Boolean
            get() = false

        override val node: DelegatableNode
            get() = object : Modifier.Node() {}
    }

    /**
     * Calculates the final `reverseDirection` value for a scrollable component.
     *
     * This is a helper function used by [androidx.compose.foundation.scrollableArea] to determine
     * whether to reverse the direction of scroll input. The goal is to provide a "natural"
     * scrolling experience where content moves with the user's gesture, while also accounting for
     * the [layoutDirection].
     *
     * The logic is as follows:
     * 1. To achieve "natural" scrolling (content moves with the gesture), scroll deltas are
     *    inverted. This function returns `true` by default when `reverseScrolling` is `false`.
     * 2. In a Right-to-Left (`Rtl`) context with a `Horizontal` orientation, the direction is
     *    flipped an additional time to maintain the natural feel, as the content is laid out from
     *    right to left.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean,
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }
}

internal interface ScrollConfig {

    /** Enables animated transition of scroll on mouse wheel events. */
    val isSmoothScrollingEnabled: Boolean
        get() = true

    fun isPreciseWheelScroll(event: PointerEvent): Boolean = false

    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal expect fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig

// TODO: provide public way to drag by mouse (especially requested for Pager)
internal val CanDragCalculation: (PointerType) -> Boolean = { type -> type != PointerType.Mouse }

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
internal class ScrollingLogic(
    var scrollableState: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    private var orientation: Orientation,
    private var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
    private var onScrollChangedDispatcher: OnScrollChangedDispatcher,
    private val isScrollableNodeAttached: () -> Boolean,
) : ScrollLogic {
    // specifies if this scrollable node is currently flinging
    override var isFlinging = false
        private set

    fun Float.toOffset(): Offset =
        when {
            this == 0f -> Offset.Zero
            orientation == Horizontal -> Offset(this, 0f)
            else -> Offset(0f, this)
        }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    /**
     * Converts this offset to a single axis delta based on the derived angle from the x and y
     * deltas.
     *
     * @return Returns a single axis delta based on the angle. If the angle is mostly horizontal,
     *   and we are in a horizontal scrollable, this will return the x component. If the angle is
     *   mostly vertical, and we are in a vertical scrollable, this will return the y component.
     *   Otherwise, this will return 0. Mostly horizontal means angles smaller than
     *   [VerticalAxisThresholdAngle].
     */
    fun Offset.toSingleAxisDeltaFromAngle(): Float {
        val angle = atan2(this.y.absoluteValue, this.x.absoluteValue)
        return if (angle >= VerticalAxisThresholdAngle) {
            if (orientation == Vertical) this.y else 0f
        } else {
            if (orientation == Horizontal) this.x else 0f
        }
    }

    fun Float.toVelocity(): Velocity =
        when {
            this == 0f -> Velocity.Zero
            orientation == Horizontal -> Velocity(this, 0f)
            else -> Velocity(0f, this)
        }

    private fun Velocity.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    private fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    private var latestScrollSource = UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope =
        object : NestedScrollScope {
            override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
                return with(outerStateScope) { performScroll(offset, source) }
            }

            override fun scrollByWithOverscroll(
                offset: Offset,
                source: NestedScrollSource,
            ): Offset {
                latestScrollSource = source
                val overscroll = overscrollEffect
                return if (overscroll != null && shouldDispatchOverscroll) {
                    overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
                } else {
                    with(outerStateScope) { performScroll(offset, source) }
                }
            }
        }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) { performScroll(delta, latestScrollSource) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun ScrollScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val singleAxisDeltaForSelfScroll =
            scrollAvailableAfterPreScroll.singleAxisOffset().reverseIfNeeded().toFloat()

        // Consume on a single axis.
        val consumedBySelfScroll =
            scrollBy(singleAxisDeltaForSelfScroll).toOffset().reverseIfNeeded()

        // Trigger on scroll changed callback
        onScrollChangedDispatcher.dispatchScrollDeltaInfo(consumedBySelfScroll)

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll =
            nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source,
            )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() = scrollableState.canScrollForward || scrollableState.canScrollBackward

    override fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    private fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollableState
            .dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
            .reverseIfNeeded()
            .toOffset()
    }

    suspend fun onScrollStopped(initialVelocity: Velocity, isMouseWheel: Boolean) {
        if (isMouseWheel && !flingBehavior.shouldBeTriggeredByMouseWheel) {
            return
        }
        val availableVelocity = initialVelocity.singleAxisVelocity()

        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher.dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling((available - velocityLeft), velocityLeft)
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        isFlinging = true
        try {
            scroll(scrollPriority = MutatePriority.Default) {
                val nestedScrollScope = this
                val reverseScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {
                            // Fling has hit the bounds or node left composition,
                            // cancel it to allow continuation. This will conclude this node's
                            // fling,
                            // allowing the onPostFling signal to be called
                            // with the leftover velocity from the fling animation. Any nested
                            // scroll
                            // node above will be able to pick up the left over velocity and
                            // continue
                            // the fling.
                            if (
                                pixels.absoluteValue != 0.0f && !isScrollableNodeAttached.invoke()
                            ) {
                                throw FlingCancellationException()
                            }

                            return nestedScrollScope
                                .scrollByWithOverscroll(
                                    offset = pixels.toOffset().reverseIfNeeded(),
                                    source = SideEffect,
                                )
                                .toFloat()
                                .reverseIfNeeded()
                        }
                    }
                with(reverseScope) {
                    with(flingBehavior) {
                        result =
                            result.update(
                                performFling(available.toFloat().reverseIfNeeded())
                                    .reverseIfNeeded()
                            )
                    }
                }
            }
        } finally {
            isFlinging = false
        }

        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress || overscrollEffect?.isInProgress ?: false
    }

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /** @return true if the pointer input should be reset */
    fun update(
        scrollableState: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }

    fun isVertical(): Boolean = orientation == Vertical
}

private val NoOpScrollScope: ScrollScope =
    object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = pixels
    }

internal class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollLogic,
    var enabled: Boolean,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (enabled) {
            scrollingLogic.performRawScroll(available)
        } else {
            Offset.Zero
        }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (enabled) {
            val velocityLeft =
                if (scrollingLogic.isFlinging) {
                    Velocity.Zero
                } else {
                    scrollingLogic.doFlingAnimation(available)
                }
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

/** Interface to allow re-use across Scrollable and Scrollable2D. */
internal interface ScrollLogic {
    val isFlinging: Boolean

    fun performRawScroll(scroll: Offset): Offset

    suspend fun doFlingAnimation(available: Velocity): Velocity
}

/** Compatibility interface for default fling behaviors that depends on [Density]. */
internal interface ScrollableDefaultFlingBehavior : FlingBehavior {
    /**
     * Update the internal parameters of FlingBehavior in accordance with the new
     * [androidx.compose.ui.unit.Density] value.
     *
     * @param density new density value.
     */
    fun updateDensity(density: Density) = Unit
}

/**
 * TODO: Move it to public interface Currently, default [FlingBehavior] is not triggered at all to
 *   avoid unexpected effects during regular scrolling. However, custom one must be triggered
 *   because it's used not only for "inertia", but also for snapping in
 *   [androidx.compose.foundation.pager.Pager] or
 *   [androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior].
 */
private val FlingBehavior.shouldBeTriggeredByMouseWheel
    get() = this !is ScrollableDefaultFlingBehavior

/**
 * This method returns [ScrollableDefaultFlingBehavior] whose density will be managed by the
 * [ScrollableElement] because it's not created inside [Composable] context. This is different from
 * [rememberPlatformDefaultFlingBehavior] which creates [FlingBehavior] whose density depends on
 * [LocalDensity] and is automatically resolved.
 */
internal expect fun platformScrollableDefaultFlingBehavior(): ScrollableDefaultFlingBehavior

/**
 * Create and remember default [FlingBehavior] that will represent natural platform fling decay
 * behavior.
 */
@Composable internal expect fun rememberPlatformDefaultFlingBehavior(): FlingBehavior

internal class DefaultFlingBehavior(
    private var flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
) : ScrollableDefaultFlingBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                val animationState =
                    AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        lastAnimationCycleCount++
                    }
                } catch (exception: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }

    override fun updateDensity(density: Density) {
        flingDecay = splineBasedDecay(density)
    }
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale =
    object : MotionDurationScale {
        override val scaleFactor: Float
            get() = DefaultScrollMotionDurationScaleFactor
    }

/**
 * (b/311181532): This could not be flattened so we moved it to TraversableNode, but ideally
 * ScrollabeNode should be the one to be travesable.
 */
internal class ScrollableContainerNode(enabled: Boolean) : Modifier.Node(), TraversableNode {
    override val traverseKey: Any = TraverseKey

    var enabled: Boolean = enabled
        private set

    companion object TraverseKey

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}

internal val UnityDensity =
    object : Density {
        override val density: Float
            get() = 1f

        override val fontScale: Float
            get() = 1f
    }

/** A scroll scope for nested scrolling and overscroll support. */
internal interface NestedScrollScope {
    fun scrollBy(offset: Offset, source: NestedScrollSource): Offset

    fun scrollByWithOverscroll(offset: Offset, source: NestedScrollSource): Offset
}

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation driven
 * event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        animate(0f, offset.toFloat()) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(offset = delta.reverseIfNeeded().toOffset(), source = UserInput)
                    .toFloat()
                    .reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue.toOffset()
}

internal class FlingCancellationException :
    PlatformOptimizedCancellationException("The fling animation was cancelled")

internal interface OnScrollChangedDispatcher {
    fun dispatchScrollDeltaInfo(delta: Offset)
}

private const val VerticalAxisThresholdAngle = PI / 4
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/Transformable.kt
```kotlin
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.TransformEvent.TransformDelta
import androidx.compose.foundation.gestures.TransformEvent.TransformStarted
import androidx.compose.foundation.gestures.TransformEvent.TransformStopped
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Enable transformation gestures of the modified UI element.
 *
 * Users should update their state themselves using default [TransformableState] and its
 * `onTransformation` callback or by implementing [TransformableState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * @sample androidx.compose.foundation.samples.TransformableSample
 * @param state [TransformableState] of the transformable. Defines how transformation events will be
 *   interpreted by the user land logic, contains useful information about on-going events and
 *   provides animation capabilities.
 * @param lockRotationOnZoomPan If `true`, rotation is allowed only if touch slop is detected for
 *   rotation before pan or zoom motions. If not, pan and zoom gestures will be detected, but
 *   rotation gestures will not be. If `false`, once touch slop is reached, all three gestures are
 *   detected.
 * @param enabled whether zooming by gestures is enabled or not
 */
fun Modifier.transformable(
    state: TransformableState,
    lockRotationOnZoomPan: Boolean = false,
    enabled: Boolean = true,
) = transformable(state, { true }, lockRotationOnZoomPan, enabled)

/**
 * Enable transformation gestures of the modified UI element.
 *
 * Users should update their state themselves using default [TransformableState] and its
 * `onTransformation` callback or by implementing [TransformableState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * This overload of transformable modifier provides [canPan] parameter, which allows the caller to
 * control when the pan can start. making pan gesture to not to start when the scale is 1f makes
 * transformable modifiers to work well within the scrollable container. See example:
 *
 * @sample androidx.compose.foundation.samples.TransformableSampleInsideScroll
 * @param state [TransformableState] of the transformable. Defines how transformation events will be
 *   interpreted by the user land logic, contains useful information about on-going events and
 *   provides animation capabilities.
 * @param canPan whether the pan gesture can be performed or not given the pan offset
 * @param lockRotationOnZoomPan If `true`, rotation is allowed only if touch slop is detected for
 *   rotation before pan or zoom motions. If not, pan and zoom gestures will be detected, but
 *   rotation gestures will not be. If `false`, once touch slop is reached, all three gestures are
 *   detected.
 * @param enabled whether zooming by gestures is enabled or not
 */
fun Modifier.transformable(
    state: TransformableState,
    canPan: (Offset) -> Boolean,
    lockRotationOnZoomPan: Boolean = false,
    enabled: Boolean = true,
) = this then TransformableElement(state, canPan, lockRotationOnZoomPan, enabled)

private class TransformableElement(
    private val state: TransformableState,
    private val canPan: (Offset) -> Boolean,
    private val lockRotationOnZoomPan: Boolean,
    private val enabled: Boolean,
) : ModifierNodeElement<TransformableNode>() {
    override fun create(): TransformableNode =
        TransformableNode(state, canPan, lockRotationOnZoomPan, enabled)

    override fun update(node: TransformableNode) {
        node.update(state, canPan, lockRotationOnZoomPan, enabled)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as TransformableElement

        if (state != other.state) return false
        if (canPan !== other.canPan) return false
        if (lockRotationOnZoomPan != other.lockRotationOnZoomPan) return false
        if (enabled != other.enabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + canPan.hashCode()
        result = 31 * result + lockRotationOnZoomPan.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "transformable"
        properties["state"] = state
        properties["canPan"] = canPan
        properties["enabled"] = enabled
        properties["lockRotationOnZoomPan"] = lockRotationOnZoomPan
    }
}

private class TransformableNode(
    private var state: TransformableState,
    private var canPan: (Offset) -> Boolean,
    private var lockRotationOnZoomPan: Boolean,
    private var enabled: Boolean,
) : DelegatingNode(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {

    private val updatedCanPan: (Offset) -> Boolean = { canPan.invoke(it) }
    private val channel = Channel<TransformEvent>(capacity = Channel.UNLIMITED)

    private var scrollConfig: ScrollConfig? = null

    override fun onAttach() {
        super.onAttach()
        scrollConfig = platformScrollConfig()
    }

    private val pointerInputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                if (!enabled) return@SuspendingPointerInputModifierNode
                coroutineScope {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        while (isActive) {
                            var event = channel.receive()
                            if (event !is TransformStarted) continue
                            try {
                                state.transform(MutatePriority.UserInput) {
                                    while (event !is TransformStopped) {
                                        (event as? TransformDelta)?.let {
                                            transformByWithCentroid(
                                                centroid = it.centroid,
                                                zoomChange = it.zoomChange,
                                                panChange = it.panChange,
                                                rotationChange = it.rotationChange,
                                            )
                                        }
                                        event = channel.receive()
                                    }
                                }
                            } catch (_: CancellationException) {
                                // ignore the cancellation and start over again.
                            }
                        }
                    }

                    awaitEachGesture {
                        try {
                            detectZoom(lockRotationOnZoomPan, channel, updatedCanPan)
                        } catch (exception: CancellationException) {
                            if (!isActive) throw exception
                        } finally {
                            channel.trySend(TransformStopped)
                        }
                    }
                }
            }
        )

    private var pointerInputModifierMouse: PointerInputModifierNode? = null

    fun update(
        state: TransformableState,
        canPan: (Offset) -> Boolean,
        lockRotationOnZoomPan: Boolean,
        enabled: Boolean,
    ) {
        this.canPan = canPan
        val needsReset =
            this.state != state ||
                this.enabled != enabled ||
                this.lockRotationOnZoomPan != lockRotationOnZoomPan
        if (needsReset) {
            this.state = state
            this.enabled = enabled
            this.lockRotationOnZoomPan = lockRotationOnZoomPan
            pointerInputNode.resetPointerInputHandler()
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        val scrollConfig = scrollConfig
        if (
            enabled &&
                pointerEvent.changes.fastAny { it.type == PointerType.Mouse } &&
                scrollConfig != null &&
                pointerInputModifierMouse == null
        ) {
            pointerInputModifierMouse =
                delegate(
                    SuspendingPointerInputModifierNode {
                        detectNonTouchGestures(channel, scrollConfig)
                    }
                )
        }
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
        pointerInputModifierMouse?.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
        pointerInputModifierMouse?.onCancelPointerInput()
    }
}

// The factor used to covert the mouse scroll to zoom.
// Every 545 pixels of scroll is converted into 2 times zoom. This value is calculated from
// curve fitting the ChromeOS's zoom factors.
internal const val SCROLL_FACTOR = 545f

/**
 * Convert non touch events into the appropriate transform events. There are 3 cases, where order of
 * determination matters:
 * - If Ctrl is pressed, and we get a scroll, either from a mouse wheel or a trackpad pan, we
 *   convert that scroll into an equivalent zoom
 * - If we get a trackpad pan, we convert that into a pan
 * - If we get a trackpad scale, we convert that into a zoom
 */
private suspend fun PointerInputScope.detectNonTouchGestures(
    channel: Channel<TransformEvent>,
    scrollConfig: ScrollConfig,
) {
    val currentContext = currentCoroutineContext()
    awaitPointerEventScope {
        while (currentContext.isActive) {
            try {
                var zoomOffset: Offset?
                var panOffset: Offset?
                var scale: Float?
                var pointer: PointerEvent
                do {
                    pointer = awaitPointerEvent()

                    // Convert non touch events into the appropriate transform events.
                    // There are 3 cases, where order of determination matters:
                    // - If Ctrl is pressed, and we get a scroll, either from a mouse wheel or a
                    //   trackpad pan, we convert that scroll into an equivalent zoom
                    // - If we get a trackpad pan, we convert that into a pan
                    // - If we get a trackpad scale, we convert that into a zoom
                    zoomOffset = consumePointerEventAsCtrlScrollOrNull(pointer, scrollConfig)
                    panOffset = consumePointerEventAsPanOrNull(pointer)
                    scale = consumePointerEventAsScaleOrNull(pointer)
                } while (zoomOffset == null && panOffset == null && scale == null)
                if (zoomOffset != null) {
                    var scrollDelta: Offset = zoomOffset
                    channel.trySend(TransformStarted)
                    while (true) {
                        // This formula is curve fitting form Chrome OS's ctrl + scroll
                        // implementation.
                        val zoomChange = 2f.pow(scrollDelta.y / SCROLL_FACTOR)
                        channel.trySend(
                            TransformDelta(
                                centroid = pointer.calculateCentroid { true },
                                zoomChange = zoomChange,
                                panChange = Offset.Zero,
                                rotationChange = 0f,
                            )
                        )
                        pointer = awaitPointerEvent()
                        scrollDelta =
                            consumePointerEventAsCtrlScrollOrNull(pointer, scrollConfig) ?: break
                    }
                } else if (panOffset != null) {
                    var panDelta: Offset = panOffset
                    channel.trySend(TransformStarted)
                    while (true) {
                        channel.trySend(
                            TransformDelta(
                                centroid = pointer.calculateCentroid { true },
                                zoomChange = 1f,
                                panChange = panDelta,
                                rotationChange = 0f,
                            )
                        )
                        pointer = awaitPointerEvent()
                        panDelta = consumePointerEventAsPanOrNull(pointer) ?: break
                    }
                } else {
                    var scaleDelta: Float =
                        checkNotNull(scale) {
                            "One of zoomOffset, panOffset and scaleDelta must be non-null"
                        }
                    channel.trySend(TransformStarted)
                    while (true) {
                        channel.trySend(
                            TransformDelta(
                                centroid = pointer.calculateCentroid { true },
                                zoomChange = scaleDelta,
                                panChange = Offset.Zero,
                                rotationChange = 0f,
                            )
                        )
                        pointer = awaitPointerEvent()
                        scaleDelta = consumePointerEventAsScaleOrNull(pointer) ?: break
                    }
                }
            } finally {
                channel.trySend(TransformStopped)
            }
        }
    }
}

/**
 * If the PointerEvent is a mouse scroll event that has non zero scrollDelta and the ctrl key is
 * pressed, its scrollDelta is returned. Otherwise, null is returned. The event is consumed when it
 * detects ctrl + mouse scroll.
 */
private fun AwaitPointerEventScope.consumePointerEventAsCtrlScrollOrNull(
    pointer: PointerEvent,
    scrollConfig: ScrollConfig,
): Offset? {
    if (
        !pointer.keyboardModifiers.isCtrlPressed ||
            (pointer.type != PointerEventType.Scroll &&
                pointer.type != PointerEventType.PanStart &&
                pointer.type != PointerEventType.PanMove &&
                pointer.type != PointerEventType.PanEnd)
    ) {
        return null
    }
    @OptIn(ExperimentalFoundationApi::class)
    val scrollDelta =
        with(scrollConfig) { calculateMouseWheelScroll(pointer, size) } +
            if (ComposeFoundationFlags.isTrackpadGestureHandlingEnabled) {
                (pointer.changes.firstOrNull()?.let {
                    -it.panOffset +
                        it.historical.fastFold(Offset.Zero) { acc, historicalChange ->
                            acc - historicalChange.panOffset
                        }
                } ?: Offset.Zero)
            } else {
                Offset.Zero
            }

    if (scrollDelta == Offset.Zero) {
        return null
    }

    pointer.changes.fastForEach { it.consume() }
    return scrollDelta
}

private fun AwaitPointerEventScope.consumePointerEventAsPanOrNull(pointer: PointerEvent): Offset? {
    @OptIn(ExperimentalFoundationApi::class)
    if (
        !ComposeFoundationFlags.isTrackpadGestureHandlingEnabled ||
            (pointer.type != PointerEventType.PanStart &&
                pointer.type != PointerEventType.PanMove &&
                pointer.type != PointerEventType.PanEnd)
    ) {
        return null
    }
    val scrollDelta =
        pointer.changes.firstOrNull()?.let {
            -it.panOffset +
                it.historical.fastFold(Offset.Zero) { acc, historicalChange ->
                    acc - historicalChange.panOffset
                }
        } ?: Offset.Zero

    if (scrollDelta == Offset.Zero) {
        return null
    }

    pointer.changes.fastForEach { it.consume() }
    return scrollDelta
}

private fun AwaitPointerEventScope.consumePointerEventAsScaleOrNull(pointer: PointerEvent): Float? {
    @OptIn(ExperimentalFoundationApi::class)
    if (
        !ComposeFoundationFlags.isTrackpadGestureHandlingEnabled ||
            (pointer.type != PointerEventType.ScaleStart &&
                pointer.type != PointerEventType.ScaleChange &&
                pointer.type != PointerEventType.ScaleEnd)
    ) {
        return null
    }
    var scaleDelta = 1f
    pointer.changes.fastForEach {
        scaleDelta *= it.scaleFactor
        it.historical.fastForEach { scaleDelta *= it.scaleFactor }
    }

    if (scaleDelta == 1f) {
        return null
    }

    pointer.changes.fastForEach { it.consume() }
    return scaleDelta
}

private suspend fun AwaitPointerEventScope.detectZoom(
    panZoomLock: Boolean,
    channel: Channel<TransformEvent>,
    canPan: (Offset) -> Boolean,
) {
    var rotation = 0f
    var zoom = 1f
    var pan = Offset.Zero
    var pastTouchSlop = false
    val touchSlop = viewConfiguration.touchSlop
    var lockedToPanZoom = false
    awaitFirstDown(requireUnconsumed = false)
    do {
        val event = awaitPointerEvent()
        @OptIn(ExperimentalFoundationApi::class)
        val canceled =
            event.changes.fastAny { it.isConsumed } ||
                (ComposeFoundationFlags.isTrackpadGestureHandlingEnabled &&
                    (event.type == PointerEventType.PanStart ||
                        event.type == PointerEventType.PanMove ||
                        event.type == PointerEventType.PanEnd ||
                        event.type == PointerEventType.ScaleStart ||
                        event.type == PointerEventType.ScaleChange ||
                        event.type == PointerEventType.ScaleEnd))
        if (!canceled) {
            val zoomChange = event.calculateZoom()
            val rotationChange = event.calculateRotation()
            val panChange = event.calculatePan()

            if (!pastTouchSlop) {
                zoom *= zoomChange
                rotation += rotationChange
                pan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize
                val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                val panMotion = pan.getDistance()

                if (
                    zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        (panMotion > touchSlop && canPan.invoke(panChange))
                ) {
                    pastTouchSlop = true
                    lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    channel.trySend(TransformStarted)
                }
            }

            if (pastTouchSlop) {
                val centroid = event.calculateCentroid(useCurrent = false)
                val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                if (
                    effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        (panChange != Offset.Zero && canPan.invoke(panChange))
                ) {
                    channel.trySend(
                        TransformDelta(centroid, zoomChange, panChange, effectiveRotation)
                    )
                }
                event.changes.fastForEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }
            }
        } else {
            channel.trySend(TransformStopped)
        }
        val finalEvent = awaitPointerEvent(pass = PointerEventPass.Final)
        // someone consumed while we were waiting for touch slop
        val finallyCanceled = finalEvent.changes.fastAny { it.isConsumed } && !pastTouchSlop
    } while (!canceled && !finallyCanceled && event.changes.fastAny { it.pressed })
}

private sealed class TransformEvent {
    object TransformStarted : TransformEvent()

    object TransformStopped : TransformEvent()

    class TransformDelta(
        val centroid: Offset,
        val zoomChange: Float,
        val panChange: Offset,
        val rotationChange: Float,
    ) : TransformEvent()
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Background.kt
```kotlin
/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
@Stable
fun Modifier.background(color: Color, shape: Shape = RectangleShape): Modifier {
    val alpha = 1.0f // for solid colors
    return this.then(
        BackgroundElement(
            color = color,
            shape = shape,
            alpha = alpha,
            inspectorInfo =
                debugInspectorInfo {
                    name = "background"
                    value = color
                    properties["color"] = color
                    properties["shape"] = shape
                },
        )
    )
}

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 * @param alpha Opacity to be applied to the [brush], with `0` being completely transparent and `1`
 *   being completely opaque. The value must be between `0` and `1`.
 */
@Stable
fun Modifier.background(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
) =
    this.then(
        BackgroundElement(
            brush = brush,
            alpha = alpha,
            shape = shape,
            inspectorInfo =
                debugInspectorInfo {
                    name = "background"
                    properties["alpha"] = alpha
                    properties["brush"] = brush
                    properties["shape"] = shape
                },
        )
    )

private class BackgroundElement(
    private val color: Color = Color.Unspecified,
    private val brush: Brush? = null,
    private val alpha: Float,
    private val shape: Shape,
    private val inspectorInfo: InspectorInfo.() -> Unit,
) : ModifierNodeElement<BackgroundNode>() {
    override fun create(): BackgroundNode {
        return BackgroundNode(color, brush, alpha, shape)
    }

    override fun update(node: BackgroundNode) {
        node.color = color
        node.brush = brush
        node.alpha = alpha
        if (node.shape != shape) {
            node.shape = shape
            node.invalidateSemantics()
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + alpha.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? BackgroundElement ?: return false
        return color == otherModifier.color &&
            brush == otherModifier.brush &&
            alpha == otherModifier.alpha &&
            shape == otherModifier.shape
    }
}

private class BackgroundNode(
    var color: Color,
    var brush: Brush?,
    var alpha: Float,
    var shape: Shape,
) : DrawModifierNode, Modifier.Node(), ObserverModifierNode, SemanticsModifierNode {

    override val shouldAutoInvalidate = false
    override val isImportantForBounds = false

    // Naively cache outline calculation if input parameters are the same, we manually observe
    // reads inside shape#createOutline separately
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null
    private var lastShape: Shape? = null
    private var tmpOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            drawOutline()
        }
        drawContent()
    }

    override fun onObservedReadsChanged() {
        // Reset cached properties
        lastSize = Size.Unspecified
        lastLayoutDirection = null
        lastOutline = null
        lastShape = null
        // Invalidate draw so we build the cache again - this is needed because observeReads within
        // the draw scope obscures the state reads from the draw scope's observer
        invalidateDraw()
    }

    private fun ContentDrawScope.drawRect() {
        if (color != Color.Unspecified) drawRect(color = color)
        brush?.let { drawRect(brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.drawOutline() {
        val outline = getOutline()
        if (color != Color.Unspecified) drawOutline(outline, color = color)
        brush?.let { drawOutline(outline, brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.getOutline(): Outline {
        val outline: Outline?
        if (size == lastSize && layoutDirection == lastLayoutDirection && lastShape == shape) {
            outline = lastOutline!!
        } else {
            // Manually observe reads so we can directly invalidate the outline when it changes
            // Use tmpOutline to avoid creating an object reference to local var outline
            observeReads { tmpOutline = shape.createOutline(size, layoutDirection, this) }
            outline = tmpOutline
            tmpOutline = null
        }
        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
        lastShape = shape
        return outline!!
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this.shape = this@BackgroundNode.shape
    }
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Border.kt
```kotlin
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

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Modify element to add border with appearance specified with a [border] and a [shape] and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSample
 * @param border [BorderStroke] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape) =
    border(width = border.width, brush = border.brush, shape = shape)

/**
 * Modify element to add border with appearance specified with a [width], a [color] and a [shape]
 * and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithDataClass
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param color color to paint the border with
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape) =
    border(width, SolidColor(color), shape)

/**
 * Modify element to add border with appearance specified with a [width], a [brush] and a [shape]
 * and clip it.
 *
 * @sample androidx.compose.foundation.samples.BorderSampleWithBrush
 * @sample androidx.compose.foundation.samples.BorderSampleWithDynamicData
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(width: Dp, brush: Brush, shape: Shape) =
    this then BorderModifierNodeElement(width, brush, shape)

internal data class BorderModifierNodeElement(val width: Dp, val brush: Brush, val shape: Shape) :
    ModifierNodeElement<BorderModifierNode>() {
    override fun create() = BorderModifierNode(width, brush, shape)

    override fun update(node: BorderModifierNode) {
        node.width = width
        node.brush = brush
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "border"
        properties["width"] = width
        if (brush is SolidColor) {
            properties["color"] = brush.value
            value = brush.value
        } else {
            properties["brush"] = brush
        }
        properties["shape"] = shape
    }
}

internal class BorderModifierNode(
    widthParameter: Dp,
    brushParameter: Brush,
    shapeParameter: Shape,
) : DelegatingNode(), SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean = false
    override val isImportantForBounds = false

    // BorderCache object that is lazily allocated depending on the type of shape
    // This object is only used for generic shapes and rounded rectangles with different corner
    // radius sizes.
    // Note: Extension functions that use BorderCache are part of this class.
    private var borderCache: BorderCache? = null

    var width = widthParameter
        set(value) {
            if (field != value) {
                field = value
                drawWithCacheModifierNode.invalidateDrawCache()
            }
        }

    var brush = brushParameter
        set(value) {
            if (field != value) {
                field = value
                drawWithCacheModifierNode.invalidateDrawCache()
            }
        }

    var shape = shapeParameter
        set(value) {
            if (field != value) {
                field = value
                drawWithCacheModifierNode.invalidateDrawCache()
                invalidateSemantics()
            }
        }

    private val drawWithCacheModifierNode =
        delegate(
            CacheDrawModifierNode {
                val hasValidBorderParams = width.toPx() >= 0f && size.minDimension > 0f
                if (!hasValidBorderParams) {
                    drawContentWithoutBorder()
                } else {
                    val strokeWidthPx =
                        min(
                            if (width == Dp.Hairline) 1f else ceil(width.toPx()),
                            ceil(size.minDimension / 2),
                        )
                    val halfStroke = strokeWidthPx / 2
                    val topLeft = Offset(halfStroke, halfStroke)
                    val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                    // The stroke is larger than the drawing area so just draw a full shape instead
                    val fillArea = (strokeWidthPx * 2) > size.minDimension
                    when (val outline = shape.createOutline(size, layoutDirection, this)) {
                        is Outline.Generic ->
                            drawGenericBorder(brush, outline, fillArea, strokeWidthPx)
                        is Outline.Rounded ->
                            drawRoundRectBorder(
                                brush,
                                outline,
                                topLeft,
                                borderSize,
                                fillArea,
                                strokeWidthPx,
                            )
                        is Outline.Rectangle ->
                            drawRectBorder(brush, topLeft, borderSize, fillArea, strokeWidthPx)
                    }
                }
            }
        )

    /**
     * Border implementation for generic paths. Note it is possible to be given paths that do not
     * make sense in the context of a border (ex. a figure 8 path or a non-enclosed shape) We do not
     * handle that here as we expect developers to give us enclosed, non-overlapping paths.
     */
    private fun CacheDrawScope.drawGenericBorder(
        brush: Brush,
        outline: Outline.Generic,
        fillArea: Boolean,
        strokeWidth: Float,
    ): DrawResult =
        if (fillArea) {
            onDrawWithContent {
                drawContent()
                drawPath(outline.path, brush = brush)
            }
        } else {
            // Optimization, if we are only drawing a solid color border, we only need an alpha8
            // mask as we can draw the mask with a tint.
            // Otherwise we need to allocate a full ImageBitmap and draw it normally
            val config: ImageBitmapConfig
            val colorFilter: ColorFilter?
            if (brush is SolidColor) {
                config = ImageBitmapConfig.Alpha8
                // The brush is drawn into the mask with the corresponding color including the
                // alpha channel so when we tint we should not apply the alpha as it would end up
                // modulating it twice
                colorFilter = ColorFilter.tint(brush.value.copy(alpha = 1f))
            } else {
                config = ImageBitmapConfig.Argb8888
                colorFilter = null
            }

            val pathBounds = outline.path.getBounds()
            // Create a mask path that includes a rectangle with the original path cut out of it.
            // Note: borderCache is part of the class that defines this extension function.
            if (borderCache == null) {
                borderCache = BorderCache()
            }
            val maskPath =
                borderCache!!.obtainPath().apply {
                    reset()
                    addRect(pathBounds)
                    op(this, outline.path, PathOperation.Difference)
                }

            val cacheImageBitmap: ImageBitmap
            val pathBoundsSize =
                IntSize(ceil(pathBounds.width).toInt(), ceil(pathBounds.height).toInt())
            with(borderCache!!) {
                // Draw into offscreen bitmap with the size of the path
                // We need to draw into this intermediate bitmap to act as a layer
                // and make sure that the clearing logic does not generate underdraw
                // into the target we are rendering into
                cacheImageBitmap =
                    drawBorderCache(pathBoundsSize, config) {
                        // Paths can have offsets, so translate to keep the drawn path
                        // within the bounds of the mask bitmap
                        translate(-pathBounds.left, -pathBounds.top) {
                            // Draw the path with a stroke width twice the provided value.
                            // Because strokes are centered, this will draw both and inner and
                            // outer stroke with the desired stroke width
                            drawPath(
                                path = outline.path,
                                brush = brush,
                                style = Stroke(strokeWidth * 2),
                            )

                            // Scale the canvas slightly to cover the background that may be visible
                            // after clearing the outer stroke
                            scale((size.width + 1) / size.width, (size.height + 1) / size.height) {
                                // Remove the outer stroke by clearing the inverted mask path
                                drawPath(
                                    path = maskPath,
                                    brush = brush,
                                    blendMode = BlendMode.Clear,
                                )
                            }
                        }
                    }
            }

            onDrawWithContent {
                drawContent()
                translate(pathBounds.left, pathBounds.top) {
                    drawImage(cacheImageBitmap, srcSize = pathBoundsSize, colorFilter = colorFilter)
                }
            }
        }

    /** Border implementation for simple rounded rects and those with different corner radii */
    private fun CacheDrawScope.drawRoundRectBorder(
        brush: Brush,
        outline: Outline.Rounded,
        topLeft: Offset,
        borderSize: Size,
        fillArea: Boolean,
        strokeWidth: Float,
    ): DrawResult {
        return if (outline.roundRect.isSimple) {
            val cornerRadius = outline.roundRect.topLeftCornerRadius
            val halfStroke = strokeWidth / 2
            val borderStroke = Stroke(strokeWidth)
            onDrawWithContent {
                drawContent()
                when {
                    fillArea -> {
                        // If the drawing area is smaller than the stroke being drawn
                        // drawn all around it just draw a filled in rounded rect
                        drawRoundRect(brush, cornerRadius = cornerRadius)
                    }
                    cornerRadius.x < halfStroke -> {
                        // If the corner radius is smaller than half of the stroke width
                        // then the interior curvature of the stroke will be a sharp edge
                        // In this case just draw a normal filled in rounded rect with the
                        // desired corner radius but clipping out the interior rectangle
                        clipRect(
                            strokeWidth,
                            strokeWidth,
                            size.width - strokeWidth,
                            size.height - strokeWidth,
                            clipOp = ClipOp.Difference,
                        ) {
                            drawRoundRect(brush, cornerRadius = cornerRadius)
                        }
                    }
                    else -> {
                        // Otherwise draw a stroked rounded rect with the corner radius
                        // shrunk by half of the stroke width. This will ensure that the
                        // outer curvature of the rounded rectangle will have the desired
                        // corner radius.
                        drawRoundRect(
                            brush = brush,
                            topLeft = topLeft,
                            size = borderSize,
                            cornerRadius = cornerRadius.shrink(halfStroke),
                            style = borderStroke,
                        )
                    }
                }
            }
        } else {
            // Note: borderCache is part of the class that defines this extension function.
            if (borderCache == null) {
                borderCache = BorderCache()
            }
            val path = borderCache!!.obtainPath()
            val roundedRectPath =
                createRoundRectPath(path, outline.roundRect, strokeWidth, fillArea)
            onDrawWithContent {
                drawContent()
                drawPath(roundedRectPath, brush = brush)
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        shape = this@BorderModifierNode.shape
    }
}

/**
 * Helper object that handles lazily allocating and re-using objects to render the border into an
 * offscreen ImageBitmap
 */
private data class BorderCache(
    private var imageBitmap: ImageBitmap? = null,
    private var canvas: androidx.compose.ui.graphics.Canvas? = null,
    private var canvasDrawScope: CanvasDrawScope? = null,
    private var borderPath: Path? = null,
) {
    inline fun CacheDrawScope.drawBorderCache(
        borderSize: IntSize,
        config: ImageBitmapConfig,
        block: DrawScope.() -> Unit,
    ): ImageBitmap {

        var targetImageBitmap = imageBitmap
        var targetCanvas = canvas
        // If we previously had allocated a full Argb888 ImageBitmap but are only requiring
        // an alpha mask, just re-use the same ImageBitmap instead of allocating a new one
        val compatibleConfig =
            targetImageBitmap?.config == ImageBitmapConfig.Argb8888 ||
                config == targetImageBitmap?.config
        if (
            targetImageBitmap == null ||
                targetCanvas == null ||
                size.width > targetImageBitmap.width ||
                size.height > targetImageBitmap.height ||
                !compatibleConfig
        ) {
            targetImageBitmap =
                ImageBitmap(borderSize.width, borderSize.height, config = config).also {
                    imageBitmap = it
                }
            targetCanvas =
                androidx.compose.ui.graphics.Canvas(targetImageBitmap).also { canvas = it }
        }

        val targetDrawScope = canvasDrawScope ?: CanvasDrawScope().also { canvasDrawScope = it }
        val drawSize = borderSize.toSize()
        targetDrawScope.draw(this, layoutDirection, targetCanvas, drawSize) {
            // Clear the previously rendered portion within this ImageBitmap as we could
            // be re-using it
            drawRect(color = Color.Black, size = drawSize, blendMode = BlendMode.Clear)
            block()
        }
        targetImageBitmap.prepareToDraw()
        return targetImageBitmap
    }

    fun obtainPath(): Path = borderPath ?: Path().also { borderPath = it }
}

/**
 * Border implementation for invalid parameters that just draws the content as the given border
 * parameters are infeasible (ex. negative border width)
 */
private fun CacheDrawScope.drawContentWithoutBorder(): DrawResult = onDrawWithContent {
    drawContent()
}

/** Border implementation for rectangular borders */
private fun CacheDrawScope.drawRectBorder(
    brush: Brush,
    topLeft: Offset,
    borderSize: Size,
    fillArea: Boolean,
    strokeWidthPx: Float,
): DrawResult {
    // If we are drawing a rectangular stroke, just offset it by half the stroke
    // width as strokes are always drawn centered on their geometry.
    // If the border is larger than the drawing area, just fill the area with a
    // solid rectangle
    val rectTopLeft = if (fillArea) Offset.Zero else topLeft
    val size = if (fillArea) size else borderSize
    val style = if (fillArea) Fill else Stroke(strokeWidthPx)
    return onDrawWithContent {
        drawContent()
        drawRect(brush = brush, topLeft = rectTopLeft, size = size, style = style)
    }
}

/**
 * Helper method that creates a round rect with the inner region removed by the given stroke width
 */
private fun createRoundRectPath(
    targetPath: Path,
    roundedRect: RoundRect,
    strokeWidth: Float,
    fillArea: Boolean,
): Path =
    targetPath.apply {
        reset()
        addRoundRect(roundedRect)
        if (!fillArea) {
            val insetPath =
                Path().apply { addRoundRect(createInsetRoundedRect(strokeWidth, roundedRect)) }
            op(this, insetPath, PathOperation.Difference)
        }
    }

private fun createInsetRoundedRect(widthPx: Float, roundedRect: RoundRect) =
    RoundRect(
        left = widthPx,
        top = widthPx,
        right = roundedRect.width - widthPx,
        bottom = roundedRect.height - widthPx,
        topLeftCornerRadius = roundedRect.topLeftCornerRadius.shrink(widthPx),
        topRightCornerRadius = roundedRect.topRightCornerRadius.shrink(widthPx),
        bottomLeftCornerRadius = roundedRect.bottomLeftCornerRadius.shrink(widthPx),
        bottomRightCornerRadius = roundedRect.bottomRightCornerRadius.shrink(widthPx),
    )

/**
 * Helper method to shrink the corner radius by the given value, clamping to 0 if the resultant
 * corner radius would be negative
 */
private fun CornerRadius.shrink(value: Float): CornerRadius =
    CornerRadius(max(0f, this.x - value), max(0f, this.y - value))
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/shape/RoundedCornerShape.kt
```kotlin
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

package androidx.compose.foundation.shape

import androidx.annotation.IntRange
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A shape describing the rectangle with rounded corners.
 *
 * This shape will automatically mirror the corner sizes in [LayoutDirection.Rtl], use
 * [AbsoluteRoundedCornerShape] for the layout direction unaware version of this shape.
 *
 * @param topStart a size of the top start corner
 * @param topEnd a size of the top end corner
 * @param bottomEnd a size of the bottom end corner
 * @param bottomStart a size of the bottom start corner
 */
class RoundedCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) :
    CornerBasedShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    ) {

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        return if (topStart + topEnd + bottomEnd + bottomStart == 0.0f) {
            Outline.Rectangle(size.toRect())
        } else {
            Outline.Rounded(
                RoundRect(
                    rect = size.toRect(),
                    topLeft = CornerRadius(if (layoutDirection == Ltr) topStart else topEnd),
                    topRight = CornerRadius(if (layoutDirection == Ltr) topEnd else topStart),
                    bottomRight =
                        CornerRadius(if (layoutDirection == Ltr) bottomEnd else bottomStart),
                    bottomLeft =
                        CornerRadius(if (layoutDirection == Ltr) bottomStart else bottomEnd),
                )
            )
        }
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ) =
        RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
        )

    override fun toString(): String {
        return "RoundedCornerShape(topStart = $topStart, topEnd = $topEnd, bottomEnd = " +
            "$bottomEnd, bottomStart = $bottomStart)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoundedCornerShape) return false

        if (topStart != other.topStart) return false
        if (topEnd != other.topEnd) return false
        if (bottomEnd != other.bottomEnd) return false
        if (bottomStart != other.bottomStart) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }

    override fun lerp(other: Any?, t: Float): Any? {
        var other: Any? = other
        if (other == RectangleShape || other == null) {
            other = RoundedCornerShape(0f)
        }
        if (other is RoundedCornerShape) {
            return lerp(this, other, t)
        }
        return null
    }
}

internal fun lerp(a: RoundedCornerShape, b: RoundedCornerShape, t: Float): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = lerp(a.topStart, b.topStart, t),
        topEnd = lerp(a.topEnd, b.topEnd, t),
        bottomEnd = lerp(a.bottomEnd, b.bottomEnd, t),
        bottomStart = lerp(a.bottomStart, b.bottomStart, t),
    )
}

internal fun lerp(a: CornerSize, b: CornerSize, t: Float): CornerSize {
    return object : CornerSize {
        override fun toPx(shapeSize: Size, density: Density): Float {
            return lerp(a.toPx(shapeSize, density), b.toPx(shapeSize, density), t)
        }
    }
}

/** Circular [Shape] with all the corners sized as the 50 percent of the shape size. */
val CircleShape = RoundedCornerShape(50)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 *
 * @param corner [CornerSize] to apply.
 */
fun RoundedCornerShape(corner: CornerSize) = RoundedCornerShape(corner, corner, corner, corner)

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 *
 * @param size Size in [Dp] to apply.
 */
fun RoundedCornerShape(size: Dp) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 *
 * @param size Size in pixels to apply.
 */
fun RoundedCornerShape(size: Float) = RoundedCornerShape(CornerSize(size))

/**
 * Creates [RoundedCornerShape] with the same size applied for all four corners.
 *
 * @param percent Size in percents to apply.
 */
fun RoundedCornerShape(percent: Int) = RoundedCornerShape(CornerSize(percent))

/** Creates [RoundedCornerShape] with sizes defined in [Dp]. */
fun RoundedCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
) =
    RoundedCornerShape(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
    )

/** Creates [RoundedCornerShape] with sizes defined in pixels. */
fun RoundedCornerShape(
    topStart: Float = 0.0f,
    topEnd: Float = 0.0f,
    bottomEnd: Float = 0.0f,
    bottomStart: Float = 0.0f,
) =
    RoundedCornerShape(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
    )

/**
 * Creates [RoundedCornerShape] with sizes defined in percents of the shape's smaller side.
 *
 * @param topStartPercent The top start corner radius as a percentage of the smaller side, with a
 *   range of 0 - 100.
 * @param topEndPercent The top end corner radius as a percentage of the smaller side, with a range
 *   of 0 - 100.
 * @param bottomEndPercent The bottom end corner radius as a percentage of the smaller side, with a
 *   range of 0 - 100.
 * @param bottomStartPercent The bottom start corner radius as a percentage of the smaller side,
 *   with a range of 0 - 100.
 */
fun RoundedCornerShape(
    @IntRange(from = 0, to = 100) topStartPercent: Int = 0,
    @IntRange(from = 0, to = 100) topEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomEndPercent: Int = 0,
    @IntRange(from = 0, to = 100) bottomStartPercent: Int = 0,
) =
    RoundedCornerShape(
        topStart = CornerSize(topStartPercent),
        topEnd = CornerSize(topEndPercent),
        bottomEnd = CornerSize(bottomEndPercent),
        bottomStart = CornerSize(bottomStartPercent),
    )
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/pager/Pager.kt
```kotlin
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

package androidx.compose.foundation.pager

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.calculateFinalSnappingBound
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.pageDown
import androidx.compose.ui.semantics.pageLeft
import androidx.compose.ui.semantics.pageRight
import androidx.compose.ui.semantics.pageUp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A Pager that scrolls horizontally. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [beyondViewportPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [snapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one. Use [pageSpacing] to add spacing between
 *   the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 *   pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot
 *   of pages to be composed, measured and placed which will defeat the purpose of using lazy
 *   loading. This should be used as an optimization to pre-load a couple of pages before and after
 *   the visible ones. This does not include the pages automatically composed and laid out by the
 *   pre-fetcher in the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 *   disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 *   position will be maintained based on the key, which means if you add/remove items before the
 *   current visible item the item with the given key will be kept as the first visible one. If null
 *   is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager]
 *   behaves with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of pages. Use this to
 *   provide different settling to different positions in the layout. This is used by [Pager] as a
 *   way to calculate [PagerState.currentPage], currentPage is the page closest to the snap position
 *   in the layout (e.g. if the snap position is the start of the layout, then currentPage will be
 *   the page closest to that).
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   Pager. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param pageContent This Pager's page Composable.
 * @sample androidx.compose.foundation.samples.SimpleHorizontalPagerSample
 * @sample androidx.compose.foundation.samples.HorizontalPagerWithScrollableContent
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 *   of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the samples to learn how to use this API.
 */
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = overscrollEffect,
        pageContent = pageContent,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal),
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = rememberOverscrollEffect(),
        pageContent = pageContent,
    )
}

/**
 * A Pager that scrolls vertically. Pages are lazily placed in accordance to the available viewport
 * size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and use a snap
 * animation (provided by [flingBehavior] to scroll pages into a specific position). You can use
 * [beyondViewportPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [snapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via [modifier] param. You can use it to add a
 *   padding before the first page or after the last one. Use [pageSpacing] to add spacing between
 *   the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 *   pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot
 *   of pages to be composed, measured and placed which will defeat the purpose of using lazy
 *   loading. This should be used as an optimization to pre-load a couple of pages before and after
 *   the visible ones. This does not include the pages automatically composed and laid out by the
 *   pre-fetcher in
 *     * the direction of the scroll during scroll events.
 *
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param horizontalAlignment How pages are aligned horizontally in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions is
 *   allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 *   disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 *   position will be maintained based on the key, which means if you add/remove items before the
 *   current visible item the item with the given key will be kept as the first visible one. If null
 *   is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager]
 *   behaves with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of Pages. Use this to
 *   provide different settling to different positions in the layout. This is used by [Pager] as a
 *   way to calculate [PagerState.currentPage], currentPage is the page closest to the snap position
 *   in the layout (e.g. if the snap position is the start of the layout, then currentPage will be
 *   the page closest to that).
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   Pager. Note that the [OverscrollEffect.node] will be applied internally as well - you do not
 *   need to use Modifier.overscroll separately.
 * @param pageContent This Pager's page Composable.
 * @sample androidx.compose.foundation.samples.SimpleVerticalPagerSample
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 *   of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the sample to learn how to use this API.
 */
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Vertical),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Vertical,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = overscrollEffect,
        pageContent = pageContent,
    )
}

@Deprecated("Use the non deprecated overload", level = DeprecationLevel.HIDDEN)
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection =
        PagerDefaults.pageNestedScrollConnection(state, Orientation.Vertical),
    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    VerticalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = snapPosition,
        overscrollEffect = rememberOverscrollEffect(),
        pageContent = pageContent,
    )
}

/** Contains the default values used by [Pager]. */
object PagerDefaults {

    /**
     * A [snapFlingBehavior] that will snap pages to the start of the layout. One can use the given
     * parameters to control how the snapping animation will happen.
     *
     * @param state The [PagerState] that controls the which to which this FlingBehavior will be
     *   applied to.
     * @param pagerSnapDistance A way to control the snapping destination for this [Pager]. The
     *   default behavior will result in any fling going to the next page in the direction of the
     *   fling (if the fling has enough velocity, otherwise the Pager will bounce back). Use
     *   [PagerSnapDistance.atMost] to define a maximum number of pages this [Pager] is allowed to
     *   fling after scrolling is finished and fling has started.
     * @param decayAnimationSpec The animation spec used to approach the target offset. When the
     *   fling velocity is large enough. Large enough means large enough to naturally decay. For
     *   single page snapping this usually never happens since there won't be enough space to run a
     *   decay animation.
     * @param snapAnimationSpec The animation spec used to finally snap to the position. This
     *   animation will be often used in 2 cases: 1) There was enough space to an approach
     *   animation, the Pager will use [snapAnimationSpec] in the last step of the animation to
     *   settle the page into position. 2) There was not enough space to run the approach animation.
     * @param snapPositionalThreshold If the fling has a low velocity (e.g. slow scroll), this fling
     *   behavior will use this snap threshold in order to determine if the pager should snap back
     *   or move forward. Use a number between 0 and 1 as a fraction of the page size that needs to
     *   be scrolled before the Pager considers it should move to the next page. For instance, if
     *   snapPositionalThreshold = 0.35, it means if this pager is scrolled with a slow velocity and
     *   the Pager scrolls more than 35% of the page size, then will jump to the next page, if not
     *   it scrolls back. Note that any fling that has high enough velocity will *always* move to
     *   the next page in the direction of the fling.
     * @return An instance of [FlingBehavior] that will perform Snapping to the next page by
     *   default. The animation will be governed by the post scroll velocity and the Pager will use
     *   either [snapAnimationSpec] or [decayAnimationSpec] to approach the snapped position If a
     *   velocity is not high enough the pager will use [snapAnimationSpec] to reach the snapped
     *   position. If the velocity is high enough, the Pager will use the logic described in
     *   [decayAnimationSpec] and [snapAnimationSpec].
     * @see androidx.compose.foundation.gestures.snapping.snapFlingBehavior for more information on
     *   what which parameter controls in the overall snapping animation.
     *
     * The animation specs used by the fling behavior will depend on 2 factors:
     * 1) The gesture velocity.
     * 2) The target page proposed by [pagerSnapDistance].
     *
     * If you're using single page snapping (the most common use case for [Pager]), there won't be
     * enough space to actually run a decay animation to approach the target page, so the Pager will
     * always use the snapping animation from [snapAnimationSpec]. If you're using multi-page
     * snapping (this means you're abs(targetPage - currentPage) > 1) the Pager may use
     * [decayAnimationSpec] or [snapAnimationSpec] to approach the targetPage, it will depend on the
     * velocity generated by the triggering gesture. If the gesture has a high enough velocity to
     * approach the target page, the Pager will use [decayAnimationSpec] followed by
     * [snapAnimationSpec] for the final step of the animation. If the gesture doesn't have enough
     * velocity, the Pager will use [snapAnimationSpec] + [snapAnimationSpec] in a similar fashion.
     */
    @Composable
    fun flingBehavior(
        state: PagerState,
        pagerSnapDistance: PagerSnapDistance = PagerSnapDistance.atMost(1),
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = Int.VisibilityThreshold.toFloat(),
            ),
        @FloatRange(from = 0.0, to = 1.0) snapPositionalThreshold: Float = 0.5f,
    ): TargetedFlingBehavior {
        requirePrecondition(snapPositionalThreshold in 0f..1f) {
            "snapPositionalThreshold should be a number between 0 and 1. " +
                "You've specified $snapPositionalThreshold"
        }
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        return remember(
            state,
            decayAnimationSpec,
            snapAnimationSpec,
            pagerSnapDistance,
            density,
            layoutDirection,
        ) {
            val snapLayoutInfoProvider =
                SnapLayoutInfoProvider(state, pagerSnapDistance) {
                    flingVelocity,
                    lowerBound,
                    upperBound ->
                    calculateFinalSnappingBound(
                        pagerState = state,
                        layoutDirection = layoutDirection,
                        snapPositionalThreshold = snapPositionalThreshold,
                        flingVelocity = flingVelocity,
                        lowerBoundOffset = lowerBound,
                        upperBoundOffset = upperBound,
                    )
                }

            snapFlingBehavior(
                snapLayoutInfoProvider = snapLayoutInfoProvider,
                decayAnimationSpec = decayAnimationSpec,
                snapAnimationSpec = snapAnimationSpec,
            )
        }
    }

    /**
     * The default implementation of Pager's pageNestedScrollConnection.
     *
     * @param state state of the pager
     * @param orientation The orientation of the pager. This will be used to determine which
     *   direction the nested scroll connection will operate and react on.
     */
    @Composable
    fun pageNestedScrollConnection(
        state: PagerState,
        orientation: Orientation,
    ): NestedScrollConnection {
        return remember(state, orientation) {
            DefaultPagerNestedScrollConnection(state, orientation)
        }
    }

    /**
     * The default value of beyondViewportPageCount used to specify the number of pages to compose
     * and layout before and after the visible pages. It does not include the pages automatically
     * composed and laid out by the pre-fetcher in the direction of the scroll during scroll events.
     */
    const val BeyondViewportPageCount = 0
}

internal fun SnapPosition.currentPageOffset(
    layoutSize: Int,
    pageSize: Int,
    spaceBetweenPages: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): Int {
    val snapOffset =
        position(
            layoutSize,
            pageSize,
            beforeContentPadding,
            afterContentPadding,
            currentPage,
            pageCount,
        )

    return (snapOffset - currentPageOffsetFraction * (pageSize + spaceBetweenPages)).roundToInt()
}

private class DefaultPagerNestedScrollConnection(
    val state: PagerState,
    val orientation: Orientation,
) : NestedScrollConnection {

    fun Velocity.consumeOnOrientation(orientation: Orientation): Velocity {
        return if (orientation == Orientation.Vertical) {
            copy(x = 0f)
        } else {
            copy(y = 0f)
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (
            // rounding error and drag only
            source == NestedScrollSource.UserInput &&
                abs(state.currentPageOffsetFraction) > 1e-6 &&
                // only need to treat deltas on this Pager's orientation
                available.toFloat().absoluteValue > 0f
        ) {
            // find the current and next page (in the direction of dragging)
            val currentPageOffset = state.currentPageOffsetFraction * state.pageSize
            val pageAvailableSpace = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
            val nextClosestPageOffset =
                currentPageOffset + pageAvailableSpace * -sign(state.currentPageOffsetFraction)

            val minBound: Float
            val maxBound: Float
            // build min and max bounds in absolute coordinates for nested scroll
            if (state.currentPageOffsetFraction > 0f) {
                minBound = nextClosestPageOffset
                maxBound = currentPageOffset
            } else {
                minBound = currentPageOffset
                maxBound = nextClosestPageOffset
            }

            val delta = available.toFloat()
            val coerced = delta.coerceIn(minBound, maxBound)
            // dispatch and return reversed as usual
            val consumed = -state.dispatchRawDelta(-coerced)
            available.copy(
                x = if (orientation == Orientation.Horizontal) consumed else available.x,
                y = if (orientation == Orientation.Vertical) consumed else available.y,
            )
        } else {
            Offset.Zero
        }
    }

    private fun Offset.toFloat() = if (orientation == Orientation.Horizontal) x else y

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.SideEffect && available.mainAxis() != 0f) {
            throw CancellationException("Scroll cancelled")
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return available.consumeOnOrientation(orientation)
    }

    private fun Offset.mainAxis(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y
}

internal fun Modifier.pagerSemantics(
    state: PagerState,
    isVertical: Boolean,
    scope: CoroutineScope,
    userScrollEnabled: Boolean,
): Modifier {
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch { state.animateToNextPage() }
            true
        } else {
            false
        }
    }

    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch { state.animateToPreviousPage() }
            true
        } else {
            false
        }
    }

    return if (userScrollEnabled) {
        this.then(
            Modifier.semantics {
                if (isVertical) {
                    pageUp { performBackwardPaging() }
                    pageDown { performForwardPaging() }
                } else {
                    pageLeft { performBackwardPaging() }
                    pageRight { performForwardPaging() }
                }
            }
        )
    } else {
        this then Modifier
    }
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.MainPagerComposable) {
        println("Pager: ${generateMsg()}")
    }
}

internal object PagerDebugConfig {
    const val MainPagerComposable = false
    const val PagerState = false
    const val MeasureLogic = false
    const val ScrollPosition = false
    const val PagerSnapDistance = false
    const val PagerSnapLayoutInfoProvider = false
}
```

## File: compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/pager/PagerState.kt
```kotlin
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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation.pager

import androidx.annotation.FloatRange
import androidx.annotation.IntRange as AndroidXIntRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ComposeFoundationFlags.isCacheWindowForPagerEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 *
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int,
): PagerState {
    return rememberSaveable(saver = DefaultPagerState.Saver) {
            DefaultPagerState(initialPage, initialPageOffsetFraction, pageCount)
        }
        .apply { pageCountState.value = pageCount }
}

/**
 * Creates a default [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 *
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 * @param currentPage The pager that should be shown first.
 * @param currentPageOffsetFraction The offset of the initial page as a fraction of the page size.
 *   This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 *   snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
fun PagerState(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    pageCount: () -> Int,
): PagerState = DefaultPagerState(currentPage, currentPageOffsetFraction, pageCount)

private class DefaultPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
) : PagerState(currentPage, currentPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()

    companion object {
        /** To keep current page and current page offset saved */
        val Saver: Saver<DefaultPagerState, *> =
            listSaver(
                save = {
                    listOf(
                        it.currentPage,
                        (it.currentPageOffsetFraction).coerceIn(MinPageOffset, MaxPageOffset),
                        it.pageCount,
                    )
                },
                restore = {
                    DefaultPagerState(
                        currentPage = it[0] as Int,
                        currentPageOffsetFraction = it[1] as Float,
                        updatedPageCount = { it[2] as Int },
                    )
                },
            )
    }
}

/** The state that can be used to control [VerticalPager] and [HorizontalPager] */
@OptIn(ExperimentalFoundationApi::class)
@Stable
abstract class PagerState
internal constructor(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    prefetchScheduler: PrefetchScheduler? = null,
) : ScrollableState {

    /**
     * @param currentPage The initial page to be displayed
     * @param currentPageOffsetFraction The offset of the initial page with respect to the start of
     *   the layout.
     */
    constructor(
        currentPage: Int = 0,
        @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    ) : this(currentPage, currentPageOffsetFraction, null)

    internal var hasLookaheadOccurred: Boolean = false
        private set

    internal var approachLayoutInfo: PagerMeasureResult? = null
        private set

    /**
     * The total amount of pages present in this pager. The source of this data should be
     * observable.
     */
    abstract val pageCount: Int

    init {
        requirePrecondition(currentPageOffsetFraction in -0.5..0.5) {
            "currentPageOffsetFraction $currentPageOffsetFraction is " +
                "not within the range -0.5 to 0.5"
        }
    }

    /** Difference between the last up and last down events of a scroll event. */
    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)

    private val scrollPosition = PagerScrollPosition(currentPage, currentPageOffsetFraction, this)

    internal var firstVisiblePage = currentPage
        private set

    internal var firstVisiblePageOffset = 0
        private set

    internal var maxScrollOffset: Long = Long.MAX_VALUE

    internal var minScrollOffset: Long = 0L

    private var accumulator: Float = 0.0f

    /**
     * The prefetch will act after the measure pass has finished and it needs to know the magnitude
     * and direction of the scroll that triggered the measure pass
     */
    private var previousPassDelta = 0f

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once we
     * reached the end of the list.
     */
    private val scrollableState = ScrollableState { performScroll(it) }

    /**
     * Within the scrolling context we can use absolute positions to determine scroll deltas and max
     * min scrolling.
     */
    private fun performScroll(delta: Float): Float {
        val currentScrollPosition = currentAbsoluteScrollOffset()
        debugLog {
            "\nDelta=$delta " +
                "\ncurrentScrollPosition=$currentScrollPosition " +
                "\naccumulator=$accumulator" +
                "\nmaxScrollOffset=$maxScrollOffset"
        }

        val decimalAccumulation = (delta + accumulator)
        val decimalAccumulationInt = decimalAccumulation.roundToLong()
        accumulator = decimalAccumulation - decimalAccumulationInt

        // nothing to scroll
        if (delta.absoluteValue < 1e-4f) return delta

        /**
         * The updated scroll position is the current position with the integer part of the delta
         * and accumulator applied.
         */
        val updatedScrollPosition = (currentScrollPosition + decimalAccumulationInt)

        /** Check if the scroll position may be larger than the maximum possible scroll. */
        val coercedScrollPosition = updatedScrollPosition.coerceIn(minScrollOffset, maxScrollOffset)

        /** Check if we actually coerced. */
        val changed = updatedScrollPosition != coercedScrollPosition

        /** Calculated the actual scroll delta to be applied */
        val scrollDelta = coercedScrollPosition - currentScrollPosition

        previousPassDelta = scrollDelta.toFloat()

        if (scrollDelta.absoluteValue != 0L) {
            isLastScrollForwardState.value = scrollDelta > 0.0f
            isLastScrollBackwardState.value = scrollDelta < 0.0f
        }

        /** Apply the scroll delta */
        var scrolledLayoutInfo =
            pagerLayoutInfoState.value.copyWithScrollDeltaWithoutRemeasure(
                delta = -scrollDelta.toInt()
            )
        if (scrolledLayoutInfo != null && this.approachLayoutInfo != null) {
            // if we were able to scroll the lookahead layout info without remeasure, lets
            // try to do the same for post lookahead layout info (sometimes they diverge).
            val scrolledApproachLayoutInfo =
                approachLayoutInfo?.copyWithScrollDeltaWithoutRemeasure(
                    delta = -scrollDelta.toInt()
                )
            if (scrolledApproachLayoutInfo != null) {
                // we can apply scroll delta for both phases without remeasure
                approachLayoutInfo = scrolledApproachLayoutInfo
            } else {
                // we can't apply scroll delta for post lookahead, so we have to remeasure
                scrolledLayoutInfo = null
            }
        }
        if (scrolledLayoutInfo != null) {
            debugLog { "Will Apply Without Remeasure" }
            applyMeasureResult(
                result = scrolledLayoutInfo,
                isLookingAhead = hasLookaheadOccurred,
                visibleItemsStayedTheSame = true,
            )
            // we don't need to remeasure, so we only trigger re-placement:
            placementScopeInvalidator.invalidateScope()
            layoutWithoutMeasurement++
        } else {
            debugLog { "Will Apply With Remeasure" }
            scrollPosition.applyScrollDelta(scrollDelta.toInt())
            remeasurement?.forceRemeasure()
            layoutWithMeasurement++
        }

        // Return the consumed value.
        return (if (changed) scrollDelta else delta).toFloat()
    }

    /** Only used for testing to confirm that we're not making too many measure passes */
    internal val numMeasurePasses: Int
        get() = layoutWithMeasurement + layoutWithoutMeasurement

    internal var layoutWithMeasurement: Int = 0
        private set

    private var layoutWithoutMeasurement: Int = 0

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    internal var prefetchingEnabled: Boolean = true

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var indexToPrefetch = -1

    /** The handle associated with the current index from [indexToPrefetch]. */
    private var currentPrefetchHandle: LazyLayoutPrefetchState.PrefetchHandle? = null

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to detect
     * the scrolling direction change.
     */
    private var wasPrefetchingForward = false

    /** Backing state for PagerLayoutInfo */
    private var pagerLayoutInfoState = mutableStateOf(EmptyLayoutInfo, neverEqualPolicy())

    /**
     * A [PagerLayoutInfo] that contains useful information about the Pager's last layout pass. For
     * instance, you can query which pages are currently visible in the layout.
     *
     * This property is observable and is updated after every scroll or remeasure. If you use it in
     * the composable function it will be recomposed on every change causing potential performance
     * issues including infinity recomposition loop. Therefore, avoid using it in the composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     *
     * @sample androidx.compose.foundation.samples.UsingPagerLayoutInfoForSideEffectSample
     */
    val layoutInfo: PagerLayoutInfo
        get() = pagerLayoutInfoState.value

    internal val pageSpacing: Int
        get() = pagerLayoutInfoState.value.pageSpacing

    internal val pageSize: Int
        get() = pagerLayoutInfoState.value.pageSize

    internal var density: Density = UnitDensity

    internal val pageSizeWithSpacing: Int
        get() = pageSize + pageSpacing

    // non state backed version
    internal var latestPageSizeWithSpacing: Int = 0

    /**
     * How far the current page needs to scroll so the target page is considered to be the next
     * page.
     */
    internal val positionThresholdFraction: Float
        get() =
            with(density) {
                val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
                minThreshold / pageSize.toFloat()
            }

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    /**
     * The page that sits closest to the snapped position. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val currentPage: Int
        get() = scrollPosition.currentPage

    private var programmaticScrollTargetPage by mutableIntStateOf(-1)

    private var settledPageState by mutableIntStateOf(currentPage)

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val settledPage by
        derivedStateOf(structuralEqualityPolicy()) {
            if (isScrollInProgress) {
                settledPageState
            } else {
                this.currentPage
            }
        }

    /**
     * The page this [Pager] intends to settle to. During fling or animated scroll (from
     * [animateScrollToPage] this will represent the page this pager intends to settle to. When no
     * scroll is ongoing, this will be equal to [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val targetPage: Int by
        derivedStateOf(structuralEqualityPolicy()) {
            val finalPage =
                if (!isScrollInProgress) {
                    this.currentPage
                } else if (programmaticScrollTargetPage != -1) {
                    programmaticScrollTargetPage
                } else {
                    // act on scroll only
                    if (abs(this.currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                        if (lastScrolledForward) {
                            firstVisiblePage + 1
                        } else {
                            firstVisiblePage
                        }
                    } else {
                        this.currentPage
                    }
                }
            finalPage.coerceInPageRange()
        }

    /**
     * Indicates how far the current page is to the snapped position, this will vary from -0.5 (page
     * is offset towards the start of the layout) to 0.5 (page is offset towards the end of the
     * layout). This is 0.0 if the [currentPage] is in the snapped position. The value will flip
     * once the current page changes.
     *
     * This property is observable and shouldn't be used as is in a composable function due to
     * potential performance issues. To use it in the composition, please consider using a derived
     * state (e.g [derivedStateOf]) to only have recompositions when the derived value changes.
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val currentPageOffsetFraction: Float
        @FrequentlyChangingValue get() = scrollPosition.currentPageOffsetFraction

    internal val prefetchState =
        LazyLayoutPrefetchState(prefetchScheduler) {
            Snapshot.withoutReadObservation { schedulePrecomposition(firstVisiblePage) }
        }

    /**
     * Cache window in Pager Initial Layout prefetching happens after the initial measure pass and
     * latestPageSizeWithSpacing is updated before the prefetching happens.
     *
     * For scroll backed prefetching we will use the last known latestPageSizeWithSpacing.
     */
    private val pagerCacheWindow =
        object : LazyLayoutCacheWindow {
            override fun Density.calculateAheadWindow(viewport: Int): Int =
                latestPageSizeWithSpacing

            override fun Density.calculateBehindWindow(viewport: Int): Int = 0
        }

    private val _scrollIndicatorState =
        object : ScrollIndicatorState {
            override val scrollOffset: Int
                get() = calculateScrollOffset()

            override val contentSize: Int
                get() = layoutInfo.calculateContentSize(pageCount)

            override val viewportSize: Int
                get() = layoutInfo.mainAxisViewportSize
        }

    private fun calculateScrollOffset(): Int {
        val totalScrollOffset =
            (pageSizeWithSpacing * firstVisiblePage.toLong()) + firstVisiblePageOffset
        return totalScrollOffset.fastCoerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    internal val cacheWindowLogic =
        PagerCacheWindowLogic(pagerCacheWindow, prefetchState) { pageCount }

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? by mutableStateOf(null)
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@PagerState.remeasurement = remeasurement
            }
        }

    /** Constraints passed to the prefetcher for premeasuring the prefetched items. */
    internal var premeasureConstraints = Constraints()

    /** Stores currently pinned pages which are always composed, used by for beyond bound pages. */
    internal val pinnedPages = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Scroll (jump immediately) to a given [page].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.ScrollToPageSample
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     */
    suspend fun scrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
    ) = scroll {
        debugLog { "Scroll from page=$currentPage to page=$page" }
        awaitScrollDependencies()
        requirePrecondition(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        snapToItem(targetPage, pageOffsetFraction, forceRemeasure = true)
    }

    /**
     * Jump immediately to a given [page] with a given [pageOffsetFraction] inside a [ScrollScope].
     * Use this method to create custom animated scrolling experiences. This will update the value
     * of [currentPage] and [currentPageOffsetFraction] immediately, but can only be used inside a
     * [ScrollScope], use [scroll] to gain access to a [ScrollScope].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     */
    fun ScrollScope.updateCurrentPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f,
    ) {
        snapToItem(page, pageOffsetFraction, forceRemeasure = true)
    }

    /**
     * Used to update [targetPage] during a programmatic scroll operation. This can only be called
     * inside a [ScrollScope] and should be called anytime a custom scroll (through [scroll]) is
     * executed in order to correctly update [targetPage]. This will not move the pages and it's
     * still the responsibility of the caller to call [ScrollScope.scrollBy] in order to actually
     * get to [targetPage]. By the end of the [scroll] block, when the [Pager] is no longer
     * scrolling [targetPage] will assume the value of [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
     */
    fun ScrollScope.updateTargetPage(targetPage: Int) {
        programmaticScrollTargetPage = targetPage.coerceInPageRange()
    }

    internal fun snapToItem(page: Int, offsetFraction: Float, forceRemeasure: Boolean) {
        val positionChanged =
            scrollPosition.currentPage != page ||
                scrollPosition.currentPageOffsetFraction != offsetFraction
        if (positionChanged) {
            // we changed positions, cancel existing requests and wait for the next scroll to
            // refill the window
            cacheWindowLogic.resetStrategy()
        }

        scrollPosition.requestPositionAndForgetLastKnownKey(page, offsetFraction)
        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        } else {
            measurementScopeInvalidator.invalidateScope()
        }
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the [page] to be at the snapped position during the next remeasure, offset by
     * [pageOffsetFraction], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the current page key (when a data set change will also be applied during the next
     * remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param page the index to which to scroll. Must be non-negative.
     * @param pageOffsetFraction the offset fraction that the page should end up after the scroll.
     */
    fun requestScrollToPage(
        @AndroidXIntRange(from = 0) page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f,
    ) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            pagerLayoutInfoState.value.coroutineScope.launch { stopScroll() }
        }

        snapToItem(page, pageOffsetFraction, forceRemeasure = false)
    }

    /**
     * Scroll animate to a given [page]'s closest snap position. If the [page] is too far away from
     * [currentPage] we will not compose all pages in the way. We will pre-jump to a nearer page,
     * compose and animate the rest of the pages until [page].
     *
     * Please refer to the sample to learn how to use this API.
     *
     * @sample androidx.compose.foundation.samples.AnimateScrollPageSample
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     *   destination page will be offset from its snapped position.
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     *   default animation.
     */
    suspend fun animateScrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring(),
    ) {
        if (
            page == currentPage && currentPageOffsetFraction == pageOffsetFraction || pageCount == 0
        )
            return
        awaitScrollDependencies()
        requirePrecondition(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        val targetPageOffsetToSnappedPosition = (pageOffsetFraction * pageSizeWithSpacing)

        scroll {
            LazyLayoutScrollScope(this@PagerState, this)
                .animateScrollToPage(
                    targetPage,
                    targetPageOffsetToSnappedPosition,
                    animationSpec,
                    updateTargetPage = { updateTargetPage(it) },
                )
        }
    }

    private suspend fun awaitScrollDependencies() {
        if (pagerLayoutInfoState.value === EmptyLayoutInfo) {
            awaitLayoutModifier.waitForFirstLayout()
        }
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        awaitScrollDependencies()
        // will scroll and it's not scrolling already update settled page
        if (!isScrollInProgress) {
            settledPageState = currentPage
        }
        scrollableState.scroll(scrollPriority, block)
        programmaticScrollTargetPage = -1 // reset animated scroll target page indicator
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return scrollableState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    final override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    final override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value

    override val scrollIndicatorState: ScrollIndicatorState?
        get() = _scrollIndicatorState

    /** Updates the state with the new calculated scroll position and consumed scroll. */
    internal fun applyMeasureResult(
        result: PagerMeasureResult,
        isLookingAhead: Boolean,
        visibleItemsStayedTheSame: Boolean = false,
    ) {
        // update the prefetch state with the number of nested prefetch items this layout
        // should use.
        prefetchState.idealNestedPrefetchCount = result.visiblePagesInfo.size

        // Update non state backed page size info
        latestPageSizeWithSpacing = result.pageSize + result.pageSpacing

        if (!isLookingAhead && hasLookaheadOccurred) {
            debugLog { "Applying Approach Measure Result" }
            // If there was already a lookahead pass, record this result as Approach result
            approachLayoutInfo = result
        } else {
            debugLog { "Applying Measure Result" }
            if (isLookingAhead) {
                hasLookaheadOccurred = true
            }
            if (visibleItemsStayedTheSame) {
                scrollPosition.updateCurrentPageOffsetFraction(result.currentPageOffsetFraction)
            } else {
                scrollPosition.updateFromMeasureResult(result)
                if (isCacheWindowForPagerEnabled) {
                    if (prefetchingEnabled) {
                        cacheWindowLogic.onVisibleItemsChanged(result)
                    }
                } else {
                    cancelPrefetchIfVisibleItemsChanged(result)
                }
            }
            pagerLayoutInfoState.value = result
            canScrollForward = result.canScrollForward
            canScrollBackward = result.canScrollBackward
            result.firstVisiblePage?.let { firstVisiblePage = it.index }
            firstVisiblePageOffset = result.firstVisiblePageScrollOffset
            tryRunPrefetch(result)
            maxScrollOffset = result.calculateNewMaxScrollOffset(pageCount)
            minScrollOffset =
                result.calculateNewMinScrollOffset(pageCount).coerceAtMost(maxScrollOffset)
            debugLog { "Finished Applying Measure Result\nNew maxScrollOffset=$maxScrollOffset" }
        }
    }

    private fun tryRunPrefetch(result: PagerMeasureResult) =
        Snapshot.withoutReadObservation {
            if (!prefetchingEnabled) return
            if (result.beyondViewportPageCount >= pageCount) return
            if (abs(previousPassDelta) <= 0.5f) return
            if (!isGestureActionMatchesScroll(previousPassDelta)) return
            if (isCacheWindowForPagerEnabled) {
                cacheWindowLogic.onScroll(previousPassDelta, result)
            } else {
                notifyPrefetch(previousPassDelta, result)
            }
        }

    private fun Int.coerceInPageRange() =
        if (pageCount > 0) {
            coerceIn(0, pageCount - 1)
        } else {
            0
        }

    // check if the scrolling will be a result of a fling operation. That is, if the scrolling
    // direction is in the opposite direction of the gesture movement. Also, return true if there
    // is no applied gesture that causes the scrolling
    private fun isGestureActionMatchesScroll(scrollDelta: Float): Boolean =
        if (layoutInfo.orientation == Orientation.Vertical) {
            sign(scrollDelta) == sign(-upDownDifference.y)
        } else {
            sign(scrollDelta) == sign(-upDownDifference.x)
        } || isNotGestureAction()

    internal fun isNotGestureAction(): Boolean =
        upDownDifference.x.toInt() == 0 && upDownDifference.y.toInt() == 0

    private fun notifyPrefetch(delta: Float, info: PagerLayoutInfo) {
        if (!prefetchingEnabled) {
            return
        }

        if (info.visiblePagesInfo.isNotEmpty()) {
            val isPrefetchingForward = delta > 0
            val indexToPrefetch = calculatePrefetchIndex(isPrefetchingForward, info)
            if (indexToPrefetch in 0 until pageCount) {
                if (indexToPrefetch != this.indexToPrefetch) {
                    if (wasPrefetchingForward != isPrefetchingForward) {
                        // the scrolling direction has been changed which means the last prefetched
                        // is not going to be reached anytime soon so it is safer to dispose it.
                        // if this item is already visible it is safe to call the method anyway
                        // as it will be no-op
                        currentPrefetchHandle?.cancel()
                    }
                    this.wasPrefetchingForward = isPrefetchingForward
                    this.indexToPrefetch = indexToPrefetch
                    currentPrefetchHandle =
                        prefetchState.schedulePrecompositionAndPremeasure(
                            indexToPrefetch,
                            premeasureConstraints,
                        )
                }
                if (isPrefetchingForward) {
                    val lastItem = info.visiblePagesInfo.last()
                    val pageSize = info.pageSize + info.pageSpacing
                    val distanceToReachNextItem =
                        lastItem.offset + pageSize - info.viewportEndOffset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToReachNextItem < delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                } else {
                    val firstItem = info.visiblePagesInfo.first()
                    val distanceToReachNextItem = info.viewportStartOffset - firstItem.offset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToReachNextItem < -delta) {
                        currentPrefetchHandle?.markAsUrgent()
                    }
                }
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: PagerLayoutInfo) {
        if (indexToPrefetch != -1 && info.visiblePagesInfo.isNotEmpty()) {
            val expectedPrefetchIndex = calculatePrefetchIndex(wasPrefetchingForward, info)
            if (indexToPrefetch != expectedPrefetchIndex) {
                indexToPrefetch = -1
                currentPrefetchHandle?.cancel()
                currentPrefetchHandle = null
            }
        }
    }

    /** Calculate the farthest page index that should be prefetched when scrolling. */
    private fun calculatePrefetchIndex(forward: Boolean, info: PagerLayoutInfo): Int {
        return if (forward) {
            val offset = info.beyondViewportPageCount + PagesToPrefetch
            if (offset < 0) { // Detect overflow from large beyondViewportPageCount
                Int.MAX_VALUE
            } else {
                info.visiblePagesInfo.last().index + offset
            }
        } else {
            info.visiblePagesInfo.first().index - info.beyondViewportPageCount - PagesToPrefetch
        }
    }

    /**
     * An utility function to help to calculate a given page's offset. This is an offset that
     * represents how far [page] is from the settled position (represented by [currentPage] offset).
     * The difference here is that [currentPageOffsetFraction] is a value between -0.5 and 0.5 and
     * the value calculated by this function can be larger than these numbers if [page] is different
     * than [currentPage].
     *
     * For instance, if currentPage=0 and we call [getOffsetDistanceInPages] for page 3, the result
     * will be 3, meaning the given page is 3 pages away from the current page (the sign represent
     * the direction of the offset, positive is forward, negative is backwards). Another example is
     * if currentPage=3 and we call [getOffsetDistanceInPages] for page 1, the result would be -2,
     * meaning we're 2 pages away (moving backwards) to the current page.
     *
     * This offset also works in conjunction with [currentPageOffsetFraction], so if [currentPage]
     * is out of its snapped position (i.e. currentPageOffsetFraction!=0) then the calculated value
     * will still represent the offset in number of pages (in this case, not whole pages). For
     * instance, if currentPage=1 and we're slightly offset, currentPageOffsetFraction=0.2, if we
     * call this to page 2, the result would be 0.8, that is 0.8 page away from current page (moving
     * forward).
     *
     * @param page The page to calculate the offset from. This should be between 0 and [pageCount].
     * @return The offset of [page] with respect to [currentPage].
     */
    fun getOffsetDistanceInPages(page: Int): Float {
        requirePrecondition(page in 0..pageCount) {
            "page $page is not within the range 0 to $pageCount"
        }
        return page - currentPage - currentPageOffsetFraction
    }

    /**
     * When the user provided custom keys for the pages we can try to detect when there were pages
     * added or removed before our current page and keep this page as the current one given that its
     * index has been changed.
     */
    internal fun matchScrollPositionWithKey(
        itemProvider: PagerLazyLayoutItemProvider,
        currentPage: Int = Snapshot.withoutReadObservation { scrollPosition.currentPage },
    ): Int = scrollPosition.matchPageWithKey(itemProvider, currentPage)
}

internal suspend fun PagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

internal suspend fun PagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

internal val DefaultPositionThreshold = 56.dp
private const val MaxPagesForAnimateScroll = 3
internal const val PagesToPrefetch = 1

private val UnitDensity =
    object : Density {
        override val density: Float = 1f
        override val fontScale: Float = 1f
    }

internal val EmptyLayoutInfo =
    PagerMeasureResult(
        visiblePagesInfo = emptyList(),
        pageSize = 0,
        pageSpacing = 0,
        afterContentPadding = 0,
        orientation = Orientation.Horizontal,
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        reverseLayout = false,
        beyondViewportPageCount = 0,
        firstVisiblePage = null,
        firstVisiblePageScrollOffset = 0,
        currentPage = null,
        currentPageOffsetFraction = 0.0f,
        canScrollForward = false,
        snapPosition = SnapPosition.Start,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0

                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = mapOf()

                override fun placeChildren() {}
            },
        remeasureNeeded = false,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        density = UnitDensity,
        childConstraints = Constraints(),
    )

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.PagerState) {
        println("PagerState: ${generateMsg()}")
    }
}

internal fun PagerLayoutInfo.calculateNewMaxScrollOffset(pageCount: Int): Long {
    val pageSizeWithSpacing = pageSpacing + pageSize
    val maxScrollPossible =
        (pageCount.toLong()) * pageSizeWithSpacing + beforeContentPadding + afterContentPadding -
            pageSpacing
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    /**
     * We need to take into consideration the snap position for max scroll position. For instance,
     * if SnapPosition.Start, the max scroll position is pageCount * pageSize - viewport. Now if
     * SnapPosition.End, it should be pageCount * pageSize. Therefore, the snap position discount
     * varies between 0 and viewport.
     */
    val snapPositionDiscount =
        layoutSize -
            (snapPosition.position(
                    layoutSize = layoutSize,
                    itemSize = pageSize,
                    itemIndex = pageCount - 1,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    itemCount = pageCount,
                ))
                .coerceIn(0, layoutSize)

    debugLog {
        "maxScrollPossible=$maxScrollPossible" +
            "\nsnapPositionDiscount=$snapPositionDiscount" +
            "\nlayoutSize=$layoutSize"
    }
    return (maxScrollPossible - snapPositionDiscount).coerceAtLeast(0L)
}

private fun PagerMeasureResult.calculateNewMinScrollOffset(pageCount: Int): Long {
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    return snapPosition
        .position(
            layoutSize = layoutSize,
            itemSize = pageSize,
            itemIndex = 0,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            itemCount = pageCount,
        )
        .coerceIn(0, layoutSize)
        .toLong()
}

private suspend fun LazyLayoutScrollScope.animateScrollToPage(
    targetPage: Int,
    targetPageOffsetToSnappedPosition: Float,
    animationSpec: AnimationSpec<Float>,
    updateTargetPage: ScrollScope.(Int) -> Unit,
) {
    updateTargetPage(targetPage)
    val forward = targetPage > firstVisibleItemIndex
    val visiblePages = lastVisibleItemIndex - firstVisibleItemIndex + 1
    if (
        ((forward && targetPage > lastVisibleItemIndex) ||
            (!forward && targetPage < firstVisibleItemIndex)) &&
            abs(targetPage - firstVisibleItemIndex) >= MaxPagesForAnimateScroll
    ) {
        val preJumpPosition =
            if (forward) {
                (targetPage - visiblePages).coerceAtLeast(firstVisibleItemIndex)
            } else {
                (targetPage + visiblePages).coerceAtMost(firstVisibleItemIndex)
            }

        debugLog { "animateScrollToPage with pre-jump to position=$preJumpPosition" }

        // Pre-jump to 1 viewport away from destination page, if possible
        snapToItem(preJumpPosition, 0)
    }

    // The final delta displacement will be the difference between the pages offsets
    // discounting whatever offset the original page had scrolled plus the offset
    // fraction requested by the user.
    val displacement = calculateDistanceTo(targetPage) + targetPageOffsetToSnappedPosition

    debugLog { "animateScrollToPage $displacement pixels" }
    var previousValue = 0f
    animate(0f, displacement, animationSpec = animationSpec) { currentValue, _ ->
        val delta = currentValue - previousValue
        val consumed = scrollBy(delta)
        debugLog { "Dispatched Delta=$delta Consumed=$consumed" }
        previousValue += consumed
    }
}
```

