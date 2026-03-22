# Compose UI Source Reference

## File: compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidCompositionLocals.android.kt
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

package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.ResourceIdCache
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val LocalConfiguration =
    compositionLocalOf<Configuration> { noLocalProvidedFor("LocalConfiguration") }

/** Provides a [Context] that can be used by Android applications. */
val LocalContext = staticCompositionLocalOf<Context> { noLocalProvidedFor("LocalContext") }

/**
 * The Android [Resources]. This will be updated when [LocalConfiguration] changes, to ensure that
 * calls to APIs such as [Resources.getString] return updated values.
 */
val LocalResources =
    compositionLocalWithComputedDefaultOf<Resources> {
        // Read LocalConfiguration here to invalidate callers of LocalResources when the
        // configuration changes. This is preferable to explicitly providing the resources object
        // because the resources object can still have the same instance, even though the
        // configuration changed, which would mean that callers would not get invalidated. To
        // resolve that we would need to use neverEqualPolicy to force an invalidation even though
        // the Resources didn't change, but then that would cause invalidations every time the
        // providing Composable is recomposed, regardless of whether a configuration change happened
        // or not.
        LocalConfiguration.currentValue
        LocalContext.currentValue.resources
    }

internal val LocalImageVectorCache =
    staticCompositionLocalOf<ImageVectorCache> { noLocalProvidedFor("LocalImageVectorCache") }

internal val LocalResourceIdCache =
    staticCompositionLocalOf<ResourceIdCache> { noLocalProvidedFor("LocalResourceIdCache") }

@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
actual val LocalLifecycleOwner
    get() = LocalLifecycleOwner

/** The CompositionLocal containing the current [SavedStateRegistryOwner]. */
@Deprecated(
    "Moved to savedstate-compose library in androidx.savedstate.compose package.",
    ReplaceWith("androidx.savedstate.compose.LocalSavedStateRegistryOwner"),
)
val LocalSavedStateRegistryOwner
    get() = LocalSavedStateRegistryOwner

/** The CompositionLocal containing the current Compose [View]. */
val LocalView = staticCompositionLocalOf<View> { noLocalProvidedFor("LocalView") }

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/Modifier.kt
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

package androidx.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.internal.PlatformOptimizedCancellationException
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.ObserverNodeOwnerScope
import androidx.compose.ui.node.requireOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

internal class ModifierNodeDetachedCancellationException :
    PlatformOptimizedCancellationException("The Modifier.Node was detached")

/**
 * An ordered, immutable collection of [modifier elements][Modifier.Element] that decorate or add
 * behavior to Compose UI elements. For example, backgrounds, padding and click event listeners
 * decorate or add behavior to rows, text or buttons.
 *
 * @sample androidx.compose.ui.samples.ModifierUsageSample
 *
 * Modifier implementations should offer a fluent factory extension function on [Modifier] for
 * creating combined modifiers by starting from existing modifiers:
 *
 * @sample androidx.compose.ui.samples.ModifierFactorySample
 *
 * Modifier elements may be combined using [then]. Order is significant; modifier elements that
 * appear first will be applied first.
 *
 * Composables that accept a [Modifier] as a parameter to be applied to the whole component
 * represented by the composable function should name the parameter `modifier` and assign the
 * parameter a default value of [Modifier]. It should appear as the first optional parameter in the
 * parameter list; after all required parameters (except for trailing lambda parameters) but before
 * any other parameters with default values. Any default modifiers desired by a composable function
 * should come after the `modifier` parameter's value in the composable function's implementation,
 * keeping [Modifier] as the default parameter value. For example:
 *
 * @sample androidx.compose.ui.samples.ModifierParameterSample
 *
 * The pattern above allows default modifiers to still be applied as part of the chain if a caller
 * also supplies unrelated modifiers.
 *
 * Composables that accept modifiers to be applied to a specific subcomponent `foo` should name the
 * parameter `fooModifier` and follow the same guidelines above for default values and behavior.
 * Subcomponent modifiers should be grouped together and follow the parent composable's modifier.
 * For example:
 *
 * @sample androidx.compose.ui.samples.SubcomponentModifierSample
 */
@Suppress("ModifierFactoryExtensionFunction")
@Stable
@JvmDefaultWithCompatibility
interface Modifier {

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldIn] may be used to accumulate a value starting from the
     * parent or head of the modifier chain to the final wrapped child.
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from inside out.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldOut] may be used to accumulate a value starting from the
     * child or tail of the modifier chain up to the parent or head of the chain.
     */
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /** Returns `true` if [predicate] returns true for any [Element] in this [Modifier]. */
    fun any(predicate: (Element) -> Boolean): Boolean

    /**
     * Returns `true` if [predicate] returns true for all [Element]s in this [Modifier] or if this
     * [Modifier] contains no [Element]s.
     */
    fun all(predicate: (Element) -> Boolean): Boolean

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [Modifier] representing this modifier followed by [other] in sequence.
     */
    infix fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    /** A single element contained within a [Modifier] chain. */
    @JvmDefaultWithCompatibility
    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)

        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)

        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)
    }

    /**
     * The longer-lived object that is created for each [Modifier.Element] applied to a
     * [androidx.compose.ui.layout.Layout]. Most [Modifier.Node] implementations will have a
     * corresponding "Modifier Factory" extension method on Modifier that will allow them to be used
     * indirectly, without ever implementing a [Modifier.Node] subclass directly. In some cases it
     * may be useful to define a custom [Modifier.Node] subclass in order to efficiently implement
     * some collection of behaviors that requires maintaining state over time and over many
     * recompositions where the various provided Modifier factories are not sufficient.
     *
     * When a [Modifier] is set on a [androidx.compose.ui.layout.Layout], each [Modifier.Element]
     * contained in that linked list will result in a corresponding [Modifier.Node] instance in a
     * matching linked list of [Modifier.Node]s that the [androidx.compose.ui.layout.Layout] will
     * hold on to. As subsequent [Modifier] chains get set on the
     * [androidx.compose.ui.layout.Layout], the linked list of [Modifier.Node]s will be diffed and
     * updated as appropriate, even though the [Modifier] instance might be completely new. As a
     * result, the lifetime of a [Modifier.Node] is the intersection of the lifetime of the
     * [androidx.compose.ui.layout.Layout] that it lives on and a corresponding [Modifier.Element]
     * being present in the [androidx.compose.ui.layout.Layout]'s [Modifier].
     *
     * If one creates a subclass of [Modifier.Node], it is expected that it will implement one or
     * more interfaces that interact with the various Compose UI subsystems. To use the
     * [Modifier.Node] subclass, it is expected that it will be instantiated by adding a
     * [androidx.compose.ui.node.ModifierNodeElement] to a [Modifier] chain.
     *
     * @see androidx.compose.ui.node.ModifierNodeElement
     * @see androidx.compose.ui.node.CompositionLocalConsumerModifierNode
     * @see androidx.compose.ui.node.DelegatableNode
     * @see androidx.compose.ui.node.DelegatingNode
     * @see androidx.compose.ui.node.LayoutModifierNode
     * @see androidx.compose.ui.node.DrawModifierNode
     * @see androidx.compose.ui.node.SemanticsModifierNode
     * @see androidx.compose.ui.node.PointerInputModifierNode
     * @see androidx.compose.ui.modifier.ModifierLocalModifierNode
     * @see androidx.compose.ui.node.ParentDataModifierNode
     * @see androidx.compose.ui.node.LayoutAwareModifierNode
     * @see androidx.compose.ui.node.GlobalPositionAwareModifierNode
     * @see androidx.compose.ui.node.ApproachLayoutModifierNode
     */
    abstract class Node : DelegatableNode {
        @Suppress("LeakingThis")
        final override var node: Node = this
            private set

        private var scope: CoroutineScope? = null

        /**
         * A [CoroutineScope] that can be used to launch tasks that should run while the node is
         * attached.
         *
         * The scope is accessible between [onAttach] and [onDetach] calls, and will be cancelled
         * after the node is detached (after [onDetach] returns).
         *
         * @sample androidx.compose.ui.samples.ModifierNodeCoroutineScopeSample
         * @throws IllegalStateException If called while the node is not attached.
         */
        val coroutineScope: CoroutineScope
            get() =
                scope
                    ?: CoroutineScope(
                            requireOwner().coroutineContext +
                                Job(parent = requireOwner().coroutineContext[Job])
                        )
                        .also { scope = it }

        internal var kindSet: Int = 0

        // NOTE: We use an aggregate mask that or's all of the type masks of the children of the
        // chain so that we can quickly prune a subtree. This INCLUDES the kindSet of this node
        // as well. Initialize this to "every node" so that before it is set it doesn't
        // accidentally cause a truncated traversal.
        internal var aggregateChildKindSet: Int = 0.inv()
        internal var parent: Node? = null
        internal var child: Node? = null
        internal var ownerScope: ObserverNodeOwnerScope? = null
        internal var coordinator: NodeCoordinator? = null
            private set

        internal var insertedNodeAwaitingAttachForInvalidation = false
        internal var updatedNodeAwaitingAttachForInvalidation = false
        private var onAttachRunExpected = false
        private var onDetachRunExpected = false

        internal var detachedListener: (() -> Unit)? = null

        /**
         * Indicates that the node is attached to a [androidx.compose.ui.layout.Layout] which is
         * part of the UI tree. This will get set to true right before [onAttach] is called, and set
         * to false right after [onDetach] is called.
         *
         * @see onAttach
         * @see onDetach
         */
        var isAttached: Boolean = false
            private set

        /**
         * If this property returns `true`, then nodes will be automatically invalidated after the
         * modifier update completes (For example, if the returned Node is a [DrawModifierNode], its
         * [DrawModifierNode.invalidateDraw] function will be invoked automatically as part of auto
         * invalidation).
         *
         * This is enabled by default, and provides a convenient mechanism to schedule invalidation
         * and apply changes made to the modifier. You may choose to set this to `false` if your
         * modifier has auto-invalidatable properties that do not frequently require invalidation to
         * improve performance by skipping unnecessary invalidation. If `autoInvalidate` is set to
         * `false`, you must call the appropriate invalidate functions manually when the modifier is
         * updated or else the updates may not be reflected in the UI appropriately.
         */
        @Suppress("GetterSetterNames")
        @get:Suppress("GetterSetterNames")
        open val shouldAutoInvalidate: Boolean
            get() = true

        internal open fun updateCoordinator(coordinator: NodeCoordinator?) {
            this.coordinator = coordinator
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun isKind(kind: NodeKind<*>) = kindSet and kind.mask != 0

        internal open fun markAsAttached() {
            checkPrecondition(!isAttached) { "node attached multiple times" }
            checkPrecondition(coordinator != null) {
                "attach invoked on a node without a coordinator"
            }
            isAttached = true
            onAttachRunExpected = true
        }

        internal open fun runAttachLifecycle() {
            checkPrecondition(isAttached) {
                "Must run markAsAttached() prior to runAttachLifecycle"
            }
            checkPrecondition(onAttachRunExpected) {
                "Must run runAttachLifecycle() only once " + "after markAsAttached()"
            }
            onAttachRunExpected = false
            onAttach()
            onDetachRunExpected = true
        }

        internal open fun runDetachLifecycle() {
            checkPrecondition(isAttached) { "node detached multiple times" }
            checkPrecondition(coordinator != null) {
                "detach invoked on a node without a coordinator"
            }
            checkPrecondition(onDetachRunExpected) {
                "Must run runDetachLifecycle() once after runAttachLifecycle() and before " +
                    "markAsDetached()"
            }
            onDetachRunExpected = false
            detachedListener?.invoke()
            onDetach()
        }

        internal open fun markAsDetached() {
            checkPrecondition(isAttached) { "Cannot detach a node that is not attached" }
            checkPrecondition(!onAttachRunExpected) {
                "Must run runAttachLifecycle() before markAsDetached()"
            }
            checkPrecondition(!onDetachRunExpected) {
                "Must run runDetachLifecycle() before markAsDetached()"
            }
            isAttached = false

            scope?.let {
                it.cancel(ModifierNodeDetachedCancellationException())
                scope = null
            }
        }

        internal open fun reset() {
            checkPrecondition(isAttached) { "reset() called on an unattached node" }
            onReset()
        }

        /**
         * Called when the node is attached to a [androidx.compose.ui.layout.Layout] which is part
         * of the UI tree. When called, `node` is guaranteed to be non-null. You can call
         * sideEffect, coroutineScope, etc. This is not guaranteed to get called at a time where the
         * rest of the Modifier.Nodes in the hierarchy are "up to date". For instance, at the time
         * of calling onAttach for this node, another node may be in the tree that will be detached
         * by the time Compose has finished applying changes. As a result, if you need to guarantee
         * that the state of the tree is "final" for this round of changes, you should use the
         * [sideEffect] API to schedule the calculation to be done at that time.
         */
        open fun onAttach() {}

        /**
         * Called when the node is not attached to a [androidx.compose.ui.layout.Layout] which is
         * not a part of the UI tree anymore. Note that the node can be reattached again.
         *
         * This should be called right before the node gets removed from the list, so you should
         * still be able to traverse inside of this method. Ideally we would not allow you to
         * trigger side effects here.
         */
        open fun onDetach() {}

        /**
         * Called when the node is about to be moved to a pool of layouts ready to be reused. For
         * example it happens when the node is part of the item of LazyColumn after this item is
         * scrolled out of the viewport. This means this node could be in future reused for a
         * [androidx.compose.ui.layout.Layout] displaying a semantically different content when the
         * list will be populating a new item.
         *
         * Use this callback to reset some local item specific state, like "is my component
         * focused".
         *
         * This callback is called while the node is attached. Right after this callback the node
         * will be detached and later reattached when reused.
         *
         * @sample androidx.compose.ui.samples.ModifierNodeResetSample
         */
        open fun onReset() {}

        /**
         * This can be called to register [effect] as a function to be executed after all of the
         * changes to the tree are applied.
         *
         * This API can only be called if the node [isAttached].
         */
        fun sideEffect(effect: () -> Unit) {
            requireOwner().registerOnEndApplyChangesListener(effect)
        }

        internal open fun setAsDelegateTo(owner: Node) {
            node = owner
        }
    }

    /**
     * The companion object `Modifier` is the empty, default, or starter [Modifier] that contains no
     * [elements][Element]. Use it to create a new [Modifier] using modifier extension factory
     * functions:
     *
     * @sample androidx.compose.ui.samples.ModifierUsageSample
     *
     * or as the default value for [Modifier] parameters:
     *
     * @sample androidx.compose.ui.samples.ModifierParameterSample
     */
    // The companion object implements `Modifier` so that it may be used as the start of a
    // modifier extension factory expression.
    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial

        override fun any(predicate: (Element) -> Boolean): Boolean = false

        override fun all(predicate: (Element) -> Boolean): Boolean = true

        override infix fun then(other: Modifier): Modifier = other

        override fun toString() = "Modifier"
    }
}

/**
 * A node in a [Modifier] chain. A CombinedModifier always contains at least two elements; a
 * Modifier [outer] that wraps around the Modifier [inner].
 */
class CombinedModifier(internal val outer: Modifier, internal val inner: Modifier) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is CombinedModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString() =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/layout/Layout.kt
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.materialize
import androidx.compose.ui.materializeWithCompositionLocalInjectionInternal
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.ComposeUiNode.Companion.ApplyOnDeactivatedNodeAssertion
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetMeasurePolicy
import androidx.compose.ui.node.ComposeUiNode.Companion.SetModifier
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.checkMeasuredSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmName

/**
 * [Layout] is the main core component for layout. It can be used to measure and position zero or
 * more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * For a composable able to define its content according to the incoming constraints, see
 * [androidx.compose.foundation.layout.BoxWithConstraints].
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.LayoutUsage
 *
 * Example usage with custom intrinsic measurements:
 *
 * @sample androidx.compose.ui.samples.LayoutWithProvidedIntrinsicsUsage
 * @param content The children composable to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 * @see Layout
 * @see MeasurePolicy
 * @see androidx.compose.foundation.layout.BoxWithConstraints
 */
@Suppress("ComposableLambdaParameterPosition")
@UiComposable
@Composable
inline fun Layout(
    content: @Composable @UiComposable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy,
) {
    val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
    val localMap = currentComposer.currentCompositionLocalMap
    val materialized = currentComposer.materialize(modifier)
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            set(compositeKeyHash, SetCompositeKeyHash)
            reconcile(ApplyOnDeactivatedNodeAssertion)
            set(materialized, SetModifier)
        },
        content = content,
    )
}

/**
 * [Layout] is the main core component for layout for "leaf" nodes. It can be used to measure and
 * position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * For a composable able to define its content according to the incoming constraints, see
 * [androidx.compose.foundation.layout.BoxWithConstraints].
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.LayoutUsage
 *
 * Example usage with custom intrinsic measurements:
 *
 * @sample androidx.compose.ui.samples.LayoutWithProvidedIntrinsicsUsage
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 * @see Layout
 * @see MeasurePolicy
 * @see androidx.compose.foundation.layout.BoxWithConstraints
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
@UiComposable
inline fun Layout(modifier: Modifier = Modifier, measurePolicy: MeasurePolicy) {
    val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
    val materialized = currentComposer.materialize(modifier)
    val localMap = currentComposer.currentCompositionLocalMap
    ReusableComposeNode<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            reconcile(ApplyOnDeactivatedNodeAssertion)
            set(materialized, SetModifier)
            set(compositeKeyHash, SetCompositeKeyHash)
        },
    )
}

/**
 * [Layout] is the main core component for layout. It can be used to measure and position zero or
 * more layout children.
 *
 * This overload accepts a list of multiple composable content lambdas, which allows treating
 * measurables put into different content lambdas differently - measure policy will provide a list
 * of lists of Measurables, not just a single list. Such list has the same size as the list of
 * contents passed into [Layout] and contains the list of measurables of the corresponding content
 * lambda in the same order.
 *
 * Note that layouts emitted as part of all [contents] lambdas will be added as a direct children
 * for this [Layout]. This means that if you set a custom z index on some children, the drawing
 * order will be calculated as if they were all provided as part of one lambda.
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.LayoutWithMultipleContentsUsage
 * @param contents The list of children composable contents to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 * @see Layout for a simpler use case when you have only one content lambda.
 */
@Suppress("ComposableLambdaParameterPosition", "NOTHING_TO_INLINE")
@UiComposable
@Composable
inline fun Layout(
    contents: List<@Composable @UiComposable () -> Unit>,
    modifier: Modifier = Modifier,
    measurePolicy: MultiContentMeasurePolicy,
) {
    Layout(
        content = combineAsVirtualLayouts(contents),
        modifier = modifier,
        measurePolicy = remember(measurePolicy) { createMeasurePolicy(measurePolicy) },
    )
}

@PublishedApi
internal fun combineAsVirtualLayouts(
    contents: List<@Composable @UiComposable () -> Unit>
): @Composable @UiComposable () -> Unit = {
    contents.fastForEach { content ->
        val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
        ReusableComposeNode<ComposeUiNode, Applier<Any>>(
            factory = ComposeUiNode.VirtualConstructor,
            update = { set(compositeKeyHash, SetCompositeKeyHash) },
            content = content,
        )
    }
}

/**
 * This function uses a JVM-Name because the original name now has a different implementation for
 * backwards compatibility [materializerOfWithCompositionLocalInjection]. More details can be found
 * at https://issuetracker.google.com/275067189
 */
@PublishedApi
@JvmName("modifierMaterializerOf")
internal fun materializerOf(
    modifier: Modifier
): @Composable SkippableUpdater<ComposeUiNode>.() -> Unit = {
    val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
    val materialized = currentComposer.materialize(modifier)
    update {
        set(materialized, SetModifier)
        set(compositeKeyHash, SetCompositeKeyHash)
    }
}

/**
 * This function exists solely for solving a backwards-incompatibility with older compilations that
 * used an older version of the `Layout` composable. New code paths should not call this. More
 * details can be found at https://issuetracker.google.com/275067189
 */
@JvmName("materializerOf")
@Deprecated(
    "Needed only for backwards compatibility. Do not use.",
    level = DeprecationLevel.WARNING,
)
@PublishedApi
internal fun materializerOfWithCompositionLocalInjection(
    modifier: Modifier
): @Composable SkippableUpdater<ComposeUiNode>.() -> Unit = {
    val compositeKeyHash = currentCompositeKeyHash.hashCode()
    val materialized = currentComposer.materializeWithCompositionLocalInjectionInternal(modifier)
    update {
        set(materialized, SetModifier)
        set(compositeKeyHash, SetCompositeKeyHash)
    }
}

@Suppress("ComposableLambdaParameterPosition")
@Composable
@UiComposable
@Deprecated(
    "This API is unsafe for UI performance at scale - using it incorrectly will lead " +
        "to exponential performance issues. This API should be avoided whenever possible."
)
fun MultiMeasureLayout(
    modifier: Modifier = Modifier,
    content: @Composable @UiComposable () -> Unit,
    measurePolicy: MeasurePolicy,
) {
    val compositeKeyHash = currentCompositeKeyHash.hashCode()
    val materialized = currentComposer.materialize(modifier)
    val localMap = currentComposer.currentCompositionLocalMap

    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            @Suppress("DEPRECATION") init { this.canMultiMeasure = true }
            reconcile(ApplyOnDeactivatedNodeAssertion)
            set(materialized, SetModifier)
            set(compositeKeyHash, SetCompositeKeyHash)
        },
        content = content,
    )
}

/** Used to return a fixed sized item for intrinsics measurements in [Layout] */
private class FixedSizeIntrinsicsPlaceable(width: Int, height: Int) : Placeable() {
    init {
        measuredSize = IntSize(width, height)
    }

    override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

    override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {}
}

/** Identifies an [IntrinsicMeasurable] as a min or max intrinsic measurement. */
internal enum class IntrinsicMinMax {
    Min,
    Max,
}

/** Identifies an [IntrinsicMeasurable] as a width or height intrinsic measurement. */
internal enum class IntrinsicWidthHeight {
    Width,
    Height,
}

// A large value to use as a replacement for Infinity with DefaultIntrinisicMeasurable.
// A layout likely won't use this dimension as it is opposite from the one being measured in
// the max/min Intrinsic Width/Height, but it is possible. For example, if the direct child
// uses normal measurement/layout, we don't want to return Infinity sizes when its parent
// asks for intrinsic size. 15 bits can fit in a Constraints, so should be safe unless
// the parent adds to it and the other dimension is also very large (> 2^15).
internal const val LargeDimension = (1 shl 15) - 1

/**
 * A wrapper around a [Measurable] for intrinsic measurements in [Layout]. Consumers of [Layout]
 * don't identify intrinsic methods, but we can give a reasonable implementation by using their
 * [measure], substituting the intrinsics gathering method for the [Measurable.measure] call.
 */
internal class DefaultIntrinsicMeasurable(
    val measurable: IntrinsicMeasurable,
    private val minMax: IntrinsicMinMax,
    private val widthHeight: IntrinsicWidthHeight,
) : Measurable {
    override val parentData: Any?
        get() = measurable.parentData

    override fun measure(constraints: Constraints): Placeable {
        if (widthHeight == IntrinsicWidthHeight.Width) {
            val width =
                if (minMax == IntrinsicMinMax.Max) {
                    measurable.maxIntrinsicWidth(constraints.maxHeight)
                } else {
                    measurable.minIntrinsicWidth(constraints.maxHeight)
                }
            // Can't use infinity for height, so use a large number
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else LargeDimension
            return FixedSizeIntrinsicsPlaceable(width, height)
        }
        val height =
            if (minMax == IntrinsicMinMax.Max) {
                measurable.maxIntrinsicHeight(constraints.maxWidth)
            } else {
                measurable.minIntrinsicHeight(constraints.maxWidth)
            }
        // Can't use infinity for width, so use a large number
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else LargeDimension
        return FixedSizeIntrinsicsPlaceable(width, height)
    }

    override fun minIntrinsicWidth(height: Int): Int {
        return measurable.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        return measurable.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        return measurable.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        return measurable.maxIntrinsicHeight(width)
    }
}

/**
 * Receiver scope for [Layout]'s and [LayoutModifier]'s layout lambda when used in an intrinsics
 * call.
 */
internal class IntrinsicsMeasureScope(
    intrinsicMeasureScope: IntrinsicMeasureScope,
    override val layoutDirection: LayoutDirection,
) : MeasureScope, IntrinsicMeasureScope by intrinsicMeasureScope {
    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<AlignmentLine, Int>,
        rulers: (RulerScope.() -> Unit)?,
        placementBlock: Placeable.PlacementScope.() -> Unit,
    ): MeasureResult {
        val w = width.fastCoerceAtLeast(0)
        val h = height.fastCoerceAtLeast(0)
        checkMeasuredSize(w, h)
        return object : MeasureResult {
            override val width: Int
                get() = w

            override val height: Int
                get() = h

            override val alignmentLines: Map<AlignmentLine, Int>
                get() = alignmentLines

            override val rulers: (RulerScope.() -> Unit)?
                get() = rulers

            override fun placeChildren() {
                // Intrinsics should never be placed
            }
        }
    }
}

internal class ApproachIntrinsicsMeasureScope(
    intrinsicMeasureScope: ApproachIntrinsicMeasureScope,
    override val layoutDirection: LayoutDirection,
) : ApproachMeasureScope, ApproachIntrinsicMeasureScope by intrinsicMeasureScope {
    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<AlignmentLine, Int>,
        rulers: (RulerScope.() -> Unit)?,
        placementBlock: Placeable.PlacementScope.() -> Unit,
    ): MeasureResult {
        val w = width.fastCoerceAtLeast(0)
        val h = height.fastCoerceAtLeast(0)
        checkMeasuredSize(w, h)
        return object : MeasureResult {
            override val width: Int
                get() = w

            override val height: Int
                get() = h

            override val alignmentLines: Map<AlignmentLine, Int>
                get() = alignmentLines

            override val rulers: (RulerScope.() -> Unit)?
                get() = rulers

            override fun placeChildren() {
                // Intrinsics should never be placed
            }
        }
    }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/layout/MeasurePolicy.kt
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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastMap

/**
 * Defines the measure and layout behavior of a [Layout]. [Layout] and [MeasurePolicy] are the way
 * Compose layouts (such as `Box`, `Column`, etc.) are built, and they can also be used to achieve
 * custom layouts.
 *
 * See [Layout] samples for examples of how to use [MeasurePolicy].
 *
 * Intrinsic measurement methods define the intrinsic size of the layout. These can be queried by
 * the layout's parent in order to obtain, in specific cases, more information about the size of the
 * layout in the absence of specific constraints:
 * - [minIntrinsicWidth] defines the minimum width this layout can take, given a specific height,
 *   such that the content of the layout will be painted correctly
 * - [minIntrinsicHeight] defines the minimum height this layout can take, given a specific width,
 *   such that the content of the layout will be painted correctly
 * - [maxIntrinsicWidth] defines the minimum width such that increasing it further will not decrease
 *   the minimum intrinsic height
 * - [maxIntrinsicHeight] defines the minimum height such that increasing it further will not
 *   decrease the minimum intrinsic width
 *
 * Most layout scenarios do not require querying intrinsic measurements. Therefore, when writing a
 * custom layout, it is common to only define the actual measurement, as most of the times the
 * intrinsic measurements of the layout will not be queried. Moreover, intrinsic measurement methods
 * have default implementations that make a best effort attempt to calculate the intrinsic
 * measurements by reusing the [measure] method. Note this will not be correct for all layouts, but
 * can be a convenient approximation.
 *
 * Intrinsic measurements can be useful when the layout system enforcement of no more than one
 * measurement per child is limiting. Layouts that use them are the `preferredWidth(IntrinsicSize)`
 * and `preferredHeight(IntrinsicSize)` modifiers. See their samples for when they can be useful.
 *
 * @see Layout
 */
@Stable
@JvmDefaultWithCompatibility
fun interface MeasurePolicy {
    /**
     * The function that defines the measurement and layout. Each [Measurable] in the [measurables]
     * list corresponds to a layout child of the layout, and children can be measured using the
     * [Measurable.measure] method. This method takes the [Constraints] which the child should
     * respect; different children can be measured with different constraints.
     *
     * Measuring a child returns a [Placeable], which reveals the size chosen by the child as a
     * result of its own measurement. According to the children sizes, the parent defines the
     * position of the children, by [placing][Placeable.PlacementScope.place] the [Placeable]s in
     * the [MeasureResult.placeChildren] of the returned [MeasureResult]. Therefore the parent needs
     * to measure its children with appropriate [Constraints], such that whatever valid sizes
     * children choose, they can be laid out correctly according to the parent's layout algorithm.
     * This is because there is no measurement negotiation between the parent and children: once a
     * child chooses its size, the parent needs to handle it correctly.
     *
     * Note that a child is allowed to choose a size that does not satisfy its constraints. However,
     * when this happens, the placeable's [width][Placeable.width] and [height][Placeable.height]
     * will not represent the real size of the child, but rather the size coerced in the child's
     * constraints. Therefore, it is common for parents to assume in their layout algorithm that its
     * children will always respect the constraints. When this does not happen in reality, the
     * position assigned to the child will be automatically offset to be centered on the space
     * assigned by the parent under the assumption that constraints were respected. Rarely, when a
     * parent really needs to know the true size of the child, they can read this from the
     * placeable's [Placeable.measuredWidth] and [Placeable.measuredHeight].
     *
     * [MeasureResult] objects are usually created using the [MeasureScope.layout] factory, which
     * takes the calculated size of this layout, its alignment lines, and a block defining the
     * positioning of the children layouts.
     */
    fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth]. It represents the
     * minimum width this layout can take, given a specific height, such that the content of the
     * layout can be painted correctly. There should be no side-effect from implementers of
     * [minIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        val mapped =
            measurables.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
            }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicHeight]. It represents the
     * minimum height this layout can take, given a specific width, such that the content of the
     * layout will be painted correctly. There should be no side-effect from implementers of
     * [minIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val mapped =
            measurables.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
            }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth]. It represents the
     * minimum width such that increasing it further will not decrease the minimum intrinsic height.
     * There should be no side-effects from implementers of [maxIntrinsicWidth].
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        val mapped =
            measurables.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
            }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicHeight]. It represents the
     * minimum height such that increasing it further will not decrease the minimum intrinsic width.
     * There should be no side-effects from implementers of [maxIntrinsicHeight].
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val mapped =
            measurables.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
            }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/layout/SubcomposeLayout.kt
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

package androidx.compose.ui.layout

import androidx.collection.IntList
import androidx.collection.MutableOrderedScatterSet
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntSetOf
import androidx.collection.mutableOrderedScatterSetOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.PausableComposition
import androidx.compose.runtime.PausedComposition
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.ShouldPauseCallback
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.internal.throwIllegalStateExceptionForNullCheck
import androidx.compose.ui.internal.throwIndexOutOfBoundsException
import androidx.compose.ui.layout.SubcomposeLayoutState.PausedPrecomposition
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode.Companion.ApplyOnDeactivatedNodeAssertion
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetModifier
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.OutOfFrameExecutor
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.checkMeasuredSize
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.createPausableSubcomposition
import androidx.compose.ui.platform.createSubcomposition
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmInline

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage for
 * example to use the values calculated during the measurement as params for the composition of the
 * children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 *   your use case with just custom [Layout] or [LayoutModifier]. See
 *   [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a list
 *   of 100 items and instead of composing all of them you only compose the ones which are currently
 *   visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
fun SubcomposeLayout(
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult,
) {
    SubcomposeLayout(
        state = remember { SubcomposeLayoutState() },
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage for
 * example to use the values calculated during the measurement as params for the composition of the
 * children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 *   your use case with just custom [Layout] or [LayoutModifier]. See
 *   [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a list
 *   of 100 items and instead of composing all of them you only compose the ones which are currently
 *   visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 * @param state the state object to be used by the layout.
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
@UiComposable
fun SubcomposeLayout(
    state: SubcomposeLayoutState,
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult,
) {
    val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
    val compositionContext = rememberCompositionContext()
    val materialized = currentComposer.materialize(modifier)
    val localMap = currentComposer.currentCompositionLocalMap
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(state, state.setRoot)
            set(compositionContext, state.setCompositionContext)
            set(measurePolicy, state.setMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            reconcile(ApplyOnDeactivatedNodeAssertion)
            set(materialized, SetModifier)
            set(compositeKeyHash, SetCompositeKeyHash)
        },
    )
    if (!currentComposer.skipping) {
        SideEffect { state.forceRecomposeChildren() }
    }
}

/**
 * The receiver scope of a [SubcomposeLayout]'s measure lambda which adds ability to dynamically
 * subcompose a content during the measuring on top of the features provided by [MeasureScope].
 */
interface SubcomposeMeasureScope : MeasureScope {
    /**
     * Performs subcomposition of the provided [content] with given [slotId].
     *
     * @param slotId unique id which represents the slot we are composing into. If you have fixed
     *   amount or slots you can use enums as slot ids, or if you have a list of items maybe an
     *   index in the list or some other unique key can work. To be able to correctly match the
     *   content between remeasures you should provide the object which is equals to the one you
     *   used during the previous measuring.
     * @param content the composable content which defines the slot. It could emit multiple layouts,
     *   in this case the returned list of [Measurable]s will have multiple elements. **Note:** When
     *   a [SubcomposeLayout] is in a [LookaheadScope], the subcomposition only happens during the
     *   lookahead pass. In the post-lookahead/main pass, [subcompose] will return the list of
     *   [Measurable]s that were subcomposed during the lookahead pass. If the structure of the
     *   subtree emitted from [content] is dependent on incoming constraints, consider using
     *   constraints received from the lookahead pass for both passes.
     */
    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable>
}

/**
 * State used by [SubcomposeLayout].
 *
 * [slotReusePolicy] the policy defining what slots should be retained to be reused later.
 */
class SubcomposeLayoutState(private val slotReusePolicy: SubcomposeSlotReusePolicy) {
    /** State used by [SubcomposeLayout]. */
    constructor() : this(NoOpSubcomposeSlotReusePolicy)

    /**
     * State used by [SubcomposeLayout].
     *
     * @param maxSlotsToRetainForReuse when non-zero the layout will keep active up to this count
     *   slots which we were used but not used anymore instead of disposing them. Later when you try
     *   to compose a new slot instead of creating a completely new slot the layout would reuse the
     *   previous slot which allows to do less work especially if the slot contents are similar.
     */
    @Deprecated(
        "This constructor is deprecated",
        ReplaceWith(
            "SubcomposeLayoutState(SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse))",
            "androidx.compose.ui.layout.SubcomposeSlotReusePolicy",
        ),
    )
    constructor(
        maxSlotsToRetainForReuse: Int
    ) : this(SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse))

    private var _state: LayoutNodeSubcompositionsState? = null
    private val state: LayoutNodeSubcompositionsState
        get() =
            requireNotNull(_state) { "SubcomposeLayoutState is not attached to SubcomposeLayout" }

    // Pre-allocated lambdas to update LayoutNode
    internal val setRoot: LayoutNode.(SubcomposeLayoutState) -> Unit = {
        _state =
            subcompositionsState
                ?: LayoutNodeSubcompositionsState(this, slotReusePolicy).also {
                    subcompositionsState = it
                }
        state.makeSureStateIsConsistent()
        state.slotReusePolicy = slotReusePolicy
    }
    internal val setCompositionContext: LayoutNode.(CompositionContext) -> Unit = {
        state.compositionContext = it
    }
    internal val setMeasurePolicy:
        LayoutNode.((SubcomposeMeasureScope.(Constraints) -> MeasureResult)) -> Unit =
        {
            measurePolicy = state.createMeasurePolicy(it)
        }

    /**
     * Composes the content for the given [slotId]. This makes the next scope.subcompose(slotId)
     * call during the measure pass faster as the content is already composed.
     *
     * If the [slotId] was precomposed already but after the future calculations ended up to not be
     * needed anymore (meaning this slotId is not going to be used during the measure pass anytime
     * soon) you can use [PrecomposedSlotHandle.dispose] on a returned object to dispose the
     * content.
     *
     * @param slotId unique id which represents the slot to compose into.
     * @param content the composable content which defines the slot.
     * @return [PrecomposedSlotHandle] instance which allows you to dispose the content.
     */
    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle =
        state.precompose(slotId, content)

    /**
     * Creates [PausedPrecomposition], which allows to perform the composition in an incremental
     * manner.
     *
     * @param slotId unique id which represents the slot to compose into.
     * @param content the composable content which defines the slot.]
     * @return [PausedPrecomposition] for the given [slotId]. It allows to perform the composition
     *   in an incremental manner. Performing full or partial precomposition makes the next
     *   scope.subcompose(slotId) call during the measure pass faster as the content is already
     *   composed.
     */
    fun createPausedPrecomposition(
        slotId: Any?,
        content: @Composable () -> Unit,
    ): PausedPrecomposition = state.precomposePaused(slotId, content)

    internal fun forceRecomposeChildren() = state.forceRecomposeChildren()

    /**
     * A [PausedPrecomposition] is a subcomposition that can be composed incrementally as it
     * supports being paused and resumed.
     *
     * Pausable subcomposition can be used between frames to prepare a subcomposition before it is
     * required by the main composition. For example, this is used in lazy lists to prepare list
     * items in between frames to that are likely to be scrolled in. The composition is paused when
     * the start of the next frame is near, allowing composition to be spread across multiple frames
     * without delaying the production of the next frame.
     *
     * @see [PausedComposition]
     */
    sealed interface PausedPrecomposition {

        /**
         * Returns `true` when the [PausedPrecomposition] is complete. [isComplete] matches the last
         * value returned from [resume]. Once a [PausedPrecomposition] is [isComplete] the [apply]
         * method should be called. If the [apply] method is not called synchronously and
         * immediately after [resume] returns `true` then this [isComplete] can return `false` as
         * any state changes read by the paused composition while it is paused will cause the
         * composition to require the paused composition to need to be resumed before it is used.
         */
        val isComplete: Boolean

        /**
         * Resume the composition that has been paused. This method should be called until [resume]
         * returns `true` or [isComplete] is `true` which has the same result as the last result of
         * calling [resume]. The [shouldPause] parameter is a lambda that returns whether the
         * composition should be paused. For example, in lazy lists this returns `false` until just
         * prior to the next frame starting in which it returns `true`
         *
         * Calling [resume] after it returns `true` or when `isComplete` is true will throw an
         * exception.
         *
         * @param shouldPause A lambda that is used to determine if the composition should be
         *   paused. This lambda is called often so should be a very simple calculation. Returning
         *   `true` does not guarantee the composition will pause, it should only be considered a
         *   request to pause the composition. Not all composable functions are pausable and only
         *   pausable composition functions will pause.
         * @return `true` if the composition is complete and `false` if one or more calls to
         *   `resume` are required to complete composition.
         */
        @Suppress("ExecutorRegistration") fun resume(shouldPause: ShouldPauseCallback): Boolean

        /**
         * Apply the composition. This is the last step of a paused composition and is required to
         * be called prior to the composition is usable.
         *
         * Calling [apply] should always be proceeded with a check of [isComplete] before it is
         * called and potentially calling [resume] in a loop until [isComplete] returns `true`. This
         * can happen if [resume] returned `true` but [apply] was not synchronously called
         * immediately afterwords. Any state that was read that changed between when [resume] being
         * called and [apply] being called may require the paused composition to be resumed before
         * applied.
         *
         * @return [PrecomposedSlotHandle] you can use to premeasure the slot as well, or to dispose
         *   the composed content.
         */
        fun apply(): PrecomposedSlotHandle

        /**
         * Cancels the paused composition. This should only be used if the composition is going to
         * be disposed and the entire composition is not going to be used.
         */
        fun cancel()
    }

    /** Instance of this interface is returned by [precompose] function. */
    interface PrecomposedSlotHandle {

        /**
         * This function allows to dispose the content for the slot which was precomposed previously
         * via [precompose].
         *
         * If this slot was already used during the regular measure pass via
         * [SubcomposeMeasureScope.subcompose] this function will do nothing.
         *
         * This could be useful if after the future calculations this item is not anymore expected
         * to be used during the measure pass anytime soon.
         */
        fun dispose()

        /** The amount of placeables composed into this slot. */
        val placeablesCount: Int
            get() = 0

        /**
         * Performs synchronous measure of the placeable at the given [index].
         *
         * @param index the placeable index. Should be smaller than [placeablesCount].
         * @param constraints Constraints to measure this placeable with.
         */
        fun premeasure(index: Int, constraints: Constraints) {}

        /**
         * Conditionally executes [block] for each [Modifier.Node] of this Composition that is a
         * [TraversableNode] with a matching [key].
         *
         * See [androidx.compose.ui.node.traverseDescendants] for the complete semantics of this
         * function.
         */
        fun traverseDescendants(key: Any?, block: (TraversableNode) -> TraverseDescendantsAction) {}

        /**
         * Retrieves the latest measured size for a given placeable [index]. This will return
         * [IntSize.Zero] if this is called before [premeasure].
         */
        fun getSize(index: Int): IntSize = IntSize.Zero
    }
}

/**
 * This policy allows [SubcomposeLayout] to retain some of slots which we were used but not used
 * anymore instead of disposing them. Next time when you try to compose a new slot instead of
 * creating a completely new slot the layout would reuse the kept slot. This allows to do less work
 * especially if the slot contents are similar.
 */
interface SubcomposeSlotReusePolicy {
    /**
     * This function will be called with [slotIds] set populated with the slot ids available to
     * reuse. In the implementation you can remove slots you don't want to retain.
     */
    fun getSlotsToRetain(slotIds: SlotIdsSet)

    /**
     * Returns true if the content previously composed with [reusableSlotId] is compatible with the
     * content which is going to be composed for [slotId]. Slots could be considered incompatible if
     * they display completely different types of the UI.
     */
    fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean

    /**
     * Set containing slot ids currently available to reuse. Used by [getSlotsToRetain]. The set
     * retains the insertion order of its elements, guaranteeing stable iteration order.
     *
     * This class works exactly as [MutableSet], but doesn't allow to add new items in it.
     */
    class SlotIdsSet
    internal constructor(
        @PublishedApi
        internal val set: MutableOrderedScatterSet<Any?> = mutableOrderedScatterSetOf()
    ) : Collection<Any?> {

        override val size: Int
            get() = set.size

        override fun isEmpty(): Boolean = set.isEmpty()

        override fun containsAll(elements: Collection<Any?>): Boolean {
            elements.forEach { element ->
                if (element !in set) {
                    return false
                }
            }
            return true
        }

        override fun contains(element: Any?): Boolean = set.contains(element)

        internal fun add(slotId: Any?) = set.add(slotId)

        override fun iterator(): MutableIterator<Any?> = set.asMutableSet().iterator()

        /**
         * Removes a [slotId] from this set, if it is present.
         *
         * @return `true` if the slot id was removed, `false` if the set was not modified.
         */
        fun remove(slotId: Any?): Boolean = set.remove(slotId)

        /**
         * Removes all slot ids from [slotIds] that are also contained in this set.
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(slotIds: Collection<Any?>): Boolean = set.remove(slotIds)

        /**
         * Removes all slot ids that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(predicate: (Any?) -> Boolean): Boolean {
            val size = set.size
            set.removeIf(predicate)
            return size != set.size
        }

        /**
         * Retains only the slot ids that are contained in [slotIds].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(slotIds: Collection<Any?>): Boolean = set.retainAll(slotIds)

        /**
         * Retains only slotIds that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(predicate: (Any?) -> Boolean): Boolean = set.retainAll(predicate)

        /** Removes all slot ids from this set. */
        fun clear() = set.clear()

        /**
         * Remove entries until [size] equals [maxSlotsToRetainForReuse]. Entries inserted last are
         * removed first.
         */
        fun trimToSize(maxSlotsToRetainForReuse: Int) = set.trimToSize(maxSlotsToRetainForReuse)

        /**
         * Iterates over every element stored in this set by invoking the specified [block] lambda.
         * The iteration order is the same as the insertion order. It is safe to remove the element
         * passed to [block] during iteration.
         *
         * NOTE: This method is obscured by `Collection<T>.forEach` since it is marked with
         *
         * @HidesMember, which means in practice this will never get called. Please use
         *   [fastForEach] instead.
         */
        fun forEach(block: (Any?) -> Unit) = set.forEach(block)

        /**
         * Iterates over every element stored in this set by invoking the specified [block] lambda.
         * The iteration order is the same as the insertion order. It is safe to remove the element
         * passed to [block] during iteration.
         *
         * NOTE: this method was added in order to allow for a more performant forEach method. It is
         * necessary because [forEach] is obscured by `Collection<T>.forEach` since it is marked
         * with @HidesMember.
         */
        inline fun fastForEach(block: (Any?) -> Unit) = set.forEach(block)
    }
}

/**
 * Creates [SubcomposeSlotReusePolicy] which retains the fixed amount of slots.
 *
 * @param maxSlotsToRetainForReuse the [SubcomposeLayout] will retain up to this amount of slots.
 */
fun SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse: Int): SubcomposeSlotReusePolicy =
    FixedCountSubcomposeSlotReusePolicy(maxSlotsToRetainForReuse)

/**
 * The inner state containing all the information about active slots and their compositions. It is
 * stored inside LayoutNode object as in fact we need to keep 1-1 mapping between this state and the
 * node: when we compose a slot we first create a virtual LayoutNode child to this node and then
 * save the extra information inside this state. Keeping this state inside LayoutNode also helps us
 * to retain the pool of reusable slots even when a new SubcomposeLayoutState is applied to
 * SubcomposeLayout and even when the SubcomposeLayout's LayoutNode is reused via the
 * ReusableComposeNode mechanism.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class LayoutNodeSubcompositionsState(
    private val root: LayoutNode,
    slotReusePolicy: SubcomposeSlotReusePolicy,
) : ComposeNodeLifecycleCallback {
    var compositionContext: CompositionContext? = null

    var slotReusePolicy: SubcomposeSlotReusePolicy = slotReusePolicy
        set(value) {
            if (field !== value) {
                field = value
                // the new policy will be applied after measure
                markActiveNodesAsReused(deactivate = false)
                root.requestRemeasure()
            }
        }

    private var currentIndex = 0
    private var currentApproachIndex = 0
    private val nodeToNodeState = mutableScatterMapOf<LayoutNode, NodeState>()

    // this map contains active slotIds (without precomposed or reusable nodes)
    private val slotIdToNode = mutableScatterMapOf<Any?, LayoutNode>()
    private val scope = Scope()
    private val approachMeasureScope = ApproachMeasureScopeImpl()

    private val precomposeMap = mutableScatterMapOf<Any?, LayoutNode>()
    private val reusableSlotIdsSet = SubcomposeSlotReusePolicy.SlotIdsSet()

    // SlotHandles precomposed in the approach pass. These slot handles are owned by the approach
    // pass, hence the approach pass is responsible for disposing them when they are no longer
    // needed. Note: if `precompose` is called on a slot owned by the approach pass, the
    // approach will yield ownership to the new caller. When the new caller disposes a slot
    // that is still needed by approach, the approach pass will be triggered to create
    // and own the slot.
    private val approachPrecomposeSlotHandleMap = mutableScatterMapOf<Any?, PrecomposedSlotHandle>()

    // Slot ids of compositions needed in the approach pass. These compositions are either owned
    // by the approach pass, or by the caller of [SubcomposeLayoutState#precompose]. For
    // compositions not created by the approach pass, if they are disposed while the approach pass
    // still needs it, the approach pass will be triggered to re-create the composition.
    // The valid slot ids are stored between 0 and currentApproachIndex - 1, beyond index
    // currentApproachIndex are [UnspecifiedSlotId]s.
    private val slotIdsOfCompositionsNeededInApproach = mutableVectorOf<Any?>()

    /**
     * `root.foldedChildren` list consist of:
     * 1) all the active children (used during the last measure pass)
     * 2) `reusableCount` nodes in the middle of the list which were active and stopped being used.
     *    now we keep them (up to `maxCountOfSlotsToReuse`) in order to reuse next time we will need
     *    to compose a new item
     * 4) `precomposedCount` nodes in the end of the list which were precomposed and are waiting to
     *    be used during the next measure passes.
     */
    private var reusableCount = 0
    private var precomposedCount = 0

    override fun onReuse() {
        markActiveNodesAsReused(deactivate = false)
    }

    override fun onDeactivate() {
        markActiveNodesAsReused(deactivate = true)
    }

    override fun onRelease() {
        disposeCurrentNodes()
    }

    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
        makeSureStateIsConsistent()
        val layoutState = root.layoutState
        checkPrecondition(
            layoutState == LayoutState.Measuring ||
                layoutState == LayoutState.LayingOut ||
                layoutState == LayoutState.LookaheadMeasuring ||
                layoutState == LayoutState.LookaheadLayingOut
        ) {
            "subcompose can only be used inside the measure or layout blocks"
        }

        val node =
            slotIdToNode.getOrPut(slotId) {
                val precomposed = precomposeMap.remove(slotId)
                if (precomposed != null) {
                    val nodeState = nodeToNodeState[precomposed]
                    if (ExtraLoggingEnabled) {
                        nodeState?.record(SLOperation.TookFromPrecomposeMap)
                    }
                    @Suppress("ExceptionMessage") checkPrecondition(precomposedCount > 0)
                    precomposedCount--
                    precomposed
                } else {
                    takeNodeFromReusables(slotId) ?: createNodeAt(currentIndex)
                }
            }

        if (root.foldedChildren.getOrNull(currentIndex) !== node) {
            // the node has a new index in the list
            val itemIndex = root.foldedChildren.indexOf(node)
            requirePrecondition(itemIndex >= currentIndex) {
                "Key \"$slotId\" was already used. If you are using LazyColumn/Row please make " +
                    "sure you provide a unique key for each item."
            }
            if (currentIndex != itemIndex) {
                move(itemIndex, currentIndex)
            }
        }
        currentIndex++

        subcompose(node, slotId, pausable = false, content)

        return if (layoutState == LayoutState.Measuring || layoutState == LayoutState.LayingOut) {
            node.childMeasurables
        } else {
            node.childLookaheadMeasurables
        }
    }

    // This may be called in approach pass, if a node is only emitted in the approach pass, but
    // not in the lookahead pass.
    private fun subcompose(
        node: LayoutNode,
        slotId: Any?,
        pausable: Boolean,
        content: @Composable () -> Unit,
    ) {
        val nodeState = nodeToNodeState.getOrPut(node) { NodeState(slotId, {}) }
        val contentChanged = nodeState.content !== content
        if (nodeState.pausedComposition != null) {
            if (contentChanged) {
                // content did change so it is not safe to apply the current paused composition.
                nodeState.cancelPausedPrecomposition()
            } else if (pausable) {
                // the paused composition is initialized and the content didn't change
                return
            } else {
                // we can apply as we are still composing the same content.
                nodeState.applyPausedPrecomposition(shouldComplete = true)
            }
        }
        val hasPendingChanges = nodeState.composition?.hasInvalidations ?: true
        if (contentChanged || hasPendingChanges || nodeState.forceRecompose) {
            nodeState.content = content
            subcompose(node, nodeState, pausable)
            nodeState.forceRecompose = false
        }
    }

    private val outOfFrameExecutor: OutOfFrameExecutor?
        get() = root.requireOwner().outOfFrameExecutor

    private fun subcompose(node: LayoutNode, nodeState: NodeState, pausable: Boolean) {
        requirePrecondition(nodeState.pausedComposition == null) {
            "new subcompose call while paused composition is still active"
        }
        Snapshot.withoutReadObservation {
            ignoreRemeasureRequests {
                val existing = nodeState.composition
                val parentComposition =
                    compositionContext
                        ?: throwIllegalStateExceptionForNullCheck(
                            "parent composition reference not set"
                        )
                if (ExtraLoggingEnabled) {
                    nodeState.record(
                        if (existing == null) SLOperation.SubcomposeNew else SLOperation.Subcompose
                    )
                    if (pausable) {
                        nodeState.record(SLOperation.SubcomposePausable)
                    }
                    if (nodeState.forceReuse) {
                        nodeState.record(SLOperation.SubcomposeForceReuse)
                    }
                }
                val composition =
                    if (existing == null || existing.isDisposed) {
                        if (pausable) {
                            createPausableSubcomposition(node, parentComposition)
                        } else {
                            createSubcomposition(node, parentComposition)
                        }
                    } else {
                        existing
                    }
                nodeState.composition = composition
                val content = nodeState.content
                val composable: @Composable () -> Unit =
                    if (outOfFrameExecutor != null) {
                        nodeState.composedWithReusableContentHost = false
                        content
                    } else {
                        nodeState.composedWithReusableContentHost = true
                        { ReusableContentHost(nodeState.active, content) }
                    }
                if (pausable) {
                    composition as PausableComposition
                    if (nodeState.forceReuse) {
                        nodeState.pausedComposition =
                            composition.setPausableContentWithReuse(composable)
                    } else {
                        nodeState.pausedComposition = composition.setPausableContent(composable)
                    }
                } else {
                    if (nodeState.forceReuse) {
                        composition.setContentWithReuse(composable)
                    } else {
                        composition.setContent(composable)
                    }
                }
                nodeState.forceReuse = false
            }
        }
    }

    private fun getSlotIdAtIndex(foldedChildren: List<LayoutNode>, index: Int): Any? {
        val node = foldedChildren[index]
        return nodeToNodeState[node]!!.slotId
    }

    fun disposeOrReuseStartingFromIndex(startIndex: Int) {
        reusableCount = 0
        val foldedChildren = root.foldedChildren
        val lastReusableIndex = foldedChildren.size - precomposedCount - 1
        var needApplyNotification = false
        if (startIndex <= lastReusableIndex) {
            // construct the set of available slot ids
            reusableSlotIdsSet.clear()
            for (i in startIndex..lastReusableIndex) {
                val slotId = getSlotIdAtIndex(foldedChildren, i)
                reusableSlotIdsSet.add(slotId)
            }

            slotReusePolicy.getSlotsToRetain(reusableSlotIdsSet)
            // iterating backwards so it is easier to remove items
            var i = lastReusableIndex
            Snapshot.withoutReadObservation {
                while (i >= startIndex) {
                    val node = foldedChildren[i]
                    val nodeState = nodeToNodeState[node]!!
                    val slotId = nodeState.slotId
                    if (slotId in reusableSlotIdsSet) {
                        reusableCount++
                        if (nodeState.active) {
                            node.resetLayoutState()
                            nodeState.reuseComposition(forceDeactivate = false)

                            if (nodeState.composedWithReusableContentHost) {
                                needApplyNotification = true
                            }
                        }
                    } else {
                        ignoreRemeasureRequests {
                            nodeToNodeState.remove(node)
                            nodeState.composition?.dispose()
                            root.removeAt(i, 1)
                        }
                    }
                    // remove it from slotIdToNode so it is not considered active
                    slotIdToNode.remove(slotId)
                    i--
                }
            }
        }

        if (needApplyNotification) {
            Snapshot.sendApplyNotifications()
        }

        makeSureStateIsConsistent()
    }

    private fun NodeState.deactivateOutOfFrame(executor: OutOfFrameExecutor) {
        executor.schedule {
            if (!active) {
                if (ExtraLoggingEnabled) {
                    record(SLOperation.DeactivateOutOfFrame)
                }
                composition?.deactivate()
            } else {
                if (ExtraLoggingEnabled) {
                    record(SLOperation.DeactivateOutOfFrameCancelled)
                }
            }
        }
    }

    private fun markActiveNodesAsReused(deactivate: Boolean) {
        precomposedCount = 0
        precomposeMap.clear()

        val foldedChildren = root.foldedChildren
        val childCount = foldedChildren.size
        if (reusableCount != childCount) {
            reusableCount = childCount
            Snapshot.withoutReadObservation {
                for (i in 0 until childCount) {
                    val node = foldedChildren[i]
                    val nodeState = nodeToNodeState[node]
                    if (nodeState != null && nodeState.active) {
                        node.resetLayoutState()
                        nodeState.reuseComposition(forceDeactivate = deactivate)
                        nodeState.slotId = ReusedSlotId
                        if (ExtraLoggingEnabled) {
                            if (deactivate) {
                                nodeState.record(SLOperation.SlotToReusedFromOnDeactivate)
                            } else {
                                nodeState.record(SLOperation.SlotToReusedFromOnReuse)
                            }
                        }
                    }
                }
            }
            slotIdToNode.clear()
        }

        makeSureStateIsConsistent()
    }

    private fun disposeCurrentNodes() {
        root.ignoreRemeasureRequests {
            nodeToNodeState.forEachValue { it.composition?.dispose() }
            root.removeAll()
        }

        nodeToNodeState.clear()
        slotIdToNode.clear()
        precomposedCount = 0
        reusableCount = 0
        precomposeMap.clear()

        makeSureStateIsConsistent()
    }

    fun makeSureStateIsConsistent() {
        val childrenCount = root.foldedChildren.size
        requirePrecondition(nodeToNodeState.size == childrenCount) {
            "Inconsistency between the count of nodes tracked by the state " +
                "(${nodeToNodeState.size}) and the children count on the SubcomposeLayout" +
                " ($childrenCount). Are you trying to use the state of the" +
                " disposed SubcomposeLayout?"
        }
        requirePrecondition(childrenCount - reusableCount - precomposedCount >= 0) {
            "Incorrect state. Total children $childrenCount. Reusable children " +
                "$reusableCount. Precomposed children $precomposedCount"
        }
        requirePrecondition(precomposeMap.size == precomposedCount) {
            "Incorrect state. Precomposed children $precomposedCount. Map size " +
                "${precomposeMap.size}"
        }
    }

    private fun LayoutNode.resetLayoutState() {
        measurePassDelegate.measuredByParent = UsageByParent.NotUsed
        lookaheadPassDelegate?.let { it.measuredByParent = UsageByParent.NotUsed }
    }

    private fun takeNodeFromReusables(slotId: Any?): LayoutNode? {
        if (reusableCount == 0) {
            return null
        }
        val foldedChildren = root.foldedChildren
        val reusableNodesSectionEnd = foldedChildren.size - precomposedCount
        val reusableNodesSectionStart = reusableNodesSectionEnd - reusableCount
        var index = reusableNodesSectionEnd - 1
        var chosenIndex = -1
        // first try to find a node with exactly the same slotId
        while (index >= reusableNodesSectionStart) {
            if (getSlotIdAtIndex(foldedChildren, index) == slotId) {
                // we have a node with the same slotId
                chosenIndex = index
                break
            } else {
                index--
            }
        }
        if (chosenIndex == -1) {
            // try to find a first compatible slotId from the end of the section
            index = reusableNodesSectionEnd - 1
            while (index >= reusableNodesSectionStart) {
                val node = foldedChildren[index]
                val nodeState = nodeToNodeState[node]!!
                if (
                    nodeState.slotId === ReusedSlotId ||
                        slotReusePolicy.areCompatible(slotId, nodeState.slotId)
                ) {
                    nodeState.slotId = slotId
                    chosenIndex = index
                    break
                }
                index--
            }
        }
        return if (chosenIndex == -1) {
            // no compatible nodes found
            null
        } else {
            if (index != reusableNodesSectionStart) {
                // we need to rearrange the items
                move(index, reusableNodesSectionStart, 1)
            }
            reusableCount--
            val node = foldedChildren[reusableNodesSectionStart]
            val nodeState = nodeToNodeState[node]!!
            // create a new instance to avoid change notifications
            if (ExtraLoggingEnabled) {
                nodeState.record(SLOperation.Reused)
            }
            nodeState.activeState = mutableStateOf(true)
            nodeState.forceReuse = true
            nodeState.forceRecompose = true
            node
        }
    }

    fun createMeasurePolicy(
        block: SubcomposeMeasureScope.(Constraints) -> MeasureResult
    ): MeasurePolicy {
        return object : LayoutNode.NoIntrinsicsMeasurePolicy(error = NoIntrinsicsMessage) {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                scope.layoutDirection = layoutDirection
                scope.density = density
                scope.fontScale = fontScale
                if (!isLookingAhead && root.lookaheadRoot != null) {
                    // Approach pass
                    currentApproachIndex = 0
                    val result = approachMeasureScope.block(constraints)
                    val indexAfterMeasure = currentApproachIndex
                    return createMeasureResult(result) {
                        currentApproachIndex = indexAfterMeasure
                        result.placeChildren()
                        // dispose
                        disposeUnusedSlotsInApproach()
                        disposeOrReuseStartingFromIndex(currentIndex)
                    }
                } else {
                    // Lookahead pass, or the main pass if not in a lookahead scope.
                    currentIndex = 0
                    val result = scope.block(constraints)
                    val indexAfterMeasure = currentIndex
                    return createMeasureResult(result) {
                        currentIndex = indexAfterMeasure
                        result.placeChildren()
                        if (root.lookaheadRoot == null) {
                            // If this is in lookahead scope, we need to dispose *after*
                            // approach placement, to give approach pass the opportunity to
                            // transfer the ownership of subcompositions before disposing.
                            disposeOrReuseStartingFromIndex(currentIndex)
                        }
                    }
                }
            }
        }
    }

    private fun disposeUnusedSlotsInApproach() {
        // Iterate over the slots owned by approach, and dispose slots if neither lookahead
        // nor approach needs it.
        approachPrecomposeSlotHandleMap.removeIf { slotId, handle ->
            val id = slotIdsOfCompositionsNeededInApproach.indexOf(slotId)
            if (id < 0 || id >= currentApproachIndex) {
                if (id >= 0) {
                    // Remove the slotId from the list before disposing
                    slotIdsOfCompositionsNeededInApproach[id] = UnspecifiedSlotId
                }
                if (precomposeMap.contains(slotId)) {
                    // Node has not been needed by lookahead, or approach.
                    handle.dispose()
                }
                true
            } else {
                false
            }
        }
    }

    private inline fun createMeasureResult(
        result: MeasureResult,
        crossinline placeChildrenBlock: () -> Unit,
    ) =
        object : MeasureResult by result {
            override fun placeChildren() {
                placeChildrenBlock()
            }
        }

    private val NoIntrinsicsMessage =
        "Asking for intrinsic measurements of SubcomposeLayout " +
            "layouts is not supported. This includes components that are built on top of " +
            "SubcomposeLayout, such as lazy lists, BoxWithConstraints, TabRow, etc. To mitigate " +
            "this:\n" +
            "- if intrinsic measurements are used to achieve 'match parent' sizing, consider " +
            "replacing the parent of the component with a custom layout which controls the order in " +
            "which children are measured, making intrinsic measurement not needed\n" +
            "- adding a size modifier to the component, in order to fast return the queried " +
            "intrinsic measurement."

    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle {
        precompose(slotId, content, pausable = false)
        return createPrecomposedSlotHandle(slotId)
    }

    private fun precompose(slotId: Any?, content: @Composable () -> Unit, pausable: Boolean) {
        if (!root.isAttached) {
            return
        }
        makeSureStateIsConsistent()
        if (!slotIdToNode.containsKey(slotId)) {
            // Yield ownership of PrecomposedHandle from approach to the caller of precompose
            approachPrecomposeSlotHandleMap.remove(slotId)
            val node =
                precomposeMap.getOrPut(slotId) {
                    val reusedNode = takeNodeFromReusables(slotId)
                    if (reusedNode != null) {
                        // now move this node to the end where we keep precomposed items
                        val nodeIndex = root.foldedChildren.indexOf(reusedNode)
                        move(nodeIndex, root.foldedChildren.size, 1)
                        precomposedCount++
                        reusedNode
                    } else {
                        createNodeAt(root.foldedChildren.size).also { precomposedCount++ }
                    }
                }
            subcompose(node, slotId, pausable = pausable, content)
        }
    }

    private fun NodeState.reuseComposition(forceDeactivate: Boolean) {
        if (!forceDeactivate && composedWithReusableContentHost) {
            // Deactivation through ReusableContentHost is controlled with the active flag
            active = false
        } else {
            // Otherwise, create a new instance to avoid state change notifications
            activeState = mutableStateOf(false)
        }

        if (pausedComposition != null) {
            // Cancelling disposes composition, so no additional work is needed.
            cancelPausedPrecomposition()
        } else if (forceDeactivate) {
            if (ExtraLoggingEnabled) {
                record(SLOperation.ReuseForceSyncDeactivation)
            }
            composition?.deactivate()
        } else {
            val outOfFrameExecutor = outOfFrameExecutor
            if (outOfFrameExecutor != null) {
                if (ExtraLoggingEnabled) {
                    record(SLOperation.ReuseScheduleOutOfFrameDeactivation)
                }
                deactivateOutOfFrame(outOfFrameExecutor)
            } else {
                if (!composedWithReusableContentHost) {
                    if (ExtraLoggingEnabled) {
                        record(SLOperation.ReuseSyncDeactivation)
                    }
                    composition?.deactivate()
                } else if (ExtraLoggingEnabled) {
                    record(SLOperation.ReuseDeactivationViaHost)
                }
            }
        }
    }

    private fun NodeState.cancelPausedPrecomposition() {
        pausedComposition?.let {
            it.cancel()
            pausedComposition = null
            composition?.dispose()
            composition = null
            if (ExtraLoggingEnabled) {
                record(SLOperation.CancelPausedPrecomposition)
            }
        }
    }

    private fun disposePrecomposedSlot(slotId: Any?) {
        makeSureStateIsConsistent()
        val node = precomposeMap.remove(slotId)
        if (node != null) {
            checkPrecondition(precomposedCount > 0) { "No pre-composed items to dispose" }
            val itemIndex = root.foldedChildren.indexOf(node)
            checkPrecondition(itemIndex >= root.foldedChildren.size - precomposedCount) {
                "Item is not in pre-composed item range"
            }
            // move this item into the reusable section
            reusableCount++
            precomposedCount--

            nodeToNodeState[node]?.cancelPausedPrecomposition()

            val reusableStart = root.foldedChildren.size - precomposedCount - reusableCount
            move(itemIndex, reusableStart, 1)
            disposeOrReuseStartingFromIndex(reusableStart)
        }
        // If the slot is not owned by approach (e.g. created for prefetch) and disposed before
        // approach finishes using it, the approach pass will be invoked to re-create the
        // composition if needed.
        if (slotIdsOfCompositionsNeededInApproach.contains(slotId)) {
            root.requestRemeasure(true)
        }
    }

    private fun createPrecomposedSlotHandle(slotId: Any?): PrecomposedSlotHandle {
        if (!root.isAttached) {
            return object : PrecomposedSlotHandle {
                override fun dispose() {}
            }
        }
        return object : PrecomposedSlotHandle {
            // Saves indices of placeables that have been premeasured in this handle
            val hasPremeasured = mutableIntSetOf()

            override fun dispose() {
                disposePrecomposedSlot(slotId)
            }

            override val placeablesCount: Int
                get() = precomposeMap[slotId]?.children?.size ?: 0

            override fun premeasure(index: Int, constraints: Constraints) {
                val node = precomposeMap[slotId]
                if (node != null && node.isAttached) {
                    val size = node.children.size
                    if (index < 0 || index >= size) {
                        throwIndexOutOfBoundsException(
                            "Index ($index) is out of bound of [0, $size)"
                        )
                    }
                    requirePrecondition(!node.isPlaced) {
                        "Pre-measure called on node that is not placed"
                    }
                    root.ignoreRemeasureRequests {
                        node.requireOwner().measureAndLayout(node.children[index], constraints)
                    }
                    hasPremeasured.add(index)
                }
            }

            override fun traverseDescendants(
                key: Any?,
                block: (TraversableNode) -> TraverseDescendantsAction,
            ) {
                precomposeMap[slotId]?.nodes?.head?.traverseDescendants(key, block)
            }

            override fun getSize(index: Int): IntSize {
                val node = precomposeMap[slotId]
                if (node != null && node.isAttached) {
                    val size = node.children.size
                    if (index < 0 || index >= size) {
                        throwIndexOutOfBoundsException(
                            "Index ($index) is out of bound of [0, $size)"
                        )
                    }

                    if (hasPremeasured.contains(index)) {
                        return IntSize(node.children[index].width, node.children[index].height)
                    }
                }
                return IntSize.Zero
            }
        }
    }

    fun precomposePaused(slotId: Any?, content: @Composable () -> Unit): PausedPrecomposition {
        if (!root.isAttached) {
            return object : PausedPrecompositionImpl {
                override val isComplete: Boolean = true

                override fun resume(shouldPause: ShouldPauseCallback) = true

                override fun apply() = createPrecomposedSlotHandle(slotId)

                override fun cancel() {}
            }
        }
        precompose(slotId, content, pausable = true)
        return object : PausedPrecompositionImpl {
            override fun cancel() {
                if (nodeState?.pausedComposition != null) {
                    // only dispose if the paused composition is still waiting to be applied
                    disposePrecomposedSlot(slotId)
                }
            }

            private val nodeState: NodeState?
                get() = precomposeMap[slotId]?.let { nodeToNodeState[it] }

            override val isComplete: Boolean
                get() = nodeState?.pausedComposition?.isComplete ?: true

            override fun resume(shouldPause: ShouldPauseCallback): Boolean {
                val nodeState = nodeState
                val pausedComposition = nodeState?.pausedComposition
                return if (pausedComposition != null && !pausedComposition.isComplete) {
                    if (ExtraLoggingEnabled) {
                        nodeState.record(SLOperation.ResumePaused)
                    }
                    val isComplete =
                        Snapshot.withoutReadObservation {
                            try {
                                pausedComposition.resume(shouldPause)
                            } catch (e: Throwable) {
                                val operations = nodeState.operations
                                if (operations != null) {
                                    throw SubcomposeLayoutPausableCompositionException(
                                        nodeState.operations,
                                        slotId,
                                        e,
                                    )
                                } else {
                                    throw e
                                }
                            }
                        }
                    if (ExtraLoggingEnabled && !isComplete) {
                        nodeState.record(SLOperation.PausePaused)
                    }
                    isComplete
                } else {
                    true
                }
            }

            override fun apply(): PrecomposedSlotHandle {
                nodeState?.applyPausedPrecomposition(shouldComplete = false)
                return createPrecomposedSlotHandle(slotId)
            }
        }
    }

    fun forceRecomposeChildren() {
        val childCount = root.foldedChildren.size
        if (reusableCount != childCount) {
            // only invalidate children if there are any non-reused ones
            // in other cases, all of them are going to be invalidated later anyways
            nodeToNodeState.forEachValue { nodeState -> nodeState.forceRecompose = true }

            if (root.lookaheadRoot != null) {
                // If the SubcomposeLayout is in a LookaheadScope, request for a lookahead measure
                // so that lookahead gets triggered again to recompose children.
                if (!root.lookaheadMeasurePending) {
                    root.requestLookaheadRemeasure()
                }
            } else {
                if (!root.measurePending) {
                    root.requestRemeasure()
                }
            }
        }
    }

    private fun createNodeAt(index: Int) =
        LayoutNode(isVirtual = true).also { node ->
            ignoreRemeasureRequests { root.insertAt(index, node) }
        }

    private fun move(from: Int, to: Int, count: Int = 1) {
        ignoreRemeasureRequests { root.move(from, to, count) }
    }

    private inline fun <T> ignoreRemeasureRequests(block: () -> T): T =
        root.ignoreRemeasureRequests(block)

    private fun NodeState.applyPausedPrecomposition(shouldComplete: Boolean) {
        val pausedComposition = pausedComposition
        if (pausedComposition != null) {
            Snapshot.withoutReadObservation {
                ignoreRemeasureRequests {
                    try {
                        if (shouldComplete) {
                            while (!pausedComposition.isComplete) {
                                pausedComposition.resume { false }
                            }
                        }
                        pausedComposition.apply()
                    } catch (e: Throwable) {
                        val operations = operations
                        if (operations != null) {
                            throw SubcomposeLayoutPausableCompositionException(
                                operations,
                                slotId,
                                e,
                            )
                        } else {
                            throw e
                        }
                    }
                    this.pausedComposition = null
                }
            }
        }
    }

    private class NodeState(
        var slotId: Any?,
        var content: @Composable () -> Unit,
        var composition: ReusableComposition? = null,
    ) {
        var forceRecompose = false
        var forceReuse = false
        var pausedComposition: PausedComposition? = null
        var activeState = mutableStateOf(true)
        var composedWithReusableContentHost = false
        var active: Boolean
            get() = activeState.value
            set(value) {
                activeState.value = value
            }

        val operations = if (ExtraLoggingEnabled) mutableIntListOf() else null

        fun record(op: SLOperation) {
            val operations = operations ?: return
            operations.add(op.value)
            if (operations.size >= 50) {
                operations.removeRange(0, 10)
            }
        }
    }

    private inner class Scope : SubcomposeMeasureScope {
        // MeasureScope delegation
        override var layoutDirection: LayoutDirection = LayoutDirection.Rtl
        override var density: Float = 0f
        override var fontScale: Float = 0f
        override val isLookingAhead: Boolean
            get() =
                root.layoutState == LayoutState.LookaheadLayingOut ||
                    root.layoutState == LayoutState.LookaheadMeasuring

        override fun subcompose(slotId: Any?, content: @Composable () -> Unit) =
            this@LayoutNodeSubcompositionsState.subcompose(slotId, content)

        override fun layout(
            width: Int,
            height: Int,
            alignmentLines: Map<AlignmentLine, Int>,
            rulers: (RulerScope.() -> Unit)?,
            placementBlock: Placeable.PlacementScope.() -> Unit,
        ): MeasureResult {
            checkMeasuredSize(width, height)
            return object : MeasureResult {
                override val width: Int
                    get() = width

                override val height: Int
                    get() = height

                override val alignmentLines: Map<AlignmentLine, Int>
                    get() = alignmentLines

                override val rulers: (RulerScope.() -> Unit)?
                    get() = rulers

                override fun placeChildren() {
                    if (isLookingAhead) {
                        val delegate = root.innerCoordinator.lookaheadDelegate
                        if (delegate != null) {
                            delegate.placementScope.placementBlock()
                            return
                        }
                    }
                    root.innerCoordinator.placementScope.placementBlock()
                }
            }
        }
    }

    private inner class ApproachMeasureScopeImpl : SubcomposeMeasureScope, MeasureScope by scope {
        /**
         * This function retrieves [Measurable]s created for [slotId] based on the subcomposition
         * that happened in the lookahead pass. If [slotId] was not subcomposed in the lookahead
         * pass, [subcompose] will return an [emptyList].
         */
        override fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
            val nodeInSlot = slotIdToNode[slotId]
            if (nodeInSlot != null && root.foldedChildren.indexOf(nodeInSlot) < currentIndex) {
                // Check that the node has been composed in lookahead. Otherwise, we need to
                // compose the node in approach pass via approachSubcompose.
                return nodeInSlot.childMeasurables
            } else {
                return approachSubcompose(slotId, content)
            }
        }
    }

    private fun approachSubcompose(
        slotId: Any?,
        content: @Composable () -> Unit,
    ): List<Measurable> {
        requirePrecondition(slotIdsOfCompositionsNeededInApproach.size >= currentApproachIndex) {
            "Error: currentApproachIndex cannot be greater than the size of the" +
                "approachComposedSlotIds list."
        }
        val nodeForSlot = slotIdToNode[slotId]
        if (slotIdsOfCompositionsNeededInApproach.size == currentApproachIndex) {
            slotIdsOfCompositionsNeededInApproach.add(slotId)
        } else {
            slotIdsOfCompositionsNeededInApproach[currentApproachIndex] = slotId
        }
        currentApproachIndex++
        val precomposed = precomposeMap.contains(slotId)
        if (!precomposed && nodeForSlot == null) {
            // The slot was not composed in the lookahead pass. And it has not been pre-composed in
            // the approach pass. Hence, we will precompose it for the approach pass, and track it
            // in approachPrecomposeSlotHandleMap so that it can be disposed when no longer needed
            // in approach.
            precompose(slotId, content).also { approachPrecomposeSlotHandleMap[slotId] = it }
        } else {
            // A non-null `nodeForSlot` here means that the slot was composed in lookahead
            // initially, but no longer needed && has not been disposed yet.
            // Move from lookahead composed to pre-composed, so that it can be disposed when
            // no longer needed in approach.
            if (!precomposed && nodeForSlot != null) {
                // Transfer ownership of the subcomposition from lookahead pass to approach pass.
                // As a result, the composition can be disposed as soon as approach pass no
                // longer needs it.
                // First, move this node to the end where we keep precomposed items
                val nodeIndex = root.foldedChildren.indexOf(nodeForSlot)
                move(nodeIndex, root.foldedChildren.size, 1)
                precomposedCount++
                // Remove the slotId from slotIdToNode so that if lookahead were to subcompose
                // this item, it'll need to take the node out of precomposeMap.
                slotIdToNode.remove(slotId)
                precomposeMap[slotId] = nodeForSlot
                approachPrecomposeSlotHandleMap[slotId] = createPrecomposedSlotHandle(slotId)

                if (root.isAttached) {
                    makeSureStateIsConsistent()
                }
            }

            // Re-subcompose if needed based on forceRecompose
            val node = precomposeMap[slotId]
            val nodeState = node?.let { nodeToNodeState[it] }
            if (nodeState?.forceRecompose == true) {
                subcompose(node, slotId, pausable = false, content)
            }

            // Finish pausable composition if it has not been completed yet
            if (nodeState?.pausedComposition != null) {
                nodeState.applyPausedPrecomposition(shouldComplete = true)
            }
        }

        return precomposeMap[slotId]?.run {
            measurePassDelegate.childDelegates.also {
                it.fastForEach { delegate -> delegate.markDetachedFromParentLookaheadPass() }
            }
        } ?: emptyList()
    }
}

private val ReusedSlotId =
    object {
        override fun toString(): String = "ReusedSlotId"
    }

private class FixedCountSubcomposeSlotReusePolicy(private val maxSlotsToRetainForReuse: Int) :
    SubcomposeSlotReusePolicy {

    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        if (slotIds.size > maxSlotsToRetainForReuse) {
            slotIds.trimToSize(maxSlotsToRetainForReuse)
        }
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean = true
}

private object NoOpSubcomposeSlotReusePolicy : SubcomposeSlotReusePolicy {
    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        slotIds.clear()
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?) = false
}

private interface PausedPrecompositionImpl : PausedPrecomposition

private val UnspecifiedSlotId = Any()

@JvmInline
private value class SLOperation(val value: Int) {
    companion object {
        val CancelPausedPrecomposition = SLOperation(0)
        val ReuseForceSyncDeactivation = SLOperation(1)
        val ReuseScheduleOutOfFrameDeactivation = SLOperation(2)
        val ReuseSyncDeactivation = SLOperation(3)
        val ReuseDeactivationViaHost = SLOperation(4)
        val TookFromPrecomposeMap = SLOperation(5)
        val Subcompose = SLOperation(6)
        val SubcomposeNew = SLOperation(7)
        val SubcomposePausable = SLOperation(8)
        val SubcomposeForceReuse = SLOperation(9)
        val DeactivateOutOfFrame = SLOperation(10)
        val DeactivateOutOfFrameCancelled = SLOperation(11)
        val SlotToReusedFromOnDeactivate = SLOperation(12)
        val SlotToReusedFromOnReuse = SLOperation(13)
        val Reused = SLOperation(14)
        val ResumePaused = SLOperation(15)
        val PausePaused = SLOperation(16)
        val ApplyPaused = SLOperation(17)
    }
}

private class SubcomposeLayoutPausableCompositionException(
    private val operations: IntList,
    private val slotId: Any?,
    cause: Throwable?,
) : IllegalStateException(cause) {

    private fun operationsList(): List<String> = buildList {
        var currentOperation = operations.size - 1
        while (currentOperation >= 0) {
            val operation = operations[currentOperation]
            val stringValue =
                when (SLOperation(operation)) {
                    SLOperation.CancelPausedPrecomposition -> "CancelPausedPrecomposition"
                    SLOperation.ReuseForceSyncDeactivation -> "ReuseForceSyncDeactivation"
                    SLOperation.ReuseScheduleOutOfFrameDeactivation ->
                        "ReuseScheduleOutOfFrameDeactivation"
                    SLOperation.ReuseSyncDeactivation -> "ReuseSyncDeactivation"
                    SLOperation.ReuseDeactivationViaHost -> "ReuseDeactivationViaHost"
                    SLOperation.TookFromPrecomposeMap -> "TookFromPrecomposeMap"
                    SLOperation.Subcompose -> "Subcompose"
                    SLOperation.SubcomposeNew -> "SubcomposeNew"
                    SLOperation.SubcomposePausable -> "SubcomposePausable"
                    SLOperation.SubcomposeForceReuse -> "SubcomposeForceReuse"
                    SLOperation.DeactivateOutOfFrame -> "DeactivateOutOfFrame"
                    SLOperation.DeactivateOutOfFrameCancelled -> "DeactivateOutOfFrameCancelled"
                    SLOperation.SlotToReusedFromOnDeactivate -> "SlotToReusedFromOnDeactivate"
                    SLOperation.SlotToReusedFromOnReuse -> "SlotToReusedFromOnReuse"
                    SLOperation.Reused -> "Reused"
                    SLOperation.ResumePaused -> "ResumePaused"
                    SLOperation.PausePaused -> "PausePaused"
                    SLOperation.ApplyPaused -> "ApplyPaused"
                    else -> "Unexpected $operation"
                }
            add("$currentOperation: $stringValue")
            currentOperation--
        }
    }

    @Suppress("ListIterator")
    override val message: String?
        get() =
            """
            |slotid=$slotId. Last operations:
            |${operationsList().joinToString("\n")}
            """
                .trimMargin()
}

private const val ExtraLoggingEnabled = false
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/layout/IntrinsicMeasurable.kt
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

package androidx.compose.ui.layout

/**
 * A part of the composition that can be measured. This represents a layout. The instance should
 * never be stored.
 */
interface IntrinsicMeasurable {
    /** Data provided by the [ParentDataModifier]. */
    val parentData: Any?

    /**
     * Calculates the minimum width that the layout can be such that the content of the layout will
     * be painted correctly. There should be no side-effects from a call to [minIntrinsicWidth].
     */
    fun minIntrinsicWidth(height: Int): Int

    /**
     * Calculates the smallest width beyond which increasing the width never decreases the height.
     * There should be no side-effects from a call to [maxIntrinsicWidth].
     */
    fun maxIntrinsicWidth(height: Int): Int

    /**
     * Calculates the minimum height that the layout can be such that the content of the layout will
     * be painted correctly. There should be no side-effects from a call to [minIntrinsicHeight].
     */
    fun minIntrinsicHeight(width: Int): Int

    /**
     * Calculates the smallest height beyond which increasing the height never decreases the width.
     * There should be no side-effects from a call to [maxIntrinsicHeight].
     */
    fun maxIntrinsicHeight(width: Int): Int
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutNode.kt
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
package androidx.compose.ui.node

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.tooling.CompositionErrorContext
import androidx.compose.runtime.tooling.LocalCompositionErrorContext
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.LayoutNodeSubcompositionsState
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.node.LayoutNode.LayoutState.Idle
import androidx.compose.ui.node.LayoutNode.LayoutState.LayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadLayingOut
import androidx.compose.ui.node.LayoutNode.LayoutState.LookaheadMeasuring
import androidx.compose.ui.node.LayoutNode.LayoutState.Measuring
import androidx.compose.ui.node.Nodes.PointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.simpleIdentityToString
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsInfo
import androidx.compose.ui.semantics.generateSemanticsId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewFactoryHolder

/** Enable to log changes to the LayoutNode tree. This logging is quite chatty. */
private const val DebugChanges = false

private val DefaultDensity = Density(1f)

/** An element in the layout hierarchy, built with compose UI. */
@OptIn(InternalComposeUiApi::class)
internal class LayoutNode(
    // Virtual LayoutNode is the temporary concept allows us to a node which is not a real node,
    // but just a holder for its children - allows us to combine some children into something we
    // can subcompose in(LayoutNode) without being required to define it as a real layout - we
    // don't want to define the layout strategy for such nodes, instead the children of the
    // virtual nodes will be treated as the direct children of the virtual node parent.
    // This whole concept will be replaced with a proper subcomposition logic which allows to
    // subcompose multiple times into the same LayoutNode and define offsets.
    private val isVirtual: Boolean = false,
    // The unique semantics ID that is used by all semantics modifiers attached to this LayoutNode.
    // TODO(b/281907968): Implement this with a getter that returns the compositeKeyHash.
    override var semanticsId: Int = generateSemanticsId(),
) :
    ComposeNodeLifecycleCallback,
    Remeasurement,
    OwnerScope,
    LayoutInfo,
    SemanticsInfo,
    ComposeUiNode,
    InteroperableComposeUiNode,
    Owner.OnLayoutCompletedListener {

    // Params managed by RectManager start:
    internal var hasPositionalLayerTransformationsInOffsetFromRoot: Boolean = false
    // this offset contains the combined offset accumulated by the coordinators attached to
    // this node, not including the offset of the outer one, as the outer offset is part of the
    // offsetFromRoot of this node, and the rest of the modifiers are affecting offsetFromRoot
    // for the children.
    internal var outerToInnerOffset: IntOffset = IntOffset.Max
    internal var outerToInnerOffsetDirty: Boolean = true
    // rect in parent is the sum of transformations for parent's coordinators not including the
    // outer one, and the transformations on this node's outer coordinator.
    internal var rectInParentDirty: Boolean = true
    internal var addedToRectList: Boolean = false
    // Params managed by RectManager end.

    override var compositeKeyHash: Int = 0

    internal var isVirtualLookaheadRoot: Boolean = false

    /**
     * This lookaheadRoot references the closest root to the LayoutNode, not the top-level lookahead
     * root.
     */
    internal var lookaheadRoot: LayoutNode? = null
        private set(newRoot) {
            if (newRoot != field) {
                field = newRoot
                if (newRoot != null) {
                    layoutDelegate.ensureLookaheadDelegateCreated()
                    forEachCoordinatorIncludingInner { it.ensureLookaheadDelegateCreated() }
                } else {
                    // When lookahead root is set to null, clear the lookahead pass delegate.
                    // This can happen when lookaheadScope is removed in one of the parents, or
                    // more likely when movableContent moves from a parent in a LookaheadScope to
                    // a parent not in a LookaheadScope.
                    layoutDelegate.onRemovedFromLookaheadScope()
                }
                invalidateMeasurements()
            }
        }

    val isPlacedInLookahead: Boolean?
        get() = lookaheadPassDelegate?.isPlaced

    private var virtualChildrenCount = 0

    // the list of nodes containing the virtual children as is
    private val _foldedChildren =
        MutableVectorWithMutationTracking(mutableVectorOf<LayoutNode>()) {
            layoutDelegate.markChildrenDirty()
        }
    internal val foldedChildren: List<LayoutNode>
        get() = _foldedChildren.asList()

    // the list of nodes where the virtual children are unfolded (their children are represented
    // as our direct children)
    private var _unfoldedChildren: MutableVector<LayoutNode>? = null

    private fun recreateUnfoldedChildrenIfDirty() {
        if (unfoldedVirtualChildrenListDirty) {
            unfoldedVirtualChildrenListDirty = false
            val unfoldedChildren =
                _unfoldedChildren ?: mutableVectorOf<LayoutNode>().also { _unfoldedChildren = it }
            unfoldedChildren.clear()
            _foldedChildren.forEach {
                if (it.isVirtual) {
                    unfoldedChildren.addAll(it._children)
                } else {
                    unfoldedChildren.add(it)
                }
            }
            layoutDelegate.markChildrenDirty()
        }
    }

    internal val childMeasurables: List<Measurable>
        get() = measurePassDelegate.childDelegates

    internal val childLookaheadMeasurables: List<Measurable>
        get() = lookaheadPassDelegate!!.childDelegates

    // when the list of our children is modified it will be set to true if we are a virtual node
    // or it will be set to true on a parent if the parent is a virtual node
    private var unfoldedVirtualChildrenListDirty = false

    private fun invalidateUnfoldedVirtualChildren() {
        if (virtualChildrenCount > 0) {
            unfoldedVirtualChildrenListDirty = true
        }
        if (isVirtual) {
            // Invalidate all virtual unfolded parent until we reach a non-virtual one
            this._foldedParent?.invalidateUnfoldedVirtualChildren()
        }
    }

    /**
     * This should **not** be mutated or even accessed directly from outside of [LayoutNode]. Use
     * [forEachChild]/[forEachChildIndexed] when there's a need to iterate through the vector.
     */
    internal val _children: MutableVector<LayoutNode>
        get() {
            updateChildrenIfDirty()
            return if (virtualChildrenCount == 0) {
                _foldedChildren.vector
            } else {
                _unfoldedChildren!!
            }
        }

    /** Update children if the list is not up to date. */
    internal fun updateChildrenIfDirty() {
        if (virtualChildrenCount > 0) {
            recreateUnfoldedChildrenIfDirty()
        }
    }

    inline fun forEachChild(block: (LayoutNode) -> Unit) = _children.forEach(block)

    inline fun forEachChildIndexed(block: (Int, LayoutNode) -> Unit) =
        _children.forEachIndexed(block)

    /** The children of this LayoutNode, controlled by [insertAt], [move], and [removeAt]. */
    internal val children: List<LayoutNode>
        get() = _children.asMutableList()

    /**
     * The parent node in the LayoutNode hierarchy. This is `null` when the [LayoutNode] is not
     * attached to a hierarchy or is the root of the hierarchy.
     */
    private var _foldedParent: LayoutNode? = null

    /*
     * The parent node in the LayoutNode hierarchy, skipping over virtual nodes.
     */
    internal val parent: LayoutNode?
        get() {
            var parent = _foldedParent
            while (parent?.isVirtual == true) {
                parent = parent._foldedParent
            }
            return parent
        }

    /** The view system [Owner]. This `null` until [attach] is called */
    internal var owner: Owner? = null
        private set

    /**
     * The [InteropViewFactoryHolder] associated with this node, which is used to instantiate and
     * manage platform View instances that are hosted in Compose.
     */
    internal var interopViewFactoryHolder: InteropViewFactoryHolder? = null

    @InternalComposeUiApi
    override fun getInteropView(): InteropView? = interopViewFactoryHolder?.getInteropView()

    /**
     * Returns true if this [LayoutNode] currently has an [LayoutNode.owner]. Semantically, this
     * means that the LayoutNode is currently a part of a component tree.
     */
    override val isAttached: Boolean
        get() = owner != null

    /**
     * The tree depth of the [LayoutNode]. This is valid only when it is attached to a hierarchy.
     */
    internal var depth: Int = 0

    /**
     * The layout state the node is currently in.
     *
     * The mutation of [layoutState] is confined to [LayoutNode], and is therefore read-only outside
     * LayoutNode. This makes the state machine easier to reason about.
     */
    internal val layoutState
        get() = layoutDelegate.layoutState

    /**
     * The lookahead pass delegate for the [LayoutNode]. This should only be used for measure and
     * layout related impl during *lookahead*. For the actual measure & layout, use
     * [measurePassDelegate].
     */
    internal val lookaheadPassDelegate
        get() = layoutDelegate.lookaheadPassDelegate

    /**
     * The measure pass delegate for the [LayoutNode]. This delegate is responsible for the actual
     * measure & layout, after lookahead if any.
     */
    internal val measurePassDelegate
        get() = layoutDelegate.measurePassDelegate

    /** [requestRemeasure] calls will be ignored while this flag is true. */
    private var ignoreRemeasureRequests = false

    /**
     * Inserts a child [LayoutNode] at a particular index. If this LayoutNode [owner] is not `null`
     * then [instance] will become [attach]ed also. [instance] must have a `null` [parent].
     */
    internal fun insertAt(index: Int, instance: LayoutNode) {
        checkPrecondition(instance._foldedParent == null || instance.owner == null) {
            exceptionMessageForParentingOrOwnership(instance)
        }

        if (DebugChanges) {
            println("$instance added to $this at index $index")
        }

        instance._foldedParent = this
        _foldedChildren.add(index, instance)
        onZSortedChildrenInvalidated()

        if (instance.isVirtual) {
            virtualChildrenCount++
        }
        invalidateUnfoldedVirtualChildren()

        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }

        if (instance.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement++
        }
        if (instance.globallyPositionedObservers > 0) {
            globallyPositionedObservers++
        }
    }

    private fun exceptionMessageForParentingOrOwnership(instance: LayoutNode) =
        "Cannot insert $instance because it already has a parent or an owner." +
            " This tree: " +
            debugTreeToString() +
            " Other tree: " +
            instance._foldedParent?.debugTreeToString()

    internal fun onZSortedChildrenInvalidated() {
        if (isVirtual) {
            parent?.onZSortedChildrenInvalidated()
        } else {
            zSortedChildrenInvalidated = true
        }
    }

    /** Removes one or more children, starting at [index]. */
    internal fun removeAt(index: Int, count: Int) {
        requirePrecondition(count >= 0) { "count ($count) must be greater than 0" }
        for (i in index + count - 1 downTo index) {
            // Call detach callbacks before removing from _foldedChildren, so the child is still
            // visible to parents traversing downwards, such as when clearing focus.
            onChildRemoved(_foldedChildren[i])
            val child = _foldedChildren.removeAt(i)
            if (DebugChanges) {
                println("$child removed from $this at index $i")
            }
        }
    }

    /** Removes all children. */
    internal fun removeAll() {
        for (i in _foldedChildren.size - 1 downTo 0) {
            onChildRemoved(_foldedChildren[i])
        }
        _foldedChildren.clear()

        if (DebugChanges) {
            println("Removed all children from $this")
        }
    }

    private fun onChildRemoved(child: LayoutNode) {
        if (child.layoutDelegate.childrenAccessingCoordinatesDuringPlacement > 0) {
            layoutDelegate.childrenAccessingCoordinatesDuringPlacement--
        }
        if (owner != null) {
            child.detach()
        }
        child._foldedParent = null
        if (child.globallyPositionedObservers > 0) {
            globallyPositionedObservers--
        }

        child.outerCoordinator.wrappedBy = null

        if (child.isVirtual) {
            virtualChildrenCount--
            child._foldedChildren.forEach { it.outerCoordinator.wrappedBy = null }
        }
        invalidateUnfoldedVirtualChildren()
        onZSortedChildrenInvalidated()
    }

    /**
     * Moves [count] elements starting at index [from] to index [to]. The [to] index is related to
     * the position before the change, so, for example, to move an element at position 1 to after
     * the element at position 2, [from] should be `1` and [to] should be `3`. If the elements were
     * LayoutNodes A B C D E, calling `move(1, 3, 1)` would result in the LayoutNodes being
     * reordered to A C B D E.
     */
    internal fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = _foldedChildren.removeAt(fromIndex)

            if (DebugChanges) {
                println("$child moved in $this from index $fromIndex to $toIndex")
            }

            _foldedChildren.add(toIndex, child)
        }
        onZSortedChildrenInvalidated()

        invalidateUnfoldedVirtualChildren()
        invalidateMeasurements()
    }

    override fun isTransparent(): Boolean = outerCoordinator.isTransparent()

    internal var isSemanticsInvalidated = false

    internal fun requestAutofill() {
        // Ignore calls while semantics are being applied (b/378114177).
        if (isCurrentlyCalculatingSemanticsConfiguration) return

        val owner = requireOwner()
        owner.requestAutofill(this)
    }

    internal fun invalidateSemantics() {
        // Ignore calls to invalidate Semantics while semantics are being applied (b/378114177).
        if (isCurrentlyCalculatingSemanticsConfiguration) return

        if (nodes.isUpdating || applyingModifierOnAttach) {
            // We are currently updating the modifier, so just schedule an invalidation. After
            // applying the modifier, we will notify listeners of semantics changes.
            isSemanticsInvalidated = true
        } else {
            // We are not currently updating the modifier, so instead of scheduling invalidation,
            // we update the semantics configuration and send the notification event right away.
            val prev = _semanticsConfiguration
            _semanticsConfiguration = calculateSemanticsConfiguration()
            isSemanticsInvalidated = false

            val owner = requireOwner()
            owner.semanticsOwner.notifySemanticsChange(this, prev)

            // This is needed for Accessibility and ContentCapture. Remove after these systems
            // are migrated to use SemanticsInfo and SemanticListeners.
            owner.onSemanticsChange()
        }
    }

    // This is needed until we completely move to the new world where we always pre-compute the
    // semantics configuration. At that point, this can just be a property with a private setter.
    private var _semanticsConfiguration: SemanticsConfiguration? = null
    override val semanticsConfiguration: SemanticsConfiguration?
        get() {
            // TODO: investigate if there's a better way to approach "half attached" state and
            // whether or not deactivated nodes should be considered removed or not.
            if (!isAttached || isDeactivated || !nodes.has(Nodes.Semantics)) return null

            return _semanticsConfiguration
        }

    private var isCurrentlyCalculatingSemanticsConfiguration = false

    private fun calculateSemanticsConfiguration(): SemanticsConfiguration {
        // Ignore calls to invalidate Semantics while semantics are being calculated.
        isCurrentlyCalculatingSemanticsConfiguration = true

        var config = SemanticsConfiguration()
        requireOwner().snapshotObserver.observeSemanticsReads(this) {
            nodes.tailToHead(Nodes.Semantics) {
                if (it.shouldClearDescendantSemantics) {
                    config = SemanticsConfiguration()
                    config.isClearingSemantics = true
                }
                if (it.shouldMergeDescendantSemantics) {
                    config.isMergingSemanticsOfDescendants = true
                }
                with(it) { config.applySemantics() }
            }
        }

        isCurrentlyCalculatingSemanticsConfiguration = false

        return config
    }

    /**
     * Set the [Owner] of this LayoutNode. This LayoutNode must not already be attached. [owner]
     * must match its [parent].[owner].
     */
    internal fun attach(owner: Owner) {
        checkPrecondition(this.owner == null) {
            "Cannot attach $this as it already is attached.  Tree: " + debugTreeToString()
        }
        checkPrecondition(_foldedParent == null || _foldedParent?.owner == owner) {
            "Attaching to a different owner($owner) than the parent's owner(${parent?.owner})." +
                " This tree: " +
                debugTreeToString() +
                " Parent tree: " +
                _foldedParent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent == null) {
            measurePassDelegate.isPlaced = true
            // regular nodes go through markNodeAndSubtreeAsPlaced(), from where we call this
            // function on rectManager. as root marked as placed here, we need to call it.
            owner.rectManager.recalculateRectIfDirty(this)
            lookaheadPassDelegate?.onAttachedToNullParent()
        }

        // Use the inner coordinator of first non-virtual parent
        outerCoordinator.wrappedBy = parent?.innerCoordinator

        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1

        pendingModifier?.let { applyModifier(it) }
        pendingModifier = null

        owner.onPreAttach(this)

        // Update lookahead root when attached. For nested cases, we'll always use the
        // closest lookahead root
        if (isVirtualLookaheadRoot) {
            lookaheadRoot = this
        } else {
            // Favor lookahead root from parent than locally created scope, unless current node
            // is a virtual lookahead root
            lookaheadRoot = _foldedParent?.lookaheadRoot ?: lookaheadRoot
            if (lookaheadRoot == null && nodes.has(Nodes.ApproachMeasure)) {
                // This could happen when movableContent containing intermediateLayout is moved
                lookaheadRoot = this
            }
        }
        if (!isDeactivated) {
            nodes.markAsAttached()
        }
        _foldedChildren.forEach { child -> child.attach(owner) }
        if (!isDeactivated) {
            nodes.runAttachLifecycle()
        }

        invalidateMeasurements()
        parent?.invalidateMeasurements()

        onAttach?.invoke(owner)

        layoutDelegate.updateParentData()

        if (!isDeactivated && nodes.has(Nodes.Semantics)) {
            invalidateSemantics()
        }

        owner.onPostAttach(this)
    }

    /**
     * Remove the LayoutNode from the [Owner]. The [owner] must not be `null` before this call and
     * its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    internal fun detach() {
        val owner = owner
        checkPreconditionNotNull(owner) {
            "Cannot detach node that is already detached!  Tree: " + parent?.debugTreeToString()
        }
        val parent = this.parent
        if (parent != null) {
            parent.invalidateLayer()
            parent.invalidateMeasurements()
            measurePassDelegate.measuredByParent = UsageByParent.NotUsed
            lookaheadPassDelegate?.let { it.measuredByParent = UsageByParent.NotUsed }
        }
        layoutDelegate.resetAlignmentLines()

        forEachCoordinatorIncludingInner { it.onLayoutNodeDetach() }
        onDetach?.invoke(owner)

        nodes.runDetachLifecycle()
        ignoreRemeasureRequests { _foldedChildren.forEach { child -> child.detach() } }
        nodes.markAsDetached()
        owner.onDetach(this)
        owner.rectManager.remove(this)
        this.owner = null

        lookaheadRoot = null
        depth = 0
        measurePassDelegate.onNodeDetached()
        lookaheadPassDelegate?.onNodeDetached()

        // Note: Don't call invalidateSemantics() from within detach() because the modifier nodes
        // are detached before the LayoutNode, and invalidateSemantics() can trigger a call to
        // calculateSemanticsConfiguration() which will encounter unattached nodes. Instead, just
        // set the semantics configuration to null over here since we know the node is detached.
        if (nodes.has(Nodes.Semantics)) {
            val prev = _semanticsConfiguration
            _semanticsConfiguration = null
            isSemanticsInvalidated = false
            owner.semanticsOwner.notifySemanticsChange(this, prev)

            // This is needed for Accessibility and ContentCapture. Remove after these systems
            // are migrated to use SemanticsInfo and SemanticListeners.
            owner.onSemanticsChange()
        }
    }

    private val _zSortedChildren = mutableVectorOf<LayoutNode>()
    private var zSortedChildrenInvalidated = true

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex] first (smaller first) and the
     * order they were placed via [Placeable.placeAt] by parent (smaller first). Please note that
     * this list contains not placed items as well, so you have to manually filter them.
     *
     * Note that the object is reused so you shouldn't save it for later.
     */
    @PublishedApi
    internal val zSortedChildren: MutableVector<LayoutNode>
        get() {
            if (zSortedChildrenInvalidated) {
                _zSortedChildren.clear()
                _zSortedChildren.addAll(_children)
                _zSortedChildren.sortWith(ZComparator)
                zSortedChildrenInvalidated = false
            }
            return _zSortedChildren
        }

    override val isValidOwnerScope: Boolean
        get() = isAttached

    override fun toString(): String {
        return "${simpleIdentityToString(this, null)} children: ${children.size} " +
            "measurePolicy: $measurePolicy deactivated: $isDeactivated"
    }

    internal val hasFixedInnerContentConstraints: Boolean
        get() {
            // it is the constraints we have after all the modifiers applied on this node,
            // the one to be passed into user provided [measurePolicy.measure]. if those
            // constraints are fixed this means the children size changes can't affect
            // this LayoutNode size.
            val innerContentConstraints = innerCoordinator.lastMeasurementConstraints
            return innerContentConstraints.hasFixedWidth && innerContentConstraints.hasFixedHeight
        }

    /** Call this method from the debugger to see a dump of the LayoutNode tree structure */
    @Suppress("unused")
    private fun debugTreeToString(depth: Int = 0): String {
        val tree = StringBuilder()
        for (i in 0 until depth) {
            tree.append("  ")
        }
        tree.append("|-")
        tree.append(toString())
        tree.append('\n')

        forEachChild { child -> tree.append(child.debugTreeToString(depth + 1)) }

        var treeString = tree.toString()
        if (depth == 0) {
            // Delete trailing newline
            treeString = treeString.substring(0, treeString.length - 1)
        }

        return treeString
    }

    internal abstract class NoIntrinsicsMeasurePolicy(private val error: String) : MeasurePolicy {
        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int,
        ) = error(error)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int,
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int,
        ) = error(error)

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int,
        ) = error(error)
    }

    /** Blocks that define the measurement and intrinsic measurement of the layout. */
    override var measurePolicy: MeasurePolicy = ErrorMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                intrinsicsPolicy?.updateFrom(measurePolicy)
                invalidateMeasurements()
            }
        }

    /**
     * The intrinsic measurements of this layout, backed up by states to trigger correct
     * remeasurement for layouts using the intrinsics of this layout when the [measurePolicy] is
     * changing.
     */
    private var intrinsicsPolicy: IntrinsicsPolicy? = null

    private fun getOrCreateIntrinsicsPolicy(): IntrinsicsPolicy {
        return intrinsicsPolicy
            ?: IntrinsicsPolicy(this, measurePolicy).also { intrinsicsPolicy = it }
    }

    fun minLookaheadIntrinsicWidth(height: Int) =
        getOrCreateIntrinsicsPolicy().minLookaheadIntrinsicWidth(height)

    fun minLookaheadIntrinsicHeight(width: Int) =
        getOrCreateIntrinsicsPolicy().minLookaheadIntrinsicHeight(width)

    fun maxLookaheadIntrinsicWidth(height: Int) =
        getOrCreateIntrinsicsPolicy().maxLookaheadIntrinsicWidth(height)

    fun maxLookaheadIntrinsicHeight(width: Int) =
        getOrCreateIntrinsicsPolicy().maxLookaheadIntrinsicHeight(width)

    fun minIntrinsicWidth(height: Int) = getOrCreateIntrinsicsPolicy().minIntrinsicWidth(height)

    fun minIntrinsicHeight(width: Int) = getOrCreateIntrinsicsPolicy().minIntrinsicHeight(width)

    fun maxIntrinsicWidth(height: Int) = getOrCreateIntrinsicsPolicy().maxIntrinsicWidth(height)

    fun maxIntrinsicHeight(width: Int) = getOrCreateIntrinsicsPolicy().maxIntrinsicHeight(width)

    /** The screen density to be used by this layout. */
    override var density: Density = DefaultDensity
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()

                nodes.headToTail { it.onDensityChange() }
            }
        }

    /** The layout direction of the layout node. */
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
        set(value) {
            if (field != value) {
                field = value
                onDensityOrLayoutDirectionChanged()

                nodes.headToTail { it.onLayoutDirectionChange() }
            }
        }

    override var viewConfiguration: ViewConfiguration = DummyViewConfiguration
        set(value) {
            if (field != value) {
                field = value

                nodes.headToTail(type = PointerInput) { it.onViewConfigurationChange() }
            }
        }

    override var compositionLocalMap = CompositionLocalMap.Empty
        set(value) {
            field = value
            density = value[LocalDensity]
            layoutDirection = value[LocalLayoutDirection]
            viewConfiguration = value[LocalViewConfiguration]
            nodes.headToTail(Nodes.CompositionLocalConsumer) { modifierNode ->
                val delegatedNode = modifierNode.node
                if (delegatedNode.isAttached) {
                    autoInvalidateUpdatedNode(delegatedNode)
                } else {
                    delegatedNode.updatedNodeAwaitingAttachForInvalidation = true
                }
            }
        }

    private val traceContext: CompositionErrorContext?
        get() = compositionLocalMap[LocalCompositionErrorContext]

    fun rethrowWithComposeStackTrace(e: Throwable): Nothing =
        throw e.also { traceContext?.apply { e.attachComposeStackTrace(this@LayoutNode) } }

    private fun onDensityOrLayoutDirectionChanged() {
        // TODO(b/242120396): it seems like we need to update some densities in the node
        // coordinators here
        // measure/layout modifiers on the node
        invalidateMeasurements()
        // draw modifiers on the node
        parent?.invalidateLayer() ?: owner?.invalidateRootLayer()
        // and draw modifiers after graphics layers on the node
        invalidateLayers()
    }

    /** The measured width of this layout and all of its [modifier]s. Shortcut for `size.width`. */
    override val width: Int
        get() = layoutDelegate.width

    /**
     * The measured height of this layout and all of its [modifier]s. Shortcut for `size.height`.
     */
    override val height: Int
        get() = layoutDelegate.height

    internal val alignmentLinesRequired: Boolean
        get() =
            layoutDelegate.run {
                alignmentLinesOwner.alignmentLines.required ||
                    lookaheadAlignmentLinesOwner?.alignmentLines?.required == true
            }

    internal val mDrawScope: LayoutNodeDrawScope
        get() = requireOwner().sharedDrawScope

    /**
     * Whether or not this [LayoutNode] and all of its parents have been placed in the hierarchy.
     */
    override val isPlaced: Boolean
        get() = measurePassDelegate.isPlaced

    /**
     * Whether or not this [LayoutNode] was placed by its parent. The node can still be considered
     * not placed if some of the modifiers on it not placed the placeable.
     */
    val isPlacedByParent: Boolean
        get() = measurePassDelegate.isPlacedByParent

    /**
     * The order in which this node was placed by its parent during the previous `layoutChildren`.
     * Before the placement the order is set to [NotPlacedPlaceOrder] to all the children. Then
     * every placed node assigns this variable to [parent]s MeasurePassDelegate's
     * nextChildPlaceOrder and increments this counter. Not placed items will still have
     * [NotPlacedPlaceOrder] set.
     */
    internal val placeOrder: Int
        get() = measurePassDelegate.placeOrder

    /** Remembers how the node was measured by the parent. */
    internal val measuredByParent: UsageByParent
        get() = measurePassDelegate.measuredByParent

    /** Remembers how the node was measured by the parent in lookahead. */
    internal val measuredByParentInLookahead: UsageByParent
        get() = lookaheadPassDelegate?.measuredByParent ?: UsageByParent.NotUsed

    /** Remembers how the node was measured using intrinsics by an ancestor. */
    internal var intrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    /**
     * We must cache a previous value of [intrinsicsUsageByParent] because measurement is sometimes
     * skipped. When it is skipped, the subtree must be restored to this value.
     */
    private var previousIntrinsicsUsageByParent: UsageByParent = UsageByParent.NotUsed

    @Deprecated("Temporary API to support ConstraintLayout prototyping.")
    internal var canMultiMeasure: Boolean = false

    internal val nodes = NodeChain(this)
    internal val innerCoordinator: NodeCoordinator
        get() = nodes.innerCoordinator

    internal val layoutDelegate = LayoutNodeLayoutDelegate(this)
    internal val outerCoordinator: NodeCoordinator
        get() = nodes.outerCoordinator

    /**
     * zIndex defines the drawing order of the LayoutNode. Children with larger zIndex are drawn on
     * top of others (the original order is used for the nodes with the same zIndex). Default zIndex
     * is 0. We use sum of the values passed as zIndex to place() by the parent layout and all the
     * applied modifiers.
     */
    private val zIndex: Float
        get() = measurePassDelegate.zIndex

    /** The inner state associated with [androidx.compose.ui.layout.SubcomposeLayout]. */
    internal var subcompositionsState: LayoutNodeSubcompositionsState? = null

    /** The inner-most layer coordinator. Used for performance for NodeCoordinator.findLayer(). */
    private var _innerLayerCoordinator: NodeCoordinator? = null
    internal var innerLayerCoordinatorIsDirty = true
    internal val innerLayerCoordinator: NodeCoordinator?
        get() {
            if (innerLayerCoordinatorIsDirty) {
                var coordinator: NodeCoordinator? = innerCoordinator
                val final = outerCoordinator.wrappedBy
                _innerLayerCoordinator = null
                while (coordinator != final) {
                    if (coordinator?.layer != null) {
                        _innerLayerCoordinator = coordinator
                        break
                    }
                    coordinator = coordinator?.wrappedBy
                }
                innerLayerCoordinatorIsDirty = false
            }
            val layerCoordinator = _innerLayerCoordinator
            if (layerCoordinator != null) {
                checkPreconditionNotNull(layerCoordinator.layer) { "layer was not set" }
            }
            return layerCoordinator
        }

    /**
     * Invalidates the inner-most layer as part of this LayoutNode or from the containing
     * LayoutNode. This is added for performance so that NodeCoordinator.invalidateLayer() can be
     * faster.
     */
    internal fun invalidateLayer() {
        val innerLayerCoordinator = innerLayerCoordinator
        if (innerLayerCoordinator != null) {
            innerLayerCoordinator.invalidateLayer()
        } else {
            val parent = this.parent
            parent?.invalidateLayer() ?: owner?.invalidateRootLayer()
        }
    }

    private var _modifier: Modifier = Modifier
    private var pendingModifier: Modifier? = null
    internal val applyingModifierOnAttach
        get() = pendingModifier != null

    /** The [Modifier] currently applied to this node. */
    override var modifier: Modifier
        get() = _modifier
        set(value) {
            requirePrecondition(!isVirtual || modifier === Modifier) {
                "Modifiers are not supported on virtual LayoutNodes"
            }
            requirePrecondition(!isDeactivated) { "modifier is updated when deactivated" }
            if (isAttached) {
                applyModifier(value)
                if (isSemanticsInvalidated) {
                    invalidateSemantics()
                }
            } else {
                pendingModifier = value
            }
        }

    private fun applyModifier(modifier: Modifier) {
        val hadPointerInput = nodes.has(Nodes.PointerInput)
        val hadFocusTarget = nodes.has(Nodes.FocusTarget)
        _modifier = modifier
        nodes.updateFrom(modifier)
        val hasPointerInput = nodes.has(Nodes.PointerInput)
        val hasFocusTarget = nodes.has(Nodes.FocusTarget)
        layoutDelegate.updateParentData()
        if (lookaheadRoot == null && nodes.has(Nodes.ApproachMeasure)) {
            lookaheadRoot = this
        }

        if (hadPointerInput != hasPointerInput || hadFocusTarget != hasFocusTarget) {
            requireOwner().rectManager.updateFlagsFor(this, hasFocusTarget, hasPointerInput)
        }
    }

    private fun resetModifierState() {
        nodes.resetState()
    }

    internal fun invalidateParentData() {
        layoutDelegate.invalidateParentData()
    }

    /**
     * Coordinates of just the contents of the [LayoutNode], after being affected by all modifiers.
     */
    override val coordinates: LayoutCoordinates
        get() = innerCoordinator

    /** Callback to be executed whenever the [LayoutNode] is attached to a new [Owner]. */
    internal var onAttach: ((Owner) -> Unit)? = null

    /** Callback to be executed whenever the [LayoutNode] is detached from an [Owner]. */
    internal var onDetach: ((Owner) -> Unit)? = null

    /**
     * Flag used by [OnPositionedDispatcher] to identify LayoutNodes that have already had their
     * [OnGloballyPositionedModifier]'s dispatch called so that they aren't called multiple times.
     */
    internal var needsOnGloballyPositionedDispatch = false

    /**
     * Count of attached [GlobalPositionAwareModifierNode] modifiers or children having such
     * modifiers in their subtree.
     */
    var globallyPositionedObservers: Int = 0
        set(value) {
            if (field != value) {
                if (value > 0 && field == 0) {
                    parent?.globallyPositionedObservers++
                }
                if (value == 0 && field > 0) {
                    parent?.globallyPositionedObservers--
                }
                field = value
            }
        }

    internal fun place(x: Int, y: Int) {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        with(parent?.innerCoordinator?.placementScope ?: requireOwner().placementScope) {
            measurePassDelegate.placeRelative(x, y)
        }
    }

    /** Place this layout node again on the same position it was placed last time */
    internal fun replace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        measurePassDelegate.replace()
    }

    internal fun lookaheadReplace() {
        if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
            // This LayoutNode may have asked children for intrinsics. If so, we should
            // clear the intrinsics usage for everything that was requested previously.
            clearSubtreePlacementIntrinsicsUsage()
        }
        lookaheadPassDelegate!!.replace()
    }

    internal fun draw(canvas: Canvas, graphicsLayer: GraphicsLayer?) =
        withComposeStackTrace(this) { outerCoordinator.draw(canvas, graphicsLayer) }

    /**
     * Carries out a hit test on the [PointerInputModifier]s associated with this [LayoutNode] and
     * all [PointerInputModifier]s on all descendant [LayoutNode]s.
     *
     * If [pointerPosition] is within the bounds of any tested [PointerInputModifier]s, the
     * [PointerInputModifier] is added to [hitTestResult] and true is returned.
     *
     * @param pointerPosition The tested pointer position, which is relative to the LayoutNode.
     * @param hitTestResult The collection that the hit [PointerInputFilter]s will be added to if
     *   hit.
     */
    internal fun hitTest(
        pointerPosition: Offset,
        hitTestResult: HitTestResult,
        pointerType: PointerType = PointerType.Unknown,
        isInLayer: Boolean = true,
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.PointerInputSource,
            positionInWrapped,
            hitTestResult,
            pointerType,
            isInLayer,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun hitTestSemantics(
        pointerPosition: Offset,
        hitSemanticsEntities: HitTestResult,
        pointerType: PointerType = PointerType.Touch,
        isInLayer: Boolean = true,
    ) {
        val positionInWrapped = outerCoordinator.fromParentPosition(pointerPosition)
        outerCoordinator.hitTest(
            NodeCoordinator.SemanticsSource,
            positionInWrapped,
            hitSemanticsEntities,
            pointerType = PointerType.Touch,
            isInLayer = isInLayer,
        )
    }

    internal fun rescheduleRemeasureOrRelayout(it: LayoutNode) {
        when (it.layoutState) {
            Idle -> {
                // this node was scheduled for remeasure or relayout while it was not
                // placed. such requests are ignored for non-placed nodes so we have to
                // re-schedule remeasure or relayout.
                if (it.lookaheadMeasurePending) {
                    it.requestLookaheadRemeasure(forceRequest = true)
                } else {
                    if (it.lookaheadLayoutPending) {
                        it.requestLookaheadRelayout(forceRequest = true)
                    }
                    if (it.measurePending) {
                        it.requestRemeasure(forceRequest = true)
                    } else if (it.layoutPending) {
                        it.requestRelayout(forceRequest = true)
                    }
                }
            }
            else -> throw IllegalStateException("Unexpected state ${it.layoutState}")
        }
    }

    /** Used to request a new measurement + layout pass from the owner. */
    internal fun requestRemeasure(
        forceRequest: Boolean = false,
        scheduleMeasureAndLayout: Boolean = true,
        invalidateIntrinsics: Boolean = true,
    ) {
        if (!ignoreRemeasureRequests && !isVirtual) {
            val owner = owner ?: return
            owner.onRequestMeasure(
                layoutNode = this,
                forceRequest = forceRequest,
                scheduleMeasureAndLayout = scheduleMeasureAndLayout,
            )
            if (invalidateIntrinsics) {
                measurePassDelegate.invalidateIntrinsicsParent(forceRequest)
            }
        }
    }

    /**
     * Used to request a new lookahead measurement, lookahead layout, and subsequently measure and
     * layout from the owner.
     */
    internal fun requestLookaheadRemeasure(
        forceRequest: Boolean = false,
        scheduleMeasureAndLayout: Boolean = true,
        invalidateIntrinsics: Boolean = true,
    ) {
        checkPrecondition(lookaheadRoot != null) {
            "Lookahead measure cannot be requested on a node that is not a part of the " +
                "LookaheadScope"
        }
        val owner = owner ?: return
        if (!ignoreRemeasureRequests && !isVirtual) {
            owner.onRequestMeasure(
                layoutNode = this,
                affectsLookahead = true,
                forceRequest = forceRequest,
                scheduleMeasureAndLayout = scheduleMeasureAndLayout,
            )
            if (invalidateIntrinsics) {
                lookaheadPassDelegate!!.invalidateIntrinsicsParent(forceRequest)
            }
        }
    }

    /**
     * This gets called when both lookahead measurement (if in a LookaheadScope) and actual
     * measurement need to be re-done. Such events include modifier change, attach/detach, etc.
     */
    internal fun invalidateMeasurements() {
        if (isVirtual) {
            // If the node is virtual, we need to invalidate the parent node (as it is non-virtual)
            // instead so that children get properly invalidated.
            parent?.invalidateMeasurements()
            return
        }
        if (lookaheadRoot != null) {
            requestLookaheadRemeasure()
        } else {
            requestRemeasure()
        }
    }

    internal fun invalidateOnPositioned() {
        // If we've already scheduled a measure, the positioned callbacks will get called anyway
        if (
            globallyPositionedObservers == 0 ||
                layoutPending ||
                measurePending ||
                needsOnGloballyPositionedDispatch
        )
            return
        requireOwner().requestOnPositionedCallback(this)
    }

    internal fun onCoordinatorRectChanged(coordinator: NodeCoordinator) {
        val rectManager = owner?.rectManager
        val placementPending = layoutState != Idle || measurePending || layoutPending
        if (addedToRectList && rectManager != null) {
            if (coordinator === outerCoordinator) {
                // transformations on the outer coordinator update the offset from parent
                rectInParentDirty = true
                if (!placementPending) {
                    // during placement we get it called right after
                    rectManager.recalculateRectIfDirty(this)
                }
            } else {
                // transformations on other coordinators invalidate outerToInnerOffset
                // and offset from parent for each child
                outerToInnerOffsetDirty = true
                forEachChild {
                    it.rectInParentDirty = true
                    // during placement it is guaranteed to get recalculateRectIfDirty() call on
                    // each child after the parent finish its placement. we don't want to call it
                    // straight away, as there are might be multiple changes on the same layout
                    // node, and we want to apply them once in batch.
                    if (!placementPending) {
                        rectManager.recalculateRectIfDirty(it)
                    }
                }

                // Since there has been an update to a coordinator somewhere in the
                // modifier chain of this layout node, we might have onRectChanged
                // callbacks that need to be notified of that change. As a result, even
                // if the outer rect of this layout node hasn't changed, we want to
                // invalidate the callbacks for them
                rectManager.invalidateCallbacksFor(this)
            }
        }

        layoutDelegate.measurePassDelegate.requestLayoutIfCoordinatesAreUsedAndNotifyChildren()
    }

    internal inline fun <T> ignoreRemeasureRequests(block: () -> T): T {
        ignoreRemeasureRequests = true
        val result = block()
        ignoreRemeasureRequests = false
        return result
    }

    /** Used to request a new layout pass from the owner. */
    internal fun requestRelayout(forceRequest: Boolean = false) {
        if (!isVirtual) {
            owner?.onRequestRelayout(this, forceRequest = forceRequest)
        }
    }

    internal fun requestLookaheadRelayout(forceRequest: Boolean = false) {
        if (!isVirtual) {
            owner?.onRequestRelayout(this, affectsLookahead = true, forceRequest)
        }
    }

    internal fun dispatchOnPositionedCallbacks() {
        if (layoutState != Idle || layoutPending || measurePending || isDeactivated) {
            return // it hasn't yet been properly positioned, so don't make a call
        }
        if (!isPlaced) {
            return // it hasn't been placed, so don't make a call
        }
        nodes.headToTail(Nodes.GlobalPositionAware) {
            it.onGloballyPositioned(it.requireCoordinator(Nodes.GlobalPositionAware))
        }
    }

    /**
     * This returns a new List of Modifiers and the coordinates and any extra information that may
     * be useful. This is used for tooling to retrieve layout modifier and layer information.
     */
    override fun getModifierInfo(): List<ModifierInfo> = nodes.getModifierInfo()

    /** Invalidates layers defined on this LayoutNode. */
    internal fun invalidateLayers() {
        forEachCoordinator { coordinator -> coordinator.layer?.invalidate() }
        innerCoordinator.layer?.invalidate()
    }

    internal fun lookaheadRemeasure(
        constraints: Constraints? = layoutDelegate.lastLookaheadConstraints
    ): Boolean {
        // Only lookahead remeasure when the constraints are valid and the node is in
        // a LookaheadScope (by checking whether the lookaheadScope is set)
        return if (constraints != null && lookaheadRoot != null) {
            lookaheadPassDelegate!!.remeasure(constraints)
        } else {
            false
        }
    }

    /** Return true if the measured size has been changed */
    internal fun remeasure(constraints: Constraints? = layoutDelegate.lastConstraints): Boolean {
        return if (constraints != null) {
            if (intrinsicsUsageByParent == UsageByParent.NotUsed) {
                // This LayoutNode may have asked children for intrinsics. If so, we should
                // clear the intrinsics usage for everything that was requested previously.
                clearSubtreeIntrinsicsUsage()
            }
            measurePassDelegate.remeasure(constraints)
        } else {
            false
        }
    }

    /**
     * Tracks whether another measure pass is needed for the LayoutNode. Mutation to
     * [measurePending] is confined to LayoutNodeLayoutDelegate. It can only be set true from
     * outside of LayoutNode via [markMeasurePending]. It is cleared (i.e. set false) during the
     * measure pass ( i.e. in [LayoutNodeLayoutDelegate.performMeasure]).
     */
    internal val measurePending: Boolean
        get() = layoutDelegate.measurePending

    /**
     * Tracks whether another layout pass is needed for the LayoutNode. Mutation to [layoutPending]
     * is confined to LayoutNode. It can only be set true from outside of LayoutNode via
     * [markLayoutPending]. It is cleared (i.e. set false) during the layout pass (i.e. in
     * layoutChildren).
     */
    internal val layoutPending: Boolean
        get() = layoutDelegate.layoutPending

    internal val lookaheadMeasurePending: Boolean
        get() = layoutDelegate.lookaheadMeasurePending

    internal val lookaheadLayoutPending: Boolean
        get() = layoutDelegate.lookaheadLayoutPending

    /** Marks the layoutNode dirty for another layout pass. */
    internal fun markLayoutPending() = layoutDelegate.markLayoutPending()

    /** Marks the layoutNode dirty for another measure pass. */
    internal fun markMeasurePending() = layoutDelegate.markMeasurePending()

    /** Marks the layoutNode dirty for another lookahead layout pass. */
    internal fun markLookaheadLayoutPending() = layoutDelegate.markLookaheadLayoutPending()

    fun invalidateSubtree(isRootOfInvalidation: Boolean = true) {
        if (isRootOfInvalidation) {
            parent?.invalidateLayer() ?: owner?.invalidateRootLayer()
        }
        invalidateSemantics()
        requestRemeasure()
        nodes.headToTail(Nodes.Layout) { it.requireCoordinator(Nodes.Layout).layer?.invalidate() }
        // TODO: invalidate parent data
        _children.forEach { it.invalidateSubtree(false) }
    }

    fun invalidateMeasurementForSubtree() {
        requestRemeasure()
        _children.forEach { it.invalidateMeasurementForSubtree() }
    }

    fun invalidateDrawForSubtree(isRootOfInvalidation: Boolean = true) {
        if (isRootOfInvalidation) {
            parent?.invalidateLayer() ?: owner?.invalidateRootLayer()
        }
        nodes.headToTail(Nodes.Layout) { it.requireCoordinator(Nodes.Layout).layer?.invalidate() }
        _children.forEach { it.invalidateDrawForSubtree(false) }
    }

    /** Marks the layoutNode dirty for another lookahead measure pass. */
    internal fun markLookaheadMeasurePending() = layoutDelegate.markLookaheadMeasurePending()

    override fun forceRemeasure() {
        // we do not schedule measure and layout as we are going to call it manually right after
        if (lookaheadRoot != null) {
            requestLookaheadRemeasure(scheduleMeasureAndLayout = false)
        } else {
            requestRemeasure(scheduleMeasureAndLayout = false)
        }
        val lastConstraints = layoutDelegate.lastConstraints
        if (lastConstraints != null) {
            owner?.measureAndLayout(this, lastConstraints)
        } else {
            owner?.measureAndLayout()
        }
    }

    override fun onLayoutComplete() {
        innerCoordinator.visitNodes(Nodes.OnPlaced) { it.onPlaced(innerCoordinator) }
    }

    /** Calls [block] on all [LayoutModifierNodeCoordinator]s in the NodeCoordinator chain. */
    internal inline fun forEachCoordinator(block: (LayoutModifierNodeCoordinator) -> Unit) {
        var coordinator: NodeCoordinator? = outerCoordinator
        val inner = innerCoordinator
        while (coordinator !== inner) {
            block(coordinator as LayoutModifierNodeCoordinator)
            coordinator = coordinator.wrapped
        }
    }

    /** Calls [block] on all [NodeCoordinator]s in the NodeCoordinator chain. */
    internal inline fun forEachCoordinatorIncludingInner(block: (NodeCoordinator) -> Unit) {
        var delegate: NodeCoordinator? = outerCoordinator
        val final = innerCoordinator.wrapped
        while (delegate != final && delegate != null) {
            block(delegate)
            delegate = delegate.wrapped
        }
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this LayoutNode's measurement
     * used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request all
     * of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure() call is normally done
     * after the intrinsics is requested and we don't want to clear the usage at that point.
     */
    internal fun clearSubtreeIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.clearSubtreeIntrinsicsUsage()
            }
        }
    }

    /**
     * Walks the subtree and clears all [intrinsicsUsageByParent] that this LayoutNode's layout
     * block used intrinsics on.
     *
     * The layout that asks for intrinsics of its children is the node to call this to request all
     * of its subtree to be cleared.
     *
     * We can't do clearing as part of measure() because the child's measure() call is normally done
     * after the intrinsics is requested and we don't want to clear the usage at that point.
     */
    private fun clearSubtreePlacementIntrinsicsUsage() {
        // save the usage in case we short-circuit the measure call
        previousIntrinsicsUsageByParent = intrinsicsUsageByParent
        intrinsicsUsageByParent = UsageByParent.NotUsed
        forEachChild {
            if (it.intrinsicsUsageByParent == UsageByParent.InLayoutBlock) {
                it.clearSubtreePlacementIntrinsicsUsage()
            }
        }
    }

    /**
     * For a subtree that skips measurement, this resets the [intrinsicsUsageByParent] to what it
     * was prior to [clearSubtreeIntrinsicsUsage].
     */
    internal fun resetSubtreeIntrinsicsUsage() {
        forEachChild {
            it.intrinsicsUsageByParent = it.previousIntrinsicsUsageByParent
            if (it.intrinsicsUsageByParent != UsageByParent.NotUsed) {
                it.resetSubtreeIntrinsicsUsage()
            }
        }
    }

    override val parentInfo: SemanticsInfo?
        get() = parent

    override val childrenInfo: List<SemanticsInfo>
        get() = children

    override var isDeactivated = false
        private set

    override fun onReuse() {
        requirePrecondition(isAttached) { "onReuse is only expected on attached node" }
        interopViewFactoryHolder?.onReuse()
        subcompositionsState?.onReuse()
        isCurrentlyCalculatingSemanticsConfiguration = false
        if (isDeactivated) {
            isDeactivated = false
            // we don't need to reset state as it was done when deactivated
        } else {
            resetModifierState()
        }
        val oldSemanticsId = semanticsId
        // semanticsId is used as the identity. we need to remove from rectlist before changing it
        owner?.rectManager?.remove(this)
        semanticsId = generateSemanticsId()
        owner?.onPreLayoutNodeReused(this, oldSemanticsId)
        // resetModifierState detaches all nodes, so we need to re-attach them upon reuse.
        nodes.markAsAttached()
        nodes.runAttachLifecycle()
        if (nodes.has(Nodes.Semantics)) {
            invalidateSemantics()
        }
        rescheduleRemeasureOrRelayout(this)
        owner?.onPostLayoutNodeReused(this, oldSemanticsId)
        // Sometimes, while scrolling with reuse, a child LayoutNode, might not
        // require measure or layout at all, but at a minimum we need to update RectManager with
        // the correct information.
        owner?.rectManager?.recalculateRectIfDirty(this)
    }

    override fun onDeactivate() {
        interopViewFactoryHolder?.onDeactivate()
        subcompositionsState?.onDeactivate()
        isDeactivated = true
        resetModifierState()
        // if the node is detached the semantics were already updated without this node.
        if (isAttached) {
            _semanticsConfiguration = null
            isSemanticsInvalidated = false
        }
        owner?.onLayoutNodeDeactivated(this)
    }

    override fun onRelease() {
        interopViewFactoryHolder?.onRelease()
        subcompositionsState?.onRelease()
        forEachCoordinatorIncludingInner { it.onRelease() }
    }

    internal companion object {
        private val ErrorMeasurePolicy: NoIntrinsicsMeasurePolicy =
            object :
                NoIntrinsicsMeasurePolicy(error = "Undefined intrinsics block and it is required") {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ) = error("Undefined measure and it is required")
            }

        /** Constant used by [placeOrder]. */
        @Suppress("ConstPropertyName") internal const val NotPlacedPlaceOrder = Int.MAX_VALUE

        /** Pre-allocated constructor to be used with ComposeNode */
        internal val Constructor: () -> LayoutNode = { LayoutNode() }

        /**
         * All of these values are only used in tests. The real ViewConfiguration should be set in
         * Layout()
         */
        internal val DummyViewConfiguration =
            object : ViewConfiguration {
                override val longPressTimeoutMillis: Long
                    get() = 400L

                override val doubleTapTimeoutMillis: Long
                    get() = 300L

                override val doubleTapMinTimeMillis: Long
                    get() = 40L

                override val touchSlop: Float
                    get() = 16f

                override val minimumTouchTargetSize: DpSize
                    get() = DpSize.Zero
            }

        /** Comparator allowing to sort nodes by zIndex and placement order. */
        internal val ZComparator =
            Comparator<LayoutNode> { node1, node2 ->
                if (node1.zIndex == node2.zIndex) {
                    // if zIndex is the same we use the placement order
                    node1.placeOrder.compareTo(node2.placeOrder)
                } else {
                    node1.zIndex.compareTo(node2.zIndex)
                }
            }
    }

    /**
     * Describes the current state the [LayoutNode] is in. A [LayoutNode] is expected to be in
     * [LookaheadMeasuring] first, followed by [LookaheadLayingOut] if it is in a LookaheadScope.
     * After the lookahead is finished, [Measuring] and then [LayingOut] will happen as needed.
     */
    internal enum class LayoutState {
        /** Node is currently being measured. */
        Measuring,

        /** Node is being measured in lookahead. */
        LookaheadMeasuring,

        /** Node is currently being laid out. */
        LayingOut,

        /** Node is being laid out in lookahead. */
        LookaheadLayingOut,

        /**
         * Node is not currently measuring or laying out. It could be pending measure or pending
         * layout depending on the [measurePending] and [layoutPending] flags.
         */
        Idle,
    }

    internal enum class UsageByParent {
        InMeasureBlock,
        InLayoutBlock,
        NotUsed,
    }
}

internal inline fun <T> withComposeStackTrace(layoutNode: LayoutNode, block: () -> T): T =
    try {
        block()
    } catch (e: Throwable) {
        layoutNode.rethrowWithComposeStackTrace(e)
    }

/** Returns [LayoutNode.owner] or throws if it is null. */
internal fun LayoutNode.requireOwner(): Owner {
    val owner = owner
    checkPreconditionNotNull(owner) { "LayoutNode should be attached to an owner" }
    return owner
}

/**
 * Inserts a child [LayoutNode] at a last index. If this LayoutNode [LayoutNode.isAttached] then
 * [child] will become [LayoutNode.isAttached] also. [child] must have a `null` [LayoutNode.parent].
 */
internal fun LayoutNode.add(child: LayoutNode) {
    insertAt(children.size, child)
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/ModifierNodeElement.kt
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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.tryPopulateReflectively

/**
 * A [Modifier.Element] which manages an instance of a particular [Modifier.Node] implementation. A
 * given [Modifier.Node] implementation can only be used when a [ModifierNodeElement] which creates
 * and updates that implementation is applied to a Layout.
 *
 * A [ModifierNodeElement] should be very lightweight, and do little more than hold the information
 * necessary to create and maintain an instance of the associated [Modifier.Node] type.
 *
 * @sample androidx.compose.ui.samples.ModifierNodeElementSample
 * @sample androidx.compose.ui.samples.SemanticsModifierNodeSample
 * @see Modifier.Node
 * @see Modifier.Element
 */
abstract class ModifierNodeElement<N : Modifier.Node> : Modifier.Element, InspectableValue {

    private var _inspectorValues: InspectorInfo? = null
    private val inspectorValues: InspectorInfo
        get() =
            _inspectorValues
                ?: InspectorInfo()
                    .apply {
                        name = this@ModifierNodeElement::class.simpleName
                        inspectableProperties()
                    }
                    .also { _inspectorValues = it }

    final override val nameFallback: String?
        get() = inspectorValues.name

    final override val valueOverride: Any?
        get() = inspectorValues.value

    final override val inspectableElements: Sequence<ValueElement>
        get() = inspectorValues.properties

    /**
     * This will be called the first time the modifier is applied to the Layout and it should
     * construct and return the corresponding [Modifier.Node] instance.
     */
    abstract fun create(): N

    /**
     * Called when a modifier is applied to a Layout whose inputs have changed from the previous
     * application. This function will have the current node instance passed in as a parameter, and
     * it is expected that the node will be brought up to date.
     */
    abstract fun update(node: N)

    /**
     * Populates an [InspectorInfo] object with attributes to display in the layout inspector. This
     * is called by tooling to resolve the properties of this modifier. By convention, implementors
     * should set the [name][InspectorInfo.name] to the function name of the modifier.
     *
     * The default implementation will attempt to reflectively populate the inspector info with the
     * properties declared on the subclass. It will also set the [name][InspectorInfo.name] property
     * to the name of this instance's class by default (not the name of the modifier function).
     * Modifier property population depends on the kotlin-reflect library. If it is not in the
     * classpath at runtime, the default implementation of this function will populate the
     * properties with an error message.
     *
     * If you override this function and provide the properties you wish to display, you do not need
     * to call `super`. Doing so may result in duplicate properties appearing in the layout
     * inspector.
     */
    open fun InspectorInfo.inspectableProperties() {
        tryPopulateReflectively(this@ModifierNodeElement)
    }

    /**
     * Require hashCode() to be implemented. Using a data class is sufficient. Singletons and
     * modifiers with no parameters may implement this function by returning an arbitrary constant.
     */
    abstract override fun hashCode(): Int

    /**
     * Require equals() to be implemented. Using a data class is sufficient. Singletons may
     * implement this function with referential equality (`this === other`). Modifiers with no
     * inputs may implement this function by checking the type of the other object.
     */
    abstract override fun equals(other: Any?): Boolean
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/input/pointer/PointerInputEventProcessor.kt
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.input.pointer

import androidx.collection.LongSparseArray
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.util.fastForEach

internal interface PositionCalculator {
    fun screenToLocal(positionOnScreen: Offset): Offset

    fun localToScreen(localPosition: Offset): Offset
}

internal interface MatrixPositionCalculator : PositionCalculator {

    /**
     * Takes a matrix which transforms some coordinate system to local coordinates, and updates the
     * matrix to transform to screen coordinates instead.
     */
    fun localToScreen(localTransform: Matrix)
}

/** The core element that receives [PointerInputEvent]s and process them in Compose UI. */
internal class PointerInputEventProcessor(val root: LayoutNode) {

    private val hitPathTracker = HitPathTracker(root.coordinates)
    private val pointerInputChangeEventProducer = PointerInputChangeEventProducer()
    private val hitResult = HitTestResult()

    /**
     * [process] doesn't currently support reentrancy. This prevents reentrant calls from causing a
     * crash with an early exit.
     */
    private var isProcessing = false

    /**
     * Receives [PointerInputEvent]s and process them through the tree rooted on [root].
     *
     * @param pointerEvent The [PointerInputEvent] to process.
     * @return the result of processing.
     * @see ProcessResult
     * @see PointerInputEvent
     */
    fun process(
        @OptIn(InternalCoreApi::class) pointerEvent: PointerInputEvent,
        positionCalculator: PositionCalculator,
        isInBounds: Boolean = true,
    ): ProcessResult {
        if (isProcessing) {
            // Processing currently does not support reentrancy.
            return ProcessResult(
                dispatchedToAPointerInputModifier = false,
                anyMovementConsumed = false,
                anyChangeConsumed = false,
            )
        }
        try {
            isProcessing = true

            // Gets a new PointerInputChangeEvent with the PointerInputEvent.
            @OptIn(InternalCoreApi::class)
            val internalPointerEvent =
                pointerInputChangeEventProducer.produce(pointerEvent, positionCalculator)

            var isHover = true
            for (i in 0 until internalPointerEvent.changes.size()) {
                val pointerInputChange = internalPointerEvent.changes.valueAt(i)
                if (pointerInputChange.pressed || pointerInputChange.previousPressed) {
                    isHover = false
                    break
                }
            }

            // Add new hit paths to the tracker due to down events.
            for (i in 0 until internalPointerEvent.changes.size()) {
                val pointerInputChange = internalPointerEvent.changes.valueAt(i)
                if (isHover || pointerInputChange.changedToDownIgnoreConsumed()) {
                    root.hitTest(pointerInputChange.position, hitResult, pointerInputChange.type)
                    if (hitResult.isNotEmpty()) {
                        hitPathTracker.addHitPath(
                            pointerId = pointerInputChange.id,
                            pointerInputNodes = hitResult,
                            // Prunes PointerIds (and changes) to support dynamically
                            // adding/removing pointer input modifier nodes.
                            // Note: We do not do this for hover because hover relies on those
                            // non hit PointerIds to trigger hover exit events.
                            prunePointerIdsAndChangesNotInNodesList =
                                pointerInputChange.changedToDownIgnoreConsumed(),
                        )
                        hitResult.clear()
                    }
                }
            }

            // Dispatch to PointerInputFilters
            val dispatchedToSomething =
                hitPathTracker.dispatchChanges(internalPointerEvent, isInBounds)

            val anyMovementConsumed =
                if (internalPointerEvent.suppressMovementConsumption) {
                    false
                } else {
                    var result = false
                    for (i in 0 until internalPointerEvent.changes.size()) {
                        val event = internalPointerEvent.changes.valueAt(i)
                        if (event.positionChangedIgnoreConsumed() && event.isConsumed) {
                            result = true
                            break
                        }
                    }
                    result
                }

            var anyChangeConsumed = false
            for (i in 0 until internalPointerEvent.changes.size()) {
                val change = internalPointerEvent.changes.valueAt(i)
                if (change.isConsumed) {
                    anyChangeConsumed = true
                    break
                }
            }

            return ProcessResult(
                dispatchedToAPointerInputModifier = dispatchedToSomething,
                anyMovementConsumed = anyMovementConsumed,
                anyChangeConsumed = anyChangeConsumed,
            )
        } finally {
            isProcessing = false
        }
    }

    /**
     * Responds appropriately to Android ACTION_CANCEL events.
     *
     * Specifically, [PointerInputFilter.onCancel] is invoked on tracked [PointerInputFilter]s and
     * and this [PointerInputEventProcessor] is reset such that it is no longer tracking any
     * [PointerInputFilter]s and expects the next [PointerInputEvent] it processes to represent only
     * new pointers.
     */
    fun processCancel() {
        if (!isProcessing) {
            // Processing currently does not support reentrancy.
            pointerInputChangeEventProducer.clear()
            hitPathTracker.processCancel()
        }
    }

    /**
     * In some cases we need to clear the HIT Modifier.Node(s) cached from previous events because
     * they are no longer relevant.
     */
    fun clearPreviouslyHitModifierNodes() {
        hitPathTracker.clearPreviouslyHitModifierNodeCache()
    }
}

/** Produces [InternalPointerEvent]s by tracking changes between [PointerInputEvent]s */
@OptIn(InternalCoreApi::class)
private class PointerInputChangeEventProducer {
    private val previousPointerInputData: LongSparseArray<PointerInputData> = LongSparseArray()

    /** Produces [InternalPointerEvent]s by tracking changes between [PointerInputEvent]s */
    fun produce(
        pointerInputEvent: PointerInputEvent,
        positionCalculator: PositionCalculator,
    ): InternalPointerEvent {
        // Set initial capacity to avoid resizing - we know the size the map will be.
        val changes: LongSparseArray<PointerInputChange> =
            LongSparseArray(pointerInputEvent.pointers.size)
        pointerInputEvent.pointers.fastForEach {
            val previousTime: Long
            val previousPosition: Offset
            val previousDown: Boolean

            val previousData = previousPointerInputData[it.id.value]
            if (previousData == null) {
                previousTime = it.uptime
                previousPosition = it.position
                previousDown = false
            } else {
                previousTime = previousData.uptime
                previousDown = previousData.down
                previousPosition = positionCalculator.screenToLocal(previousData.positionOnScreen)
            }

            changes.put(
                it.id.value,
                PointerInputChange(
                    id = it.id,
                    uptimeMillis = it.uptime,
                    position = it.position,
                    pressed = it.down,
                    pressure = it.pressure,
                    previousUptimeMillis = previousTime,
                    previousPosition = previousPosition,
                    previousPressed = previousDown,
                    isInitiallyConsumed = false,
                    type = it.type,
                    historical = it.historical,
                    scrollDelta = it.scrollDelta,
                    scaleGestureFactor = it.scaleGestureFactor,
                    panGestureOffset = it.panGestureOffset,
                    originalEventPosition = it.originalEventPosition,
                ),
            )
            if (it.down) {
                previousPointerInputData.put(
                    it.id.value,
                    PointerInputData(it.uptime, it.positionOnScreen, it.down),
                )
            } else {
                previousPointerInputData.remove(it.id.value)
            }
        }

        return InternalPointerEvent(changes, pointerInputEvent)
    }

    /** Clears all tracked information. */
    fun clear() {
        previousPointerInputData.clear()
    }

    private class PointerInputData(
        val uptime: Long,
        val positionOnScreen: Offset,
        val down: Boolean,
    )
}

/** The result of a call to [PointerInputEventProcessor.process]. */
@kotlin.jvm.JvmInline
internal value class ProcessResult(val value: Int) {
    /** It's true when any [PointerInputFilter] has processed a [PointerInputChange] */
    val dispatchedToAPointerInputModifier
        inline get() = (value and 0x1) != 0

    /** It's true when [PointerInputChange] was consumed and Pointer's position was changed */
    val anyMovementConsumed
        inline get() = (value and 0x2) != 0

    /** It's true when any [PointerInputChange] was consumed. */
    val anyChangeConsumed
        inline get() = (value and 0x4) != 0
}

/**
 * Constructs a new ProcessResult.
 *
 * @param dispatchedToAPointerInputModifier True if the dispatch resulted in at least 1
 *   [PointerInputModifier] receiving the event.
 * @param anyMovementConsumed True if any movement occurred and was consumed.
 */
internal fun ProcessResult(
    dispatchedToAPointerInputModifier: Boolean,
    anyMovementConsumed: Boolean,
    anyChangeConsumed: Boolean,
): ProcessResult {
    return ProcessResult(
        value =
            dispatchedToAPointerInputModifier.toInt() or
                (anyMovementConsumed.toInt() shl 1) or
                (anyChangeConsumed.toInt() shl 2)
    )
}

private inline fun Boolean.toInt() = if (this) 1 else 0
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/draw/DrawModifier.kt
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

package androidx.compose.ui.draw

import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.ShadowContext
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize

/** A [Modifier.Element] that draws into the space of the layout. */
@JvmDefaultWithCompatibility
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects to be referenced across
 * draw calls
 */
@JvmDefaultWithCompatibility
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls. This is useful to
     * conditionally recreate objects only if the size of the drawing environment changes, or if
     * state parameters that are inputs to objects change. This method is guaranteed to be called
     * before [DrawModifier.draw].
     *
     * @param params The params to be used to build the cache.
     */
    fun onBuildCache(params: BuildDrawCacheParams)
}

/**
 * The set of parameters which could be used to build the drawing cache.
 *
 * @see DrawCacheModifier.onBuildCache
 */
interface BuildDrawCacheParams {
    /** The current size of the drawing environment */
    val size: Size

    /** The current layout direction. */
    val layoutDirection: LayoutDirection

    /** The current screen density to provide the ability to convert between */
    val density: Density
}

/** Draw into a [Canvas] behind the modified content. */
fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit) = this then DrawBehindElement(onDraw)

private class DrawBehindElement(val onDraw: DrawScope.() -> Unit) :
    ModifierNodeElement<DrawBackgroundModifier>() {
    override fun create() = DrawBackgroundModifier(onDraw)

    override fun update(node: DrawBackgroundModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBehind"
        properties["onDraw"] = onDraw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBehindElement) return false

        if (onDraw !== other.onDraw) return false

        return true
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
    }
}

internal class DrawBackgroundModifier(var onDraw: DrawScope.() -> Unit) :
    Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
        drawContent()
    }
}

/**
 * Draw into a [DrawScope] with content that is persisted across draw calls as long as the size of
 * the drawing area is the same or any state objects that are read have not changed. In the event
 * that the drawing area changes, or the underlying state values that are being read change, this
 * method is invoked again to recreate objects to be used during drawing
 *
 * For example, a [androidx.compose.ui.graphics.LinearGradient] that is to occupy the full bounds of
 * the drawing area can be created once the size has been defined and referenced for subsequent draw
 * calls without having to re-allocate.
 *
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierSample
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierStateParameterSample
 * @sample androidx.compose.ui.samples.DrawWithCacheContentSample
 */
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult) =
    this then DrawWithCacheElement(onBuildDrawCache)

private class DrawWithCacheElement(val onBuildDrawCache: CacheDrawScope.() -> DrawResult) :
    ModifierNodeElement<CacheDrawModifierNodeImpl>() {
    override fun create(): CacheDrawModifierNodeImpl {
        return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
    }

    override fun update(node: CacheDrawModifierNodeImpl) {
        node.block = onBuildDrawCache
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithCache"
        properties["onBuildDrawCache"] = onBuildDrawCache
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawWithCacheElement) return false

        if (onBuildDrawCache !== other.onBuildDrawCache) return false

        return true
    }

    override fun hashCode(): Int {
        return onBuildDrawCache.hashCode()
    }
}

fun CacheDrawModifierNode(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
): CacheDrawModifierNode {
    return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
}

/**
 * Expands on the [androidx.compose.ui.node.DrawModifierNode] by adding the ability to invalidate
 * the draw cache for changes in things like shapes and bitmaps (see Modifier.border for a usage
 * examples).
 */
sealed interface CacheDrawModifierNode : DrawModifierNode {
    fun invalidateDrawCache()
}

/**
 * Wrapper [GraphicsContext] implementation that maintains a list of the [GraphicsLayer] instances
 * that were created through this instance so it can release only those [GraphicsLayer]s when it is
 * disposed of within the corresponding Modifier is disposed
 */
private class ScopedGraphicsContext : GraphicsContext {

    private var allocatedGraphicsLayers: MutableObjectList<GraphicsLayer>? = null

    var graphicsContext: GraphicsContext? = null
        set(value) {
            releaseGraphicsLayers()
            field = value
        }

    override fun createGraphicsLayer(): GraphicsLayer {
        val gContext = graphicsContext
        checkPrecondition(gContext != null) { "GraphicsContext not provided" }
        val layer = gContext.createGraphicsLayer()
        val layers = allocatedGraphicsLayers
        if (layers == null) {
            mutableObjectListOf(layer).also { allocatedGraphicsLayers = it }
        } else {
            layers.add(layer)
        }

        return layer
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        graphicsContext?.releaseGraphicsLayer(layer)
    }

    override val shadowContext: ShadowContext
        get() {
            val gContext = graphicsContext
            checkPrecondition(gContext != null) { "GraphicsContext not provided" }
            return gContext.shadowContext
        }

    fun releaseGraphicsLayers() {
        allocatedGraphicsLayers?.let { layers ->
            layers.forEach { layer -> releaseGraphicsLayer(layer) }
            layers.clear()
        }
    }
}

private class CacheDrawModifierNodeImpl(
    private val cacheDrawScope: CacheDrawScope,
    block: CacheDrawScope.() -> DrawResult,
) : Modifier.Node(), CacheDrawModifierNode, ObserverModifierNode, BuildDrawCacheParams {

    private var isCacheValid = false
    private var cachedGraphicsContext: ScopedGraphicsContext? = null

    var block: CacheDrawScope.() -> DrawResult = block
        set(value) {
            field = value
            invalidateDrawCache()
        }

    init {
        cacheDrawScope.cacheParams = this
        cacheDrawScope.graphicsContextProvider = { graphicsContext }
    }

    override val density: Density
        get() = requireDensity()

    override val layoutDirection: LayoutDirection
        get() = requireLayoutDirection()

    override val size: Size
        get() = requireCoordinator(Nodes.Draw).size.toSize()

    val graphicsContext: GraphicsContext
        get() {
            var localGraphicsContext = cachedGraphicsContext
            if (localGraphicsContext == null) {
                localGraphicsContext = ScopedGraphicsContext().also { cachedGraphicsContext = it }
            }
            if (localGraphicsContext.graphicsContext == null) {
                localGraphicsContext.graphicsContext = requireGraphicsContext()
            }
            return localGraphicsContext
        }

    override fun onDetach() {
        super.onDetach()
        cachedGraphicsContext?.releaseGraphicsLayers()
    }

    override fun onReset() {
        super.onReset()
        invalidateDrawCache()
    }

    override fun onMeasureResultChanged() {
        invalidateDrawCache()
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    override fun invalidateDrawCache() {
        // Release all previously allocated graphics layers to the recycling pool
        // if a layer is needed in a subsequent draw, it will be obtained from the pool again and
        // reused
        cachedGraphicsContext?.releaseGraphicsLayers()
        isCacheValid = false
        cacheDrawScope.drawResult = null
        invalidateDraw()
    }

    override fun onDensityChange() {
        invalidateDrawCache()
    }

    override fun onLayoutDirectionChange() {
        invalidateDrawCache()
    }

    private fun getOrBuildCachedDrawBlock(contentDrawScope: ContentDrawScope): DrawResult {
        if (!isCacheValid) {
            cacheDrawScope.apply {
                drawResult = null
                this.contentDrawScope = contentDrawScope
                observeReads { block() }
                checkPreconditionNotNull(drawResult) {
                    "DrawResult not defined, did you forget to call onDraw?"
                }
            }
            isCacheValid = true
        }
        return cacheDrawScope.drawResult!!
    }

    override fun ContentDrawScope.draw() {
        getOrBuildCachedDrawBlock(this).block(this)
    }
}

/**
 * Handle to a drawing environment that enables caching of content based on the resolved size.
 * Consumers define parameters and refer to them in the captured draw callback provided in
 * [onDrawBehind] or [onDrawWithContent].
 *
 * [onDrawBehind] will draw behind the layout's drawing contents however, [onDrawWithContent] will
 * provide the ability to draw before or after the layout's contents
 */
class CacheDrawScope internal constructor() : Density {
    internal var cacheParams: BuildDrawCacheParams = EmptyBuildDrawCacheParams
    internal var drawResult: DrawResult? = null
    internal var contentDrawScope: ContentDrawScope? = null
    internal var graphicsContextProvider: (() -> GraphicsContext)? = null

    /** Provides the dimensions of the current drawing environment */
    val size: Size
        get() = cacheParams.size

    /** Provides the [LayoutDirection]. */
    val layoutDirection: LayoutDirection
        get() = cacheParams.layoutDirection

    /**
     * Returns a managed [GraphicsLayer] instance. This [GraphicsLayer] maybe newly created or
     * return a previously allocated instance. Consumers are not expected to release this instance
     * as it is automatically recycled upon invalidation of the CacheDrawScope and released when the
     * [DrawCacheModifier] is detached.
     */
    fun obtainGraphicsLayer(): GraphicsLayer =
        graphicsContextProvider!!.invoke().createGraphicsLayer()

    /**
     * Returns the [ShadowContext] used to create [InnerShadowPainter] and [DropShadowPainter] to
     * render inner and drop shadows respectively
     */
    fun obtainShadowContext(): ShadowContext = graphicsContextProvider!!.invoke().shadowContext

    /**
     * Record the drawing commands into the [GraphicsLayer] with the [Density], [LayoutDirection]
     * and [Size] are given from the provided [CacheDrawScope]
     */
    fun GraphicsLayer.record(
        density: Density = this@CacheDrawScope,
        layoutDirection: LayoutDirection = this@CacheDrawScope.layoutDirection,
        size: IntSize = this@CacheDrawScope.size.toIntSize(),
        block: ContentDrawScope.() -> Unit,
    ) {
        val scope = contentDrawScope!!
        with(scope) {
            val prevDensity = drawContext.density
            val prevLayoutDirection = drawContext.layoutDirection
            record(size) {
                drawContext.apply {
                    this.density = density
                    this.layoutDirection = layoutDirection
                }
                try {
                    block(scope)
                } finally {
                    drawContext.apply {
                        this.density = prevDensity
                        this.layoutDirection = prevLayoutDirection
                    }
                }
            }
        }
    }

    /** Issue drawing commands to be executed before the layout content is drawn */
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = onDrawWithContent {
        block()
        drawContent()
    }

    /** Issue drawing commands before or after the layout's drawing contents */
    fun onDrawWithContent(block: ContentDrawScope.() -> Unit): DrawResult {
        return DrawResult(block).also { drawResult = it }
    }

    override val density: Float
        get() = cacheParams.density.density

    override val fontScale: Float
        get() = cacheParams.density.fontScale
}

private object EmptyBuildDrawCacheParams : BuildDrawCacheParams {
    override val size: Size = Size.Unspecified
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val density: Density = Density(1f, 1f)
}

/**
 * Holder to a callback to be invoked during draw operations. This lambda captures and reuses
 * parameters defined within the CacheDrawScope receiver scope lambda.
 */
class DrawResult internal constructor(internal var block: ContentDrawScope.() -> Unit)

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's contents.
 * It also allows the modifier to adjust the layout's canvas.
 */
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit): Modifier =
    this then DrawWithContentElement(onDraw)

private class DrawWithContentElement(val onDraw: ContentDrawScope.() -> Unit) :
    ModifierNodeElement<DrawWithContentModifier>() {
    override fun create() = DrawWithContentModifier(onDraw)

    override fun update(node: DrawWithContentModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithContent"
        properties["onDraw"] = onDraw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawWithContentElement) return false

        if (onDraw !== other.onDraw) return false

        return true
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
    }
}

private class DrawWithContentModifier(var onDraw: ContentDrawScope.() -> Unit) :
    Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
    }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsModifier.kt
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

package androidx.compose.ui.semantics

import androidx.compose.ui.Modifier
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.AtomicInt
import androidx.compose.ui.platform.InspectorInfo

private var lastIdentifier = AtomicInt(0)

internal fun generateSemanticsId() = lastIdentifier.addAndGet(1)

/**
 * A [Modifier.Element] that adds semantics key/value for use in testing, accessibility, and similar
 * use cases.
 */
@JvmDefaultWithCompatibility
interface SemanticsModifier : Modifier.Element {
    @Deprecated(
        message =
            "SemanticsModifier.id is now unused and has been set to a fixed value. " +
                "Retrieve the id from LayoutInfo instead.",
        replaceWith = ReplaceWith(""),
    )
    val id: Int
        get() = -1

    /**
     * The SemanticsConfiguration holds substantive data, especially a list of key/value pairs such
     * as (label -> "buttonName").
     */
    val semanticsConfiguration: SemanticsConfiguration
}

internal class CoreSemanticsModifierNode(
    var mergeDescendants: Boolean,
    var isClearingSemantics: Boolean,
    var properties: SemanticsPropertyReceiver.() -> Unit,
) : Modifier.Node(), SemanticsModifierNode {
    override val shouldClearDescendantSemantics: Boolean
        get() = isClearingSemantics

    override val shouldMergeDescendantSemantics: Boolean
        get() = mergeDescendants

    override fun SemanticsPropertyReceiver.applySemantics() {
        properties()
    }
}

internal class EmptySemanticsModifier : Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {}
}

/**
 * Add semantics key/value pairs to the layout node, for use in testing, accessibility, etc.
 *
 * The provided lambda receiver scope provides "key = value"-style setters for any
 * [SemanticsPropertyKey]. Additionally, chaining multiple semantics modifiers is also a supported
 * style.
 *
 * The resulting semantics produce two [SemanticsNode] trees:
 *
 * The "unmerged tree" rooted at [SemanticsOwner.unmergedRootSemanticsNode] has one [SemanticsNode]
 * per layout node which has any [SemanticsModifier] on it. This [SemanticsNode] contains all the
 * properties set in all the [SemanticsModifier]s on that node.
 *
 * The "merged tree" rooted at [SemanticsOwner.rootSemanticsNode] has equal-or-fewer nodes: it
 * simplifies the structure based on [mergeDescendants] and [clearAndSetSemantics]. For most
 * purposes (especially accessibility, or the testing of accessibility), the merged semantics tree
 * should be used.
 *
 * @param mergeDescendants Whether the semantic information provided by the owning component and its
 *   descendants should be treated as one logical entity. Most commonly set on
 *   screen-reader-focusable items such as buttons or form fields. In the merged semantics tree, all
 *   descendant nodes (except those themselves marked [mergeDescendants]) will disappear from the
 *   tree, and their properties will get merged into the parent's configuration (using a merging
 *   algorithm that varies based on the type of property -- for example, text properties will get
 *   concatenated, separated by commas). In the unmerged semantics tree, the node is simply marked
 *   with [SemanticsConfiguration.isMergingSemanticsOfDescendants].
 * @param properties properties to add to the semantics. [SemanticsPropertyReceiver] will be
 *   provided in the scope to allow access for common properties and its values.
 *
 *   Note: The [properties] block should be used to set semantic properties or semantic actions.
 *   Don't call [SemanticsModifierNode.applySemantics] from within the [properties] block. It will
 *   result in an infinite loop.
 */
fun Modifier.semantics(
    mergeDescendants: Boolean = false,
    properties: (SemanticsPropertyReceiver.() -> Unit),
): Modifier =
    this then AppendedSemanticsElement(mergeDescendants = mergeDescendants, properties = properties)

// Implement SemanticsModifier to allow tooling to inspect the semantics configuration
internal class AppendedSemanticsElement(
    val mergeDescendants: Boolean,
    val properties: (SemanticsPropertyReceiver.() -> Unit),
) : ModifierNodeElement<CoreSemanticsModifierNode>(), SemanticsModifier {

    // This should only ever be called by layout inspector
    override val semanticsConfiguration: SemanticsConfiguration
        get() =
            SemanticsConfiguration().apply {
                isMergingSemanticsOfDescendants = mergeDescendants
                properties()
            }

    override fun create(): CoreSemanticsModifierNode {
        return CoreSemanticsModifierNode(
            mergeDescendants = mergeDescendants,
            isClearingSemantics = false,
            properties = properties,
        )
    }

    override fun update(node: CoreSemanticsModifierNode) {
        node.mergeDescendants = mergeDescendants
        node.properties = properties
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "semantics"
        properties["mergeDescendants"] = mergeDescendants
        addSemanticsPropertiesFrom(semanticsConfiguration)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppendedSemanticsElement) return false

        if (mergeDescendants != other.mergeDescendants) return false
        if (properties !== other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mergeDescendants.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}

/**
 * Clears the semantics of all the descendant nodes and sets new semantics.
 *
 * In the merged semantics tree, this clears the semantic information provided by the node's
 * descendants (but not those of the layout node itself, if any) and sets the provided semantics.
 * (In the unmerged tree, the semantics node is marked with
 * "[SemanticsConfiguration.isClearingSemantics]", but nothing is actually cleared.)
 *
 * Compose's default semantics provide baseline usability for screen-readers, but this can be used
 * to provide a more polished screen-reader experience: for example, clearing the semantics of a
 * group of tiny buttons, and setting equivalent actions on the card containing them.
 *
 * @param properties properties to add to the semantics. [SemanticsPropertyReceiver] will be
 *   provided in the scope to allow access for common properties and its values.
 *
 *   Note: The [properties] lambda should be used to set semantic properties or semantic actions.
 *   Don't call [SemanticsModifierNode.applySemantics] from within the [properties] block. It will
 *   result in an infinite loop.
 */
fun Modifier.clearAndSetSemantics(properties: (SemanticsPropertyReceiver.() -> Unit)): Modifier =
    this then ClearAndSetSemanticsElement(properties)

// Implement SemanticsModifier to allow tooling to inspect the semantics configuration
internal class ClearAndSetSemanticsElement(val properties: SemanticsPropertyReceiver.() -> Unit) :
    ModifierNodeElement<CoreSemanticsModifierNode>(), SemanticsModifier {

    // This should only ever be called by layout inspector
    override val semanticsConfiguration: SemanticsConfiguration
        get() =
            SemanticsConfiguration().apply {
                isMergingSemanticsOfDescendants = false
                isClearingSemantics = true
                properties()
            }

    override fun create(): CoreSemanticsModifierNode {
        return CoreSemanticsModifierNode(
            mergeDescendants = false,
            isClearingSemantics = true,
            properties = properties,
        )
    }

    override fun update(node: CoreSemanticsModifierNode) {
        node.properties = properties
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clearAndSetSemantics"
        addSemanticsPropertiesFrom(semanticsConfiguration)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClearAndSetSemanticsElement) return false

        if (properties !== other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        return properties.hashCode()
    }
}

private fun InspectorInfo.addSemanticsPropertiesFrom(
    semanticsConfiguration: SemanticsConfiguration
) {
    properties["properties"] =
        semanticsConfiguration.associate { (key, value) -> key.name to value }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt
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

package androidx.compose.ui.semantics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension instead
 * of used directly.
 */
/*@VisibleForTesting*/
object SemanticsProperties {
    /** @see SemanticsPropertyReceiver.contentDescription */
    val ContentDescription =
        AccessibilityKey<List<String>>(
            name = "ContentDescription",
            mergePolicy = { parentValue, childValue ->
                parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
            },
        )

    /** @see SemanticsPropertyReceiver.stateDescription */
    val StateDescription = AccessibilityKey<String>("StateDescription")

    /** @see SemanticsPropertyReceiver.progressBarRangeInfo */
    val ProgressBarRangeInfo = AccessibilityKey<ProgressBarRangeInfo>("ProgressBarRangeInfo")

    /** @see SemanticsPropertyReceiver.paneTitle */
    val PaneTitle =
        AccessibilityKey<String>(
            name = "PaneTitle",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property PaneTitle."
                )
            },
        )

    /** @see SemanticsPropertyReceiver.selectableGroup */
    val SelectableGroup = AccessibilityKey<Unit>("SelectableGroup")

    /** @see SemanticsPropertyReceiver.collectionInfo */
    val CollectionInfo = AccessibilityKey<CollectionInfo>("CollectionInfo")

    /** @see SemanticsPropertyReceiver.collectionItemInfo */
    val CollectionItemInfo = AccessibilityKey<CollectionItemInfo>("CollectionItemInfo")

    /** @see SemanticsPropertyReceiver.heading */
    val Heading = AccessibilityKey<Unit>("Heading")

    /** @see SemanticsPropertyReceiver.textEntryKey */
    val TextEntryKey = AccessibilityKey<Unit>("TextEntryKey")

    /** @see SemanticsPropertyReceiver.disabled */
    val Disabled = AccessibilityKey<Unit>("Disabled")

    /** @see SemanticsPropertyReceiver.liveRegion */
    val LiveRegion = AccessibilityKey<LiveRegionMode>("LiveRegion")

    /** @see SemanticsPropertyReceiver.focused */
    val Focused = AccessibilityKey<Boolean>("Focused")

    /** @see SemanticsPropertyReceiver.isContainer */
    @Deprecated("Use `isTraversalGroup` instead.", replaceWith = ReplaceWith("IsTraversalGroup"))
    // TODO(mnuzen): `isContainer` should not need to be an accessibility key after a new
    //  pruning API is added. See b/347038246 for more details.
    val IsContainer = AccessibilityKey<Boolean>("IsContainer")

    /** @see SemanticsPropertyReceiver.isTraversalGroup */
    val IsTraversalGroup = SemanticsPropertyKey<Boolean>("IsTraversalGroup")

    /** @see SemanticsPropertyReceiver.IsSensitiveData */
    val IsSensitiveData = SemanticsPropertyKey<Boolean>("IsSensitiveData")

    /** @see SemanticsPropertyReceiver.invisibleToUser */
    @Deprecated(
        "Use `hideFromAccessibility` instead.",
        replaceWith = ReplaceWith("HideFromAccessibility"),
    )
    // Retain for binary compatibility with aosp/3341487 in 1.7
    val InvisibleToUser =
        SemanticsPropertyKey<Unit>(
            name = "InvisibleToUser",
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.hideFromAccessibility */
    val HideFromAccessibility =
        SemanticsPropertyKey<Unit>(
            name = "HideFromAccessibility",
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.contentType */
    val ContentType =
        SemanticsPropertyKey<ContentType>(
            name = "ContentType",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.contentDataType */
    val ContentDataType =
        SemanticsPropertyKey<ContentDataType>(
            name = "ContentDataType",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill data types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.fillableData */
    val FillableData =
        SemanticsPropertyKey<FillableData>(
            name = "FillableData",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.traversalIndex */
    val TraversalIndex =
        SemanticsPropertyKey<Float>(
            name = "TraversalIndex",
            mergePolicy = { parentValue, _ ->
                // Never merge traversal indices
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.horizontalScrollAxisRange */
    val HorizontalScrollAxisRange = AccessibilityKey<ScrollAxisRange>("HorizontalScrollAxisRange")

    /** @see SemanticsPropertyReceiver.verticalScrollAxisRange */
    val VerticalScrollAxisRange = AccessibilityKey<ScrollAxisRange>("VerticalScrollAxisRange")

    /** @see SemanticsPropertyReceiver.popup */
    val IsPopup =
        AccessibilityKey<Unit>(
            name = "IsPopup",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property IsPopup. " +
                        "A popup should not be a child of a clickable/focusable node."
                )
            },
        )

    /** @see SemanticsPropertyReceiver.dialog */
    val IsDialog =
        AccessibilityKey<Unit>(
            name = "IsDialog",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property IsDialog. " +
                        "A dialog should not be a child of a clickable/focusable node."
                )
            },
        )

    /**
     * The type of user interface element. Accessibility services might use this to describe the
     * element or do customizations. Most roles can be automatically resolved by the semantics
     * properties of this element. But some elements with subtle differences need an exact role. If
     * an exact role is not listed in [Role], this property should not be set and the framework will
     * automatically resolve it.
     *
     * @see SemanticsPropertyReceiver.role
     */
    val Role = AccessibilityKey<Role>("Role") { parentValue, _ -> parentValue }

    /** @see SemanticsPropertyReceiver.testTag */
    val TestTag =
        SemanticsPropertyKey<String>(
            name = "TestTag",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ ->
                // Never merge TestTags, to avoid leaking internal test tags to parents.
                parentValue
            },
        )

    /**
     * Marks a link within a text node (a link is represented by a
     * [androidx.compose.ui.text.LinkAnnotation]) for identification during automated testing. This
     * property is for internal use only and not intended for general use by developers.
     */
    val LinkTestMarker =
        SemanticsPropertyKey<Unit>(
            name = "LinkTestMarker",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.text */
    val Text =
        AccessibilityKey<List<AnnotatedString>>(
            name = "Text",
            mergePolicy = { parentValue, childValue ->
                parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
            },
        )

    /** @see SemanticsPropertyReceiver.textSubstitution */
    val TextSubstitution = SemanticsPropertyKey<AnnotatedString>(name = "TextSubstitution")

    /** @see SemanticsPropertyReceiver.isShowingTextSubstitution */
    val IsShowingTextSubstitution = SemanticsPropertyKey<Boolean>("IsShowingTextSubstitution")

    /** @see SemanticsPropertyReceiver.inputText */
    val InputText = AccessibilityKey<AnnotatedString>(name = "InputText")

    /** @see SemanticsPropertyReceiver.editableText */
    val EditableText = AccessibilityKey<AnnotatedString>(name = "EditableText")

    /** @see SemanticsPropertyReceiver.textSelectionRange */
    val TextSelectionRange = AccessibilityKey<TextRange>("TextSelectionRange")

    /** @see SemanticsPropertyReceiver.onImeAction */
    val ImeAction = AccessibilityKey<ImeAction>("ImeAction")

    /** @see SemanticsPropertyReceiver.selected */
    val Selected = AccessibilityKey<Boolean>("Selected")

    /** @see SemanticsPropertyReceiver.toggleableState */
    val ToggleableState = AccessibilityKey<ToggleableState>("ToggleableState")

    /** @see SemanticsPropertyReceiver.password */
    val Password = AccessibilityKey<Unit>("Password")

    /** @see SemanticsPropertyReceiver.error */
    val Error = AccessibilityKey<String>("Error")

    /** @see SemanticsPropertyReceiver.indexForKey */
    val IndexForKey = SemanticsPropertyKey<(Any) -> Int>("IndexForKey")

    /** @see SemanticsPropertyReceiver.isEditable */
    val IsEditable = SemanticsPropertyKey<Boolean>("IsEditable")

    /** @see SemanticsPropertyReceiver.maxTextLength */
    val MaxTextLength = SemanticsPropertyKey<Int>("MaxTextLength")

    /** @see SemanticsPropertyReceiver.shape */
    val Shape =
        SemanticsPropertyKey<Shape>(
            name = "Shape",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ ->
                // Never merge shapes
                parentValue
            },
        )
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension instead
 * of used directly.
 */
/*@VisibleForTesting*/
object SemanticsActions {
    /** @see SemanticsPropertyReceiver.getTextLayoutResult */
    val GetTextLayoutResult =
        ActionPropertyKey<(MutableList<TextLayoutResult>) -> Boolean>("GetTextLayoutResult")

    /** @see SemanticsPropertyReceiver.onClick */
    val OnClick = ActionPropertyKey<() -> Boolean>("OnClick")

    /** @see SemanticsPropertyReceiver.onLongClick */
    val OnLongClick = ActionPropertyKey<() -> Boolean>("OnLongClick")

    /** @see SemanticsPropertyReceiver.scrollBy */
    val ScrollBy = ActionPropertyKey<(x: Float, y: Float) -> Boolean>("ScrollBy")

    /** @see SemanticsPropertyReceiver.scrollByOffset */
    val ScrollByOffset = SemanticsPropertyKey<suspend (offset: Offset) -> Offset>("ScrollByOffset")

    /** @see SemanticsPropertyReceiver.scrollToIndex */
    val ScrollToIndex = ActionPropertyKey<(Int) -> Boolean>("ScrollToIndex")

    @Suppress("unused")
    @Deprecated(
        message = "Use `SemanticsActions.OnFillData` instead.",
        replaceWith =
            ReplaceWith("OnFillData", "androidx.compose.ui.semantics.SemanticsActions.OnFillData"),
        level = DeprecationLevel.WARNING,
    )
    val OnAutofillText = ActionPropertyKey<(AnnotatedString) -> Boolean>("OnAutofillText")

    /** @see SemanticsPropertyReceiver.onFillData */
    val OnFillData = ActionPropertyKey<(FillableData) -> Boolean>("OnFillData")

    /** @see SemanticsPropertyReceiver.setProgress */
    val SetProgress = ActionPropertyKey<(progress: Float) -> Boolean>("SetProgress")

    /** @see SemanticsPropertyReceiver.setSelection */
    val SetSelection = ActionPropertyKey<(Int, Int, Boolean) -> Boolean>("SetSelection")

    /** @see SemanticsPropertyReceiver.setText */
    val SetText = ActionPropertyKey<(AnnotatedString) -> Boolean>("SetText")

    /** @see SemanticsPropertyReceiver.setTextSubstitution */
    val SetTextSubstitution = ActionPropertyKey<(AnnotatedString) -> Boolean>("SetTextSubstitution")

    /** @see SemanticsPropertyReceiver.showTextSubstitution */
    val ShowTextSubstitution = ActionPropertyKey<(Boolean) -> Boolean>("ShowTextSubstitution")

    /** @see SemanticsPropertyReceiver.clearTextSubstitution */
    val ClearTextSubstitution = ActionPropertyKey<() -> Boolean>("ClearTextSubstitution")

    /** @see SemanticsPropertyReceiver.insertTextAtCursor */
    val InsertTextAtCursor = ActionPropertyKey<(AnnotatedString) -> Boolean>("InsertTextAtCursor")

    /** @see SemanticsPropertyReceiver.onImeAction */
    val OnImeAction = ActionPropertyKey<() -> Boolean>("PerformImeAction")

    // b/322269946
    @Suppress("unused")
    @Deprecated(
        message = "Use `SemanticsActions.OnImeAction` instead.",
        replaceWith =
            ReplaceWith(
                "OnImeAction",
                "androidx.compose.ui.semantics.SemanticsActions.OnImeAction",
            ),
        level = DeprecationLevel.ERROR,
    )
    val PerformImeAction = ActionPropertyKey<() -> Boolean>("PerformImeAction")

    /** @see SemanticsPropertyReceiver.copyText */
    val CopyText = ActionPropertyKey<() -> Boolean>("CopyText")

    /** @see SemanticsPropertyReceiver.cutText */
    val CutText = ActionPropertyKey<() -> Boolean>("CutText")

    /** @see SemanticsPropertyReceiver.pasteText */
    val PasteText = ActionPropertyKey<() -> Boolean>("PasteText")

    /** @see SemanticsPropertyReceiver.expand */
    val Expand = ActionPropertyKey<() -> Boolean>("Expand")

    /** @see SemanticsPropertyReceiver.collapse */
    val Collapse = ActionPropertyKey<() -> Boolean>("Collapse")

    /** @see SemanticsPropertyReceiver.dismiss */
    val Dismiss = ActionPropertyKey<() -> Boolean>("Dismiss")

    /** @see SemanticsPropertyReceiver.requestFocus */
    val RequestFocus = ActionPropertyKey<() -> Boolean>("RequestFocus")

    /** @see SemanticsPropertyReceiver.customActions */
    val CustomActions =
        AccessibilityKey<List<CustomAccessibilityAction>>(
            name = "CustomActions",
            mergePolicy = { parentValue, childValue -> parentValue.orEmpty() + childValue },
        )

    /** @see SemanticsPropertyReceiver.pageUp */
    val PageUp = ActionPropertyKey<() -> Boolean>("PageUp")

    /** @see SemanticsPropertyReceiver.pageLeft */
    val PageLeft = ActionPropertyKey<() -> Boolean>("PageLeft")

    /** @see SemanticsPropertyReceiver.pageDown */
    val PageDown = ActionPropertyKey<() -> Boolean>("PageDown")

    /** @see SemanticsPropertyReceiver.pageRight */
    val PageRight = ActionPropertyKey<() -> Boolean>("PageRight")

    /** @see SemanticsPropertyReceiver.getScrollViewportLength */
    val GetScrollViewportLength =
        ActionPropertyKey<(MutableList<Float>) -> Boolean>("GetScrollViewportLength")
}

/**
 * SemanticsPropertyKey is the infrastructure for setting key/value pairs inside semantics blocks in
 * a type-safe way. Each key has one particular statically defined value type T.
 */
class SemanticsPropertyKey<T>(
    /** The name of the property. Should be the same as the constant from which it is accessed. */
    val name: String,
    internal val mergePolicy: (T?, T) -> T? = { parentValue, childValue ->
        parentValue ?: childValue
    },
) {
    /**
     * Whether this type of property provides information relevant to accessibility services.
     *
     * Most built-in semantics properties are relevant to accessibility, but a very common exception
     * is testTag. Nodes with only a testTag still need to be included in the AccessibilityNodeInfo
     * tree because UIAutomator tests rely on that, but we mark them `isImportantForAccessibility =
     * false` on the AccessibilityNodeInfo to inform accessibility services that they are best
     * ignored.
     *
     * The default value is false and it is not exposed as a public API. That's because it is
     * impossible in the first place for `SemanticsPropertyKey`s defined outside the UI package to
     * be relevant to accessibility, because for each accessibility-relevant SemanticsProperty type
     * to get plumbed into the AccessibilityNodeInfo, the private `createNodeInfo` implementation
     * must also have a line of code.
     */
    internal var isImportantForAccessibility = false
        private set

    /**
     * If this value is non-null, this semantics property will be exposed as an accessibility extra
     * via AccessibilityNodeInfo.getExtras with this value used as the key for the extra.
     */
    internal var accessibilityExtraKey: String? = null

    internal constructor(name: String, isImportantForAccessibility: Boolean) : this(name) {
        this.isImportantForAccessibility = isImportantForAccessibility
    }

    internal constructor(
        name: String,
        isImportantForAccessibility: Boolean,
        mergePolicy: (T?, T) -> T?,
        accessibilityExtraKey: String? = null,
    ) : this(name, mergePolicy) {
        this.isImportantForAccessibility = isImportantForAccessibility
        this.accessibilityExtraKey = accessibilityExtraKey
    }

    /**
     * Method implementing the semantics merge policy of a particular key.
     *
     * When mergeDescendants is set on a semantics node, then this function will called for each
     * descendant node of a given key in depth-first-search order. The parent value accumulates the
     * result of merging the values seen so far, similar to reduce().
     *
     * The default implementation returns the parent value if one exists, otherwise uses the child
     * element. This means by default, a SemanticsNode with mergeDescendants = true winds up with
     * the first value found for each key in its subtree in depth-first-search order.
     */
    fun merge(parentValue: T?, childValue: T): T? {
        return mergePolicy(parentValue, childValue)
    }

    /** Throws [UnsupportedOperationException]. Should not be called. */
    // TODO(KT-6519): Remove this getter
    // TODO(KT-32770): Cannot deprecate this either as the getter is considered called by "by"
    final operator fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        return throwSemanticsGetNotSupported()
    }

    final operator fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T,
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "AccessibilityKey: $name"
    }
}

private fun <T> throwSemanticsGetNotSupported(): T {
    throw UnsupportedOperationException(
        "You cannot retrieve a semantics property directly - " +
            "use one of the SemanticsConfiguration.getOr* methods instead"
    )
}

@Suppress("NOTHING_TO_INLINE")
// inline to avoid different static initialization order on different targets.
// See https://youtrack.jetbrains.com/issue/KT-65040 for more information.
internal inline fun <T> AccessibilityKey(name: String) =
    SemanticsPropertyKey<T>(name = name, isImportantForAccessibility = true)

@Suppress("NOTHING_TO_INLINE")
// inline to avoid different static initialization order on different targets
// See https://youtrack.jetbrains.com/issue/KT-65040 for more information.
internal inline fun <T> AccessibilityKey(name: String, noinline mergePolicy: (T?, T) -> T?) =
    SemanticsPropertyKey(name = name, isImportantForAccessibility = true, mergePolicy = mergePolicy)

/**
 * Standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return a
 *   boolean result indicating whether the action is successfully handled. For example, a scroll
 *   forward action should return false if the widget is not enabled or has reached the end of the
 *   list. If multiple semantics blocks with the same AccessibilityAction are provided, the
 *   resulting AccessibilityAction's label/action will be the label/action of the outermost modifier
 *   with this key and nonnull label/action, or null if no nonnull label/action is found.
 */
class AccessibilityAction<T : Function<Boolean>>(val label: String?, val action: T?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessibilityAction<*>) return false

        if (label != other.label) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "AccessibilityAction(label=$label, action=$action)"
    }
}

@Suppress("NOTHING_TO_INLINE")
// inline to break static initialization cycle issue
private inline fun <T : Function<Boolean>> ActionPropertyKey(name: String) =
    AccessibilityKey<AccessibilityAction<T>>(
        name = name,
        mergePolicy = { parentValue, childValue ->
            AccessibilityAction(
                parentValue?.label ?: childValue.label,
                parentValue?.action ?: childValue.action,
            )
        },
    )

/**
 * Custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 *   arguments and return a boolean result indicating whether the action is successfully handled.
 */
class CustomAccessibilityAction(val label: String, val action: () -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomAccessibilityAction) return false

        if (label != other.label) return false
        if (action !== other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "CustomAccessibilityAction(label=$label, action=$action)"
    }
}

/**
 * Accessibility range information, to represent the status of a progress bar or seekable progress
 * bar.
 *
 * @param current current value in the range. Must not be NaN.
 * @param range range of this node
 * @param steps if greater than `0`, specifies the number of discrete values, evenly distributed
 *   between across the whole value range. If `0`, any value from the range specified can be chosen.
 *   Cannot be less than `0`.
 */
class ProgressBarRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>,
    /*@IntRange(from = 0)*/
    val steps: Int = 0,
) {
    init {
        require(!current.isNaN()) { "current must not be NaN" }
    }

    companion object {
        /** Accessibility range information to present indeterminate progress bar */
        val Indeterminate = ProgressBarRangeInfo(0f, 0f..0f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressBarRangeInfo) return false

        if (current != other.current) return false
        if (range != other.range) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = current.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + steps
        return result
    }

    override fun toString(): String {
        return "ProgressBarRangeInfo(current=$current, range=$range, steps=$steps)"
    }
}

/**
 * Information about the collection.
 *
 * A collection of items has [rowCount] rows and [columnCount] columns. For example, a vertical list
 * is a collection with one column, as many rows as the list items that are important for
 * accessibility; A table is a collection with several rows and several columns.
 *
 * @param rowCount the number of rows in the collection, or -1 if unknown
 * @param columnCount the number of columns in the collection, or -1 if unknown
 */
class CollectionInfo(val rowCount: Int, val columnCount: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CollectionInfo) return false

        if (rowCount != other.rowCount) return false
        if (columnCount != other.columnCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rowCount.hashCode()
        result = 31 * result + columnCount.hashCode()
        return result
    }

    override fun toString(): String {
        return "CollectionInfo(rowCount=$rowCount, columnCount=$columnCount)"
    }
}

/**
 * Information about the item of a collection.
 *
 * A collection item is contained in a collection, it starts at a given [rowIndex] and [columnIndex]
 * in the collection, and spans one or more rows and columns. For example, a header of two related
 * table columns starts at the first row and the first column, spans one row and two columns.
 *
 * @param rowIndex the index of the row at which item is located
 * @param rowSpan the number of rows the item spans
 * @param columnIndex the index of the column at which item is located
 * @param columnSpan the number of columns the item spans
 */
class CollectionItemInfo(
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int,
    val columnSpan: Int,
)

/**
 * The scroll state of one axis if this node is scrollable.
 *
 * @param value current 0-based scroll position value (either in pixels, or lazy-item count)
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 *   when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 *   mean bottom, when `false`, 0 [value] will mean top
 */
class ScrollAxisRange(
    val value: () -> Float,
    val maxValue: () -> Float,
    val reverseScrolling: Boolean = false,
) {
    override fun toString(): String =
        "ScrollAxisRange(value=${value()}, maxValue=${maxValue()}, " +
            "reverseScrolling=$reverseScrolling)"
}

/**
 * The type of user interface element. Accessibility services might use this to describe the element
 * or do customizations. Most roles can be automatically resolved by the semantics properties of
 * this element. But some elements with subtle differences need an exact role. If an exact role is
 * not listed, [SemanticsPropertyReceiver.role] should not be set and the framework will
 * automatically resolve it.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Role private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * This element is a button control. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsActions.OnClick]
         */
        val Button = Role(0)

        /**
         * This element is a Checkbox which is a component that represents two states (checked /
         * unchecked). Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Checkbox = Role(1)

        /**
         * This element is a Switch which is a two state toggleable component that provides on/off
         * like options. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Switch = Role(2)

        /**
         * This element is a RadioButton which is a component to represent two states, selected and
         * not selected. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val RadioButton = Role(3)

        /**
         * This element is a Tab which represents a single page of content using a text label and/or
         * icon. A Tab also has two states: selected and not selected. Associated semantics
         * properties for accessibility: [SemanticsProperties.Disabled],
         * [SemanticsProperties.StateDescription], [SemanticsActions.OnClick]
         */
        val Tab = Role(4)

        /**
         * This element is an image. Associated semantics properties for accessibility:
         * [SemanticsProperties.ContentDescription]
         */
        val Image = Role(5)

        /**
         * This element is associated with a drop down menu. Associated semantics properties for
         * accessibility: [SemanticsActions.OnClick]
         */
        val DropdownList = Role(6)

        /**
         * This element is a value picker. It should support the following accessibility actions to
         * enable selection of the next and previous values:
         *
         * [android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD]: Select the next
         * value.
         *
         * [android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD]: Select the
         * previous value.
         *
         * These actions allow accessibility services to interact with this node programmatically on
         * behalf of users, facilitating navigation within sets of selectable values.
         */
        val ValuePicker = Role(7)

        /**
         * This element is a Carousel. This means that even if Pager actions are added, this element
         * will behave like a regular List collection.
         *
         * Associated semantics properties for Pager accessibility actions:
         * [SemanticsActions.PageUp],[SemanticsActions.PageDown],[SemanticsActions.PageLeft],
         * [SemanticsActions.PageRight]
         */
        val Carousel = Role(8)
    }

    override fun toString() =
        when (this) {
            Button -> "Button"
            Checkbox -> "Checkbox"
            Switch -> "Switch"
            RadioButton -> "RadioButton"
            Tab -> "Tab"
            Image -> "Image"
            DropdownList -> "DropdownList"
            ValuePicker -> "Picker"
            Carousel -> "Carousel"
            else -> "Unknown"
        }
}

/**
 * The mode of live region. Live region indicates to accessibility services they should
 * automatically notify the user about changes to the node's content description or text, or to the
 * content descriptions or text of the node's children (where applicable).
 */
@Immutable
@kotlin.jvm.JvmInline
value class LiveRegionMode private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Live region mode specifying that accessibility services should announce changes to this
         * node.
         */
        val Polite = LiveRegionMode(0)

        /**
         * Live region mode specifying that accessibility services should interrupt ongoing speech
         * to immediately announce changes to this node.
         */
        val Assertive = LiveRegionMode(1)
    }

    override fun toString() =
        when (this) {
            Polite -> "Polite"
            Assertive -> "Assertive"
            else -> "Unknown"
        }
}

/**
 * SemanticsPropertyReceiver is the scope provided by semantics {} blocks, letting you set key/value
 * pairs primarily via extension functions.
 */
interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node.
 *
 * If this is not set, accessibility services will present the [text][SemanticsProperties.Text] of
 * this node as the content.
 *
 * This typically should not be set directly by applications, because some screen readers will cease
 * presenting other relevant information when this property is present. This is intended to be used
 * via Foundation components which are inherently intractable to automatically describe, such as
 * Image, Icon, and Canvas.
 */
var SemanticsPropertyReceiver.contentDescription: String
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.ContentDescription, listOf(value))
    }

/**
 * Developer-set state description of the semantics node.
 *
 * For example: on/off. If this not set, accessibility services will derive the state from other
 * semantics properties, like [ProgressBarRangeInfo], but it is not guaranteed and the format will
 * be decided by accessibility services.
 */
var SemanticsPropertyReceiver.stateDescription by SemanticsProperties.StateDescription

/**
 * The semantics represents a range of possible values with a current value. For example, when used
 * on a slider control, this will allow screen readers to communicate the slider's state.
 */
var SemanticsPropertyReceiver.progressBarRangeInfo by SemanticsProperties.ProgressBarRangeInfo

/**
 * The node is marked as heading for accessibility.
 *
 * @see SemanticsProperties.Heading
 */
fun SemanticsPropertyReceiver.heading() {
    this[SemanticsProperties.Heading] = Unit
}

/**
 * The node is marked as a text entry key for accessibility. This is used to indicate that this
 * composable acts as a key within a text entry interface, such as a custom on-screen keyboard.
 * Accessibility services can use this information to provide a better experience for users
 * interacting with custom text input methods.
 *
 * See
 * [AccessibilityNodeInfo.setTextEntryKey](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#setTextEntryKey(boolean))
 * for more details.
 *
 * @see SemanticsProperties.TextEntryKey
 */
fun SemanticsPropertyReceiver.textEntryKey() {
    this[SemanticsProperties.TextEntryKey] = Unit
}

/**
 * Accessibility-friendly title for a screen's pane. For accessibility purposes, a pane is a
 * visually distinct portion of a window, such as the contents of a open drawer. In order for
 * accessibility services to understand a pane's window-like behavior, you should give descriptive
 * titles to your app's panes. Accessibility services can then provide more granular information to
 * users when a pane's appearance or content changes.
 *
 * @see SemanticsProperties.PaneTitle
 */
var SemanticsPropertyReceiver.paneTitle by SemanticsProperties.PaneTitle

/**
 * Whether this semantics node is disabled. Note that proper [SemanticsActions] should still be
 * added when this property is set.
 *
 * @see SemanticsProperties.Disabled
 */
fun SemanticsPropertyReceiver.disabled() {
    this[SemanticsProperties.Disabled] = Unit
}

/**
 * This node is marked as live region for accessibility. This indicates to accessibility services
 * they should automatically notify the user about changes to the node's content description or
 * text, or to the content descriptions or text of the node's children (where applicable). It should
 * be used with caution, especially with assertive mode which immediately stops the current audio
 * and the user does not hear the rest of the content. An example of proper use is a Snackbar which
 * is marked as [LiveRegionMode.Polite].
 *
 * @see SemanticsProperties.LiveRegion
 * @see LiveRegionMode
 */
var SemanticsPropertyReceiver.liveRegion by SemanticsProperties.LiveRegion

/**
 * Whether this semantics node is focused. The presence of this property indicates this node is
 * focusable
 *
 * @see SemanticsProperties.Focused
 */
var SemanticsPropertyReceiver.focused by SemanticsProperties.Focused

/**
 * Whether this semantics node is a container. This is defined as a node whose function is to serve
 * as a boundary or border in organizing its children.
 *
 * @see SemanticsProperties.IsContainer
 */
@Deprecated("Use `isTraversalGroup` instead.", replaceWith = ReplaceWith("isTraversalGroup"))
@Suppress("DEPRECATION")
var SemanticsPropertyReceiver.isContainer by SemanticsProperties.IsContainer

/**
 * Whether this semantics node is a traversal group.
 *
 * See https://developer.android.com/develop/ui/compose/accessibility/traversal
 *
 * @see SemanticsProperties.IsTraversalGroup
 */
var SemanticsPropertyReceiver.isTraversalGroup by SemanticsProperties.IsTraversalGroup

/**
 * Whether this semantics node should only allow interactions from
 * [android.accessibilityservice.AccessibilityService]s with the
 * [android.accessibilityservice.AccessibilityServiceInfo.isAccessibilityTool] property set to true.
 *
 * This property allows the node to remain visible and interactive to Accessibility Services
 * declared as accessibility tools that assist users with disabilities, while simultaneously hiding
 * this node and its generated AccessibilityEvents from other Accessibility Services that are not
 * declared as accessibility tools.
 *
 * If looking for a way to hide the node from all Accessibility Services then consider
 * [SemanticsProperties.HideFromAccessibility] instead.
 *
 * @see SemanticsProperties.IsSensitiveData
 */
var SemanticsPropertyReceiver.isSensitiveData by SemanticsProperties.IsSensitiveData

/**
 * Whether this node is specially known to be invisible to the user.
 *
 * For example, if the node is currently occluded by a dark semitransparent pane above it, then for
 * all practical purposes the node is invisible to the user, but the system cannot automatically
 * determine that. To make the screen reader linear navigation skip over this type of invisible
 * node, this property can be set.
 *
 * If looking for a way to hide semantics of small items from screen readers because they're
 * redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
@Deprecated(
    "Use `hideFromAccessibility()` instead.",
    replaceWith = ReplaceWith("hideFromAccessibility()"),
)
@Suppress("DEPRECATION")
// Retain for binary compatibility with aosp/3341487 in 1.7
fun SemanticsPropertyReceiver.invisibleToUser() {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

/**
 * If present, this node is considered hidden from accessibility services.
 *
 * For example, if the node is currently occluded by a dark semitransparent pane above it, then for
 * all practical purposes the node should not be announced to the user. Since the system cannot
 * automatically determine that, this property can be set to make the screen reader linear
 * navigation skip over this type of node.
 *
 * If looking for a way to clear semantics of small items from the UI tree completely because they
 * are redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
fun SemanticsPropertyReceiver.hideFromAccessibility() {
    this[SemanticsProperties.HideFromAccessibility] = Unit
}

/**
 * Content field type information.
 *
 * This API can be used to indicate to Autofill services what _kind of field_ is associated with
 * this node. Not to be confused with the _data type_ to be entered into the field.
 *
 * @see SemanticsProperties.ContentType
 */
var SemanticsPropertyReceiver.contentType by SemanticsProperties.ContentType

/**
 * Content data type information.
 *
 * This API can be used to indicate to Autofill services what _kind of data_ is meant to be
 * suggested for this field. Not to be confused with the _type_ of the field.
 *
 * @see SemanticsProperties.ContentType
 */
var SemanticsPropertyReceiver.contentDataType by SemanticsProperties.ContentDataType

/**
 * The current value of a component that can be autofilled.
 *
 * This property is used to expose the component's current data *to* the autofill service. The
 * service can then read this value, for example, to save it for future autofill suggestions.
 *
 * This is the counterpart to the [onFillData] action, which is used to *receive* data from the
 * autofill service.
 *
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithFillableDataSemantics
 * @see SemanticsProperties.FillableData
 */
var SemanticsPropertyReceiver.fillableData by SemanticsProperties.FillableData

/**
 * A value to manually control screenreader traversal order.
 *
 * This API can be used to customize TalkBack traversal order. When the `traversalIndex` property is
 * set on a traversalGroup or on a screenreader-focusable node, then the sorting algorithm will
 * prioritize nodes with smaller `traversalIndex`s earlier. The default traversalIndex value is
 * zero, and traversalIndices are compared at a peer level.
 *
 * For example,` traversalIndex = -1f` can be used to force a top bar to be ordered earlier, and
 * `traversalIndex = 1f` to make a bottom bar ordered last, in the edge cases where this does not
 * happen by default. As another example, if you need to reorder two Buttons within a Row, then you
 * can set `isTraversalGroup = true` on the Row, and set `traversalIndex` on one of the Buttons.
 *
 * Note that if `traversalIndex` seems to have no effect, be sure to set `isTraversalGroup = true`
 * as well.
 */
var SemanticsPropertyReceiver.traversalIndex by SemanticsProperties.TraversalIndex

/** The horizontal scroll state of this node if this node is scrollable. */
var SemanticsPropertyReceiver.horizontalScrollAxisRange by
    SemanticsProperties.HorizontalScrollAxisRange

/** The vertical scroll state of this node if this node is scrollable. */
var SemanticsPropertyReceiver.verticalScrollAxisRange by SemanticsProperties.VerticalScrollAxisRange

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is _part of_
 * a Popup.
 */
fun SemanticsPropertyReceiver.popup() {
    this[SemanticsProperties.IsPopup] = Unit
}

/**
 * Whether this element is a Dialog. Not to be confused with if this element is _part of_ a Dialog.
 */
fun SemanticsPropertyReceiver.dialog() {
    this[SemanticsProperties.IsDialog] = Unit
}

/**
 * The type of user interface element. Accessibility services might use this to describe the element
 * or do customizations. Most roles can be automatically resolved by the semantics properties of
 * this element. But some elements with subtle differences need an exact role. If an exact role is
 * not listed in [Role], this property should not be set and the framework will automatically
 * resolve it.
 */
var SemanticsPropertyReceiver.role by SemanticsProperties.Role

/**
 * Test tag attached to this semantics node.
 *
 * This can be used to find nodes in testing frameworks:
 * - In Compose's built-in unit test framework, use with
 *   [onNodeWithTag][androidx.compose.ui.test.onNodeWithTag].
 * - For newer AccessibilityNodeInfo-based integration test frameworks, it can be matched in the
 *   extras with key "androidx.compose.ui.semantics.testTag"
 * - For legacy AccessibilityNodeInfo-based integration tests, it's optionally exposed as the
 *   resource id if [testTagsAsResourceId] is true (for matching with 'By.res' in UIAutomator).
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsPropertyReceiver.editableText
 */
var SemanticsPropertyReceiver.text: AnnotatedString
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.Text, listOf(value))
    }

/**
 * Text substitution of the semantics node. This property is only available after calling
 * [SemanticsActions.SetTextSubstitution].
 */
var SemanticsPropertyReceiver.textSubstitution by SemanticsProperties.TextSubstitution

/**
 * Whether this element is showing the text substitution. This property is only available after
 * calling [SemanticsActions.SetTextSubstitution].
 */
var SemanticsPropertyReceiver.isShowingTextSubstitution by
    SemanticsProperties.IsShowingTextSubstitution

/**
 * The raw value of the text field after input transformations have been applied.
 *
 * This is an actual user input of the fields, e.g. a real password, after any input transformations
 * that might change or reject that input have been applied. This value is not affected by visual
 * transformations.
 */
var SemanticsPropertyReceiver.inputText by SemanticsProperties.InputText

/**
 * A visual value of the text field after output transformations that change the visual
 * representation of the field's state have been applied.
 *
 * This is the value displayed to the user, for example "*******" in a password field.
 */
var SemanticsPropertyReceiver.editableText by SemanticsProperties.EditableText

/** Text selection range for the text field. */
var SemanticsPropertyReceiver.textSelectionRange by SemanticsProperties.TextSelectionRange

/**
 * Contains the IME action provided by the node.
 *
 * For example, "go to next form field" or "submit".
 *
 * A node that specifies an action should also specify a callback to perform the action via
 * [onImeAction].
 */
@Deprecated("Pass the ImeAction to onImeAction instead.")
@get:Deprecated("Pass the ImeAction to onImeAction instead.")
@set:Deprecated("Pass the ImeAction to onImeAction instead.")
var SemanticsPropertyReceiver.imeAction by SemanticsProperties.ImeAction

/**
 * Whether this element is selected (out of a list of possible selections).
 *
 * The presence of this property indicates that the element is selectable.
 */
var SemanticsPropertyReceiver.selected by SemanticsProperties.Selected

/**
 * This semantics marks node as a collection and provides the required information.
 *
 * @see collectionItemInfo
 */
var SemanticsPropertyReceiver.collectionInfo by SemanticsProperties.CollectionInfo

/**
 * This semantics marks node as an items of a collection and provides the required information.
 *
 * If you mark items of a collection, you should also be marking the collection with
 * [collectionInfo].
 */
var SemanticsPropertyReceiver.collectionItemInfo by SemanticsProperties.CollectionItemInfo

/**
 * The state of a toggleable component.
 *
 * The presence of this property indicates that the element is toggleable.
 */
var SemanticsPropertyReceiver.toggleableState by SemanticsProperties.ToggleableState

/** Whether this semantics node is editable, e.g. an editable text field. */
var SemanticsPropertyReceiver.isEditable by SemanticsProperties.IsEditable

/** The node is marked as a password. */
fun SemanticsPropertyReceiver.password() {
    this[SemanticsProperties.Password] = Unit
}

/**
 * Mark semantics node that contains invalid input or error.
 *
 * @param [description] a localized description explaining an error to the accessibility user
 */
fun SemanticsPropertyReceiver.error(description: String) {
    this[SemanticsProperties.Error] = description
}

/**
 * The index of an item identified by a given key. The key is usually defined during the creation of
 * the container. If the key did not match any of the items' keys, the [mapping] must return -1.
 */
fun SemanticsPropertyReceiver.indexForKey(mapping: (Any) -> Int) {
    this[SemanticsProperties.IndexForKey] = mapping
}

/**
 * Limits the number of characters that can be entered, e.g. in an editable text field. By default
 * this value is -1, signifying there is no maximum text length limit.
 */
var SemanticsPropertyReceiver.maxTextLength by SemanticsProperties.MaxTextLength

/** The shape of the UI element. */
var SemanticsPropertyReceiver.shape by SemanticsProperties.Shape

/**
 * The node is marked as a collection of horizontally or vertically stacked selectable elements.
 *
 * Unlike [collectionInfo] which marks a collection of any elements and asks developer to provide
 * all the required information like number of elements etc., this semantics will populate the
 * number of selectable elements automatically. Note that if you use this semantics with lazy
 * collections, it won't get the number of elements in the collection.
 *
 * @see SemanticsPropertyReceiver.selected
 */
fun SemanticsPropertyReceiver.selectableGroup() {
    this[SemanticsProperties.SelectableGroup] = Unit
}

/** Custom actions which are defined by app developers. */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * Action to get a Text/TextField node's [TextLayoutResult]. The result is the first element of
 * layout (the argument of the AccessibilityAction).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetTextLayoutResult] is called.
 */
fun SemanticsPropertyReceiver.getTextLayoutResult(
    label: String? = null,
    action: ((MutableList<TextLayoutResult>) -> Boolean)?,
) {
    this[SemanticsActions.GetTextLayoutResult] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is clicked (single-tapped).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is long clicked (long-pressed).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
fun SemanticsPropertyReceiver.onLongClick(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnLongClick] = AccessibilityAction(label, action)
}

/**
 * Action to asynchronously scroll by a specified amount.
 *
 * [scrollByOffset] should be preferred in most cases, since it is synchronous and returns the
 * amount of scroll that was actually consumed.
 *
 * Expected to be used in conjunction with [verticalScrollAxisRange]/[horizontalScrollAxisRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ScrollBy] is called.
 */
fun SemanticsPropertyReceiver.scrollBy(
    label: String? = null,
    action: ((x: Float, y: Float) -> Boolean)?,
) {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * Action to scroll by a specified amount and return how much of the offset was actually consumed.
 * E.g. if the node can't scroll at all in the given direction, [Offset.Zero] should be returned.
 * The action should not return until the scroll operation has finished.
 *
 * Expected to be used in conjunction with [verticalScrollAxisRange]/[horizontalScrollAxisRange].
 *
 * Unlike [scrollBy], this action is synchronous, and returns the amount of scroll consumed.
 *
 * @param action Action to be performed when [SemanticsActions.ScrollByOffset] is called.
 */
fun SemanticsPropertyReceiver.scrollByOffset(action: suspend (offset: Offset) -> Offset) {
    this[SemanticsActions.ScrollByOffset] = action
}

/**
 * Action to scroll a container to the index of one of its items.
 *
 * The [action] should throw an [IllegalArgumentException] if the index is out of bounds.
 */
fun SemanticsPropertyReceiver.scrollToIndex(label: String? = null, action: (Int) -> Boolean) {
    this[SemanticsActions.ScrollToIndex] = AccessibilityAction(label, action)
}

/**
 * Action to autofill a TextField.
 *
 * Expected to be used in conjunction with [contentType] and [contentDataType] properties.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnAutofillText] is called.
 */
@Deprecated(
    message = "Use onFillData instead",
    replaceWith = ReplaceWith("onFillData"),
    level = DeprecationLevel.WARNING,
)
fun SemanticsPropertyReceiver.onAutofillText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
) {
    @Suppress("DEPRECATION")
    this[SemanticsActions.OnAutofillText] = AccessibilityAction(label, action)
}

/**
 * Action that an autofill service can invoke to fill the component with data.
 *
 * The [action] will be called by the system, passing the [FillableData] that should be used to
 * update the component's state.
 *
 * This is the counterpart to the [fillableData] property, which is used to *provide* the
 * component's current data to the autofill service.
 *
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithFillableDataSemantics
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.OnFillData] is called. The lambda
 *   receives the [FillableData] from the autofill service.
 */
fun SemanticsPropertyReceiver.onFillData(
    label: String? = null,
    action: ((FillableData) -> Boolean)?,
) {
    this[SemanticsActions.OnFillData] = AccessibilityAction(label, action)
}

/**
 * Action to set the current value of the progress bar.
 *
 * Expected to be used in conjunction with progressBarRangeInfo.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(label: String? = null, action: ((Float) -> Boolean)?) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}

/**
 * Action to set the text contents of this node.
 *
 * Expected to be used on editable text fields.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.SetText] is called.
 */
fun SemanticsPropertyReceiver.setText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
) {
    this[SemanticsActions.SetText] = AccessibilityAction(label, action)
}

/**
 * Action to set the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * Note, this action doesn't show the text substitution. Please call
 * [SemanticsPropertyReceiver.showTextSubstitution] to show the text substitution.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.SetTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.setTextSubstitution(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
) {
    this[SemanticsActions.SetTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to show or hide the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * Note, this action only takes effect when the node has the text substitution.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ShowTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.showTextSubstitution(
    label: String? = null,
    action: ((Boolean) -> Boolean)?,
) {
    this[SemanticsActions.ShowTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to clear the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ClearTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.clearTextSubstitution(
    label: String? = null,
    action: (() -> Boolean)?,
) {
    this[SemanticsActions.ClearTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to insert text into this node at the current cursor position, or replacing the selection
 * if text is selected.
 *
 * Expected to be used on editable text fields.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.InsertTextAtCursor] is called.
 */
fun SemanticsPropertyReceiver.insertTextAtCursor(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
) {
    this[SemanticsActions.InsertTextAtCursor] = AccessibilityAction(label, action)
}

/**
 * Action to invoke the IME action handler configured on the node, as well as specify the type of
 * IME action provided by the node.
 *
 * Expected to be used on editable text fields.
 *
 * @param imeActionType The IME type, such as [ImeAction.Next] or [ImeAction.Search]
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.OnImeAction] is called.
 * @see SemanticsProperties.ImeAction
 * @see SemanticsActions.OnImeAction
 */
fun SemanticsPropertyReceiver.onImeAction(
    imeActionType: ImeAction,
    label: String? = null,
    action: (() -> Boolean)?,
) {
    this[SemanticsProperties.ImeAction] = imeActionType
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

// b/322269946
@Suppress("unused")
@Deprecated(
    message = "Use `SemanticsPropertyReceiver.onImeAction` instead.",
    replaceWith =
        ReplaceWith(
            "onImeAction(imeActionType = ImeAction.Default, label = label, action = action)",
            "androidx.compose.ui.semantics.onImeAction",
            "androidx.compose.ui.text.input.ImeAction",
        ),
    level = DeprecationLevel.ERROR,
)
fun SemanticsPropertyReceiver.performImeAction(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

/**
 * Action to set text selection by character index range.
 *
 * If this action is provided, the selection data must be provided using [textSelectionRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetSelection] is called. The
 *   parameters to the action are: `startIndex`, `endIndex`, and whether the indices are relative to
 *   the original text or the transformed text (when a `VisualTransformation` is applied).
 */
fun SemanticsPropertyReceiver.setSelection(
    label: String? = null,
    action: ((startIndex: Int, endIndex: Int, relativeToOriginalText: Boolean) -> Boolean)?,
) {
    this[SemanticsActions.SetSelection] = AccessibilityAction(label, action)
}

/**
 * Action to copy the text to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CopyText] is called.
 */
fun SemanticsPropertyReceiver.copyText(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.CopyText] = AccessibilityAction(label, action)
}

/**
 * Action to cut the text and copy it to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CutText] is called.
 */
fun SemanticsPropertyReceiver.cutText(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.CutText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.PasteText] to the [SemanticsPropertyReceiver]. Use it to
 * indicate that element is open for accepting paste data from the clipboard. There is no need to
 * check if the clipboard data available as this is done by the framework. For this action to be
 * triggered, the element must also have the [SemanticsProperties.Focused] property set.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PasteText] is called.
 * @see focused
 */
fun SemanticsPropertyReceiver.pasteText(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.PasteText] = AccessibilityAction(label, action)
}

/**
 * Action to expand an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Expand] is called.
 */
fun SemanticsPropertyReceiver.expand(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.Expand] = AccessibilityAction(label, action)
}

/**
 * Action to collapse an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Collapse] is called.
 */
fun SemanticsPropertyReceiver.collapse(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.Collapse] = AccessibilityAction(label, action)
}

/**
 * Action to dismiss a dismissible node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Dismiss] is called.
 */
fun SemanticsPropertyReceiver.dismiss(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.Dismiss] = AccessibilityAction(label, action)
}

/**
 * Action that gives input focus to this node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.RequestFocus] is called.
 */
fun SemanticsPropertyReceiver.requestFocus(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.RequestFocus] = AccessibilityAction(label, action)
}

/**
 * Action to page up.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageUp] is called.
 * @see [Role.Carousel] for more information.
 */
fun SemanticsPropertyReceiver.pageUp(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.PageUp] = AccessibilityAction(label, action)
}

/**
 * Action to page down.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageDown] is called.
 * @see [Role.Carousel] for more information.
 */
fun SemanticsPropertyReceiver.pageDown(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.PageDown] = AccessibilityAction(label, action)
}

/**
 * Action to page left.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageLeft] is called.
 * @see [Role.Carousel] for more information.
 */
fun SemanticsPropertyReceiver.pageLeft(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.PageLeft] = AccessibilityAction(label, action)
}

/**
 * Action to page right.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageRight] is called.
 * @see [Role.Carousel] for more information.
 */
fun SemanticsPropertyReceiver.pageRight(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.PageRight] = AccessibilityAction(label, action)
}

/**
 * Action to get a scrollable's active view port amount for scrolling actions.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetScrollViewportLength] is
 *   called.
 */
fun SemanticsPropertyReceiver.getScrollViewportLength(
    label: String? = null,
    action: (() -> Float?),
) {
    this[SemanticsActions.GetScrollViewportLength] =
        AccessibilityAction(label) {
            val viewport = action.invoke()
            if (viewport == null) {
                false
            } else {
                it.add(viewport)
                true
            }
        }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/focus/FocusRequester.kt
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

package androidx.compose.ui.focus

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitChildren

private const val FocusRequesterNotInitialized =
    """
   FocusRequester is not initialized. Here are some possible fixes:

   1. Remember the FocusRequester: val focusRequester = remember { FocusRequester() }
   2. Did you forget to add a Modifier.focusRequester() ?
   3. Are you attempting to request focus during composition? Focus requests should be made in
   response to some event. Eg Modifier.clickable { focusRequester.requestFocus() }
"""

private const val InvalidFocusRequesterInvocation =
    """
    Please check whether the focusRequester is FocusRequester.Cancel or FocusRequester.Default
    before invoking any functions on the focusRequester.
"""

/**
 * The [FocusRequester] is used in conjunction with
 * [Modifier.focusRequester][androidx.compose.ui.focus.focusRequester] to send requests to change
 * focus.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 * @see androidx.compose.ui.focus.focusRequester
 */
@Stable
class FocusRequester @RememberInComposition constructor() {

    internal val focusRequesterNodes: MutableVector<FocusRequesterModifierNode> = mutableVectorOf()

    /**
     * Use this function to request focus. If the system grants focus to a component associated with
     * this [FocusRequester], its [onFocusChanged] modifiers will receive a [FocusState] object
     * where [FocusState.isFocused] is true.
     *
     * @sample androidx.compose.ui.samples.RequestFocusSample
     */
    @Deprecated(
        message = "use the version the has a FocusDirection",
        replaceWith = ReplaceWith("this.requestFocus()"),
        level = DeprecationLevel.HIDDEN,
    )
    fun requestFocus() {
        requestFocus(Enter)
    }

    /**
     * Use this function to request focus with a specific direction. If the system grants focus to a
     * component associated with this [FocusRequester], its [onFocusChanged] modifiers will receive
     * a [FocusState] object where [FocusState.isFocused] is true.
     *
     * @param focusDirection The direction passed to the [FocusTargetModifierNode] to indicate the
     *   direction that the focus request comes from.
     * @return `true` if the focus was successfully requested or `false` if the focus request was
     *   canceled.
     * @sample androidx.compose.ui.samples.RequestFocusSample
     */
    fun requestFocus(focusDirection: FocusDirection = Enter): Boolean {
        return findFocusTarget { it.requestFocus(focusDirection) }
    }

    /**
     * Deny requests to clear focus.
     *
     * Use this function to send a request to capture focus. If a component captures focus, it will
     * send a [FocusState] object to its associated [onFocusChanged] modifiers where
     * [FocusState.isCaptured]() == true.
     *
     * When a component is in a Captured state, all focus requests from other components are
     * declined.
     *
     * @return true if the focus was successfully captured by one of the [focus][focusTarget]
     *   modifiers associated with this [FocusRequester]. False otherwise.
     * @sample androidx.compose.ui.samples.CaptureFocusSample
     */
    fun captureFocus(): Boolean {
        if (focusRequesterNodes.isEmpty()) {
            println("$FocusWarning: $FocusRequesterNotInitialized")
            return false
        }
        focusRequesterNodes.forEach {
            if (it.captureFocus()) {
                return true
            }
        }
        return false
    }

    /**
     * Use this function to send a request to free focus when one of the components associated with
     * this [FocusRequester] is in a Captured state. If a component frees focus, it will send a
     * [FocusState] object to its associated [onFocusChanged] modifiers where
     * [FocusState.isCaptured]() == false.
     *
     * When a component is in a Captured state, all focus requests from other components are
     * declined. .
     *
     * @return true if the captured focus was successfully released. i.e. At the end of this
     *   operation, one of the components associated with this [focusRequester] freed focus.
     * @sample androidx.compose.ui.samples.CaptureFocusSample
     */
    fun freeFocus(): Boolean {
        if (focusRequesterNodes.isEmpty()) {
            println("$FocusWarning: $FocusRequesterNotInitialized")
            return false
        }
        focusRequesterNodes.forEach {
            if (it.freeFocus()) {
                return true
            }
        }
        return false
    }

    /**
     * Use this function to request the focus target to save a reference to the currently focused
     * child in its saved instance state. After calling this, focus can be restored to the saved
     * child by making a call to [restoreFocusedChild].
     *
     * @return true if the focus target associated with this [FocusRequester] has a focused child
     *   and we successfully saved a reference to it.
     * @sample androidx.compose.ui.samples.RestoreFocusSample
     */
    // TODO: Deprecate once focus restoration is enabled by default via flags.
    // @Deprecated(
    //    message =
    //        "The focused child is now saved automatically whenever focus changes. Just call" +
    //            " restoreFocusedChild to restore focus.",
    //    level = DeprecationLevel.WARNING,
    // )
    fun saveFocusedChild(): Boolean {
        if (focusRequesterNodes.isEmpty()) {
            println("$FocusWarning: $FocusRequesterNotInitialized")
            return false
        }
        focusRequesterNodes.forEach { if (it.saveFocusedChild()) return true }
        return false
    }

    /**
     * Use this function to restore focus to one of the children of the node pointed to by this
     * [FocusRequester]. This restores focus to a previously focused child that was saved by using
     * [saveFocusedChild].
     *
     * @return true if we successfully restored focus to one of the children of the [focusTarget]
     *   associated with this [FocusRequester]
     * @sample androidx.compose.ui.samples.RestoreFocusSample
     */
    fun restoreFocusedChild(): Boolean {
        if (focusRequesterNodes.isEmpty()) {
            println("$FocusWarning: $FocusRequesterNotInitialized")
            return false
        }
        var success = false
        focusRequesterNodes.forEach { success = it.restoreFocusedChild() || success }
        return success
    }

    companion object {
        /**
         * Default [focusRequester], which when used in [Modifier.focusProperties][focusProperties]
         * implies that we want to use the default system focus order, that is based on the position
         * of the items on the screen.
         */
        val Default = FocusRequester()

        /**
         * Cancelled [focusRequester], which when used in
         * [Modifier.focusProperties][focusProperties] implies that we want to block focus search
         * from proceeding in the specified [direction][FocusDirection].
         *
         * @sample androidx.compose.ui.samples.CancelFocusMoveSample
         */
        val Cancel = FocusRequester()

        /** Used to indicate that the focus has been redirected during an enter/exit lambda. */
        internal val Redirect = FocusRequester()

        /**
         * Convenient way to create multiple [FocusRequester] instances.
         *
         * @sample androidx.compose.ui.samples.CreateFocusRequesterRefsSample
         */
        object FocusRequesterFactory {
            operator fun component1() = FocusRequester()

            operator fun component2() = FocusRequester()

            operator fun component3() = FocusRequester()

            operator fun component4() = FocusRequester()

            operator fun component5() = FocusRequester()

            operator fun component6() = FocusRequester()

            operator fun component7() = FocusRequester()

            operator fun component8() = FocusRequester()

            operator fun component9() = FocusRequester()

            operator fun component10() = FocusRequester()

            operator fun component11() = FocusRequester()

            operator fun component12() = FocusRequester()

            operator fun component13() = FocusRequester()

            operator fun component14() = FocusRequester()

            operator fun component15() = FocusRequester()

            operator fun component16() = FocusRequester()
        }

        /**
         * Convenient way to create multiple [FocusRequester]s, which can to be used to request
         * focus, or to specify a focus traversal order.
         *
         * @sample androidx.compose.ui.samples.CreateFocusRequesterRefsSample
         */
        fun createRefs(): FocusRequesterFactory = FocusRequesterFactory
    }

    /**
     * This function searches down the hierarchy and calls [onFound] for all focus nodes associated
     * with this [FocusRequester].
     *
     * @param onFound the callback that is run when the child is found.
     * @return false if no focus nodes were found or if the FocusRequester is
     *   [FocusRequester.Cancel]. Returns a logical or of the result of calling [onFound] for each
     *   focus node associated with this [FocusRequester].
     */
    internal inline fun findFocusTarget(onFound: (FocusTargetNode) -> Boolean): Boolean {
        check(this !== Default) { InvalidFocusRequesterInvocation }
        check(this !== Cancel) { InvalidFocusRequesterInvocation }
        if (focusRequesterNodes.isEmpty()) {
            println("$FocusWarning: $FocusRequesterNotInitialized")
            return false
        }
        var success = false
        focusRequesterNodes.forEach { node ->
            node.visitChildren(Nodes.FocusTarget) {
                if (onFound(it)) {
                    success = true
                    return@forEach
                }
            }
        }
        return success
    }
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/focus/FocusManager.kt
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

package androidx.compose.ui.focus

import androidx.compose.ui.internal.JvmDefaultWithCompatibility

@JvmDefaultWithCompatibility
interface FocusManager {
    /**
     * Call this function to clear focus from the currently focused component, and set the focus to
     * the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have any
     *   components that have Captured focus.
     * @sample androidx.compose.ui.samples.ClearFocusSample
     */
    fun clearFocus(force: Boolean = false)

    /**
     * Moves focus in the specified [direction][FocusDirection].
     *
     * If you are not satisfied with the default focus order, consider setting a custom order using
     * [Modifier.focusProperties()][focusProperties].
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     * @sample androidx.compose.ui.samples.MoveFocusSample
     */
    fun moveFocus(focusDirection: FocusDirection): Boolean
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/CompositionLocals.kt
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.Owner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LifecycleOwner

/** The CompositionLocal to provide communication with platform accessibility service. */
val LocalAccessibilityManager = staticCompositionLocalOf<AccessibilityManager?> { null }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg.
 * [Autofill.requestAutofillForNode].
 */
@Deprecated(
    """
        Use the new semantics-based Autofill APIs androidx.compose.ui.autofill.ContentType and
        androidx.compose.ui.autofill.ContentDataType instead.
        """
)
val LocalAutofill = staticCompositionLocalOf<Autofill?> { null }

/**
 * The CompositionLocal that can be used to add [AutofillNode][import
 * androidx.compose.ui.autofill.AutofillNode]s to the autofill tree. The [AutofillTree] is a
 * temporary data structure that will be replaced by Autofill Semantics (b/138604305).
 */
@Deprecated(
    """
        Use the new semantics-based Autofill APIs androidx.compose.ui.autofill.ContentType and
        androidx.compose.ui.autofill.ContentDataType instead.
        """
)
val LocalAutofillTree =
    staticCompositionLocalOf<AutofillTree> { noLocalProvidedFor("LocalAutofillTree") }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg. [AutofillManager.commit].
 */
val LocalAutofillManager =
    staticCompositionLocalOf<AutofillManager?> { noLocalProvidedFor("LocalAutofillManager") }

/** The CompositionLocal to provide communication with platform clipboard service. */
@Deprecated(
    "Use LocalClipboard instead which supports suspend functions",
    ReplaceWith("LocalClipboard", "androidx.compose.ui.platform.LocalClipboard"),
)
val LocalClipboardManager =
    staticCompositionLocalOf<ClipboardManager> { noLocalProvidedFor("LocalClipboardManager") }

/** The CompositionLocal to provide communication with platform clipboard service. */
val LocalClipboard = staticCompositionLocalOf<Clipboard> { noLocalProvidedFor("LocalClipboard") }

/**
 * The CompositionLocal to provide access to a [GraphicsContext] instance for creation of
 * [GraphicsLayer]s.
 *
 * Consumers that access this Local directly and call [GraphicsContext.createGraphicsLayer] are
 * responsible for calling [GraphicsContext.releaseGraphicsLayer].
 *
 * It is recommended that consumers invoke [rememberGraphicsLayer][import
 * androidx.compose.ui.graphics.rememberGraphicsLayer] instead to ensure that a [GraphicsLayer] is
 * released when the corresponding composable is disposed.
 */
val LocalGraphicsContext =
    staticCompositionLocalOf<GraphicsContext> { noLocalProvidedFor("LocalGraphicsContext") }

/**
 * Provides the [Density] to be used to transform between
 * [density-independent pixel units (DP)][androidx.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and pixel units. This is
 * typically used when a [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in
 * the body of [Layout] or [DrawModifier].
 */
val LocalDensity = staticCompositionLocalOf<Density> { noLocalProvidedFor("LocalDensity") }

/** The CompositionLocal that can be used to control focus within Compose. */
val LocalFocusManager =
    staticCompositionLocalOf<FocusManager> { noLocalProvidedFor("LocalFocusManager") }

/** The CompositionLocal to provide platform font loading methods. */
@Suppress("DEPRECATION")
@Deprecated(
    "LocalFontLoader is replaced with LocalFontFamilyResolver",
    replaceWith = ReplaceWith("LocalFontFamilyResolver"),
)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalFontLoader =
    staticCompositionLocalOf<Font.ResourceLoader> { noLocalProvidedFor("LocalFontLoader") }

/** The CompositionLocal for compose font resolution from FontFamily. */
val LocalFontFamilyResolver =
    staticCompositionLocalOf<FontFamily.Resolver> { noLocalProvidedFor("LocalFontFamilyResolver") }

/** The CompositionLocal to provide haptic feedback to the user. */
val LocalHapticFeedback =
    staticCompositionLocalOf<HapticFeedback> { noLocalProvidedFor("LocalHapticFeedback") }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current input
 * mode.
 */
val LocalInputModeManager =
    staticCompositionLocalOf<InputModeManager> { noLocalProvidedFor("LocalInputManager") }

/** The CompositionLocal to provide the layout direction. */
val LocalLayoutDirection =
    staticCompositionLocalOf<LayoutDirection> { noLocalProvidedFor("LocalLayoutDirection") }

/** The providable CompositionLocal to provide the locale list. This list can never be empty. */
@get:VisibleForTesting
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalProvidableLocaleList: ProvidableCompositionLocal<LocaleList> = staticCompositionLocalOf {
    noLocalProvidedFor("LocalProvidableLocaleList")
}

/** The CompositionLocal to provide the locale list. This list will never be empty. */
val LocalLocaleList: CompositionLocal<LocaleList>
    get() = LocalProvidableLocaleList

/** The CompositionLocal to provide the locale. */
val LocalLocale: CompositionLocal<Locale> = compositionLocalWithComputedDefaultOf {
    LocalLocaleList.currentValue.first()
}

/** The CompositionLocal to provide communication with platform text input service. */
@Deprecated("Use PlatformTextInputModifierNode instead.")
val LocalTextInputService = staticCompositionLocalOf<TextInputService?> { null }

/**
 * The [CompositionLocal] to provide a [SoftwareKeyboardController] that can control the current
 * software keyboard.
 *
 * Will be null if the software keyboard cannot be controlled.
 */
val LocalSoftwareKeyboardController = staticCompositionLocalOf<SoftwareKeyboardController?> { null }

/** The CompositionLocal to provide text-related toolbar. */
val LocalTextToolbar =
    staticCompositionLocalOf<TextToolbar> { noLocalProvidedFor("LocalTextToolbar") }

/** The CompositionLocal to provide functionality related to URL, e.g. open URI. */
val LocalUriHandler = staticCompositionLocalOf<UriHandler> { noLocalProvidedFor("LocalUriHandler") }

/** The CompositionLocal that provides the ViewConfiguration. */
val LocalViewConfiguration =
    staticCompositionLocalOf<ViewConfiguration> { noLocalProvidedFor("LocalViewConfiguration") }

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
val LocalWindowInfo = staticCompositionLocalOf<WindowInfo> { noLocalProvidedFor("LocalWindowInfo") }

/** The CompositionLocal containing the current [LifecycleOwner]. */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
expect val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>

internal val LocalPointerIconService = staticCompositionLocalOf<PointerIconService?> { null }

/** @see LocalScrollCaptureInProgress */
internal val LocalProvidableScrollCaptureInProgress = compositionLocalOf { false }

/**
 * True when the system is currently capturing the contents of a scrollable in this compose view or
 * any parent compose view.
 */
val LocalScrollCaptureInProgress: CompositionLocal<Boolean>
    get() = LocalProvidableScrollCaptureInProgress

/**
 * Text cursor blinking
 * - _true_ normal cursor behavior (interactive blink)
 * - _false_ never blink (always on)
 *
 * The default of _true_ is the user-expected system behavior for Text editing.
 *
 * Typically you should not set _false_ outside of screenshot tests without also providing a
 * `cursorBrush` to `BasicTextField` to implement a custom design
 */
val LocalCursorBlinkEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(
    owner: Owner,
    uriHandler: UriHandler,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAccessibilityManager provides owner.accessibilityManager,
        LocalAutofill provides owner.autofill,
        LocalAutofillManager provides owner.autofillManager,
        LocalAutofillTree provides owner.autofillTree,
        LocalClipboardManager provides owner.clipboardManager,
        LocalClipboard provides owner.clipboard,
        LocalDensity provides owner.density,
        LocalFocusManager provides owner.focusOwner,
        @Suppress("DEPRECATION") LocalFontLoader providesDefault
            @Suppress("DEPRECATION") owner.fontLoader,
        LocalFontFamilyResolver providesDefault owner.fontFamilyResolver,
        LocalHapticFeedback provides owner.hapticFeedBack,
        LocalInputModeManager provides owner.inputModeManager,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalTextInputService provides owner.textInputService,
        LocalSoftwareKeyboardController provides owner.softwareKeyboardController,
        LocalTextToolbar provides owner.textToolbar,
        LocalUriHandler provides uriHandler,
        LocalViewConfiguration provides owner.viewConfiguration,
        LocalWindowInfo provides owner.windowInfo,
        LocalPointerIconService provides owner.pointerIconService,
        LocalGraphicsContext provides owner.graphicsContext,
        LocalRetainedValuesStore provides owner.retainedValuesStore,
        LocalProvidableLocaleList provides owner.localeList,
        content = content,
    )
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
```

## File: compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/ComposeUiFlags.kt
```kotlin
/*
 * Copyright 2024 The Android Open Source Project
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
@file:JvmName("ComposeRuntimeFlags")

package androidx.compose.ui

import androidx.compose.ui.node.findNearestAncestor
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * This is a collection of flags which are used to guard against regressions in some of the
 * "riskier" refactors or new feature support that is added to this module. These flags are always
 * "on" in the published artifact of this module, however these flags allow end consumers of this
 * module to toggle them "off" in case this new path is causing a regression.
 *
 * These flags are considered temporary, and there should be no expectation for these flags be
 * around for an extended period of time. If you have a regression that one of these flags fixes, it
 * is strongly encouraged for you to file a bug ASAP.
 *
 * **Usage:**
 *
 * In order to turn a feature off in a debug environment, it is recommended to set this to false in
 * as close to the initial loading of the application as possible. Changing this value after compose
 * library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              ComposeUiFlags.SomeFeatureEnabled = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.ui.ComposeUiFlags {
 *          public static int isRectTrackingEnabled return false
 *      }
 */
@ExperimentalComposeUiApi
object ComposeUiFlags {
    /**
     * This enables fixes for View focus. The changes are large enough to require a flag to allow
     * disabling them.
     */
    // TODO: b/455588830
    @field:Suppress("MutableBareField") @JvmField var isViewFocusFixEnabled: Boolean = false

    /**
     * This flag enables an alternate approach to fixing the issues addressed by the
     * [isViewFocusFixEnabled] flag.
     */
    // TODO: b/455592447
    @field:Suppress("MutableBareField")
    @JvmField
    var isBypassUnfocusableComposeViewEnabled: Boolean = true

    /** Enable initial focus when a focusable is added to a screen with no focusable content. */
    // TODO: b/455601824
    @field:Suppress("MutableBareField")
    @JvmField
    var isInitialFocusOnFocusableAvailable: Boolean = false

    /**
     * Enable focus restoration, by always saving focus. This flag depends on
     * [isInitialFocusOnFocusableAvailable] also being true.
     */
    // TODO: b/485962036
    @field:Suppress("MutableBareField") @JvmField var isFocusRestorationEnabled: Boolean = false

    /** Flag for enabling indirect pointer event navigation gestures in Compose. */
    // TODO: b/455601135
    @field:Suppress("MutableBareField")
    @JvmField
    var isIndirectPointerNavigationGestureDetectorEnabled: Boolean = true

    /** Flag enables optimized focus change dispatching logic. */
    // TODO: b/455603009
    @field:Suppress("MutableBareField")
    @JvmField
    var isOptimizedFocusEventDispatchEnabled: Boolean = true

    /** This flag enables setting the shape semantics property in the graphicsLayer modifiers. */
    // TODO: b/455600081
    @field:Suppress("MutableBareField")
    @JvmField
    var isGraphicsLayerShapeSemanticsEnabled: Boolean = true

    /**
     * Enables a fix where [TraversableNode] traversal method [findNearestAncestor] will take into
     * consideration any delegates that might also be traversable.
     */
    // TODO: b/485962494
    @field:Suppress("MutableBareField")
    @JvmField
    var isTraversableDelegatesFixEnabled: Boolean = true

    /**
     * Enables a change where off-screen children of the partially visible merging nodes (e.g. a
     * Text node of a Button) inside scrollable container are now also reported in the semantics
     * tree for Accessibility needs.
     *
     * Enabled is correct, and it should be enabled in all apps.
     */
    // TODO: b/484259656
    @field:Suppress("MutableBareField")
    @JvmField
    var isAccessibilityShouldIncludeOffscreenChildrenEnabled: Boolean = true

    /**
     * Enables support of trackpad gesture events.
     *
     * If enabled, [androidx.compose.ui.input.pointer.PointerEvent]s can have type of
     * [androidx.compose.ui.input.pointer.PointerEventType.PanMove] and
     * [androidx.compose.ui.input.pointer.PointerEventType.ScaleChange], corresponding to
     * system-recognized gestures on a trackpad.
     *
     * These trackpad gestures will also generally be treated as mouse, with the exact behavior
     * depending on platform specifics.
     */
    // TODO: b/475634969 remove the temporary flag
    @field:Suppress("MutableBareField")
    @JvmField
    var isTrackpadGestureHandlingEnabled: Boolean = true

    /**
     * Enable the integration of [LocalUiMediaScope] at the root compose view which provides various
     * signals for adapting the UI across different devices.
     *
     * This feature is experimental and is disabled by default.
     */
    // TODO: b/485160699 - Remove once the API goes stable
    @field:Suppress("MutableBareField")
    @JvmField
    var isMediaQueryIntegrationEnabled: Boolean = false

    /**
     * Enables hit test to continue searching for "semantic nodes" if the initial node that is hit
     * is unimportant from an accessibility semantics node point of view.
     */
    // TODO: b/487663967
    @field:Suppress("MutableBareField")
    @JvmField
    var isSkipNonImportantSemanticsNodesHitTestEnabled: Boolean = true
}
```

