# Compose Runtime Source Reference

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Composable.kt
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

package androidx.compose.runtime

/**
 * [Composable] functions are the fundamental building blocks of an application built with Compose.
 *
 * [Composable] can be applied to a function or lambda to indicate that the function/lambda can be
 * used as part of a composition to describe a transformation from application data into a tree or
 * hierarchy.
 *
 * Annotating a function or expression with [Composable] changes the type of that function or
 * expression. For example, [Composable] functions can only ever be called from within another
 * [Composable] function. A useful mental model for [Composable] functions is that an implicit
 * "composable context" is passed into a [Composable] function, and is done so implicitly when it is
 * called from within another [Composable] function. This "context" can be used to store information
 * from previous executions of the function that happened at the same logical point of the tree.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    // function declarations
    // @Composable fun Foo() { ... }
    // lambda expressions
    // val foo = @Composable { ... }
    AnnotationTarget.FUNCTION,

    // type declarations
    // var foo: @Composable () -> Unit = { ... }
    // parameter types
    // foo: @Composable () -> Unit
    AnnotationTarget.TYPE,

    // composable types inside of type signatures
    // foo: (@Composable () -> Unit) -> Unit
    AnnotationTarget.TYPE_PARAMETER,

    // composable property getters and setters
    // val foo: Int @Composable get() { ... }
    // var bar: Int
    //   @Composable get() { ... }
    AnnotationTarget.PROPERTY_GETTER,
)
public annotation class Composable
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Composition.kt
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.collection.MutableScatterSet
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.ScatterSet
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.fastForEach
import androidx.compose.runtime.composer.DebugStringFormattable
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.SlotTable
import androidx.compose.runtime.composer.gapbuffer.asGapBufferSlotTable
import androidx.compose.runtime.composer.gapbuffer.changelist.ChangeList
import androidx.compose.runtime.composer.linkbuffer.asLinkBufferSlotTable
import androidx.compose.runtime.internal.AtomicReference
import androidx.compose.runtime.internal.RememberEventDispatcher
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.snapshots.ReaderKind
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.fastAll
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.ObservableComposition

/**
 * A composition object is usually constructed for you, and returned from an API that is used to
 * initially compose a UI. For instance, [setContent] returns a Composition.
 *
 * The [dispose] method should be used when you would like to dispose of the UI and the Composition.
 */
public interface Composition {
    /**
     * Returns true if any pending invalidations have been scheduled. An invalidation is schedule if
     * [RecomposeScope.invalidate] has been called on any composition scopes create for the
     * composition.
     *
     * Modifying [MutableState.value] of a value produced by [mutableStateOf] will automatically
     * call [RecomposeScope.invalidate] for any scope that read [State.value] of the mutable state
     * instance during composition.
     *
     * @see RecomposeScope
     * @see mutableStateOf
     */
    public val hasInvalidations: Boolean

    /** True if [dispose] has been called. */
    public val isDisposed: Boolean

    /**
     * Clear the hierarchy that was created from the composition and release resources allocated for
     * composition. After calling [dispose] the composition will no longer be recomposed and calling
     * [setContent] will throw an [IllegalStateException]. Calling [dispose] is idempotent, all
     * calls after the first are a no-op.
     */
    public fun dispose()

    /**
     * Update the composition with the content described by the [content] composable. After this has
     * been called the changes to produce the initial composition has been calculated and applied to
     * the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the content of the composition.
     * @exception IllegalStateException thrown in the composition has been [dispose]d.
     */
    public fun setContent(content: @Composable () -> Unit)
}

/**
 * A [ReusableComposition] is a [Composition] that can be reused for different composable content.
 *
 * This interface is used by components that have to synchronize lifecycle of parent and child
 * compositions and efficiently reuse the nodes emitted by [ReusableComposeNode].
 */
public sealed interface ReusableComposition : Composition {
    /**
     * Update the composition with the content described by the [content] composable. After this has
     * been called the changes to produce the initial composition has been calculated and applied to
     * the composition.
     *
     * This method forces this composition into "reusing" state before setting content. In reusing
     * state, all remembered content is discarded, and nodes emitted by [ReusableComposeNode] are
     * re-used for the new content. The nodes are only reused if the group structure containing the
     * node matches new content.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the content of the composition.
     * @exception IllegalStateException thrown in the composition has been [dispose]d.
     */
    public fun setContentWithReuse(content: @Composable () -> Unit)

    /**
     * Deactivate all observation scopes in composition and remove all remembered slots while
     * preserving nodes in place. The composition can be re-activated by calling [setContent] with a
     * new content.
     */
    public fun deactivate()
}

/**
 * A key to locate a service using the [CompositionServices] interface optionally implemented by
 * implementations of [Composition].
 */
public interface CompositionServiceKey<T>

/**
 * Allows finding composition services from the runtime. The services requested through this
 * interface are internal to the runtime and cannot be provided directly.
 *
 * The [CompositionServices] interface is used by the runtime to provide optional and/or
 * experimental services through public extension functions.
 *
 * Implementation of [Composition] that delegate to another [Composition] instance should implement
 * this interface and delegate calls to [getCompositionService] to the original [Composition].
 */
public interface CompositionServices {
    /** Find a service of class [T]. */
    public fun <T> getCompositionService(key: CompositionServiceKey<T>): T?
}

/**
 * Find a Composition service.
 *
 * Find services that implement optional and/or experimental services provided through public or
 * experimental extension functions.
 */
internal fun <T> Composition.getCompositionService(key: CompositionServiceKey<T>) =
    (this as? CompositionServices)?.getCompositionService(key)

/**
 * A controlled composition is a [Composition] that can be directly controlled by the caller.
 *
 * This is the interface used by the [Recomposer] to control how and when a composition is
 * invalidated and subsequently recomposed.
 *
 * Normally a composition is controlled by the [Recomposer] but it is often more efficient for tests
 * to take direct control over a composition by calling [ControlledComposition] instead of
 * [Composition].
 *
 * @see ControlledComposition
 */
public sealed interface ControlledComposition : Composition {
    /**
     * True if the composition is actively compositing such as when actively in a call to
     * [composeContent] or [recompose].
     */
    public val isComposing: Boolean

    /**
     * True after [composeContent] or [recompose] has been called and [applyChanges] is expected as
     * the next call. An exception will be throw in [composeContent] or [recompose] is called while
     * there are pending from the previous composition pending to be applied.
     */
    public val hasPendingChanges: Boolean

    /**
     * Called by the parent composition in response to calling [setContent]. After this method the
     * changes should be calculated but not yet applied. DO NOT call this method directly if this is
     * interface is controlled by a [Recomposer], either use [setContent] or
     * [Recomposer.composeInitial] instead.
     *
     * @param content A composable function that describes the tree.
     */
    public fun composeContent(content: @Composable () -> Unit)

    /**
     * Record the values that were modified after the last call to [recompose] or from the initial
     * call to [composeContent]. This should be called before [recompose] is called to record which
     * parts of the composition need to be recomposed.
     *
     * @param values the set of values that have changed since the last composition.
     */
    public fun recordModificationsOf(values: Set<Any>)

    /**
     * Returns true if any of the object instances in [values] is observed by this composition. This
     * allows detecting if values changed by a previous composition will potentially affect this
     * composition.
     */
    public fun observesAnyOf(values: Set<Any>): Boolean

    /**
     * Execute [block] with [isComposing] set temporarily to `true`. This allows treating
     * invalidations reported during [prepareCompose] as if they happened while composing to avoid
     * double invalidations when propagating changes from a parent composition while before
     * composing the child composition.
     */
    public fun prepareCompose(block: () -> Unit)

    /**
     * Record that [value] has been read. This is used primarily by the [Recomposer] to inform the
     * composer when the a [MutableState] instance has been read implying it should be observed for
     * changes.
     *
     * @param value the instance from which a property was read
     */
    public fun recordReadOf(value: Any)

    /**
     * Record that [value] has been modified. This is used primarily by the [Recomposer] to inform
     * the composer when the a [MutableState] instance been change by a composable function.
     */
    public fun recordWriteOf(value: Any)

    /**
     * Recompose the composition to calculate any changes necessary to the composition state and the
     * tree maintained by the applier. No changes have been made yet. Changes calculated will be
     * applied when [applyChanges] is called.
     *
     * @return returns `true` if any changes are pending and [applyChanges] should be called.
     */
    public fun recompose(): Boolean

    /**
     * Insert the given list of movable content with their paired state in potentially a different
     * composition. If the second part of the pair is null then the movable content should be
     * inserted as new. If second part of the pair has a value then the state should be moved into
     * the referenced location and then recomposed there.
     */
    @InternalComposeApi
    public fun insertMovableContent(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    )

    /** Dispose the value state that is no longer needed. */
    @InternalComposeApi public fun disposeUnusedMovableContent(state: MovableContentState)

    /**
     * Apply the changes calculated during [setContent] or [recompose]. If an exception is thrown by
     * [applyChanges] the composition is irreparably damaged and should be [dispose]d.
     */
    public fun applyChanges()

    /**
     * Apply change that must occur after the main bulk of changes have been applied. Late changes
     * are the result of inserting movable content and it must be performed after [applyChanges]
     * because, for content that have moved must be inserted only after it has been removed from the
     * previous location. All deletes must be executed before inserts. To ensure this, all deletes
     * are performed in [applyChanges] and all inserts are performed in [applyLateChanges].
     */
    public fun applyLateChanges()

    /**
     * Call when all changes, including late changes, have been applied. This signals to the
     * composition that any transitory composition state can now be discarded. This is advisory only
     * and a controlled composition will execute correctly when this is not called.
     */
    public fun changesApplied()

    /**
     * Abandon current changes and reset composition state. Called when recomposer cannot proceed
     * with current recomposition loop and needs to reset composition.
     */
    public fun abandonChanges()

    /**
     * Invalidate all invalidation scopes. This is called, for example, by [Recomposer] when the
     * Recomposer becomes active after a previous period of inactivity, potentially missing more
     * granular invalidations.
     */
    public fun invalidateAll()

    /**
     * Throws an exception if the internal state of the composer has been corrupted and is no longer
     * consistent. Used in testing the composer itself.
     */
    @InternalComposeApi public fun verifyConsistent()

    /**
     * Temporarily delegate all invalidations sent to this composition to the [to] composition. This
     * is used when movable content moves between compositions. The recompose scopes are not
     * redirected until after the move occurs during [applyChanges] and [applyLateChanges]. This is
     * used to compose as if the scopes have already been changed.
     */
    public fun <R> delegateInvalidations(
        to: ControlledComposition?,
        groupIndex: Int,
        block: () -> R,
    ): R

    /**
     * Sets the [shouldPause] callback allowing a composition to be pausable if it is not `null`.
     * Setting the callback to `null` disables pausing.
     *
     * @return the previous value of the callback which will be restored once the callback is no
     *   longer needed.
     * @see PausableComposition
     */
    @Suppress("ExecutorRegistration")
    public fun getAndSetShouldPauseCallback(shouldPause: ShouldPauseCallback?): ShouldPauseCallback?
}

/** Utility function to set and restore a should pause callback. */
internal inline fun <R> ControlledComposition.pausable(
    shouldPause: ShouldPauseCallback,
    block: () -> R,
): R {
    val previous = getAndSetShouldPauseCallback(shouldPause)
    return try {
        block()
    } finally {
        getAndSetShouldPauseCallback(previous)
    }
}

/**
 * This method is the way to initiate a composition. [parent] [CompositionContext] can be
 * * provided to make the composition behave as a sub-composition of the parent. If composition does
 * * not have a parent, [Recomposer] instance should be provided.
 *
 * It is important to call [Composition.dispose] when composition is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
public fun Composition(applier: Applier<*>, parent: CompositionContext): Composition =
    CompositionImpl(parent, applier)

/**
 * This method is the way to initiate a reusable composition. [parent] [CompositionContext] can be
 * provided to make the composition behave as a sub-composition of the parent. If composition does
 * not have a parent, [Recomposer] instance should be provided.
 *
 * It is important to call [Composition.dispose] when composition is no longer needed in order to
 * release resources.
 *
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see ReusableComposition
 * @see rememberCompositionContext
 */
public fun ReusableComposition(
    applier: Applier<*>,
    parent: CompositionContext,
): ReusableComposition = CompositionImpl(parent, applier)

/**
 * This method is a way to initiate a composition. Optionally, a [parent] [CompositionContext] can
 * be provided to make the composition behave as a sub-composition of the parent or a [Recomposer]
 * can be provided.
 *
 * A controlled composition allows direct control of the composition instead of it being controlled
 * by the [Recomposer] passed ot the root composition.
 *
 * It is important to call [Composition.dispose] this composer is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
@TestOnly
public fun ControlledComposition(
    applier: Applier<*>,
    parent: CompositionContext,
): ControlledComposition = CompositionImpl(parent, applier)

private val PendingApplyNoModifications = Any()

@OptIn(ExperimentalComposeRuntimeApi::class)
internal val ObservableCompositionServiceKey =
    object : CompositionServiceKey<ObservableComposition> {}

private const val RUNNING = 0
private const val DEACTIVATED = 1
private const val INCONSISTENT = 2
private const val DISPOSED = 3

internal abstract class SlotStorage {
    abstract val isEmpty: Boolean

    /** Clear the content of the slot table. Report removes to the remember manager */
    abstract fun clear(rememberManager: RememberManager)

    /** Tell the slot storage to collect call-by information (used by live-edit) */
    abstract fun collectCalledByInformation()

    /** Tell the slot storage to collect source information (used by tooling) */
    abstract fun collectSourceInformation()

    /** Deactivate all nodes in the storage (used by lazy) */
    abstract fun deactivateAll(rememberManager: RememberManager)

    abstract fun dispose()

    /** Extract one or more states of movable content that is nested in the slot storage */
    abstract fun extractNestedStates(
        applier: Applier<*>,
        references: ObjectList<MovableContentStateReference>,
    ): ScatterMap<MovableContentStateReference, MovableContentState>

    abstract fun disposeUnusedMovableContent(
        rememberManager: RememberManager,
        state: MovableContentState,
    )

    /** Invalidate all scopes in the storage (used by live-edit) */
    abstract fun invalidateAll()

    /** Invalidates all groups with the [target] group key (used by live-edit) */
    abstract fun invalidateGroupsWithKey(target: Int): List<RecomposeScopeImpl>?

    /** Returns true if the recompose scope is in the slot storage */
    abstract fun ownsRecomposeScope(scope: RecomposeScopeImpl): Boolean

    /** Returns true if the group indicated by group owns the recompose scope */
    abstract fun groupContainsAnchor(group: Int, anchor: Anchor): Boolean

    /** Returns true if the [parent] group contains the [child] group */
    abstract fun inGroup(parent: Anchor, child: Anchor): Boolean

    /** Debugging */
    abstract fun toDebugString(): String

    /**
     * Testing. Throws an exception if the slot table is not well-formed. A well-formed slot storage
     * is a slot storage where all the internal invariants hold.
     */
    @TestOnly abstract fun verifyWellFormed()

    @TestOnly abstract fun getSlots(): Iterable<Any?>
}

internal abstract class Changes : DebugStringFormattable() {
    abstract fun clear()

    abstract fun execute(
        slotStorage: SlotStorage,
        applier: Applier<*>,
        rememberManager: RememberManager,
        errorContext: CompositionErrorContextImpl?,
    )

    abstract fun isEmpty(): Boolean

    fun isNotEmpty() = !isEmpty()
}

/**
 * The implementation of the [Composition] interface.
 *
 * @param parent An optional reference to the parent composition.
 * @param applier The applier to use to manage the tree built by the composer.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal class CompositionImpl(
    /**
     * The parent composition from [rememberCompositionContext], for sub-compositions, or the an
     * instance of [Recomposer] for root compositions.
     */
    @get:TestOnly val parent: CompositionContext,

    /** The applier to use to update the tree managed by the composition. */
    private val applier: Applier<*>,
) :
    ControlledComposition,
    ReusableComposition,
    RecomposeScopeOwner,
    CompositionServices,
    PausableComposition,
    ObservableComposition {

    /**
     * `null` if a composition isn't pending to apply. `Set<Any>` or `Array<Set<Any>>` if there are
     * modifications to record [PendingApplyNoModifications] if a composition is pending to apply,
     * no modifications. any set contents will be sent to [recordModificationsOf] after applying
     * changes before releasing [lock]
     */
    private val pendingModifications = AtomicReference<Any?>(null)

    // Held when making changes to self or composer
    private val lock = makeSynchronizedObject()

    /**
     * A set of remember observers that were potentially abandoned between [composeContent] or
     * [recompose] and [applyChanges]. When inserting new content any newly remembered
     * [RememberObserver]s are added to this set and then removed as [RememberObserver.onRemembered]
     * is dispatched. If any are left in this when exiting [applyChanges] they have been abandoned
     * and are sent an [RememberObserver.onAbandoned] notification.
     */
    @Suppress("AsCollectionCall") // Requires iterator API when dispatching abandons
    private val abandonSet = MutableScatterSet<RememberObserver>().asMutableSet()

    /** The slot table is used to store the composition information required for recomposition. */
    @Suppress("MemberVisibilityCanBePrivate") // published as internal
    internal val slotStorage: SlotStorage =
        createSlotStorage().also {
            if (parent.collectingCallByInformation) it.collectCalledByInformation()
            if (parent.collectingSourceInformation) it.collectSourceInformation()
        }

    @OptIn(ExperimentalComposeApi::class)
    private fun createSlotStorage(): SlotStorage =
        if (ComposeRuntimeFlags.isLinkBufferComposerEnabled) {
            androidx.compose.runtime.composer.linkbuffer.SlotTable()
        } else {
            androidx.compose.runtime.composer.gapbuffer.SlotTable()
        }

    /**
     * A map of observable objects to the [RecomposeScope]s that observe the object. If the key
     * object is modified the associated scopes should be invalidated.
     */
    private val observations = ScopeMap<Any, RecomposeScopeImpl>()

    /** Used for testing. Returns the objects that are observed */
    internal val observedObjects
        @TestOnly @Suppress("AsCollectionCall") get() = observations.map.asMap().keys

    /**
     * A set of scopes that were invalidated by a call from [recordModificationsOf]. This set is
     * only used in [addPendingInvalidationsLocked], and is reused between invocations.
     */
    private val invalidatedScopes = MutableScatterSet<RecomposeScopeImpl>()

    /**
     * A set of scopes that were invalidated conditionally (that is they were invalidated by a
     * [derivedStateOf] object) by a call from [recordModificationsOf]. They need to be held in the
     * [observations] map until invalidations are drained for composition as a later call to
     * [recordModificationsOf] might later cause them to be unconditionally invalidated.
     */
    private val conditionallyInvalidatedScopes = MutableScatterSet<RecomposeScopeImpl>()

    /** A map of object read during derived states to the corresponding derived state. */
    private val derivedStates = ScopeMap<Any, DerivedState<*>>()

    /** Used for testing. Returns dependencies of derived states that are currently observed. */
    internal val derivedStateDependencies
        @TestOnly @Suppress("AsCollectionCall") get() = derivedStates.map.asMap().keys

    /** Used for testing. Returns the conditional scopes being tracked by the composer */
    internal val conditionalScopes: List<RecomposeScopeImpl>
        @TestOnly
        @Suppress("AsCollectionCall")
        get() = conditionallyInvalidatedScopes.asSet().toList()

    /**
     * A list of changes calculated by [Composer] to be applied to the [Applier] and the [SlotTable]
     * to reflect the result of composition. This is a list of lambdas that need to be invoked in
     * order to produce the desired effects.
     */
    private val changes = createChangeList()

    /**
     * A list of changes calculated by [Composer] to be applied after all other compositions have
     * had [applyChanges] called. These changes move [MovableContent] state from one composition to
     * another and must be applied after [applyChanges] because [applyChanges] copies and removes
     * the state out of the previous composition so it can be inserted into the new location. As
     * inserts might be earlier in the composition than the position it is deleted, this move must
     * be done in two phases.
     */
    private val lateChanges = createChangeList()

    /**
     * When an observable object is modified during composition any recompose scopes that are
     * observing that object are invalidated immediately. Since they have already been processed
     * there is no need to process them again, so this set maintains a set of the recompose scopes
     * that were already dismissed by composition and should be ignored in the next call to
     * [recordModificationsOf].
     */
    private val observationsProcessed = ScopeMap<Any, RecomposeScopeImpl>()

    /**
     * A map of the invalid [RecomposeScope]s. If this map is non-empty the current state of the
     * composition does not reflect the current state of the objects it observes and should be
     * recomposed by calling [recompose]. Tbe value is a map of values that invalidated the scope.
     * The scope is checked with these instances to ensure the value has changed. This is used to
     * only invalidate the scope if a [derivedStateOf] object changes.
     */
    private var invalidations = ScopeMap<RecomposeScopeImpl, Any>()

    /**
     * As [RecomposeScope]s are removed the corresponding entries in the observations set must be
     * removed as well. This process is expensive so should only be done if it is certain the
     * [observations] set contains [RecomposeScope] that is no longer needed. [pendingInvalidScopes]
     * is set to true whenever a [RecomposeScope] is removed from the [slotStorage].
     */
    @Suppress("MemberVisibilityCanBePrivate") // published as internal
    internal var pendingInvalidScopes = false

    /**
     * If the [shouldPause] callback is set the composition is pausable and should pause whenever
     * the [shouldPause] callback returns `true`.
     */
    private var shouldPause: ShouldPauseCallback? = null

    private var pendingPausedComposition: PausedCompositionImpl? = null

    private var invalidationDelegate: CompositionImpl? = null

    private var invalidationDelegateGroup: Int = 0

    internal val observerHolder = CompositionObserverHolder(parent = parent)

    private val rememberManager = RememberEventDispatcher()

    /** The [Composer] to use to create and update the tree managed by this composition. */
    internal val composer: InternalComposer = createComposer().also { parent.registerComposer(it) }

    @OptIn(ExperimentalComposeApi::class)
    private fun createComposer(): InternalComposer =
        if (ComposeRuntimeFlags.isLinkBufferComposerEnabled) {
            LinkComposer(
                applier = applier,
                parentContext = parent,
                slotTable = slotStorage.asLinkBufferSlotTable(),
                abandonSet = abandonSet,
                changes = changes,
                lateChanges = lateChanges,
                composition = this,
                observerHolder = observerHolder,
            )
        } else {
            GapComposer(
                applier = applier,
                parentContext = parent,
                slotTable = slotStorage.asGapBufferSlotTable(),
                abandonSet = abandonSet,
                changes = changes,
                lateChanges = lateChanges,
                composition = this,
                observerHolder = observerHolder,
            )
        }

    @OptIn(ExperimentalComposeApi::class)
    private fun createChangeList(): Changes =
        if (ComposeRuntimeFlags.isLinkBufferComposerEnabled) {
            androidx.compose.runtime.composer.linkbuffer.changelist.ChangeList()
        } else {
            androidx.compose.runtime.composer.gapbuffer.changelist.ChangeList()
        }

    /** Return true if this is a root (non-sub-) composition. */
    val isRoot: Boolean = parent is Recomposer

    /** True if [dispose] has been called. */
    private var state = RUNNING

    /** True if a sub-composition of this composition is current composing. */
    private val areChildrenComposing
        get() = composer.areChildrenComposing

    /**
     * The [Composable] function used to define the tree managed by this composition. This is set by
     * [setContent].
     */
    var composable: @Composable () -> Unit = {}

    override val isComposing: Boolean
        get() = composer.isComposing

    override val isDisposed: Boolean
        get() = state == DISPOSED

    override val hasPendingChanges: Boolean
        get() = synchronized(lock) { composer.hasPendingChanges }

    override fun setContent(content: @Composable () -> Unit) {
        val wasDeactivated = clearDeactivated()
        ensureRunning()

        if (wasDeactivated) {
            composeInitialWithReuse(content)
        } else {
            composeInitial(content)
        }
    }

    override fun setContentWithReuse(content: @Composable () -> Unit) {
        clearDeactivated()
        ensureRunning()

        composeInitialWithReuse(content)
    }

    override fun setPausableContent(content: @Composable () -> Unit): PausedComposition {
        val wasDeactivated = clearDeactivated()
        return composeInitialPaused(reusable = wasDeactivated, content)
    }

    override fun setPausableContentWithReuse(content: @Composable () -> Unit): PausedComposition {
        clearDeactivated()
        ensureRunning()

        return composeInitialPaused(reusable = true, content)
    }

    internal fun pausedCompositionFinished(ignoreSet: ScatterSet<RememberObserverHolder>?) {
        pendingPausedComposition = null
        if (ignoreSet != null) {
            rememberManager.ignoreForgotten(ignoreSet)
            state = INCONSISTENT
        }
    }

    private fun composeInitial(content: @Composable () -> Unit) {
        this.composable = content
        parent.composeInitial(this, composable)
    }

    private fun composeInitialPaused(
        reusable: Boolean,
        content: @Composable () -> Unit,
    ): PausedComposition {
        checkPrecondition(pendingPausedComposition == null) {
            "A pausable composition is in progress"
        }
        val pausedComposition =
            PausedCompositionImpl(
                composition = this,
                context = parent,
                composer = composer,
                content = content,
                reusable = reusable,
                abandonSet = abandonSet,
                applier = applier,
                lock = lock,
            )
        pendingPausedComposition = pausedComposition
        return pausedComposition
    }

    private fun composeInitialWithReuse(content: @Composable () -> Unit) {
        composer.startReuseFromRoot()
        composeInitial(content)
        composer.endReuseFromRoot()
    }

    private fun ensureRunning() {
        checkPrecondition(state == RUNNING) {
            when (state) {
                INCONSISTENT ->
                    "A previous pausable composition for this composition was cancelled. This " +
                        "composition must be disposed."
                DISPOSED -> "The composition is disposed"
                DEACTIVATED -> "The composition should be activated before setting content."
                else -> "" // Excluded by the precondition check
            }
        }
        checkPrecondition(pendingPausedComposition == null) {
            "A pausable composition is in progress"
        }
    }

    private fun clearDeactivated(): Boolean =
        synchronized(lock) {
            val isDeactivated = state == DEACTIVATED
            if (isDeactivated) {
                state = RUNNING
            }
            isDeactivated
        }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun setObserver(observer: CompositionObserver): CompositionObserverHandle {
        synchronized(lock) {
            observerHolder.observer = observer
            observerHolder.root = true
        }
        return object : CompositionObserverHandle {
            override fun dispose() {
                synchronized(lock) {
                    if (observerHolder.observer == observer) {
                        observerHolder.observer = null
                        observerHolder.root = false
                    }
                }
            }
        }
    }

    fun invalidateGroupsWithKey(key: Int) {
        val scopesToInvalidate = synchronized(lock) { slotStorage.invalidateGroupsWithKey(key) }
        // Calls to invalidate must be performed without the lock as the they may cause the
        // recomposer to take its lock to respond to the invalidation and that takes the locks
        // in the opposite order of composition so if composition begins in another thread taking
        // the recomposer lock with the composer lock held will deadlock.
        val forceComposition =
            scopesToInvalidate == null ||
                scopesToInvalidate.fastAny {
                    it.invalidateForResult(null) == InvalidationResult.IGNORED
                }
        if (forceComposition && composer.forceRecomposeScopes()) {
            parent.invalidate(this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsForCompositionLocked() {
        // Recording modifications may race for lock. If there are pending modifications
        // and we won the lock race, drain them before composing.
        when (val toRecord = pendingModifications.getAndSet(PendingApplyNoModifications)) {
            null -> {
                // Do nothing, just start composing.
            }
            PendingApplyNoModifications -> {
                composeRuntimeError("pending composition has not been applied")
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = true)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = true)
                }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsLocked() {
        when (val toRecord = pendingModifications.getAndSet(null)) {
            PendingApplyNoModifications -> {
                // No work to do
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = false)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = false)
                }
            null -> {
                if (pendingPausedComposition == null)
                    composeImmediateRuntimeError(
                        "calling recordModificationsOf and applyChanges concurrently is not supported"
                    )
                // otherwise, the paused composition may be being resumed concurrently.
            }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    // Drain the modification out of the normal recordModificationsOf(), composition() cycle.
    // This avoids the checks to make sure the two calls are called in order.
    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsOutOfBandLocked() {
        when (val toRecord = pendingModifications.getAndSet(emptySet<Any>())) {
            PendingApplyNoModifications,
            null -> {
                // No work to do
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = false)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = false)
                }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    override fun composeContent(content: @Composable () -> Unit) {
        // TODO: This should raise a signal to any currently running recompose calls
        //   to halt and return
        guardChanges {
            synchronized(lock) {
                drainPendingModificationsForCompositionLocked()
                guardInvalidationsLocked { invalidations ->
                    composer.composeContent(invalidations, content, shouldPause)
                }
            }
        }
    }

    internal fun updateMovingInvalidations() {
        synchronized(lock) {
            drainPendingModificationsOutOfBandLocked()
            guardInvalidationsLocked { invalidations ->
                composer.updateComposerInvalidations(invalidations)
            }
        }
    }

    override fun dispose() {
        synchronized(lock) {
            checkPrecondition(!composer.isComposing) {
                "Composition is disposed while composing. If dispose is triggered by a call in " +
                    "@Composable function, consider wrapping it with SideEffect block."
            }
            if (state != DISPOSED) {
                state = DISPOSED
                composable = {}

                // Changes are deferred if the composition contains movable content that needs
                // to be released. NOTE: Applying these changes leaves the slot table in
                // potentially invalid state. The routine use to produce this change list reuses
                // code that extracts movable content from groups that are being deleted. This code
                // does not bother to correctly maintain the node counts of a group nested groups
                // that are going to be removed anyway so the node counts of the groups affected
                // are might be incorrect after the changes have been applied.
                val deferredChanges = composer.deferredChanges
                if (deferredChanges != null) {
                    applyChangesInLocked(deferredChanges)
                }

                // Dispatch all the `onForgotten` events for object that are no longer part of a
                // composition because this composition is being discarded. It is important that
                // this is done after applying deferred changes above to avoid sending `
                // onForgotten` notification to objects that are still part of movable content that
                // will be moved to a new location.
                val nonEmptySlotTable = !slotStorage.isEmpty
                if (nonEmptySlotTable || abandonSet.isNotEmpty()) {
                    rememberManager.use(abandonSet, composer.errorContext) {
                        if (nonEmptySlotTable) {
                            applier.onBeginChanges()
                            slotStorage.clear(rememberManager)
                            applier.clear()
                            applier.onEndChanges()
                            dispatchRememberObservers()
                        }
                        dispatchAbandons()
                    }
                }
                composer.dispose()
            }
        }
        parent.unregisterComposition(this)
    }

    override val hasInvalidations
        get() = synchronized(lock) { invalidations.size > 0 }

    /**
     * To bootstrap multithreading handling, recording modifications is now deferred between
     * recomposition with changes to apply and the application of those changes.
     * [pendingModifications] will contain a queue of changes to apply once all current changes have
     * been successfully processed. Draining this queue is the responsibility of [recompose] if it
     * would return `false` (changes do not need to be applied) or [applyChanges].
     */
    @Suppress("UNCHECKED_CAST")
    override fun recordModificationsOf(values: Set<Any>) {
        while (true) {
            val old = pendingModifications.get()
            val new: Any =
                when (old) {
                    null,
                    PendingApplyNoModifications -> values
                    is Set<*> -> arrayOf(old, values)
                    is Array<*> -> (old as Array<Set<Any>>) + values
                    else -> error("corrupt pendingModifications: $pendingModifications")
                }
            if (pendingModifications.compareAndSet(old, new)) {
                if (old == null) {
                    synchronized(lock) { drainPendingModificationsLocked() }
                }
                break
            }
        }
    }

    override fun observesAnyOf(values: Set<Any>): Boolean {
        values.fastForEach { value ->
            if (value in observations || value in derivedStates) return true
        }
        return false
    }

    override fun prepareCompose(block: () -> Unit) = composer.prepareCompose(block)

    /**
     * Extract the invalidations that are in the group with the given marker. This is used when
     * movable content is moved between tables and the content was invalidated. This is used to move
     * the invalidations with the content.
     */
    internal fun extractInvalidationsOf(anchor: Anchor): List<Pair<RecomposeScopeImpl, Any>> {
        return if (invalidations.size > 0) {
            val result = mutableListOf<Pair<RecomposeScopeImpl, Any>>()
            val slotStorage = slotStorage
            invalidations.removeIf { scope, value ->
                val scopeAnchor = scope.anchor
                if (scopeAnchor != null && slotStorage.inGroup(anchor, scopeAnchor)) {
                    result.add(scope to value)
                    // Remove the invalidation
                    true
                } else {
                    // Keep the invalidation
                    false
                }
            }
            result
        } else emptyList()
    }

    /**
     * Extract the invalidations that are in the group with the given marker. This is used when
     * movable content is moved between tables and the content was invalidated. This is used to move
     * the invalidations with the content.
     */
    internal inline fun extractInvalidationsOfGroup(
        inGroup: (Anchor) -> Boolean
    ): List<Pair<RecomposeScopeImpl, Any>> {
        return if (invalidations.size > 0) {
            val result = mutableListOf<Pair<RecomposeScopeImpl, Any>>()
            invalidations.removeIf { scope, value ->
                val scopeAnchor = scope.anchor
                if (scopeAnchor != null && inGroup(scopeAnchor)) {
                    result.add(scope to value)

                    // Remove the invalidation
                    true
                } else {
                    // Keep the invalidation
                    false
                }
            }
            result
        } else emptyList()
    }

    private fun addPendingInvalidationsLocked(value: Any, forgetConditionalScopes: Boolean) {
        observations.forEachScopeOf(value) { scope ->
            if (
                !observationsProcessed.remove(value, scope) &&
                    scope.invalidateForResult(value) != InvalidationResult.IGNORED
            ) {
                if (scope.isConditional && !forgetConditionalScopes) {
                    conditionallyInvalidatedScopes.add(scope)
                } else {
                    invalidatedScopes.add(scope)
                }
            }
        }
    }

    private fun addPendingInvalidationsLocked(values: Set<Any>, forgetConditionalScopes: Boolean) {
        values.fastForEach { value ->
            if (value is RecomposeScopeImpl) {
                value.invalidateForResult(null)
            } else {
                addPendingInvalidationsLocked(value, forgetConditionalScopes)
                derivedStates.forEachScopeOf(value) {
                    addPendingInvalidationsLocked(it, forgetConditionalScopes)
                }
            }
        }

        val conditionallyInvalidatedScopes = conditionallyInvalidatedScopes
        val invalidatedScopes = invalidatedScopes
        if (forgetConditionalScopes && conditionallyInvalidatedScopes.isNotEmpty()) {
            observations.removeScopeIf { scope ->
                scope in conditionallyInvalidatedScopes || scope in invalidatedScopes
            }
            conditionallyInvalidatedScopes.clear()
            cleanUpDerivedStateObservations()
        } else if (invalidatedScopes.isNotEmpty()) {
            observations.removeScopeIf { scope -> scope in invalidatedScopes }
            cleanUpDerivedStateObservations()
            invalidatedScopes.clear()
        }
    }

    private fun cleanUpDerivedStateObservations() {
        derivedStates.removeScopeIf { derivedState -> derivedState !in observations }
        if (conditionallyInvalidatedScopes.isNotEmpty()) {
            conditionallyInvalidatedScopes.removeIf { scope -> !scope.isConditional }
        }
    }

    override fun recordReadOf(value: Any) {
        // Not acquiring lock since this happens during composition with it already held
        if (!areChildrenComposing) {
            composer.currentRecomposeScope?.let { scope ->
                scope.used = true

                val alreadyRead = scope.recordRead(value)

                observer()?.onReadInScope(scope, value)

                if (!alreadyRead) {
                    if (value is StateObjectImpl) {
                        value.recordReadIn(ReaderKind.Composition)
                    }

                    observations.add(value, scope)

                    // Record derived state dependency mapping
                    if (value is DerivedState<*>) {
                        val record = value.currentRecord
                        derivedStates.removeScope(value)
                        record.dependencies.forEachKey { dependency ->
                            if (dependency is StateObjectImpl) {
                                dependency.recordReadIn(ReaderKind.Composition)
                            }
                            derivedStates.add(dependency, value)
                        }
                        scope.recordDerivedStateValue(value, record.currentValue)
                    }
                }
            }
        }
    }

    private fun invalidateScopeOfLocked(value: Any) {
        // Invalidate any recompose scopes that read this value.
        observations.forEachScopeOf(value) { scope ->
            if (scope.invalidateForResult(value) == InvalidationResult.IMMINENT) {
                // If we process this during recordWriteOf, ignore it when recording modifications
                observationsProcessed.add(value, scope)
            }
        }
    }

    override fun recordWriteOf(value: Any) =
        synchronized(lock) {
            invalidateScopeOfLocked(value)

            // If writing to dependency of a derived value and the value is changed, invalidate the
            // scopes that read the derived value.
            derivedStates.forEachScopeOf(value) { invalidateScopeOfLocked(it) }
        }

    override fun recompose(): Boolean =
        synchronized(lock) {
            val pendingPausedComposition = pendingPausedComposition
            if (pendingPausedComposition != null && !pendingPausedComposition.isRecomposing) {
                // If the composition is pending do not recompose it now as the recomposition
                // is in the control of the pausable composition and is supposed to happen when
                // the resume is called. However, this may cause the pausable composition to go
                // revert to an incomplete state. If isRecomposing is true then this is being
                // called in resume()
                pendingPausedComposition.markIncomplete()
                pendingPausedComposition.pausableApplier.markRecomposePending()
                return false
            }
            drainPendingModificationsForCompositionLocked()
            guardChanges {
                guardInvalidationsLocked { invalidations ->
                    composer.recompose(invalidations, shouldPause).also { shouldDrain ->
                        // Apply would normally do this for us; do it now if apply shouldn't happen.
                        if (!shouldDrain) drainPendingModificationsLocked()
                    }
                }
            }
        }

    override fun insertMovableContent(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        runtimeCheck(references.fastAll { it.first.composition == this })
        guardChanges { composer.insertMovableContentReferences(references) }
    }

    override fun disposeUnusedMovableContent(state: MovableContentState) {
        rememberManager.use(abandonSet, composer.errorContext) {
            state.slotStorage.disposeUnusedMovableContent(rememberManager, state)
            dispatchRememberObservers()
        }
    }

    private fun applyChangesInLocked(changes: Changes) {
        rememberManager.prepare(abandonSet, composer.errorContext)
        try {
            if (changes.isEmpty()) return
            val applier = pendingPausedComposition?.pausableApplier ?: applier
            val traceName =
                if (applier == pendingPausedComposition?.pausableApplier) {
                    "Compose:recordChanges"
                } else {
                    "Compose:applyChanges"
                }
            trace(traceName) {
                val rememberManager = pendingPausedComposition?.rememberManager ?: rememberManager
                applier.onBeginChanges()

                changes.execute(slotStorage, applier, rememberManager, composer.errorContext)

                applier.onEndChanges()
            }

            // Side effects run after lifecycle observers so that any remembered objects
            // that implement RememberObserver receive onRemembered before a side effect
            // that captured it and operates on it can run.
            rememberManager.dispatchRememberObservers()
            rememberManager.dispatchSideEffects()

            if (pendingInvalidScopes) {
                trace("Compose:unobserve") {
                    pendingInvalidScopes = false
                    observations.removeScopeIf { scope -> !scope.valid }
                    cleanUpDerivedStateObservations()
                }
            }
        } finally {
            // Only dispatch abandons if we do not have any late changes or pending paused
            // compositions. The instances in the abandon set can be remembered in the late changes
            // or when the paused composition is applied.
            try {
                if (this.lateChanges.isEmpty() && pendingPausedComposition == null) {
                    rememberManager.dispatchAbandons()
                }
            } finally {
                rememberManager.clear()
            }
        }
    }

    override fun applyChanges() {
        synchronized(lock) {
            guardChanges {
                applyChangesInLocked(changes)
                drainPendingModificationsLocked()
            }
        }
    }

    override fun applyLateChanges() {
        synchronized(lock) {
            guardChanges {
                if (lateChanges.isNotEmpty()) {
                    applyChangesInLocked(lateChanges)
                }
            }
        }
    }

    override fun changesApplied() {
        synchronized(lock) {
            guardChanges {
                composer.changesApplied()

                // By this time all abandon objects should be notified that they have been
                // abandoned.
                if (this.abandonSet.isNotEmpty()) {
                    rememberManager.use(abandonSet, traceContext = composer.errorContext) {
                        dispatchAbandons()
                    }
                }
            }
        }
    }

    private inline fun <T> guardInvalidationsLocked(
        block: (changes: ScopeMap<RecomposeScopeImpl, Any>) -> T
    ): T {
        val invalidations = takeInvalidations()
        return try {
            block(invalidations)
        } catch (e: Throwable) {
            this.invalidations = invalidations
            throw e
        }
    }

    private inline fun <T> guardChanges(block: () -> T): T =
        try {
            trackAbandonedValues(block)
        } catch (e: Throwable) {
            abandonChanges()
            throw e
        }

    override fun abandonChanges() {
        pendingModifications.set(null)
        changes.clear()
        lateChanges.clear()

        if (abandonSet.isNotEmpty()) {
            rememberManager.use(abandonSet, composer.errorContext) { dispatchAbandons() }
        }
    }

    override fun invalidateAll() {
        slotStorage.invalidateAll()
    }

    override fun verifyConsistent() {
        synchronized(lock) {
            if (!isComposing) {
                composer.verifyConsistent()
                slotStorage.verifyWellFormed()
            }
        }
    }

    override fun <R> delegateInvalidations(
        to: ControlledComposition?,
        groupIndex: Int,
        block: () -> R,
    ): R {
        return if (to != null && to != this && groupIndex >= 0) {
            invalidationDelegate = to as CompositionImpl
            invalidationDelegateGroup = groupIndex
            try {
                block()
            } finally {
                invalidationDelegate = null
                invalidationDelegateGroup = 0
            }
        } else block()
    }

    override fun getAndSetShouldPauseCallback(
        shouldPause: ShouldPauseCallback?
    ): ShouldPauseCallback? {
        val previous = this.shouldPause
        this.shouldPause = shouldPause
        return previous
    }

    override fun invalidate(scope: RecomposeScopeImpl, instance: Any?): InvalidationResult {
        if (scope.defaultsInScope) {
            scope.defaultsInvalid = true
        }
        val anchor = scope.anchor
        if (anchor == null || !anchor.valid)
            return InvalidationResult.IGNORED // The scope was removed from the composition
        if (!slotStorage.ownsRecomposeScope(scope)) {
            // The scope might be owned by the delegate
            val delegate = synchronized(lock) { invalidationDelegate }
            if (delegate?.tryImminentInvalidation(scope, instance) == true)
                return InvalidationResult.IMMINENT // The scope was owned by the delegate

            return InvalidationResult.IGNORED // The scope has not yet entered the composition
        }
        if (!scope.canRecompose)
            return InvalidationResult.IGNORED // The scope isn't able to be recomposed/invalidated
        return invalidateChecked(scope, anchor, instance).also {
            if (it != InvalidationResult.IGNORED) {
                observer()?.onScopeInvalidated(scope, instance)
            }
        }
    }

    override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
        pendingInvalidScopes = true

        observer()?.onScopeDisposed(scope)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCompositionService(key: CompositionServiceKey<T>): T? =
        if (key == ObservableCompositionServiceKey) this as T else null

    private fun tryImminentInvalidation(scope: RecomposeScopeImpl, instance: Any?): Boolean =
        isComposing && composer.tryImminentInvalidation(scope, instance)

    private fun invalidateChecked(
        scope: RecomposeScopeImpl,
        anchor: Anchor,
        instance: Any?,
    ): InvalidationResult {
        val delegate =
            synchronized(lock) {
                val delegate =
                    invalidationDelegate?.let { changeDelegate ->
                        // Invalidations are delegated when recomposing changes to movable content
                        // that is destined to be moved. The movable content is composed in the
                        // destination composer but all the recompose scopes point the current
                        // composer and will arrive here. this redirects the invalidations that
                        // will be moved to the destination composer instead of recording an
                        // invalid invalidation in the from composer.
                        if (slotStorage.groupContainsAnchor(invalidationDelegateGroup, anchor)) {
                            changeDelegate
                        } else null
                    }
                if (delegate == null) {
                    if (tryImminentInvalidation(scope, instance)) {
                        // The invalidation was redirected to the composer.
                        return InvalidationResult.IMMINENT
                    }

                    // Observer requires a map of scope -> states, so we have to fill it if observer
                    // is set.
                    if (instance == null) {
                        // invalidations[scope] containing ScopeInvalidated means it was invalidated
                        // unconditionally.
                        invalidations.set(scope, ScopeInvalidated)
                    } else if (instance !is DerivedState<*>) {
                        // If observer is not set, we only need to add derived states to
                        // invalidation, as regular states are always going to invalidate.
                        invalidations.set(scope, ScopeInvalidated)
                    } else {
                        if (!invalidations.anyScopeOf(scope) { it === ScopeInvalidated }) {
                            invalidations.add(scope, instance)
                        }
                    }
                }
                delegate
            }

        // We call through the delegate here to ensure we don't nest synchronization scopes.
        if (delegate != null) {
            return delegate.invalidateChecked(scope, anchor, instance)
        }
        parent.invalidate(this)
        return if (isComposing) InvalidationResult.DEFERRED else InvalidationResult.SCHEDULED
    }

    internal fun removeObservation(instance: Any, scope: RecomposeScopeImpl) {
        observations.remove(instance, scope)
    }

    internal fun removeDerivedStateObservation(state: DerivedState<*>) {
        // remove derived state if it is not observed in other scopes
        if (state !in observations) {
            derivedStates.removeScope(state)
        }
    }

    /**
     * This takes ownership of the current invalidations and sets up a new array map to hold the new
     * invalidations.
     */
    private fun takeInvalidations(): ScopeMap<RecomposeScopeImpl, Any> {
        val invalidations = invalidations
        this.invalidations = ScopeMap()
        return invalidations
    }

    private inline fun <T> trackAbandonedValues(block: () -> T): T {
        var success = false
        return try {
            block().also { success = true }
        } finally {
            if (!success && abandonSet.isNotEmpty()) {
                rememberManager.use(abandonSet, composer.errorContext) { dispatchAbandons() }
            }
        }
    }

    private fun observer(): CompositionObserver? = observerHolder.current()

    override fun deactivate() {
        synchronized(lock) {
            checkPrecondition(pendingPausedComposition == null) {
                "Deactivate is not supported while pausable composition is in progress"
            }
            val nonEmptySlotTable = !slotStorage.isEmpty
            if (nonEmptySlotTable || abandonSet.isNotEmpty()) {
                trace("Compose:deactivate") {
                    rememberManager.use(abandonSet, composer.errorContext) {
                        if (nonEmptySlotTable) {
                            applier.onBeginChanges()
                            slotStorage.deactivateAll(rememberManager)
                            applier.onEndChanges()
                            dispatchRememberObservers()
                        }
                        dispatchAbandons()
                    }
                }
            }
            observations.clear()
            derivedStates.clear()
            invalidations.clear()
            changes.clear()
            lateChanges.clear()
            composer.deactivate()

            state = DEACTIVATED
        }
    }

    // This is only used in tests to ensure the stacks do not silently leak.
    internal fun composerStacksSizes(): Int = composer.stacksSize()
}

internal object ScopeInvalidated

@OptIn(ExperimentalComposeRuntimeApi::class)
internal class CompositionObserverHolder(
    var observer: CompositionObserver? = null,
    var root: Boolean = false,
    private val parent: CompositionContext,
) {
    fun current(): CompositionObserver? {
        return if (root) {
            observer
        } else {
            val parentHolder = parent.observerHolder
            val parentObserver = parentHolder?.observer
            if (parentObserver != observer) {
                observer = parentObserver
            }
            parentObserver
        }
    }
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/CompositionLocal.kt
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

package androidx.compose.runtime

/**
 * Compose passes data through the composition tree explicitly through means of parameters to
 * composable functions. This is often times the simplest and best way to have data flow through the
 * tree.
 *
 * Sometimes this model can be cumbersome or break down for data that is needed by lots of
 * components, or when components need to pass data between one another but keep that implementation
 * detail private. For these cases, [CompositionLocal]s can be used as an implicit way to have data
 * flow through a composition.
 *
 * [CompositionLocal]s by their nature are hierarchical. They make sense when the value of the
 * [CompositionLocal] needs to be scoped to a particular sub-hierarchy of the composition.
 *
 * One must create a [CompositionLocal] instance, which can be referenced by the consumers
 * statically. [CompositionLocal] instances themselves hold no data, and can be thought of as a
 * type-safe identifier for the data being passed down a tree. [CompositionLocal] factory functions
 * take a single parameter: a factory to create a default value in cases where a [CompositionLocal]
 * is used without a Provider. If this is a situation you would rather not handle, you can throw an
 * error in this factory.
 *
 * @sample androidx.compose.runtime.samples.createCompositionLocal
 *
 * Somewhere up the tree, a [CompositionLocalProvider] component can be used, which provides a value
 * for the [CompositionLocal]. This would often be at the "root" of a tree, but could be anywhere,
 * and can also be used in multiple places to override the provided value for a sub-tree.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 *
 * Intermediate components do not need to know about the [CompositionLocal] value, and can have zero
 * dependencies on it. For example, `SomeScreen` might look like this:
 *
 * @sample androidx.compose.runtime.samples.someScreenSample
 *
 * Finally, a component that wishes to consume the [CompositionLocal] value can use the [current]
 * property of the [CompositionLocal] key which returns the current value of the [CompositionLocal],
 * and subscribes the component to changes of it.
 *
 * @sample androidx.compose.runtime.samples.consumeCompositionLocal
 */
@Stable
public sealed class CompositionLocal<T>(defaultFactory: () -> T) {
    internal open val defaultValueHolder: ValueHolder<T> = LazyValueHolder(defaultFactory)

    internal abstract fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?,
    ): ValueHolder<T>

    /**
     * Return the value provided by the nearest [CompositionLocalProvider] component that invokes,
     * directly or indirectly, the composable function that uses this property.
     *
     * @sample androidx.compose.runtime.samples.consumeCompositionLocal
     */
    @OptIn(InternalComposeApi::class)
    public inline val current: T
        @ReadOnlyComposable @Composable get() = currentComposer.consume(this)
}

/**
 * A [ProvidableCompositionLocal] can be used in [CompositionLocalProvider] to provide values.
 *
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 * @see CompositionLocal
 * @see CompositionLocalProvider
 */
@Stable
public abstract class ProvidableCompositionLocal<T> internal constructor(defaultFactory: () -> T) :
    CompositionLocal<T>(defaultFactory) {
    internal abstract fun defaultProvidedValue(value: T): ProvidedValue<T>

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider].
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    public infix fun provides(value: T): ProvidedValue<T> = defaultProvidedValue(value)

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider] if the
     * key does not already have an associated value.
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    public infix fun providesDefault(value: T): ProvidedValue<T> =
        defaultProvidedValue(value).ifNotAlreadyProvided()

    /**
     * Associates a [CompositionLocal] key to a lambda, [compute], in a call to [CompositionLocal].
     * The [compute] lambda is invoked whenever the key is retrieved. The lambda is executed in the
     * context of a [CompositionLocalContext] which allow retrieving the current values of other
     * composition locals by calling [CompositionLocalAccessorScope.currentValue], which is an
     * extension function provided by the context for a [CompositionLocal] key.
     *
     * The lambda passed to [providesComputed] will be invoked every time the
     * [CompositionLocal.current] is evaluated for the composition local and computes its value
     * based on the current value of the locals referenced in the lambda at the time
     * [CompositionLocal.current] is evaluated. This allows providing values that can be derived
     * from other locals. For example, if accent colors can be calculated from a single base color,
     * the accent colors can be provided as computed composition locals. Providing a new base color
     * would automatically update all the accent colors.
     *
     * @sample androidx.compose.runtime.samples.compositionLocalProvidedComputed
     * @sample androidx.compose.runtime.samples.compositionLocalComputedAfterProvidingLocal
     * @see CompositionLocal
     * @see CompositionLocalContext
     * @see ProvidableCompositionLocal
     */
    public infix fun providesComputed(
        compute: CompositionLocalAccessorScope.() -> T
    ): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = this,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = null,
            compute = compute,
            isDynamic = false,
        )

    override fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?,
    ): ValueHolder<T> {
        return when (previous) {
            is DynamicValueHolder ->
                if (value.isDynamic) {
                    previous.state.value = value.effectiveValue
                    previous
                } else null
            is StaticValueHolder ->
                if (value.isStatic && value.effectiveValue == previous.value) previous else null
            is ComputedValueHolder -> if (value.compute === previous.compute) previous else null
            else -> null
        } ?: valueHolderOf(value)
    }

    private fun valueHolderOf(value: ProvidedValue<T>): ValueHolder<T> =
        when {
            value.isDynamic ->
                DynamicValueHolder(
                    value.state
                        ?: mutableStateOf(
                            value.value,
                            value.mutationPolicy ?: structuralEqualityPolicy(),
                        )
                )
            value.compute != null -> ComputedValueHolder(value.compute)
            value.state != null -> DynamicValueHolder(value.state)
            else -> StaticValueHolder(value.effectiveValue)
        }
}

/**
 * A [DynamicProvidableCompositionLocal] is a [CompositionLocal] backed by [mutableStateOf].
 * Providing new values using a [DynamicProvidableCompositionLocal] will provide the same [State]
 * with a different value. Reading the [CompositionLocal] value of a
 * [DynamicProvidableCompositionLocal] will record a read in the [RecomposeScope] of the
 * composition. Changing the provided value will invalidate the [RecomposeScope]s.
 *
 * @see compositionLocalOf
 */
internal class DynamicProvidableCompositionLocal<T>(
    private val policy: SnapshotMutationPolicy<T>,
    defaultFactory: () -> T,
) : ProvidableCompositionLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = policy,
            state = null,
            compute = null,
            isDynamic = true,
        )
}

/**
 * A [StaticProvidableCompositionLocal] is a value that is expected to rarely change.
 *
 * @see staticCompositionLocalOf
 */
internal class StaticProvidableCompositionLocal<T>(defaultFactory: () -> T) :
    ProvidableCompositionLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = false,
        )
}

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider]. Changing
 * the value provided during recomposition will invalidate the content of [CompositionLocalProvider]
 * that read the value using [CompositionLocal.current].
 *
 * [compositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a call to
 * [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made public as
 * [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param policy a policy to determine when a [CompositionLocal] is considered changed. See
 *   [SnapshotMutationPolicy] for details.
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 *   factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 *   of the component using [CompositionLocal.current]. If no reasonable default can be provided
 *   then consider throwing an exception.
 * @see CompositionLocal
 * @see staticCompositionLocalOf
 * @see mutableStateOf
 */
public fun <T> compositionLocalOf(
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    defaultFactory: () -> T,
): ProvidableCompositionLocal<T> = DynamicProvidableCompositionLocal(policy, defaultFactory)

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider].
 *
 * Unlike [compositionLocalOf], reads of a [staticCompositionLocalOf] are not tracked by the
 * composer and changing the value provided in the [CompositionLocalProvider] call will cause the
 * entirety of the content to be recomposed instead of just the places where in the composition the
 * local value is used. This lack of tracking, however, makes a [staticCompositionLocalOf] more
 * efficient when the value provided is highly unlikely to or will never change. For example, the
 * android context, font loaders, or similar shared values, are unlikely to change for the
 * components in the content of a the [CompositionLocalProvider] and should consider using a
 * [staticCompositionLocalOf]. A color, or other theme like value, might change or even be animated
 * therefore a [compositionLocalOf] should be used.
 *
 * [staticCompositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a call
 * to [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made public as
 * [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 *   factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 *   of the component using [CompositionLocal.current]. If no reasonable default can be provided
 *   then consider throwing an exception.
 * @see CompositionLocal
 * @see compositionLocalOf
 */
public fun <T> staticCompositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> =
    StaticProvidableCompositionLocal(defaultFactory)

/**
 * Create a [CompositionLocal] that behaves like it was provided using
 * [ProvidableCompositionLocal.providesComputed] by default. If a value is provided using
 * [ProvidableCompositionLocal.provides] it behaves as if the [CompositionLocal] was produced by
 * calling [compositionLocalOf].
 *
 * In other words, a [CompositionLocal] produced by can be provided identically to
 * [CompositionLocal] created with [compositionLocalOf] with the only difference is how it behaves
 * when the value is not provided. For a [compositionLocalOf] the default value is returned. If no
 * default value has be computed for [CompositionLocal] the default computation is called.
 *
 * The lambda passed to [compositionLocalWithComputedDefaultOf] will be invoked every time the
 * [CompositionLocal.current] is evaluated for the composition local and computes its value based on
 * the current value of the locals referenced in the lambda at the time [CompositionLocal.current]
 * is evaluated. This allows providing values that can be derived from other locals. For example, if
 * accent colors can be calculated from a single base color, the accent colors can be provided as
 * computed composition locals. Providing a new base color would automatically update all the accent
 * colors.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalComputedByDefault
 * @sample androidx.compose.runtime.samples.compositionLocalComputedAfterProvidingLocal
 * @param defaultComputation the default computation to use when this [CompositionLocal] is not
 *   provided.
 * @see CompositionLocal
 * @see ProvidableCompositionLocal
 */
public fun <T> compositionLocalWithComputedDefaultOf(
    defaultComputation: CompositionLocalAccessorScope.() -> T
): ProvidableCompositionLocal<T> = ComputedProvidableCompositionLocal(defaultComputation)

internal class ComputedProvidableCompositionLocal<T>(
    defaultComputation: CompositionLocalAccessorScope.() -> T
) : ProvidableCompositionLocal<T>({ composeRuntimeError("Unexpected call to default provider") }) {
    override val defaultValueHolder = ComputedValueHolder(defaultComputation)

    override fun defaultProvidedValue(value: T): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = true,
        )
}

/**
 * Creates a [ProvidableCompositionLocal] where the default value is resolved by querying the
 * [LocalHostDefaultProvider] with the given [key].
 *
 * If a value is provided using [ProvidableCompositionLocal.provides], this behaves identically to a
 * [CompositionLocal] created with [compositionLocalOf].
 *
 * When no value is provided, the default value is resolved by querying the
 * [LocalHostDefaultProvider] currently present in the composition. This mechanism allows the
 * default value to be determined dynamically by the hosting environment (such as an Android View)
 * rather than being hardcoded or requiring an explicit provider at the root of the composition.
 *
 * This effectively acts as a bridge, decoupling the definition of the [CompositionLocal] from the
 * platform-specific logic required to resolve its default. For example, a
 * `LocalViewModelStoreOwner` can use this to ask the host for the owner without having a direct
 * dependency on the Android View system.
 *
 * @param key An opaque key used to identify the requested value within the host's context. The type
 *   and meaning of the key are defined by the [HostDefaultProvider] implementation (e.g., on
 *   Android, this is typically a Resource ID).
 * @throws NullPointerException If the host cannot find a value for [key], it returns `null`. If [T]
 *   is a non-nullable type (e.g., `compositionLocalWithHostDefaultOf<String>`), a missing key will
 *   result in a [NullPointerException] when the value is accessed.
 */
public fun <T> compositionLocalWithHostDefaultOf(
    key: HostDefaultKey<T>
): ProvidableCompositionLocal<T> = compositionLocalWithComputedDefaultOf {
    LocalHostDefaultProvider.currentValue.getHostDefault(key)
}

public interface CompositionLocalAccessorScope {
    /**
     * An extension property that allows accessing the current value of a composition local in the
     * context of this scope. This scope is the type of the `this` parameter when in a computed
     * composition. Computed composition locals can be provided by either using
     * [compositionLocalWithComputedDefaultOf] or by using the
     * [ProvidableCompositionLocal.providesComputed] infix operator.
     *
     * @sample androidx.compose.runtime.samples.compositionLocalProvidedComputed
     * @see ProvidableCompositionLocal
     * @see ProvidableCompositionLocal.providesComputed
     * @see ProvidableCompositionLocal.provides
     * @see CompositionLocalProvider
     */
    public val <T> CompositionLocal<T>.currentValue: T
}

/**
 * Stores [CompositionLocal]'s and their values.
 *
 * Can be obtained via [currentCompositionLocalContext] and passed to another composition via
 * [CompositionLocalProvider].
 *
 * [CompositionLocalContext] is immutable and won't be changed after its obtaining.
 */
@Stable
public class CompositionLocalContext
internal constructor(internal val compositionLocals: PersistentCompositionLocalMap) {
    override fun equals(other: Any?): Boolean {
        return other is CompositionLocalContext && other.compositionLocals == this.compositionLocals
    }

    override fun hashCode(): Int {
        return compositionLocals.hashCode()
    }
}

/**
 * [CompositionLocalProvider] binds values to [ProvidableCompositionLocal] keys. Reading the
 * [CompositionLocal] using [CompositionLocal.current] will return the value provided in
 * [CompositionLocalProvider]'s [values] parameter for all composable functions called directly or
 * indirectly in the [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
@OptIn(InternalComposeApi::class)
@NonSkippableComposable
public fun CompositionLocalProvider(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    currentComposer.startProviders(values)
    content()
    currentComposer.endProviders()
}

/**
 * [CompositionLocalProvider] binds value to [ProvidableCompositionLocal] key. Reading the
 * [CompositionLocal] using [CompositionLocal.current] will return the value provided in
 * [CompositionLocalProvider]'s [value] parameter for all composable functions called directly or
 * indirectly in the [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
@OptIn(InternalComposeApi::class)
@NonSkippableComposable
public fun CompositionLocalProvider(value: ProvidedValue<*>, content: @Composable () -> Unit) {
    currentComposer.startProvider(value)
    content()
    currentComposer.endProvider()
}

/**
 * [CompositionLocalProvider] binds values to [CompositionLocal]'s, provided by [context]. Reading
 * the [CompositionLocal] using [CompositionLocal.current] will return the value provided in values
 * stored inside [context] for all composable functions called directly or indirectly in the
 * [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
public fun CompositionLocalProvider(
    context: CompositionLocalContext,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        *context.compositionLocals.map { it.value.toProvided(it.key) }.toTypedArray(),
        content = content,
    )
}

/**
 * [withCompositionLocal] binds value to [ProvidableCompositionLocal] key and returns the result
 * produced by the [content] lambda. Use with non-unit returning [content] lambdas or else use
 * [CompositionLocalProvider]. Reading the [CompositionLocal] using [CompositionLocal.current] will
 * return the value provided in [CompositionLocalProvider]'s [value] parameter for all composable
 * functions called directly or indirectly in the [content] lambda.
 *
 * @see CompositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Suppress("BanInlineOptIn") // b/430604046 - These APIs are stable so are ok to inline
@OptIn(InternalComposeApi::class)
@Composable
public inline fun <T> withCompositionLocal(
    value: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProvider(value)
    return content().also { currentComposer.endProvider() }
}

/**
 * [withCompositionLocals] binds values to [ProvidableCompositionLocal] key and returns the result
 * produced by the [content] lambda. Use with non-unit returning [content] lambdas or else use
 * [CompositionLocalProvider]. Reading the [CompositionLocal] using [CompositionLocal.current] will
 * return the values provided in [CompositionLocalProvider]'s [values] parameter for all composable
 * functions called directly or indirectly in the [content] lambda.
 *
 * @see CompositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Suppress("BanInlineOptIn") // b/430604046 - These APIs are stable so are ok to inline
@OptIn(InternalComposeApi::class)
@Composable
public inline fun <T> withCompositionLocals(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProviders(values)
    return content().also { currentComposer.endProvider() }
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/CompositionContext.kt
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

package androidx.compose.runtime

import androidx.collection.ScatterSet
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.tooling.CompositionData
import kotlin.coroutines.CoroutineContext

private val EmptyPersistentCompositionLocalMap: PersistentCompositionLocalMap =
    persistentCompositionLocalHashMapOf()

/**
 * A [CompositionContext] is an opaque type that is used to logically "link" two compositions
 * together. The [CompositionContext] instance represents a reference to the "parent" composition in
 * a specific position of that composition's tree, and the instance can then be given to a new
 * "child" composition. This reference ensures that invalidations and [CompositionLocal]s flow
 * logically through the two compositions as if they were not separate.
 *
 * The "parent" of a root composition is a [Recomposer].
 *
 * @see rememberCompositionContext
 */
@OptIn(InternalComposeApi::class, ExperimentalComposeRuntimeApi::class)
public abstract class CompositionContext internal constructor() {
    internal abstract val compositeKeyHashCode: CompositeKeyHashCode
    internal abstract val collectingParameterInformation: Boolean
    internal abstract val collectingSourceInformation: Boolean
    internal abstract val collectingCallByInformation: Boolean
    internal abstract val stackTraceEnabled: Boolean
    internal open val observerHolder: CompositionObserverHolder?
        get() = null

    /** The [CoroutineContext] with which effects for the composition will be executed in. */
    public abstract val effectCoroutineContext: CoroutineContext

    /** Associated composition if one exists. */
    internal abstract val composition: Composition?

    internal abstract fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit,
    )

    internal abstract fun composeInitialPaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        content: @Composable () -> Unit,
    ): ScatterSet<RecomposeScopeImpl>

    internal abstract fun recomposePaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        invalidScopes: ScatterSet<RecomposeScopeImpl>,
    ): ScatterSet<RecomposeScopeImpl>

    internal abstract fun reportPausedScope(scope: RecomposeScopeImpl)

    internal abstract fun invalidate(composition: ControlledComposition)

    internal abstract fun invalidateScope(scope: RecomposeScopeImpl)

    internal open fun recordInspectionTable(table: MutableSet<CompositionData>) {}

    internal open fun registerComposer(composer: Composer) {}

    internal open fun unregisterComposer(composer: Composer) {}

    internal abstract fun registerComposition(composition: ControlledComposition)

    internal abstract fun unregisterComposition(composition: ControlledComposition)

    internal open fun getCompositionLocalScope(): PersistentCompositionLocalMap =
        EmptyPersistentCompositionLocalMap

    internal open fun startComposing() {}

    internal open fun doneComposing() {}

    internal abstract fun insertMovableContent(reference: MovableContentStateReference)

    internal abstract fun deletedMovableContent(reference: MovableContentStateReference)

    internal abstract fun movableContentStateReleased(
        reference: MovableContentStateReference,
        data: MovableContentState,
        applier: Applier<*>,
    )

    internal open fun movableContentStateResolve(
        reference: MovableContentStateReference
    ): MovableContentState? = null

    internal abstract fun reportRemovedComposition(composition: ControlledComposition)

    public abstract fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Composer.kt
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

@file:OptIn(InternalComposeApi::class)
@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.runtime

import androidx.compose.runtime.Composer.Companion.Empty
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.SlotReader
import androidx.compose.runtime.composer.gapbuffer.SlotTable
import androidx.compose.runtime.composer.gapbuffer.SlotWriter
import androidx.compose.runtime.composer.gapbuffer.asGapAnchor
import androidx.compose.runtime.tooling.ComposeStackTrace
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

/**
 * Internal compose compiler plugin API that is used to update the function the composer will call
 * to recompose a recomposition scope. This should not be used or called directly.
 */
@ComposeCompilerApi
public interface ScopeUpdateScope {
    /**
     * Called by generated code to update the recomposition scope with the function to call
     * recompose the scope. This is called by code generated by the compose compiler plugin and
     * should not be called directly.
     */
    public fun updateScope(block: (Composer, Int) -> Unit)
}

internal enum class InvalidationResult {
    /**
     * The invalidation was ignored because the associated recompose scope is no longer part of the
     * composition or has yet to be entered in the composition. This could occur for invalidations
     * called on scopes that are no longer part of composition or if the scope was invalidated
     * before [ControlledComposition.applyChanges] was called that will enter the scope into the
     * composition.
     */
    IGNORED,

    /**
     * The composition is not currently composing and the invalidation was recorded for a future
     * composition. A recomposition requested to be scheduled.
     */
    SCHEDULED,

    /**
     * The composition that owns the recompose scope is actively composing but the scope has already
     * been composed or is in the process of composing. The invalidation is treated as SCHEDULED
     * above.
     */
    DEFERRED,

    /**
     * The composition that owns the recompose scope is actively composing and the invalidated scope
     * has not been composed yet but will be recomposed before the composition completes. A new
     * recomposition was not scheduled for this invalidation.
     */
    IMMINENT,
}

/**
 * An instance to hold a value provided by [CompositionLocalProvider] and is created by the
 * [ProvidableCompositionLocal.provides] infix operator. If [canOverride] is `false`, the provided
 * value will not overwrite a potentially already existing value in the scope.
 *
 * This value cannot be created directly. It can only be created by using one of the `provides`
 * operators of [ProvidableCompositionLocal].
 *
 * @see ProvidableCompositionLocal.provides
 * @see ProvidableCompositionLocal.providesDefault
 * @see ProvidableCompositionLocal.providesComputed
 */
public class ProvidedValue<T>
internal constructor(
    /**
     * The composition local that is provided by this value. This is the left-hand side of the
     * [ProvidableCompositionLocal.provides] infix operator.
     */
    public val compositionLocal: CompositionLocal<T>,
    value: T?,
    private val explicitNull: Boolean,
    internal val mutationPolicy: SnapshotMutationPolicy<T>?,
    internal val state: MutableState<T>?,
    internal val compute: (CompositionLocalAccessorScope.() -> T)?,
    internal val isDynamic: Boolean,
) {
    private val providedValue: T? = value

    /**
     * The value provided by the [ProvidableCompositionLocal.provides] infix operator. This is the
     * right-hand side of the operator.
     */
    @Suppress("UNCHECKED_CAST")
    public val value: T
        get() = providedValue as T

    /**
     * This value is `true` if the provided value will override any value provided above it. This
     * value is `true` when using [ProvidableCompositionLocal.provides] but `false` when using
     * [ProvidableCompositionLocal.providesDefault].
     *
     * @see ProvidableCompositionLocal.provides
     * @see ProvidableCompositionLocal.providesDefault
     */
    @get:JvmName("getCanOverride")
    public var canOverride: Boolean = true
        private set

    @Suppress("UNCHECKED_CAST")
    internal val effectiveValue: T
        get() =
            when {
                explicitNull -> null as T
                state != null -> state.value
                providedValue != null -> providedValue
                else -> composeRuntimeError("Unexpected form of a provided value")
            }

    internal val isStatic
        get() = (explicitNull || value != null) && !isDynamic

    internal fun ifNotAlreadyProvided() = this.also { canOverride = false }
}

private val SlotWriter.nextGroup
    get() = currentGroup + groupSize(currentGroup)

/**
 * Composer is the interface that is targeted by the Compose Kotlin compiler plugin and used by code
 * generation helpers. It is highly recommended that direct calls these be avoided as the runtime
 * assumes that the calls are generated by the compiler and contain only a minimum amount of state
 * validation.
 */
public sealed interface Composer {
    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Changes calculated and recorded during composition and are sent to [applier] which makes the
     * physical changes to the node tree implied by a composition.
     *
     * Composition has two discrete phases, 1) calculate and record changes and 2) making the
     * changes via the [applier]. While a [Composable] functions is executing, none of the [applier]
     * methods are called. The recorded changes are sent to the [applier] all at once after all
     * [Composable] functions have completed.
     */
    @ComposeCompilerApi public val applier: Applier<*>

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects that a new part of the composition is being created, that is, the composition will
     * insert new nodes into the resulting tree.
     */
    @ComposeCompilerApi public val inserting: Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects whether the [Composable] function can skip. Even if a [Composable] function is
     * called with the same parameters it might still need to run because, for example, a new value
     * was provided for a [CompositionLocal] created by [staticCompositionLocalOf].
     */
    @ComposeCompilerApi public val skipping: Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reflects whether the default parameter block of a [Composable] function is valid. This is
     * `false` if a [State] object read in the [startDefaults] group was modified since the last
     * time the [Composable] function was run.
     */
    @ComposeCompilerApi public val defaultsInvalid: Boolean

    /**
     * A Compose internal property. DO NOT call directly. Use [currentRecomposeScope] instead.
     *
     * The invalidation current invalidation scope. An new invalidation scope is created whenever
     * [startRestartGroup] is called. when this scope's [RecomposeScope.invalidate] is called then
     * lambda supplied to [endRestartGroup]'s [ScopeUpdateScope] will be scheduled to be run.
     */
    @InternalComposeApi public val recomposeScope: RecomposeScope?

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Return an object that can be used to uniquely identity of the current recomposition scope.
     * This identity will be the same even if the recompose scope instance changes.
     *
     * This is used internally by tooling track composable function invocations.
     */
    @ComposeCompilerApi public val recomposeScopeIdentity: Any?

    /**
     * A Compose internal property. DO NOT call directly. Use [currentCompositeKeyHash] instead.
     *
     * This a hash value used to map externally stored state to the composition. For example, this
     * is used by saved instance state to preserve state across activity lifetime boundaries.
     *
     * This value is likely but not guaranteed to be unique. There are known cases, such as for
     * loops without a unique [key], where the runtime does not have enough information to make the
     * compound key hash unique.
     */
    @Deprecated(
        "Prefer the higher-precision compositeKeyHashCode instead",
        ReplaceWith("compositeKeyHashCode"),
    )
    @InternalComposeApi
    public val compoundKeyHash: Int
        get() = compositeKeyHashCode.hashCode()

    /**
     * A Compose internal property. DO NOT call directly. Use [currentCompositeKeyHashCode] instead.
     *
     * This a hash value used to map externally stored state to the composition. For example, this
     * is used by saved instance state to preserve state across activity lifetime boundaries.
     *
     * This value is likely but not guaranteed to be unique. There are known cases, such as for
     * loops without a unique [key], where the runtime does not have enough information to make the
     * compound key hash unique.
     */
    @InternalComposeApi public val compositeKeyHashCode: CompositeKeyHashCode

    // Groups

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a replaceable group. A replaceable group is a group that cannot be moved during
     * execution and can only either inserted, removed, or replaced. For example, the group created
     * by most control flow constructs such as an `if` statement are replaceable groups.
     *
     * Warning: Versions of the compiler that generate calls to this function also contain subtle
     * bug that does not generate a group around a loop containing code that just creates composable
     * lambdas (AnimatedContent from androidx.compose.animation, for example) which makes replacing
     * the group unsafe and the this must treat this like a movable group. [startReplaceGroup] was
     * added that will replace the group as described above and is only called by versions of the
     * compiler that correctly generate code around loops that create lambdas. This method is kept
     * to maintain compatibility with code generated by older versions of the compose compiler
     * plugin.
     *
     * @param key A compiler generated key based on the source location of the call.
     */
    @ComposeCompilerApi public fun startReplaceableGroup(key: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a replaceable group.
     *
     * @see startRestartGroup
     */
    @ComposeCompilerApi public fun endReplaceableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a replace group. A replace group is a group that cannot be moved during must only
     * either be inserted, removed, or replaced. For example, the group created by most control flow
     * constructs such as an `if` statement are replaceable groups.
     *
     * Note: This method replaces [startReplaceableGroup] which is only generated by older versions
     * of the compose compiler plugin that predate the addition of this method. The runtime is now
     * required to replace the group if a different group is detected instead of treating it like a
     * movable group.
     *
     * @param key A compiler generated key based on the source location of the call.
     * @see endReplaceGroup
     */
    @ComposeCompilerApi public fun startReplaceGroup(key: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a replace group.
     *
     * @see startReplaceGroup
     */
    @ComposeCompilerApi public fun endReplaceGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a movable group. A movable group is one that can be moved based on the value of
     * [dataKey] which is typically supplied by the [key][androidx.compose.runtime.key] pseudo
     * compiler function.
     *
     * A movable group implements the semantics of [key][androidx.compose.runtime.key] which allows
     * the state and nodes generated by a loop to move with the composition implied by the key
     * passed to [key][androidx.compose.runtime.key].
     *
     * @param key a compiler generated key based on the source location of the call.
     * @param dataKey an additional object that is used as a second part of the key. This key
     *   produced from the `keys` parameter supplied to the [key][androidx.compose.runtime.key]
     *   pseudo compiler function.
     */
    @ComposeCompilerApi public fun startMovableGroup(key: Int, dataKey: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a movable group.
     *
     * @see startMovableGroup
     */
    @ComposeCompilerApi public fun endMovableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to start the group that calculates the default parameters of a [Composable] function.
     *
     * This method is called near the beginning of a [Composable] function with default parameters
     * and surrounds the remembered values or [Composable] calls necessary to produce the default
     * parameters. For example, for `model: Model = remember { DefaultModel() }` the call to
     * [remember] is called inside a [startDefaults] group.
     */
    @ComposeCompilerApi public fun startDefaults()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of defaults group.
     *
     * @see startDefaults
     */
    @ComposeCompilerApi public fun endDefaults()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to record a group for a [Composable] function and starts a group that can be
     * recomposed on demand based on the lambda passed to
     * [updateScope][ScopeUpdateScope.updateScope] when [endRestartGroup] is called
     *
     * @param key A compiler generated key based on the source location of the call.
     * @return the instance of the composer to use for the rest of the function.
     */
    @ComposeCompilerApi public fun startRestartGroup(key: Int): Composer

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called to end a restart group.
     */
    @ComposeCompilerApi public fun endRestartGroup(): ScopeUpdateScope?

    /**
     * A Compose internal API. DO NOT call directly.
     *
     * Request movable content be inserted at the current location. This will schedule with the root
     * composition parent a call to [insertMovableContent] with the correct [MovableContentState] if
     * one was released in another part of composition.
     */
    @InternalComposeApi public fun insertMovableContent(value: MovableContent<*>, parameter: Any?)

    /**
     * A Compose internal API. DO NOT call directly.
     *
     * Perform a late composition that adds to the current late apply that will insert the given
     * references to [MovableContent] into the composition. If a [MovableContent] is paired then
     * this is a request to move a released [MovableContent] from a different location or from a
     * different composition. If it is not paired (i.e. the `second` [MovableContentStateReference]
     * is `null`) then new state for the [MovableContent] is inserted into the composition.
     */
    @InternalComposeApi
    public fun insertMovableContentReferences(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    )

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Record the source information string for a group. This must be immediately called after the
     * start of a group.
     *
     * @param sourceInformation An string value to that provides the compose tools enough
     *   information to calculate the source location of calls to composable functions.
     */
    public fun sourceInformation(sourceInformation: String)

    /**
     * A compose compiler plugin API. DO NOT call directly.
     *
     * Record a source information marker. This marker can be used in place of a group that would
     * have contained the information but was elided as the compiler plugin determined the group was
     * not necessary such as when a function is marked with [ReadOnlyComposable].
     *
     * @param key A compiler generated key based on the source location of the call.
     * @param sourceInformation An string value to that provides the compose tools enough
     *   information to calculate the source location of calls to composable functions.
     */
    public fun sourceInformationMarkerStart(key: Int, sourceInformation: String)

    /**
     * A compose compiler plugin API. DO NOT call directly.
     *
     * Record the end of the marked source information range.
     */
    public fun sourceInformationMarkerEnd()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Skips the composer to the end of the current group. This generated by the compiler to when
     * the body of a [Composable] function can be skipped typically because the parameters to the
     * function are equal to the values passed to it in the previous composition.
     */
    @ComposeCompilerApi public fun skipToGroupEnd()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Deactivates the content to the end of the group by treating content as if it was deleted and
     * replaces all slot table entries for calls to [cache] to be [Empty]. This must be called as
     * the first call for a group.
     */
    @ComposeCompilerApi public fun deactivateToEndGroup(changed: Boolean)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Skips the current group. This called by the compiler to indicate that the current group can
     * be skipped, for example, this is generated to skip the [startDefaults] group the default
     * group is was not invalidated.
     */
    @ComposeCompilerApi public fun skipCurrentGroup()

    // Nodes

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a group that tracks a the code that will create or update a node that is generated as
     * part of the tree implied by the composition.
     */
    @ComposeCompilerApi public fun startNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a group that tracks a the code that will create or update a node that is generated as
     * part of the tree implied by the composition. A reusable node can be reused in a reusable
     * group even if the group key is changed.
     */
    @ComposeCompilerApi public fun startReusableNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Report the [factory] that will be used to create the node that will be generated into the
     * tree implied by the composition. This will only be called if [inserting] is is `true`.
     *
     * @param factory a factory function that will generate a node that will eventually be supplied
     *   to [applier] though [Applier.insertBottomUp] and [Applier.insertTopDown].
     */
    @ComposeCompilerApi public fun <T> createNode(factory: () -> T)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Report that the node is still being used. This will be called in the same location as the
     * corresponding [createNode] when [inserting] is `false`.
     */
    @ComposeCompilerApi public fun useNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a node group.
     */
    @ComposeCompilerApi public fun endNode()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Start a reuse group. Unlike a movable group, in a reuse group if the [dataKey] changes the
     * composition shifts into a reusing state cause the composer to act like it is inserting (e.g.
     * [cache] acts as if all values are invalid, [changed] always returns true, etc.) even though
     * it is recomposing until it encounters a reusable node. If the node is reusable it temporarily
     * shifts into recomposition for the node and then shifts back to reusing for the children. If a
     * non-reusable node is generated the composer shifts to inserting for the node and all of its
     * children.
     *
     * @param key An compiler generated key based on the source location of the call.
     * @param dataKey A key provided by the [ReusableContent] composable function that is used to
     *   determine if the composition shifts into a reusing state for this group.
     */
    @ComposeCompilerApi public fun startReusableGroup(key: Int, dataKey: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Called at the end of a reusable group.
     */
    @ComposeCompilerApi public fun endReusableGroup()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Temporarily disable reusing if it is enabled.
     */
    @ComposeCompilerApi public fun disableReusing()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Reenable reusing if it was previously enabled before the last call to [disableReusing].
     */
    @ComposeCompilerApi public fun enableReusing()

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Return a marker for the current group that can be used in a call to [endToMarker].
     */
    @ComposeCompilerApi public val currentMarker: Int

    /**
     * Compose compiler plugin API. DO NOT call directly.
     *
     * Ends all the groups up to but not including the group that is the parent group when
     * [currentMarker] was called to produce [marker]. All groups ended must have been started with
     * either [startReplaceableGroup] or [startMovableGroup]. Ending other groups can cause the
     * state of the composer to become inconsistent.
     */
    @ComposeCompilerApi public fun endToMarker(marker: Int)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Schedule [block] to called with [value]. This is intended to update the node generated by
     * [createNode] to changes discovered by composition.
     *
     * @param value the new value to be set into some property of the node.
     * @param block the block that sets the some property of the node to [value].
     */
    @ComposeCompilerApi public fun <V, T> apply(value: V, block: T.(V) -> Unit)

    // State

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Produce an object that will compare equal an iff [left] and [right] compare equal to some
     * [left] and [right] of a previous call to [joinKey]. This is used by [key] to handle multiple
     * parameters. Since the previous composition stored [left] and [right] in a "join key" object
     * this call is used to return the previous value without an allocation instead of blindly
     * creating a new value that will be immediately discarded.
     *
     * @param left the first part of a a joined key.
     * @param right the second part of a joined key.
     * @return an object that will compare equal to a value previously returned by [joinKey] iff
     *   [left] and [right] compare equal to the [left] and [right] passed to the previous call.
     */
    @ComposeCompilerApi public fun joinKey(left: Any?, right: Any?): Any

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Remember a value into the composition state. This is a primitive method used to implement
     * [remember].
     *
     * @return [Composer.Empty] when [inserting] is `true` or the value passed to
     *   [updateRememberedValue] from the previous composition.
     * @see cache
     */
    @ComposeCompilerApi public fun rememberedValue(): Any?

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Update the remembered value correspond to the previous call to [rememberedValue]. The [value]
     * will be returned by [rememberedValue] for the next composition.
     */
    @ComposeCompilerApi public fun updateRememberedValue(value: Any?)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Any?): Boolean

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Boolean): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Char): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Byte): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Short): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Int): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Float): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Long): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition. This is used, for
     * example, to check parameter values to determine if they have changed.
     *
     * This overload is provided to avoid boxing [value] to compare with a potentially boxed version
     * of [value] in the composition state.
     *
     * @param value the value to check
     * @return `true` if the value if [equals] of the previous value returns `false` when passed
     *   [value].
     */
    @ComposeCompilerApi public fun changed(value: Double): Boolean = changed(value)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Check [value] is different than the value used in the previous composition using `===`
     * instead of `==` equality. This is used, for example, to check parameter values to determine
     * if they have changed for values that use value equality but, for correct behavior, the
     * composer needs reference equality.
     *
     * @param value the value to check
     * @return `true` if the value is === equal to the previous value and returns `false` when
     *   [value] is different.
     */
    @ComposeCompilerApi public fun changedInstance(value: Any?): Boolean = changed(value)

    // Scopes

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Mark [scope] as used. [endReplaceableGroup] will return `null` unless [recordUsed] is called
     * on the corresponding [scope]. This is called implicitly when [State] objects are read during
     * composition is called when [currentRecomposeScope] is called in the [Composable] function.
     */
    @InternalComposeApi public fun recordUsed(scope: RecomposeScope)

    /**
     * A Compose compiler plugin API. DO NOT call directly.
     *
     * Generated by the compile to determine if the composable function should be executed. It may
     * not execute if parameter has not changed and the nothing else is forcing the function to
     * execute (such as its scope was invalidated or a static composition local it was changed) or
     * the composition is pausable and the composition is pausing.
     *
     * @param parametersChanged `true` if the parameters to the composable function have changed.
     *   This is also `true` if the composition is [inserting] or if content is being reused.
     * @param flags The `$changed` parameter that contains the forced recompose bit to allow the
     *   composer to disambiguate when the parameters changed due the execution being forced or if
     *   the parameters actually changed. This is only ambiguous in a [PausableComposition] and is
     *   necessary to determine if the function can be paused. The bits, other than 0, are reserved
     *   for future use (which would required the bit 31, which is unused in `$changed` values, to
     *   be set to indicate that the flags carry additional information). Passing the `$changed`
     *   flags directly, instead of masking the 0 bit, is more efficient as it allows less code to
     *   be generated per call to `shouldExecute` which is every called in every restartable
     *   function, as well as allowing for the API to be extended without a breaking changed.
     */
    @InternalComposeApi public fun shouldExecute(parametersChanged: Boolean, flags: Int): Boolean

    // Internal API

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Record a function to call when changes to the corresponding tree are applied to the
     * [applier]. This is used to implement [SideEffect].
     *
     * @param effect a lambda to invoke after the changes calculated up to this point have been
     *   applied.
     */
    @InternalComposeApi public fun recordSideEffect(effect: () -> Unit)

    /**
     * Returns the active set of CompositionLocals at the current position in the composition
     * hierarchy. This is a lower level API that can be used to export and access CompositionLocal
     * values outside of Composition.
     *
     * This API does not track reads of CompositionLocals and does not automatically dispatch new
     * values to previous readers when the value of a CompositionLocal changes. To use this API as
     * intended, you must set up observation manually. This means:
     * - For [non-static CompositionLocals][compositionLocalOf], composables reading this map need
     *   to observe the snapshot state for CompositionLocals being read to be notified when their
     *   values in this map change.
     * - For [static CompositionLocals][staticCompositionLocalOf], all composables including the
     *   composable reading this map will be recomposed and you will need to re-obtain this map to
     *   get the latest values.
     *
     * Most applications shouldn't use this API directly, and should instead use
     * [CompositionLocal.current].
     */
    public val currentCompositionLocalMap: CompositionLocalMap

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Return the [CompositionLocal] value associated with [key]. This is the primitive function
     * used to implement [CompositionLocal.current].
     *
     * @param key the [CompositionLocal] value to be retrieved.
     */
    @InternalComposeApi public fun <T> consume(key: CompositionLocal<T>): T

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Provide the given values for the associated [CompositionLocal] keys. This is the primitive
     * function used to implement [CompositionLocalProvider].
     *
     * @param values an array of value to provider key pairs.
     */
    @InternalComposeApi public fun startProviders(values: Array<out ProvidedValue<*>>)

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * End the provider group.
     *
     * @see startProviders
     */
    @InternalComposeApi public fun endProviders()

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Provide the given value for the associated [CompositionLocal] key. This is the primitive
     * function used to implement [CompositionLocalProvider].
     *
     * @param value a value to provider key pairs.
     */
    @InternalComposeApi public fun startProvider(value: ProvidedValue<*>)

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * End the provider group.
     *
     * @see startProvider
     */
    @InternalComposeApi public fun endProvider()

    /**
     * A tooling API function. DO NOT call directly.
     *
     * The data stored for the composition. This is used by Compose tools, such as the preview and
     * the inspector, to display or interpret the result of composition.
     */
    public val compositionData: CompositionData

    /**
     * A tooling API function. DO NOT call directly.
     *
     * Called by the inspector to inform the composer that it should collect additional information
     * about call parameters. By default, only collect parameter information for scopes that are
     * [recordUsed] has been called on. If [collectParameterInformation] is called it will attempt
     * to collect all calls even if the runtime doesn't need them.
     *
     * WARNING: calling this will result in a significant number of additional allocations that are
     * typically avoided.
     */
    public fun collectParameterInformation()

    /**
     * Schedules an [action] to be invoked when the recomposer finishes the next composition of a
     * frame (including the completion of subcompositions). If a frame is currently in-progress,
     * [action] will be invoked when the current frame fully finishes composing. If a frame isn't
     * currently in-progress, a new frame will be scheduled (if one hasn't been already) and
     * [action] will execute at the completion of the next frame's composition. If a new frame is
     * scheduled and there is no other work to execute, [action] will still execute.
     *
     * [action] will always execute on the applier thread.
     *
     * Note that [action] runs at the end of a frame scheduled by the recomposer. If a callback is
     * scheduled via this method during the initial composition, it will not execute until the
     * _next_ frame.
     *
     * @return A [CancellationHandle] that can be used to unregister the [action]. The returned
     *   handle is thread-safe and may be cancelled from any thread. Cancelling the handle only
     *   removes the callback from the queue. If [action] is currently executing, it will not be
     *   cancelled by this handle.
     */
    public fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * Build a composition context that can be used to created a subcomposition. A composition
     * reference is used to communicate information from this composition to the subcompositions
     * such as the all the [CompositionLocal]s provided at the point the reference is created.
     */
    @InternalComposeApi public fun buildContext(): CompositionContext

    /**
     * A Compose internal function. DO NOT call directly.
     *
     * The coroutine context for the composition. This is used, for example, to implement
     * [LaunchedEffect]. This context is managed by the [Recomposer].
     */
    @InternalComposeApi
    public val applyCoroutineContext: CoroutineContext
        @TestOnly get

    /** The composition that is used to control this composer. */
    public val composition: ControlledComposition
        @TestOnly get

    /**
     * Disable the collection of source information, that may introduce groups to store the source
     * information, in order to be able to more accurately calculate the actual number of groups a
     * composable function generates in a release build.
     *
     * This function is only safe to call in a test and will produce incorrect composition results
     * if called on a composer not under test.
     */
    @TestOnly public fun disableSourceInformation()

    public companion object {
        /**
         * A special value used to represent no value was stored (e.g. an empty slot). This is
         * returned, for example by [Composer.rememberedValue] while it is [Composer.inserting] is
         * `true`.
         */
        public val Empty: Any =
            object {
                override fun toString() = "Empty"
            }

        /**
         * Internal API for specifying a tracer used for instrumenting frequent operations, e.g.
         * recompositions.
         */
        @InternalComposeTracingApi
        public fun setTracer(tracer: CompositionTracer?) {
            compositionTracer = tracer
        }

        /**
         * Set the mode for collecting composition stack traces. See [ComposeStackTraceMode] for
         * more information about available modes. The stack traces are disabled by default.
         *
         * Note: changing stack trace collection mode will not affect already running compositions.
         */
        public fun setDiagnosticStackTraceMode(mode: ComposeStackTraceMode) {
            composeStackTraceMode = mode
        }

        /**
         * Enable composition stack traces based on the source information. When this flag is
         * enabled, composition will record source information at runtime. When crash occurs,
         * Compose will append a suppressed exception that contains a stack trace pointing to the
         * place in composition closest to the crash.
         *
         * @see [ComposeStackTraceMode.SourceInformation] for more information.
         */
        @Deprecated(message = "Use setDiagnosticStackTraceMode instead")
        @ExperimentalComposeRuntimeApi
        public fun setDiagnosticStackTraceEnabled(enabled: Boolean) {
            composeStackTraceMode =
                if (enabled) ComposeStackTraceMode.SourceInformation else ComposeStackTraceMode.None
        }
    }
}

internal abstract class InternalComposer : Composer {
    internal abstract val areChildrenComposing: Boolean
    internal abstract val isComposing: Boolean
    internal abstract val hasPendingChanges: Boolean
    internal abstract val currentRecomposeScope: RecomposeScopeImpl?
    internal abstract val errorContext: CompositionErrorContextImpl?
    internal abstract val deferredChanges: Changes?
    internal abstract val sourceMarkersEnabled: Boolean

    internal abstract fun startReuseFromRoot()

    internal abstract fun endReuseFromRoot()

    internal abstract fun changesApplied()

    internal abstract fun forceRecomposeScopes(): Boolean

    internal abstract fun dispose()

    internal abstract fun deactivate()

    internal abstract fun verifyConsistent()

    internal abstract fun stacksSize(): Int

    internal abstract fun stackTraceForValue(value: Any?): ComposeStackTrace

    internal abstract fun parentStackTrace(): List<ComposeStackTraceFrame>

    internal abstract fun prepareCompose(block: () -> Unit)

    internal abstract fun composeContent(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        content: @Composable () -> Unit,
        shouldPause: ShouldPauseCallback?,
    )

    internal abstract fun recompose(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        shouldPause: ShouldPauseCallback?,
    ): Boolean

    internal abstract fun tryImminentInvalidation(
        scope: RecomposeScopeImpl,
        instance: Any?,
    ): Boolean

    internal abstract fun updateComposerInvalidations(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>
    )

    @TestOnly internal abstract fun parentKey(): Int
}

/**
 * A Compose compiler plugin API. DO NOT call directly.
 *
 * Cache, that is remember, a value in the composition data of a composition. This is used to
 * implement [remember] and used by the compiler plugin to generate more efficient calls to
 * [remember] when it determines these optimizations are safe.
 */
@ComposeCompilerApi
public inline fun <T> Composer.cache(invalid: Boolean, block: @DisallowComposableCalls () -> T): T {
    @Suppress("UNCHECKED_CAST")
    return rememberedValue().let {
        if (invalid || it === Composer.Empty) {
            val value = block()
            updateRememberedValue(value)
            value
        } else it
    } as T
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records source information that can be used for tooling to determine the source location of the
 * corresponding composable function. By default, this function is declared as having no
 * side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove it.
 */
@ComposeCompilerApi
public fun sourceInformation(composer: Composer, sourceInformation: String) {
    composer.sourceInformation(sourceInformation)
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records the start of a source information marker that can be used for tooling to determine the
 * source location of the corresponding composable function that otherwise don't require tracking
 * information such as [ReadOnlyComposable] functions. By default, this function is declared as
 * having no side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove
 * it.
 *
 * Important that both [sourceInformationMarkerStart] and [sourceInformationMarkerEnd] are removed
 * together or both kept. Removing only one will cause incorrect runtime behavior.
 */
@ComposeCompilerApi
public fun sourceInformationMarkerStart(composer: Composer, key: Int, sourceInformation: String) {
    composer.sourceInformationMarkerStart(key, sourceInformation)
}

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@InternalComposeTracingApi
public interface CompositionTracer {
    public fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String): Unit

    public fun traceEventEnd(): Unit

    public fun isTraceInProgress(): Boolean
}

@OptIn(InternalComposeTracingApi::class) private var compositionTracer: CompositionTracer? = null

internal var composeStackTraceMode = ComposeStackTraceMode.None

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun isTraceInProgress(): Boolean =
    compositionTracer.let { it != null && it.isTraceInProgress() }

@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
@Deprecated(
    message = "Use the overload with \$dirty metadata instead",
    ReplaceWith("traceEventStart(key, dirty1, dirty2, info)"),
    DeprecationLevel.HIDDEN,
)
public fun traceEventStart(key: Int, info: String): Unit = traceEventStart(key, -1, -1, info)

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 *
 * @param key is a group key generated by the compiler plugin for the function being traced. This
 *   key is unique the function.
 * @param dirty1 $dirty metadata: forced-recomposition and function parameters 1..10 if present
 * @param dirty2 $dirty2 metadata: forced-recomposition and function parameters 11..20 if present
 * @param info is a user displayable string that describes the function for which this is the start
 *   event.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
    compositionTracer?.traceEventStart(key, dirty1, dirty2, info)
}

/**
 * Internal tracing API.
 *
 * Should be called without thread synchronization with occasional information loss.
 */
@OptIn(InternalComposeTracingApi::class)
@ComposeCompilerApi
public fun traceEventEnd() {
    compositionTracer?.traceEventEnd()
}

/**
 * A Compose internal function. DO NOT call directly.
 *
 * Records the end of a source information marker that can be used for tooling to determine the
 * source location of the corresponding composable function that otherwise don't require tracking
 * information such as [ReadOnlyComposable] functions. By default, this function is declared as
 * having no side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to remove
 * it.
 *
 * Important that both [sourceInformationMarkerStart] and [sourceInformationMarkerEnd] are removed
 * together or both kept. Removing only one will cause incorrect runtime behavior.
 */
@ComposeCompilerApi
public fun sourceInformationMarkerEnd(composer: Composer) {
    composer.sourceInformationMarkerEnd()
}

/**
 * A helper receiver scope class used by [ComposeNode] to help write code to initialized and update
 * a node.
 *
 * @see ComposeNode
 */
@JvmInline
public value class Updater<T> constructor(@PublishedApi internal val composer: Composer) {
    /**
     * Set the value property of the emitted node.
     *
     * Schedules [block] to be run when the node is first created or when [value] is different than
     * the previous composition.
     *
     * @see update
     */
    @Deprecated("Boxes more than than the generic overload", level = DeprecationLevel.HIDDEN)
    @Suppress("NOTHING_TO_INLINE")
    public inline fun set(value: Int, noinline block: T.(value: Int) -> Unit): Unit =
        with(composer) {
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                composer.apply(value, block)
            }
        }

    /**
     * Set the value property of the emitted node.
     *
     * Schedules [block] to be run when the node is first created or when [value] is different than
     * the previous composition.
     *
     * @see update
     */
    public fun <V> set(value: V, block: T.(value: V) -> Unit): Unit =
        with(composer) {
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                composer.apply(value, block)
            }
        }

    /**
     * Update the value of a property of the emitted node.
     *
     * Schedules [block] to be run when [value] is different than the previous composition. It is
     * different than [set] in that it does not run when the node is created. This is used when
     * initial value set by the [ComposeNode] in the constructor callback already has the correct
     * value. For example, use [update} when [value] is passed into of the classes constructor
     * parameters.
     *
     * @see set
     */
    @Deprecated("Boxes more than the generic overload", level = DeprecationLevel.HIDDEN)
    @Suppress("NOTHING_TO_INLINE")
    public inline fun update(value: Int, noinline block: T.(value: Int) -> Unit): Unit =
        with(composer) {
            val inserting = inserting
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                if (!inserting) apply(value, block)
            }
        }

    /**
     * Update the value of a property of the emitted node.
     *
     * Schedules [block] to be run when [value] is different than the previous composition. It is
     * different than [set] in that it does not run when the node is created. This is used when
     * initial value set by the [ComposeNode] in the constructor callback already has the correct
     * value. For example, use [update} when [value] is passed into of the classes constructor
     * parameters.
     *
     * @see set
     */
    public fun <V> update(value: V, block: T.(value: V) -> Unit): Unit =
        with(composer) {
            val inserting = inserting
            if (inserting || rememberedValue() != value) {
                updateRememberedValue(value)
                if (!inserting) apply(value, block)
            }
        }

    /**
     * Initialize emitted node.
     *
     * Schedule [block] to be executed after the node is created.
     *
     * This is only executed once. The can be used to call a method or set a value on a node
     * instance that is required to be set after one or more other properties have been set.
     *
     * @see reconcile
     */
    public fun init(block: T.() -> Unit) {
        if (composer.inserting) composer.apply<Unit, T>(Unit) { block() }
    }

    /**
     * Initialize emitted node.
     *
     * Schedule [block] to be executed after the node is created.
     *
     * This is only executed once. The can be used to call a method or set a value on a node
     * instance that is required to be set after one or more other properties have been set.
     *
     * This is different from the other [init] overload in that it does not force creating a lambda
     * to capture [value].
     */
    public fun <V> init(value: V, block: T.(V) -> Unit) {
        if (composer.inserting) composer.apply(value, block)
    }

    /**
     * Reconcile the node to the current state.
     *
     * This is used when [set] and [update] are insufficient to update the state of the node based
     * on changes passed to the function calling [ComposeNode].
     *
     * Schedules [block] to execute. As this unconditionally schedules [block] to executed it might
     * be executed unnecessarily as no effort is taken to ensure it only executes when the values
     * [block] captures have changed. It is highly recommended that [set] and [update] be used
     * instead as they will only schedule their blocks to executed when the value passed to them has
     * changed.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun reconcile(block: T.() -> Unit) {
        composer.apply<Unit, T>(Unit) { this.block() }
    }
}

@JvmInline
public value class SkippableUpdater<T> constructor(@PublishedApi internal val composer: Composer) {
    public inline fun update(block: Updater<T>.() -> Unit) {
        composer.startReplaceableGroup(0x1e65194f)
        Updater<T>(composer).block()
        composer.endReplaceableGroup()
    }
}

internal fun SlotWriter.removeCurrentGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.

    forAllDataInRememberOrder(currentGroup) { _, slot ->
        // even that in the documentation we claim ComposeNodeLifecycleCallback should be only
        // implemented on the nodes we do not really enforce it here as doing so will be expensive.
        if (slot is ComposeNodeLifecycleCallback) {
            rememberManager.releasing(slot)
        }
        if (slot is RememberObserverHolder) {
            rememberManager.forgetting(slot)
        }
        if (slot is RecomposeScopeImpl) {
            slot.release()
        }
    }

    removeGroup()
}

internal inline fun <R> SlotWriter.withAfterAnchorInfo(anchor: Anchor?, cb: (Int, Int) -> R) {
    var priority = -1
    var endRelativeAfter = -1
    if (anchor != null && anchor.valid) {
        priority = anchorIndex(anchor.asGapAnchor())
        endRelativeAfter = slotsSize - slotsEndAllIndex(priority)
    }
    cb(priority, endRelativeAfter)
}

internal val SlotWriter.isAfterFirstChild
    get() = currentGroup > parent + 1
internal val SlotReader.isAfterFirstChild
    get() = currentGroup > parent + 1

/**
 * Remember observer which is not removed during reuse/deactivate of the group. It is used to
 * preserve composition locals between group deactivation.
 */
internal interface ReusableRememberObserverHolder : RememberObserverHolder

internal interface RememberObserverHolder {
    var wrapped: RememberObserver
}

// An arbitrary key value that marks the default parameter group
internal const val defaultsKey = -127

@PublishedApi internal const val invocationKey: Int = 200

@PublishedApi internal val invocation: Any = OpaqueKey("provider")

@PublishedApi internal const val providerKey: Int = 201

@PublishedApi internal val provider: Any = OpaqueKey("provider")

@PublishedApi internal const val compositionLocalMapKey: Int = 202

@PublishedApi internal val compositionLocalMap: Any = OpaqueKey("compositionLocalMap")

@PublishedApi internal const val providerValuesKey: Int = 203

@PublishedApi internal val providerValues: Any = OpaqueKey("providerValues")

@PublishedApi internal const val providerMapsKey: Int = 204

@PublishedApi internal val providerMaps: Any = OpaqueKey("providers")

@PublishedApi internal const val referenceKey: Int = 206

@PublishedApi internal val reference: Any = OpaqueKey("reference")

@PublishedApi internal const val reuseKey: Int = 207

private const val invalidGroupLocation = -2

internal class ComposeRuntimeError(override val message: String) : IllegalStateException()

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun runtimeCheck(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        composeImmediateRuntimeError(lazyMessage())
    }
}

internal const val EnableDebugRuntimeChecks = false

/**
 * A variation of [composeRuntimeError] that gets stripped from R8-minified builds. Use this for
 * more expensive checks or assertions along a hotpath that, if failed, would still lead to an
 * application crash that could be traced back to this assertion if removed from the final program
 * binary.
 */
internal inline fun debugRuntimeCheck(value: Boolean, lazyMessage: () -> String) {
    if (EnableDebugRuntimeChecks && !value) {
        composeImmediateRuntimeError(lazyMessage())
    }
}

internal inline fun debugRuntimeCheck(value: Boolean) = debugRuntimeCheck(value) { "Check failed" }

internal inline fun runtimeCheck(value: Boolean) = runtimeCheck(value) { "Check failed" }

internal fun composeRuntimeError(message: String): Nothing {
    throw ComposeRuntimeError(
        "Compose Runtime internal error. Unexpected or incorrect use of the Compose " +
            "internal runtime API ($message). Please report to Google or use " +
            "https://goo.gle/compose-feedback"
    )
}

// Unit variant of composeRuntimeError() so the call site doesn't add 3 extra
// instructions to throw a KotlinNothingValueException
internal fun composeImmediateRuntimeError(message: String) {
    throw ComposeRuntimeError(
        "Compose Runtime internal error. Unexpected or incorrect use of the Compose " +
            "internal runtime API ($message). Please report to Google or use " +
            "https://goo.gle/compose-feedback"
    )
}

/**
 * Extract the state of movable content from the given writer. A new slot table is created and the
 * content is removed from [slots] (leaving a movable content group that, if composed over, will
 * create new content) and added to this new slot table. The invalidations that occur to recompose
 * scopes in the movable content state will be collected and forwarded to the new composition if the
 * state is used.
 */
internal fun extractMovableContentAtCurrent(
    composition: ControlledComposition,
    reference: MovableContentStateReference,
    slots: SlotWriter,
    applier: Applier<*>?,
): MovableContentState {
    val slotTable = SlotTable()
    if (slots.collectingSourceInformation) {
        slotTable.collectSourceInformation()
    }
    if (slots.collectingCalledInformation) {
        slotTable.collectCalledByInformation()
    }

    // If an applier is provided then we are extracting a state from the middle of an
    // already extracted state. If the group has nodes then the nodes need to be removed
    // from their parent so they can potentially be inserted into a destination.
    val currentGroup = slots.currentGroup
    if (applier != null && slots.nodeCount(currentGroup) > 0) {
        @Suppress("UNCHECKED_CAST")
        applier as Applier<Any?>

        // Find the parent node by going up until the first node group
        var parentNodeGroup = slots.parent
        while (parentNodeGroup > 0 && !slots.isNode(parentNodeGroup)) {
            parentNodeGroup = slots.parent(parentNodeGroup)
        }

        // If we don't find a node group the nodes in the state have already been removed
        // as they are the nodes that were removed when the state was removed from the original
        // table.
        if (parentNodeGroup >= 0 && slots.isNode(parentNodeGroup)) {
            val node = slots.node(parentNodeGroup)
            var currentChild = parentNodeGroup + 1
            val end = parentNodeGroup + slots.groupSize(parentNodeGroup)

            // Find the node index
            var nodeIndex = 0
            while (currentChild < end) {
                val size = slots.groupSize(currentChild)
                if (currentChild + size > currentGroup) {
                    break
                }
                nodeIndex += if (slots.isNode(currentChild)) 1 else slots.nodeCount(currentChild)
                currentChild += size
            }

            // Remove the nodes
            val count = if (slots.isNode(currentGroup)) 1 else slots.nodeCount(currentGroup)
            applier.down(node)
            applier.remove(nodeIndex, count)
            applier.up()
        }
    }

    // Transfer invalidations before moving the scopes, since we could have accumulated more after
    // creating the state.
    val anchor = reference.anchor
    if (anchor.valid) {
        val extracted =
            (composition as CompositionImpl).extractInvalidationsOfGroup {
                slots.inGroup(anchor.asGapAnchor(), it.asGapAnchor())
            }
        reference.invalidations += extracted
    }

    // Write a table that as if it was written by a calling invokeMovableContentLambda because this
    // might be removed from the composition before the new composition can be composed to receive
    // it. When the new composition receives the state it must recompose over the state by calling
    // invokeMovableContentLambda.
    val anchors =
        slotTable.write { writer ->
            writer.beginInsert()

            // This is the prefix created by invokeMovableContentLambda
            writer.startGroup(movableContentKey, reference.content)
            writer.markGroup()
            writer.update(reference.parameter)

            // Move the content into current location
            val anchors = slots.moveTo(reference.anchor.asGapAnchor(), 1, writer)

            // skip the group that was just inserted.
            writer.skipGroup()

            // End the group that represents the call to invokeMovableContentLambda
            writer.endGroup()

            writer.endInsert()

            anchors
        }

    val state = MovableContentState(slotTable)
    if (RecomposeScopeImpl.hasAnchoredRecomposeScopes(slotTable, anchors)) {
        // If any recompose scopes are invalidated while the movable content is outside a
        // composition, ensure the reference is updated to contain the invalidation.
        val movableContentRecomposeScopeOwner =
            object : RecomposeScopeOwner {
                override fun invalidate(
                    scope: RecomposeScopeImpl,
                    instance: Any?,
                ): InvalidationResult {
                    // Try sending this to the original owner first.
                    val result =
                        (composition as? RecomposeScopeOwner)?.invalidate(scope, instance)
                            ?: InvalidationResult.IGNORED

                    // If the original owner ignores this then we need to record it in the
                    // reference
                    if (result == InvalidationResult.IGNORED) {
                        reference.invalidations += scope to instance
                        return InvalidationResult.SCHEDULED
                    }
                    return result
                }

                // The only reason [recomposeScopeReleased] is called is when the recompose scope is
                // removed from the table. First, this never happens for content that is moving, and
                // 2) even if it did the only reason we tell the composer is to clear tracking
                // tables that contain this information which is not relevant here.
                override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
                    // Nothing to do
                }

                // [recordReadOf] this is also something that would happen only during active
                // recomposition which doesn't happened to a slot table that is moving.
                override fun recordReadOf(value: Any) {
                    // Nothing to do
                }
            }
        slotTable.write { writer ->
            RecomposeScopeImpl.adoptAnchoredScopes(
                slots = writer,
                anchors = anchors,
                newOwner = movableContentRecomposeScopeOwner,
            )
        }
    }
    return state
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Recomposer.kt
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

package androidx.compose.runtime

import androidx.collection.MutableObjectList
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyObjectList
import androidx.collection.emptyScatterSet
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.collection.MultiValueMap
import androidx.compose.runtime.collection.fastForEach
import androidx.compose.runtime.collection.fastMap
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.collection.wrapIntoSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentSetOf
import androidx.compose.runtime.internal.AtomicReference
import androidx.compose.runtime.internal.SnapshotThreadLocal
import androidx.compose.runtime.internal.logError
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.platform.SynchronizedObject
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.ReaderKind
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.TransparentObserverMutableSnapshot
import androidx.compose.runtime.snapshots.TransparentObserverSnapshot
import androidx.compose.runtime.snapshots.fastAll
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastFilterIndexed
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastGroupBy
import androidx.compose.runtime.snapshots.fastMap
import androidx.compose.runtime.snapshots.fastMapNotNull
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.runtime.tooling.observe
import kotlin.collections.removeLast as removeLastKt
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.native.concurrent.ThreadLocal
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal const val recomposerKey = 1000

// TODO: Can we use rootKey for this since all compositions will have an eventual Recomposer parent?
private inline val RecomposerCompoundHashKey
    get() = CompositeKeyHashCode(recomposerKey)

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 * The [Recomposer] will be [closed][Recomposer.close] after [block] returns.
 * [withRunningRecomposer] will return once the [Recomposer] is [Recomposer.State.ShutDown] and all
 * child jobs launched by [block] have [joined][Job.join].
 */
public suspend fun <R> withRunningRecomposer(
    block: suspend CoroutineScope.(recomposer: Recomposer) -> R
): R = coroutineScope {
    val recomposer = Recomposer(coroutineContext)
    // Will be cancelled when recomposerJob cancels
    launch { recomposer.runRecomposeAndApplyChanges() }
    block(recomposer).also {
        recomposer.close()
        recomposer.join()
    }
}

/**
 * Read-only information about a [Recomposer]. Used when code should only monitor the activity of a
 * [Recomposer], and not attempt to alter its state or create new compositions from it.
 */
public interface RecomposerInfo {
    /** The current [State] of the [Recomposer]. See each [State] value for its meaning. */
    // TODO: Mirror the currentState/StateFlow API change here once we can safely add
    // default interface methods. https://youtrack.jetbrains.com/issue/KT-47000
    public val state: Flow<Recomposer.State>

    /**
     * `true` if the [Recomposer] has been assigned work to do and it is currently performing that
     * work or awaiting an opportunity to do so.
     */
    public val hasPendingWork: Boolean

    /**
     * The running count of the number of times the [Recomposer] awoke and applied changes to one or
     * more [Composer]s. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    public val changeCount: Long

    /**
     * Get flow of error states captured in composition. This flow is only available when recomposer
     * is in hot reload mode.
     *
     * @return a flow of error states captured during composition
     */
    @ComposeToolingApi
    public val errorState: StateFlow<RecomposerErrorInformation?>
        get() = DefaultErrorStateFlow

    /**
     * Register an observer to be notified when a composition is added to or removed from the given
     * [Recomposer]. When this method is called, the observer will be notified of all currently
     * registered compositions per the documentation in
     * [CompositionRegistrationObserver.onCompositionRegistered].
     */
    @ExperimentalComposeRuntimeApi
    public fun observe(observer: CompositionRegistrationObserver): CompositionObserverHandle? = null

    private companion object {
        @ComposeToolingApi
        private val DefaultErrorStateFlow: StateFlow<RecomposerErrorInformation?> =
            MutableStateFlow(null)
    }
}

/** Read only information about [Recomposer] error state. */
@ComposeToolingApi
public interface RecomposerErrorInformation {
    /** Exception which forced recomposition to halt. */
    public val cause: Throwable

    /**
     * Whether composition can recover from the error by itself. If the error is not recoverable,
     * recomposer will not react to invalidate calls until state is reloaded.
     */
    public val isRecoverable: Boolean
}

/**
 * Read only information about [Recomposer] error state. This is an internal API only kept for
 * backward compatibility.
 */
// TODO(b/469471141): Remove when Live Edit no longer depends on this API.
@InternalComposeApi
internal interface RecomposerErrorInfo {
    /** Exception which forced recomposition to halt. */
    val cause: Throwable

    /**
     * Whether composition can recover from the error by itself. If the error is not recoverable,
     * recomposer will not react to invalidate calls until state is reloaded.
     */
    val recoverable: Boolean
}

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 */
// RedundantVisibilityModifier suppressed because metalava picks up internal function overrides
// if 'internal' is not explicitly specified - b/171342041
// NotCloseable suppressed because this is Kotlin-only common code; [Auto]Closeable not available.
@Suppress("RedundantVisibilityModifier", "NotCloseable")
@OptIn(InternalComposeApi::class)
public class Recomposer(effectCoroutineContext: CoroutineContext) : CompositionContext() {
    /**
     * This is a running count of the number of times the recomposer awoke and applied changes to
     * one or more composers. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    public var changeCount: Long = 0L
        private set

    private val broadcastFrameClock = BroadcastFrameClock { onNewFrameAwaiter() }
    private val nextFrameEndCallbackQueue = NextFrameEndCallbackQueue { onNewFrameAwaiter() }

    /** Valid operational states of a [Recomposer]. */
    public enum class State {
        /**
         * [cancel] was called on the [Recomposer] and all cleanup work has completed. The
         * [Recomposer] is no longer available for use.
         */
        ShutDown,

        /**
         * [cancel] was called on the [Recomposer] and it is no longer available for use. Cleanup
         * work has not yet been fully completed and composition effect coroutines may still be
         * running.
         */
        ShuttingDown,

        /**
         * The [Recomposer] is not tracking invalidations for known composers and it will not
         * recompose them in response to changes. Call [runRecomposeAndApplyChanges] to await and
         * perform work. This is the initial state of a newly constructed [Recomposer].
         */
        Inactive,

        /**
         * The [Recomposer] is [Inactive] but at least one effect associated with a managed
         * composition is awaiting a frame. This frame will not be produced until the [Recomposer]
         * is [running][runRecomposeAndApplyChanges].
         */
        InactivePendingWork,

        /**
         * The [Recomposer] is tracking composition and snapshot invalidations but there is
         * currently no work to do.
         */
        Idle,

        /**
         * The [Recomposer] has been notified of pending work it must perform and is either actively
         * performing it or awaiting the appropriate opportunity to perform it. This work may
         * include invalidated composers that must be recomposed, snapshot state changes that must
         * be presented to known composers to check for invalidated compositions, or coroutines
         * awaiting a frame using the Recomposer's [MonotonicFrameClock].
         */
        PendingWork,
    }

    private val stateLock = makeSynchronizedObject()

    // Begin properties guarded by stateLock
    private var runnerJob: Job? = null
    private var closeCause: Throwable? = null
    private val _knownCompositions = mutableListOf<ControlledComposition>()
    private var _knownCompositionsCache: List<ControlledComposition>? = null
    private var snapshotInvalidations = MutableScatterSet<Any>()
    private val compositionInvalidations = mutableVectorOf<ControlledComposition>()
    private val compositionsAwaitingApply = mutableListOf<ControlledComposition>()
    private val movableContentAwaitingInsert = mutableListOf<MovableContentStateReference>()
    private val movableContentRemoved =
        MultiValueMap<MovableContent<Any?>, MovableContentStateReference>()
    private val movableContentNestedStatesAvailable = NestedContentMap()
    private val movableContentStatesAvailable =
        mutableScatterMapOf<MovableContentStateReference, MovableContentState>()
    private val movableContentNestedExtractionsPending =
        MultiValueMap<MovableContentStateReference, MovableContentStateReference>()
    private var failedCompositions: MutableList<ControlledComposition>? = null
    private var compositionsRemoved: MutableScatterSet<ControlledComposition>? = null
    private var workContinuation: CancellableContinuation<Unit>? = null
    private var concurrentCompositionsOutstanding = 0
    private var isClosed: Boolean = false
    private var errorState = MutableStateFlow<RecomposerErrorState?>(null)

    private var frameClockPaused: Boolean = false
    // End properties guarded by stateLock

    private val _state = MutableStateFlow(State.Inactive)
    private val pausedScopes = SnapshotThreadLocal<MutableScatterSet<RecomposeScopeImpl>?>()

    /**
     * A [Job] used as a parent of any effects created by this [Recomposer]'s compositions. Its
     * cleanup is used to advance to [State.ShuttingDown] or [State.ShutDown].
     *
     * Initialized after other state above, since it is possible for [Job.invokeOnCompletion] to run
     * synchronously during construction if the [Recomposer] is constructed with a completed or
     * cancelled [Job].
     */
    private val effectJob =
        Job(effectCoroutineContext[Job]).apply {
            invokeOnCompletion { throwable ->
                // Since the running recompose job is operating in a disjoint job if present,
                // kick it out and make sure no new ones start if we have one.
                val cancellation =
                    CancellationException("Recomposer effect job completed", throwable)

                var continuationToResume: CancellableContinuation<Unit>? = null
                synchronized(stateLock) {
                    val runnerJob = runnerJob
                    if (runnerJob != null) {
                        _state.value = State.ShuttingDown
                        // If the recomposer is closed we will let the runnerJob return from
                        // runRecomposeAndApplyChanges normally and consider ourselves shut down
                        // immediately.
                        if (!isClosed) {
                            // This is the job hosting frameContinuation; no need to resume it
                            // otherwise
                            runnerJob.cancel(cancellation)
                        } else if (workContinuation != null) {
                            continuationToResume = workContinuation
                        }
                        workContinuation = null
                        runnerJob.invokeOnCompletion { runnerJobCause ->
                            synchronized(stateLock) {
                                closeCause =
                                    throwable?.apply {
                                        runnerJobCause
                                            ?.takeIf { it !is CancellationException }
                                            ?.let { addSuppressed(it) }
                                    }
                                _state.value = State.ShutDown
                            }
                        }
                    } else {
                        closeCause = cancellation
                        _state.value = State.ShutDown
                    }
                }
                continuationToResume?.resume(Unit)
            }
        }

    /** The [effectCoroutineContext] is derived from the parameter of the same name. */
    override val effectCoroutineContext: CoroutineContext =
        effectCoroutineContext + broadcastFrameClock + effectJob

    private val hasBroadcastFrameClockAwaitersLocked: Boolean
        get() = !frameClockPaused && broadcastFrameClock.hasAwaiters

    private val hasNextFrameEndAwaitersLocked: Boolean
        get() = !frameClockPaused && nextFrameEndCallbackQueue.hasAwaiters

    private val hasBroadcastFrameClockAwaiters: Boolean
        get() = synchronized(stateLock) { hasBroadcastFrameClockAwaitersLocked }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private var registrationObservers: MutableObjectList<CompositionRegistrationObserver>? = null

    /**
     * Determine the new value of [_state]. Call only while locked on [stateLock]. If it returns a
     * continuation, that continuation should be resumed after releasing the lock.
     */
    private fun deriveStateLocked(): CancellableContinuation<Unit>? {
        if (_state.value <= State.ShuttingDown) {
            clearKnownCompositionsLocked()
            snapshotInvalidations = MutableScatterSet()
            compositionInvalidations.clear()
            compositionsAwaitingApply.clear()
            movableContentAwaitingInsert.clear()
            failedCompositions = null
            workContinuation?.cancel()
            workContinuation = null
            errorState.value = null
            return null
        }

        val newState =
            when {
                errorState.value != null -> {
                    State.Inactive
                }
                runnerJob == null -> {
                    snapshotInvalidations = MutableScatterSet()
                    compositionInvalidations.clear()
                    if (hasBroadcastFrameClockAwaitersLocked || hasNextFrameEndAwaitersLocked)
                        State.InactivePendingWork
                    else State.Inactive
                }
                compositionInvalidations.isNotEmpty() ||
                    snapshotInvalidations.isNotEmpty() ||
                    compositionsAwaitingApply.isNotEmpty() ||
                    movableContentAwaitingInsert.isNotEmpty() ||
                    concurrentCompositionsOutstanding > 0 ||
                    hasBroadcastFrameClockAwaitersLocked ||
                    hasNextFrameEndAwaitersLocked ||
                    movableContentRemoved.isNotEmpty() -> State.PendingWork
                else -> State.Idle
            }

        _state.value = newState
        return if (newState == State.PendingWork) {
            workContinuation.also { workContinuation = null }
        } else null
    }

    private fun onNewFrameAwaiter() {
        synchronized(stateLock) {
                deriveStateLocked().also {
                    if (_state.value <= State.ShuttingDown)
                        throw CancellationException(
                            "Recomposer shutdown; frame clock awaiter will never resume",
                            closeCause,
                        )
                }
            }
            ?.resume(Unit)
    }

    /** `true` if there is still work to do for an active caller of [runRecomposeAndApplyChanges] */
    private val shouldKeepRecomposing: Boolean
        get() = synchronized(stateLock) { !isClosed } || effectJob.children.any { it.isActive }

    /** The current [State] of this [Recomposer]. See each [State] value for its meaning. */
    @Deprecated("Replaced by currentState as a StateFlow", ReplaceWith("currentState"))
    public val state: Flow<State>
        get() = currentState

    /** The current [State] of this [Recomposer], available synchronously. */
    public val currentState: StateFlow<State>
        get() = _state

    // A separate private object to avoid the temptation of casting a RecomposerInfo
    // to a Recomposer if Recomposer itself were to implement RecomposerInfo.
    private inner class RecomposerInfoImpl : RecomposerInfo {
        override val state: Flow<State>
            get() = this@Recomposer.currentState

        override val hasPendingWork: Boolean
            get() = this@Recomposer.hasPendingWork

        override val changeCount: Long
            get() = this@Recomposer.changeCount

        @ComposeToolingApi
        override val errorState: StateFlow<RecomposerErrorInformation?>
            get() = this@Recomposer.errorState

        @ComposeToolingApi
        val currentError: RecomposerErrorInformation?
            get() = synchronized(stateLock) { this@Recomposer.errorState.value }

        @OptIn(ExperimentalComposeRuntimeApi::class)
        override fun observe(observer: CompositionRegistrationObserver): CompositionObserverHandle =
            this@Recomposer.observe(observer)

        fun invalidateGroupsWithKey(key: Int) {
            val compositions: List<ControlledComposition> = knownCompositions()
            compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastForEach { it.invalidateGroupsWithKey(key) }
        }

        fun saveStateAndDisposeForHotReload(): List<HotReloadable> {
            val compositions: List<ControlledComposition> = knownCompositions()
            return compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastMap { HotReloadable(it).apply { clearContent() } }
        }

        fun resetErrorState(): RecomposerErrorState? = this@Recomposer.resetErrorState()

        fun retryFailedCompositions() = this@Recomposer.retryFailedCompositions()
    }

    private class HotReloadable(private val composition: CompositionImpl) {
        private var composable: @Composable () -> Unit = composition.composable

        fun clearContent() {
            if (composition.isRoot) {
                composition.setContent {}
            }
        }

        fun resetContent() {
            composition.composable = composable
        }

        fun recompose() {
            if (composition.isRoot) {
                composition.setContent(composable)
            }
        }
    }

    @OptIn(ComposeToolingApi::class)
    private class RecomposerErrorState(
        override val cause: Throwable,
        override val isRecoverable: Boolean,
    ) : RecomposerErrorInfo, RecomposerErrorInformation {
        override val recoverable: Boolean
            get() = isRecoverable
    }

    private val recomposerInfo = RecomposerInfoImpl()

    /** Obtain a read-only [RecomposerInfo] for this [Recomposer]. */
    public fun asRecomposerInfo(): RecomposerInfo = recomposerInfo

    /**
     * Propagate all invalidations from `snapshotInvalidations` to all the known compositions.
     *
     * @return `true` if the frame has work to do (e.g. [hasFrameWorkLocked])
     */
    private fun recordComposerModifications(): Boolean {
        var compositions: List<ControlledComposition> = emptyList()
        val changes =
            synchronized(stateLock) {
                if (snapshotInvalidations.isEmpty()) return hasFrameWorkLocked
                compositions = knownCompositionsLocked()
                snapshotInvalidations.wrapIntoSet().also {
                    snapshotInvalidations = MutableScatterSet()
                }
            }
        var complete = false
        try {
            run {
                compositions.fastForEach { composition ->
                    composition.recordModificationsOf(changes)

                    // Stop dispatching if the recomposer if we detect the recomposer
                    // is shutdown.
                    if (_state.value <= State.ShuttingDown) return@run
                }
            }
            complete = true
        } finally {
            if (!complete) {
                // If the previous loop was not complete, we have not sent all of theses
                // changes to all the composers so try again after the exception that caused
                // the early exit is handled and we can then retry sending the changes.
                synchronized(stateLock) { snapshotInvalidations.addAll(changes) }
            }
        }
        return synchronized(stateLock) {
            if (deriveStateLocked() != null) {
                error("called outside of runRecomposeAndApplyChanges")
            }
            hasFrameWorkLocked
        }
    }

    private fun registerRunnerJob(callingJob: Job) {
        synchronized(stateLock) {
            closeCause?.let { throw it }
            if (_state.value <= State.ShuttingDown) error("Recomposer shut down")
            if (runnerJob != null) error("Recomposer already running")
            runnerJob = callingJob
            if (deriveStateLocked() != null) {
                composeImmediateRuntimeError("called outside of runRecomposeAndApplyChanges")
            }
        }
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their changes
     * to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method will not return unless the [Recomposer] is [close]d and all effects in managed
     * compositions complete. Unhandled failure exceptions from child coroutines will be thrown by
     * this method.
     */
    public suspend fun runRecomposeAndApplyChanges(): Unit =
        recompositionRunner { parentFrameClock ->
            val toRecompose = mutableListOf<ControlledComposition>()
            val toInsert = mutableListOf<MovableContentStateReference>()
            val toApply = mutableListOf<ControlledComposition>()
            val toLateApply = mutableScatterSetOf<ControlledComposition>()
            val toComplete = mutableScatterSetOf<ControlledComposition>()
            val modifiedValues = MutableScatterSet<Any>()
            val modifiedValuesSet = modifiedValues.wrapIntoSet()
            val alreadyComposed = mutableScatterSetOf<ControlledComposition>()

            fun clearRecompositionState() {
                synchronized(stateLock) {
                    toRecompose.clear()
                    toInsert.clear()

                    toApply.fastForEach {
                        it.abandonChanges()
                        recordFailedCompositionLocked(it)
                    }
                    toApply.clear()

                    toLateApply.forEach {
                        it.abandonChanges()
                        recordFailedCompositionLocked(it)
                    }
                    toLateApply.clear()

                    toComplete.forEach { it.changesApplied() }
                    toComplete.clear()

                    modifiedValues.clear()

                    alreadyComposed.forEach {
                        it.abandonChanges()
                        recordFailedCompositionLocked(it)
                    }
                    alreadyComposed.clear()
                }
            }

            fun fillToInsert() {
                toInsert.clear()
                synchronized(stateLock) {
                    movableContentAwaitingInsert.fastForEach { toInsert += it }
                    movableContentAwaitingInsert.clear()
                }
            }

            while (shouldKeepRecomposing) {
                awaitWorkAvailable()

                // Don't await a new frame if we don't have frame-scoped work
                if (!recordComposerModifications()) continue

                // Align work with the next frame to coalesce changes.
                // Note: it is possible to resume from the above with no recompositions pending,
                // instead someone might be awaiting our frame clock dispatch below.
                // We use the cached frame clock from above not just so that we don't locate it
                // each time, but because we've installed the broadcastFrameClock as the scope
                // clock above for user code to locate.
                parentFrameClock.withFrameNanos { frameTime ->
                    // Dispatch MonotonicFrameClock frames first; this may produce new
                    // composer invalidations that we must handle during the same frame.
                    if (hasBroadcastFrameClockAwaiters) {
                        trace("Recomposer:animation") {
                            // Propagate the frame time to anyone who is awaiting from the
                            // recomposer clock.
                            broadcastFrameClock.sendFrame(frameTime)

                            // Ensure any global changes are observed
                            Snapshot.sendApplyNotifications()
                        }
                    }

                    trace("Recomposer:recompose") {
                        // Drain any composer invalidations from snapshot changes and record
                        // composers to work on
                        recordComposerModifications()
                        synchronized(stateLock) {
                            compositionInvalidations.forEach { toRecompose += it }
                            compositionInvalidations.clear()
                        }

                        // Perform recomposition for any invalidated composers
                        modifiedValues.clear()
                        alreadyComposed.clear()
                        while (toRecompose.isNotEmpty() || toInsert.isNotEmpty()) {
                            try {
                                toRecompose.fastForEach { composition ->
                                    performRecompose(composition, modifiedValues)?.let {
                                        toApply += it
                                    }
                                    alreadyComposed.add(composition)
                                }
                            } catch (e: Throwable) {
                                processCompositionError(e, recoverable = true)
                                clearRecompositionState()
                                return@withFrameNanos
                            } finally {
                                toRecompose.clear()
                            }

                            // Find any trailing recompositions that need to be composed because
                            // of a value change by a composition. This can happen, for example, if
                            // a CompositionLocal changes in a parent and was read in a child
                            // composition that was otherwise valid.
                            if (
                                modifiedValues.isNotEmpty() || compositionInvalidations.isNotEmpty()
                            ) {
                                synchronized(stateLock) {
                                    knownCompositionsLocked().fastForEach { value ->
                                        if (
                                            value !in alreadyComposed &&
                                                value.observesAnyOf(modifiedValuesSet)
                                        ) {
                                            toRecompose += value
                                        }
                                    }

                                    // Composable lambda is a special kind of value that is not
                                    // observed
                                    // by the snapshot system, but invalidates composition scope
                                    // directly instead.
                                    compositionInvalidations.removeIf { value ->
                                        if (value !in alreadyComposed && value !in toRecompose) {
                                            toRecompose += value
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                }
                            }

                            if (toRecompose.isEmpty()) {
                                try {
                                    fillToInsert()
                                    while (toInsert.isNotEmpty()) {
                                        toLateApply += performInsertValues(toInsert, modifiedValues)
                                        fillToInsert()
                                    }
                                } catch (e: Throwable) {
                                    processCompositionError(e, recoverable = true)
                                    clearRecompositionState()
                                    return@withFrameNanos
                                }
                            }
                        }

                        // This is an optimization to avoid reallocating TransparentSnapshot for
                        // each observeChanges within `apply`. Many modifiers use observation in
                        // `onAttach` and other lifecycle methods, and allocations can be mitigated
                        // by updating read observer in the snapshot allocated here.
                        withTransparentSnapshot {
                            if (toApply.isNotEmpty()) {
                                changeCount++

                                // Perform apply changes
                                try {
                                    // We could do toComplete += toApply but doing it like below
                                    // avoids unnecessary allocations since toApply is a mutable
                                    // list
                                    // toComplete += toApply
                                    toApply.fastForEach { composition ->
                                        toComplete.add(composition)
                                    }
                                    toApply.fastForEach { composition ->
                                        composition.applyChanges()
                                    }
                                } catch (e: Throwable) {
                                    processCompositionError(e)
                                    clearRecompositionState()
                                    return@withFrameNanos
                                } finally {
                                    toApply.clear()
                                }
                            }

                            if (toLateApply.isNotEmpty()) {
                                try {
                                    toComplete += toLateApply
                                    toLateApply.forEach { composition ->
                                        composition.applyLateChanges()
                                    }
                                } catch (e: Throwable) {
                                    processCompositionError(e)
                                    clearRecompositionState()
                                    return@withFrameNanos
                                } finally {
                                    toLateApply.clear()
                                }
                            }

                            if (toComplete.isNotEmpty()) {
                                try {
                                    toComplete.forEach { composition ->
                                        composition.changesApplied()
                                    }
                                } catch (e: Throwable) {
                                    processCompositionError(e)
                                    clearRecompositionState()
                                    return@withFrameNanos
                                } finally {
                                    toComplete.clear()
                                }
                            }
                        }

                        synchronized(stateLock) {
                            runtimeCheck(deriveStateLocked() == null) {
                                "unexpected to get continuation here"
                            }
                        }

                        // Ensure any state objects that were written during apply changes, e.g.
                        // nodes with state-backed properties, get sent apply notifications to
                        // invalidate anything observing the nodes. Call this method instead of
                        // sendApplyNotifications to ensure that objects that were _created_ in this
                        // snapshot are also considered changed after this point.
                        Snapshot.notifyObjectsInitialized()
                        alreadyComposed.clear()
                        modifiedValues.clear()
                        compositionsRemoved = null
                    }
                }

                discardUnusedMovableContentState()
                nextFrameEndCallbackQueue.markFrameComplete()
            }
        }

    private fun processCompositionError(
        e: Throwable,
        failedInitialComposition: ControlledComposition? = null,
        recoverable: Boolean = false,
    ) {
        if (_hotReloadEnabled.get() && e !is ComposeRuntimeError) {
            synchronized(stateLock) {
                logError("Error was captured in composition while live edit was enabled.", e)

                compositionsAwaitingApply.clear()
                compositionInvalidations.clear()
                snapshotInvalidations = MutableScatterSet()

                movableContentAwaitingInsert.clear()
                movableContentRemoved.clear()
                movableContentStatesAvailable.clear()

                errorState.value = RecomposerErrorState(isRecoverable = recoverable, cause = e)

                if (failedInitialComposition != null) {
                    recordFailedCompositionLocked(failedInitialComposition)
                }

                if (deriveStateLocked() != null) {
                    composeImmediateRuntimeError(
                        "expected to go to inactive state due to composition error"
                    )
                }
            }
        } else {
            // withFrameNanos uses `runCatching` to ensure that crashes are not propagated to
            // AndroidUiDispatcher. This means that errors that happen during recomposition might
            // be delayed by a frame and swallowed if composed into inconsistent state caused by
            // the error.
            // Common case is subcomposition: if measure occurs after recomposition has thrown,
            // composeInitial will throw because of corrupted composition while original exception
            // won't be recorded.
            synchronized(stateLock) {
                logError("Error was captured in composition.", e)
                val errorState = errorState.value
                if (errorState == null) {
                    // Record exception if current error state is empty.
                    this.errorState.value = RecomposerErrorState(isRecoverable = false, cause = e)
                } else {
                    // Re-throw original cause if we recorded it previously.
                    throw errorState.cause
                }
            }

            throw e
        }
    }

    private inline fun withTransparentSnapshot(block: () -> Unit) {
        val currentSnapshot = Snapshot.current

        val snapshot =
            if (currentSnapshot is MutableSnapshot) {
                TransparentObserverMutableSnapshot(
                    currentSnapshot,
                    null,
                    null,
                    mergeParentObservers = true,
                    ownsParentSnapshot = false,
                )
            } else {
                TransparentObserverSnapshot(
                    currentSnapshot,
                    null,
                    mergeParentObservers = true,
                    ownsParentSnapshot = false,
                )
            }
        try {
            snapshot.enter(block)
        } finally {
            snapshot.dispose()
        }
    }

    /**
     * Returns a cached copy of the list of known compositions that can be iterated safely without
     * holding the `stateLock`.
     */
    private fun knownCompositions(): List<ControlledComposition> {
        return synchronized(stateLock) { knownCompositionsLocked() }
    }

    private fun knownCompositionsLocked(): List<ControlledComposition> {
        val cache = _knownCompositionsCache
        if (cache != null) return cache

        val compositions = _knownCompositions
        val newCache = if (compositions.isEmpty()) emptyList() else ArrayList(compositions)
        _knownCompositionsCache = newCache
        return newCache
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private fun clearKnownCompositionsLocked() {
        knownCompositionsLocked().fastForEach { composition ->
            unregisterCompositionLocked(composition)
        }
        _knownCompositions.clear()
        _knownCompositionsCache = emptyList()
    }

    private fun removeKnownCompositionLocked(composition: ControlledComposition) {
        if (_knownCompositions.remove(composition)) {
            _knownCompositionsCache = null
            unregisterCompositionLocked(composition)
        }
    }

    private fun addKnownCompositionLocked(composition: ControlledComposition) {
        _knownCompositions += composition
        _knownCompositionsCache = null
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private fun registerCompositionLocked(composition: ControlledComposition) {
        registrationObservers?.forEach {
            if (composition is ObservableComposition) {
                it.onCompositionRegistered(composition)
            }
        }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    private fun unregisterCompositionLocked(composition: ControlledComposition) {
        registrationObservers?.forEach {
            if (composition is ObservableComposition) {
                it.onCompositionUnregistered(composition)
            }
        }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    internal fun addCompositionRegistrationObserver(
        observer: CompositionRegistrationObserver
    ): CompositionObserverHandle {
        synchronized(stateLock) {
            val observers =
                registrationObservers
                    ?: MutableObjectList<CompositionRegistrationObserver>().also {
                        registrationObservers = it
                    }

            observers += observer
            _knownCompositions.fastForEach { composition ->
                if (composition is ObservableComposition) {
                    observer.onCompositionRegistered(composition)
                }
            }
        }

        return object : CompositionObserverHandle {
            override fun dispose() {
                synchronized(stateLock) { registrationObservers?.remove(observer) }
            }
        }
    }

    private fun resetErrorState(): RecomposerErrorState? {
        var error: RecomposerErrorState? = null
        synchronized(stateLock) {
                error = errorState.value
                if (error != null) {
                    errorState.value = null
                    deriveStateLocked()
                } else {
                    null
                }
            }
            ?.resume(Unit)
        return error
    }

    private fun retryFailedCompositions() {
        val compositionsToRetry =
            synchronized(stateLock) { failedCompositions.also { failedCompositions = null } }
                ?: return
        try {
            while (compositionsToRetry.isNotEmpty()) {
                val composition = compositionsToRetry.removeLastKt()
                if (composition !is CompositionImpl) continue

                composition.invalidateAll()
                composition.setContent(composition.composable)

                if (errorState.value != null) break
            }
        } finally {
            if (compositionsToRetry.isNotEmpty()) {
                // If we did not complete the last list then add the remaining compositions back
                // into the failedCompositions list
                synchronized(stateLock) {
                    compositionsToRetry.fastForEach { recordFailedCompositionLocked(it) }
                }
            }
        }
    }

    private fun recordFailedCompositionLocked(composition: ControlledComposition) {
        val failedCompositions =
            failedCompositions
                ?: mutableListOf<ControlledComposition>().also { failedCompositions = it }

        if (composition !in failedCompositions) {
            failedCompositions += composition
        }
        removeKnownCompositionLocked(composition)
    }

    private val hasSchedulingWork: Boolean
        get() =
            synchronized(stateLock) {
                snapshotInvalidations.isNotEmpty() ||
                    compositionInvalidations.isNotEmpty() ||
                    hasBroadcastFrameClockAwaitersLocked ||
                    hasNextFrameEndAwaitersLocked
            }

    private suspend fun awaitWorkAvailable() {
        if (!hasSchedulingWork) {
            // NOTE: Do not remove the `<Unit>` from the next line even if the IDE reports it as
            // redundant. Removing this causes reports it cannot infer the type. (KT-79553)
            @Suppress("RemoveExplicitTypeArguments") // See note above
            suspendCancellableCoroutine<Unit> { co ->
                synchronized(stateLock) {
                        if (hasSchedulingWork) {
                            co
                        } else {
                            workContinuation = co
                            null
                        }
                    }
                    ?.resume(Unit)
            }
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    private suspend fun recompositionRunner(
        block: suspend CoroutineScope.(parentFrameClock: MonotonicFrameClock) -> Unit
    ) {
        val parentFrameClock = coroutineContext.monotonicFrameClock
        withContext(broadcastFrameClock) {
            // Enforce mutual exclusion of callers; register self as current runner
            val callingJob = coroutineContext.job
            registerRunnerJob(callingJob)

            // Observe snapshot changes and propagate them to known composers only from
            // this caller's dispatcher, never working with the same composer in parallel.
            // unregisterApplyObserver is called as part of the big finally below
            val unregisterApplyObserver =
                Snapshot.registerApplyObserver { changed, _ ->
                    synchronized(stateLock) {
                            if (_state.value >= State.Idle) {
                                val snapshotInvalidations = snapshotInvalidations
                                changed.fastForEach {
                                    if (
                                        it is StateObjectImpl &&
                                            !it.isReadIn(ReaderKind.Composition)
                                    ) {
                                        // continue if we know that state is never read in
                                        // composition
                                        return@fastForEach
                                    }
                                    snapshotInvalidations.add(it)
                                }
                                deriveStateLocked()
                            } else null
                        }
                        ?.resume(Unit)
                }

            addRunning(recomposerInfo)

            try {
                // Invalidate all registered composers when we start since we weren't observing
                // snapshot changes on their behalf. Assume anything could have changed.
                knownCompositions().fastForEach { it.invalidateAll() }

                coroutineScope { block(parentFrameClock) }
            } finally {
                unregisterApplyObserver.dispose()
                synchronized(stateLock) {
                    if (runnerJob === callingJob) {
                        runnerJob = null
                    }
                    if (deriveStateLocked() != null) {
                        composeImmediateRuntimeError(
                            "called outside of runRecomposeAndApplyChanges"
                        )
                    }
                }
                removeRunning(recomposerInfo)
            }
        }
    }

    /**
     * Permanently shut down this [Recomposer] for future use. [currentState] will immediately
     * reflect [State.ShuttingDown] (or a lower state) before this call returns. All ongoing
     * recompositions will stop, new composer invalidations with this [Recomposer] at the root will
     * no longer occur, and any [LaunchedEffect]s currently running in compositions managed by this
     * [Recomposer] will be cancelled. Any [rememberCoroutineScope] scopes from compositions managed
     * by this [Recomposer] will also be cancelled. See [join] to await the completion of all of
     * these outstanding tasks.
     */
    public fun cancel() {
        // Move to State.ShuttingDown immediately rather than waiting for effectJob to join
        // if we're cancelling to shut down the Recomposer. This permits other client code
        // to use `state.first { it < State.Idle }` or similar to reliably and immediately detect
        // that the recomposer can no longer be used.
        // It looks like a CAS loop would be more appropriate here, but other occurrences
        // of taking stateLock assume that the state cannot change without holding it.
        synchronized(stateLock) {
            if (_state.value >= State.Idle) {
                _state.value = State.ShuttingDown
            }
        }
        effectJob.cancel()
    }

    /**
     * Close this [Recomposer]. Once all effects launched by managed compositions complete, any
     * active call to [runRecomposeAndApplyChanges] will return normally and this [Recomposer] will
     * be [State.ShutDown]. See [join] to await the completion of all of these outstanding tasks.
     */
    public fun close() {
        if (effectJob.complete()) {
            synchronized(stateLock) { isClosed = true }
        }
    }

    /** Await the completion of a [cancel] operation. */
    public suspend fun join() {
        currentState.first { it == State.ShutDown }
    }

    /**
     * Schedules an [action] to be invoked when the recomposer finishes the next composition of a
     * frame (including the completion of subcompositions). If a frame is currently in-progress,
     * [action] will be invoked when the current frame fully finishes composing. If a frame isn't
     * currently in-progress, a new frame will be scheduled (if one hasn't been already) and
     * [action] will execute at the completion of the next frame's composition. If a new frame is
     * scheduled and there is no other work to execute, [action] will still execute.
     *
     * [action] will always execute on the applier thread.
     *
     * @return A [CancellationHandle] that can be used to unregister the [action]. The returned
     *   handle is thread-safe and may be cancelled from any thread. Cancelling the handle only
     *   removes the callback from the queue. If [action] is currently executing, it will not be
     *   cancelled by this handle.
     */
    public override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
        return nextFrameEndCallbackQueue.scheduleFrameEndCallback(action)
    }

    internal override fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit,
    ) {
        val composerWasComposing = composition.isComposing

        val newComposition =
            synchronized(stateLock) {
                if (_state.value > State.ShuttingDown) {
                    val new = composition !in knownCompositionsLocked()
                    if (new) {
                        registerCompositionLocked(composition)
                    }
                    new
                } else {
                    true
                }
            }

        try {
            composing(composition, null) { composition.composeContent(content) }
        } catch (e: Throwable) {
            if (newComposition) {
                synchronized(stateLock) { unregisterCompositionLocked(composition) }
            }

            processCompositionError(e, composition, recoverable = true)
            return
        }

        synchronized(stateLock) {
            if (_state.value > State.ShuttingDown) {
                if (composition !in knownCompositionsLocked()) {
                    addKnownCompositionLocked(composition)
                }
            } else {
                unregisterCompositionLocked(composition)
            }
        }

        // TODO(b/143755743)
        if (!composerWasComposing) {
            Snapshot.notifyObjectsInitialized()
        }

        try {
            performInitialMovableContentInserts(composition)
        } catch (e: Throwable) {
            processCompositionError(e, composition, recoverable = true)
            return
        }

        try {
            composition.applyChanges()
            composition.applyLateChanges()
        } catch (e: Throwable) {
            processCompositionError(e)
            return
        }

        if (!composerWasComposing) {
            // Ensure that any state objects created during applyChanges are seen as changed
            // if modified after this call.
            Snapshot.notifyObjectsInitialized()
        }
    }

    internal override fun composeInitialPaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        content: @Composable () -> Unit,
    ): ScatterSet<RecomposeScopeImpl> {
        return try {
            composition.pausable(shouldPause) {
                composeInitial(composition, content)
                pausedScopes.get() ?: emptyScatterSet()
            }
        } finally {
            pausedScopes.set(null)
        }
    }

    internal override fun recomposePaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        invalidScopes: ScatterSet<RecomposeScopeImpl>,
    ): ScatterSet<RecomposeScopeImpl> {
        return try {
            recordComposerModifications()
            composition.recordModificationsOf(invalidScopes.wrapIntoSet())
            composition.pausable(shouldPause) {
                val needsApply = performRecompose(composition, null)
                if (needsApply != null) {
                    performInitialMovableContentInserts(composition)
                    needsApply.applyChanges()
                    needsApply.applyLateChanges()
                }
                pausedScopes.get() ?: emptyScatterSet()
            }
        } finally {
            pausedScopes.set(null)
        }
    }

    override fun reportPausedScope(scope: RecomposeScopeImpl) {
        val scopes =
            pausedScopes.get()
                ?: run {
                    val newScopes = mutableScatterSetOf<RecomposeScopeImpl>()
                    pausedScopes.set(newScopes)
                    newScopes
                }
        scopes.add(scope)
    }

    private fun performInitialMovableContentInserts(composition: ControlledComposition) {
        synchronized(stateLock) {
            if (!movableContentAwaitingInsert.fastAny { it.composition == composition }) return
        }
        val toInsert = mutableListOf<MovableContentStateReference>()
        fun fillToInsert() {
            toInsert.clear()
            synchronized(stateLock) {
                val iterator = movableContentAwaitingInsert.iterator()
                while (iterator.hasNext()) {
                    val value = iterator.next()
                    if (value.composition == composition) {
                        toInsert.add(value)
                        iterator.remove()
                    }
                }
            }
        }
        fillToInsert()
        while (toInsert.isNotEmpty()) {
            performInsertValues(toInsert, null)
            fillToInsert()
        }
    }

    private fun performRecompose(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?,
    ): ControlledComposition? {
        if (
            composition.isComposing ||
                composition.isDisposed ||
                compositionsRemoved?.contains(composition) == true
        )
            return null

        return if (
            composing(composition, modifiedValues) {
                if (modifiedValues?.isNotEmpty() == true) {
                    // Record write performed by a previous composition as if they happened during
                    // composition.
                    composition.prepareCompose {
                        modifiedValues.forEach { composition.recordWriteOf(it) }
                    }
                }
                composition.recompose()
            }
        )
            composition
        else null
    }

    @OptIn(ExperimentalComposeApi::class)
    private fun performInsertValues(
        references: List<MovableContentStateReference>,
        modifiedValues: MutableScatterSet<Any>?,
    ): List<ControlledComposition> {
        val tasks = references.fastGroupBy { it.composition }
        for ((composition, refs) in tasks) {
            runtimeCheck(!composition.isComposing)
            composing(composition, modifiedValues) {
                // Map insert movable content to movable content states that have been released
                // during `performRecompose`.
                val pairs =
                    synchronized(stateLock) {
                        refs
                            .fastMap { reference ->
                                reference to
                                    movableContentRemoved.removeLast(reference.content).also {
                                        if (it != null) {
                                            movableContentNestedStatesAvailable.usedContainer(it)
                                        }
                                    }
                            }
                            .let { pairs ->
                                // Check for any nested states
                                if (
                                    pairs.fastAny {
                                        it.second == null &&
                                            it.first.content in movableContentNestedStatesAvailable
                                    }
                                ) {
                                    // We have at least one nested state we could use, if a state
                                    // is available for the container then schedule the state to be
                                    // removed from the container when it is released.
                                    pairs.fastMap { pair ->
                                        if (pair.second == null) {
                                            val nestedContentReference =
                                                movableContentNestedStatesAvailable.removeLast(
                                                    pair.first.content
                                                )
                                            if (nestedContentReference == null) return@fastMap pair
                                            val content = nestedContentReference.content
                                            val container = nestedContentReference.container
                                            movableContentNestedExtractionsPending.add(
                                                container,
                                                content,
                                            )
                                            pair.first to content
                                        } else pair
                                    }
                                } else pairs
                            }
                    }

                // Avoid mixing creating new content with moving content as the moved content
                // may release content when it is moved as it is recomposed when move.
                val toInsert =
                    if (
                        pairs.fastAll { it.second == null } || pairs.fastAll { it.second != null }
                    ) {
                        pairs
                    } else {
                        // Return the content not moving to the awaiting list. These will come back
                        // here in the next iteration of the caller's loop and either have content
                        // to move or by still needing to create the content.
                        val toReturn =
                            pairs.fastMapNotNull { item ->
                                if (item.second == null) item.first else null
                            }
                        synchronized(stateLock) { movableContentAwaitingInsert += toReturn }

                        // Only insert the moving content this time
                        pairs.fastFilterIndexed { _, item -> item.second != null }
                    }

                // toInsert is guaranteed to be not empty as,
                // 1) refs is guaranteed to be not empty as a condition of groupBy
                // 2) pairs is guaranteed to be not empty as it is a map of refs
                // 3) toInsert is guaranteed to not be empty because the toReturn and toInsert
                //    lists have at least one item by the condition of the guard in the if
                //    expression. If one would be empty the condition is true and the filter is not
                //    performed. As both have at least one item toInsert has at least one item. If
                //    the filter is not performed the list is pairs which has at least one item.
                composition.insertMovableContent(toInsert)
            }
        }
        return tasks.keys.toList()
    }

    private fun discardUnusedMovableContentState() {
        val unusedValues =
            synchronized(stateLock) {
                if (movableContentRemoved.isNotEmpty()) {
                    val references = movableContentRemoved.values()
                    movableContentRemoved.clear()
                    movableContentNestedStatesAvailable.clear()
                    movableContentNestedExtractionsPending.clear()
                    val unusedValues =
                        references.fastMap { it to movableContentStatesAvailable[it] }
                    movableContentStatesAvailable.clear()
                    unusedValues
                } else emptyObjectList()
            }
        unusedValues.forEach { (reference, state) ->
            if (state != null) {
                reference.composition.disposeUnusedMovableContent(state)
            }
        }
    }

    private fun readObserverOf(composition: ControlledComposition): (Any) -> Unit {
        return { value -> composition.recordReadOf(value) }
    }

    private fun writeObserverOf(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?,
    ): (Any) -> Unit {
        return { value ->
            composition.recordWriteOf(value)
            modifiedValues?.add(value)
        }
    }

    private inline fun <T> composing(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?,
        block: () -> T,
    ): T {
        val snapshot =
            Snapshot.takeMutableSnapshot(
                readObserverOf(composition),
                writeObserverOf(composition, modifiedValues),
            )
        try {
            return snapshot.enter(block)
        } finally {
            applyAndCheck(snapshot)
        }
    }

    private fun applyAndCheck(snapshot: MutableSnapshot) {
        try {
            val applyResult = snapshot.apply()
            if (applyResult is SnapshotApplyResult.Failure) {
                error(
                    "Unsupported concurrent change during composition. A state object was " +
                        "modified by composition as well as being modified outside composition."
                )
            }
        } finally {
            snapshot.dispose()
        }
    }

    /**
     * `true` if this [Recomposer] has any pending work scheduled, regardless of whether or not it
     * is currently [running][runRecomposeAndApplyChanges].
     */
    public val hasPendingWork: Boolean
        get() =
            synchronized(stateLock) {
                snapshotInvalidations.isNotEmpty() ||
                    compositionInvalidations.isNotEmpty() ||
                    concurrentCompositionsOutstanding > 0 ||
                    compositionsAwaitingApply.isNotEmpty() ||
                    hasBroadcastFrameClockAwaitersLocked ||
                    hasNextFrameEndAwaitersLocked ||
                    movableContentRemoved.isNotEmpty()
            }

    private val hasFrameWorkLocked: Boolean
        get() =
            compositionInvalidations.isNotEmpty() ||
                hasBroadcastFrameClockAwaitersLocked ||
                hasNextFrameEndAwaitersLocked ||
                movableContentRemoved.isNotEmpty()

    /**
     * Suspends until the currently pending recomposition frame is complete. Any recomposition for
     * this recomposer triggered by actions before this call begins will be complete and applied (if
     * recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    public suspend fun awaitIdle() {
        currentState.takeWhile { it > State.Idle }.collect()
    }

    /**
     * Pause broadcasting the frame clock while recomposing. This effectively pauses animations, or
     * any other use of the [withFrameNanos], while the frame clock is paused.
     *
     * [pauseCompositionFrameClock] should be called when the recomposer is not being displayed for
     * some reason such as not being the current activity in Android, for example.
     *
     * Calls to [pauseCompositionFrameClock] are thread-safe and idempotent (calling it when the
     * frame clock is already paused is a no-op).
     */
    public fun pauseCompositionFrameClock() {
        synchronized(stateLock) { frameClockPaused = true }
    }

    /**
     * Resume broadcasting the frame clock after is has been paused. Pending calls to
     * [withFrameNanos] will start receiving frame clock broadcasts at the beginning of the frame
     * and a frame will be requested if there are pending calls to [withFrameNanos] if a frame has
     * not already been scheduled.
     *
     * Calls to [resumeCompositionFrameClock] are thread-safe and idempotent (calling it when the
     * frame clock is running is a no-op).
     */
    public fun resumeCompositionFrameClock() {
        synchronized(stateLock) {
                if (frameClockPaused) {
                    frameClockPaused = false
                    deriveStateLocked()
                } else null
            }
            ?.resume(Unit)
    }

    // Recomposer always starts with a constant compound hash
    internal override val compositeKeyHashCode: CompositeKeyHashCode
        get() = RecomposerCompoundHashKey

    internal override val collectingCallByInformation: Boolean
        get() = _hotReloadEnabled.get()

    // Collecting parameter happens at the level of a composer; starts as false
    internal override val collectingParameterInformation: Boolean
        get() = false

    internal override val collectingSourceInformation: Boolean
        get() = composeStackTraceMode == ComposeStackTraceMode.SourceInformation

    internal override val stackTraceEnabled: Boolean
        get() = composeStackTraceMode != ComposeStackTraceMode.None

    internal override fun recordInspectionTable(table: MutableSet<CompositionData>) {
        // TODO: The root recomposer might be a better place to set up inspection
        // than the current configuration with an CompositionLocal
    }

    internal override fun registerComposition(composition: ControlledComposition) {
        // Do nothing.
    }

    internal override fun unregisterComposition(composition: ControlledComposition) {
        synchronized(stateLock) {
            removeKnownCompositionLocked(composition)
            compositionInvalidations -= composition
            compositionsAwaitingApply -= composition
        }
    }

    internal override fun invalidate(composition: ControlledComposition) {
        synchronized(stateLock) {
                if (composition !in compositionInvalidations) {
                    compositionInvalidations += composition
                    deriveStateLocked()
                } else null
            }
            ?.resume(Unit)
    }

    internal override fun invalidateScope(scope: RecomposeScopeImpl) {
        synchronized(stateLock) {
                snapshotInvalidations.add(scope)
                deriveStateLocked()
            }
            ?.resume(Unit)
    }

    internal override fun insertMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
                movableContentAwaitingInsert += reference
                deriveStateLocked()
            }
            ?.resume(Unit)
    }

    internal override fun deletedMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
                movableContentRemoved.add(reference.content, reference)
                if (reference.nestedReferences != null) {
                    val container = reference
                    fun recordNestedStatesOf(reference: MovableContentStateReference) {
                        reference.nestedReferences?.fastForEach { nestedReference ->
                            movableContentNestedStatesAvailable.add(
                                nestedReference.content,
                                NestedMovableContent(nestedReference, container),
                            )
                            recordNestedStatesOf(nestedReference)
                        }
                    }
                    recordNestedStatesOf(reference)
                }
                deriveStateLocked()
            }
            ?.resume(Unit)
    }

    internal override fun movableContentStateReleased(
        reference: MovableContentStateReference,
        data: MovableContentState,
        applier: Applier<*>,
    ) {
        synchronized(stateLock) {
            movableContentStatesAvailable[reference] = data
            val extractions = movableContentNestedExtractionsPending[reference]
            if (extractions.isNotEmpty()) {
                val states = data.slotStorage.extractNestedStates(applier, extractions)
                states.forEach { reference, state ->
                    movableContentStatesAvailable[reference] = state
                }
            }
        }
    }

    internal override fun reportRemovedComposition(composition: ControlledComposition) {
        synchronized(stateLock) {
            val compositionsRemoved =
                compositionsRemoved
                    ?: mutableScatterSetOf<ControlledComposition>().also {
                        compositionsRemoved = it
                    }
            compositionsRemoved.add(composition)
        }
    }

    override fun movableContentStateResolve(
        reference: MovableContentStateReference
    ): MovableContentState? =
        synchronized(stateLock) { movableContentStatesAvailable.remove(reference) }

    override val composition: Composition?
        get() = null

    /**
     * hack: the companion object is thread local in Kotlin/Native to avoid freezing
     * [_runningRecomposers] with the current memory model. As a side effect, recomposers are now
     * forced to be single threaded in Kotlin/Native targets.
     *
     * This annotation WILL BE REMOVED with the new memory model of Kotlin/Native.
     */
    @ThreadLocal
    public companion object {

        private val _runningRecomposers = MutableStateFlow(persistentSetOf<RecomposerInfoImpl>())

        private val _hotReloadEnabled = AtomicReference(false)

        /**
         * An observable [Set] of [RecomposerInfo]s for currently
         * [running][runRecomposeAndApplyChanges] [Recomposer]s. Emitted sets are immutable.
         */
        public val runningRecomposers: StateFlow<Set<RecomposerInfo>>
            get() = _runningRecomposers

        internal fun setHotReloadEnabled(value: Boolean) {
            _hotReloadEnabled.set(value)
        }

        private fun addRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.add(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        private fun removeRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.remove(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        internal fun saveStateAndDisposeForHotReload(): Any {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            _hotReloadEnabled.set(true)
            return _runningRecomposers.value.flatMap { it.saveStateAndDisposeForHotReload() }
        }

        internal fun loadStateAndComposeForHotReload(token: Any) {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            _hotReloadEnabled.set(true)

            _runningRecomposers.value.forEach { it.resetErrorState() }

            @Suppress("UNCHECKED_CAST") val holders = token as List<HotReloadable>
            holders.fastForEach { it.resetContent() }
            holders.fastForEach { it.recompose() }

            _runningRecomposers.value.forEach { it.retryFailedCompositions() }
        }

        @OptIn(ComposeToolingApi::class)
        internal fun invalidateGroupsWithKey(key: Int) {
            _hotReloadEnabled.set(true)
            _runningRecomposers.value.forEach {
                if (it.currentError?.isRecoverable == false) {
                    return@forEach
                }

                it.resetErrorState()

                it.invalidateGroupsWithKey(key)

                it.retryFailedCompositions()
            }
        }

        /** This is an internal API only kept for backward compatibility. */
        @OptIn(ComposeToolingApi::class)
        internal fun getCurrentErrors(): List<RecomposerErrorInfo> =
            _runningRecomposers.value.mapNotNull { it.currentError as? RecomposerErrorInfo }

        @OptIn(ComposeToolingApi::class)
        internal fun getRecomposerErrors(): List<RecomposerErrorInformation> =
            _runningRecomposers.value.mapNotNull { it.currentError }

        internal fun clearErrors() {
            _runningRecomposers.value.mapNotNull { it.resetErrorState() }
        }
    }
}

/** Sentinel used by [ProduceFrameSignal] */
private val ProduceAnotherFrame = Any()
private val FramePending = Any()

/**
 * Multiple producer, single consumer conflated signal that tells concurrent composition when it
 * should try to produce another frame. This class is intended to be used along with a lock shared
 * between producers and consumer.
 */
private class ProduceFrameSignal {
    private var pendingFrameContinuation: Any? = null

    /**
     * Suspend until a frame is requested. After this method returns the signal is in a
     * [FramePending] state which must be acknowledged by a call to [takeFrameRequestLocked] once
     * all data that will be used to produce the frame has been claimed.
     */
    suspend fun awaitFrameRequest(lock: SynchronizedObject) {
        synchronized(lock) {
            if (pendingFrameContinuation === ProduceAnotherFrame) {
                pendingFrameContinuation = FramePending
                return
            }
        }
        suspendCancellableCoroutine<Unit> { co ->
            synchronized(lock) {
                    if (pendingFrameContinuation === ProduceAnotherFrame) {
                        pendingFrameContinuation = FramePending
                        co
                    } else {
                        pendingFrameContinuation = co
                        null
                    }
                }
                ?.resume(Unit)
        }
    }

    /**
     * Signal from the frame request consumer that the frame is beginning with data that was
     * available up until this point. (Synchronizing access to that data is up to the caller.)
     */
    fun takeFrameRequestLocked() {
        checkPrecondition(pendingFrameContinuation === FramePending) { "frame not pending" }
        pendingFrameContinuation = null
    }

    fun requestFrameLocked(): Continuation<Unit>? =
        when (val co = pendingFrameContinuation) {
            is Continuation<*> -> {
                pendingFrameContinuation = FramePending
                @Suppress("UNCHECKED_CAST")
                co as Continuation<Unit>
            }
            ProduceAnotherFrame,
            FramePending -> null
            null -> {
                pendingFrameContinuation = ProduceAnotherFrame
                null
            }
            else -> error("invalid pendingFrameContinuation $co")
        }
}

@OptIn(InternalComposeApi::class)
private class NestedContentMap {
    private val contentMap = MultiValueMap<MovableContent<Any?>, NestedMovableContent>()
    private val containerMap = MultiValueMap<MovableContentStateReference, MovableContent<Any?>>()

    fun add(content: MovableContent<Any?>, nestedContent: NestedMovableContent) {
        contentMap.add(content, nestedContent)
        containerMap.add(nestedContent.container, content)
    }

    fun clear() {
        contentMap.clear()
        containerMap.clear()
    }

    fun removeLast(key: MovableContent<Any?>) =
        contentMap.removeLast(key).also { if (contentMap.isEmpty()) containerMap.clear() }

    operator fun contains(key: MovableContent<Any?>) = key in contentMap

    fun usedContainer(reference: MovableContentStateReference) {
        containerMap.forEachValue(reference) { value ->
            contentMap.removeValueIf(value) { it.container == reference }
        }
    }
}

@InternalComposeApi
private class NestedMovableContent(
    val content: MovableContentStateReference,
    val container: MovableContentStateReference,
)
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/SnapshotState.kt
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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.GlobalSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotId
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.overwritable
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.toSnapshotId
import androidx.compose.runtime.snapshots.withCurrent
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * Return a new [MutableState] initialized with the passed in [value]
 *
 * The MutableState class is a single value holder whose reads and writes are observed by Compose.
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the initial value for the [MutableState]
 * @param policy a policy to controls how changes are handled in mutable snapshots.
 * @sample androidx.compose.runtime.samples.SimpleStateSample
 * @sample androidx.compose.runtime.samples.DestructuredStateSample
 * @sample androidx.compose.runtime.samples.observeUserSample
 * @sample androidx.compose.runtime.samples.stateSample
 * @see State
 * @see MutableState
 * @see SnapshotMutationPolicy
 * @see mutableIntStateOf
 * @see mutableLongStateOf
 * @see mutableFloatStateOf
 * @see mutableDoubleStateOf
 */
@StateFactoryMarker
public fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
): MutableState<T> = createSnapshotMutableState(value, policy)

/**
 * A value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value.
 *
 * @see [MutableState]
 * @see [mutableStateOf]
 */
@Stable
public interface State<out T> {
    public val value: T
}

/**
 * Permits property delegation of `val`s using `by` for [State].
 *
 * @sample androidx.compose.runtime.samples.DelegatedReadOnlyStateSample
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

/**
 * A mutable value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value. When the
 * [value] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [value] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [State]
 * @see [mutableStateOf]
 */
@Stable
public interface MutableState<T> : State<T> {
    override var value: T

    public operator fun component1(): T

    public operator fun component2(): (T) -> Unit
}

/**
 * Permits property delegation of `var`s using `by` for [MutableState].
 *
 * @sample androidx.compose.runtime.samples.DelegatedStateSample
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T> MutableState<T>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

/** Returns platform specific implementation based on [SnapshotMutableStateImpl]. */
internal expect fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>,
): SnapshotMutableState<T>

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 * @param policy a policy to control how changes are handled in a mutable snapshot.
 * @see mutableStateOf
 * @see SnapshotMutationPolicy
 */
internal open class SnapshotMutableStateImpl<T>(
    value: T,
    override val policy: SnapshotMutationPolicy<T>,
) : StateObjectImpl(), SnapshotMutableState<T> {
    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = next.readable(this).value
        set(value) =
            next.withCurrent {
                if (!policy.equivalent(it.value, value)) {
                    next.overwritable(this, it) { this.value = value }
                }
            }

    private var next: StateStateRecord<T> =
        currentSnapshot().let { snapshot ->
            StateStateRecord(snapshot.snapshotId, value).also {
                if (snapshot !is GlobalSnapshot) {
                    it.next = StateStateRecord(Snapshot.PreexistingSnapshotId.toSnapshotId(), value)
                }
            }
        }

    override val firstStateRecord: StateRecord
        get() = next

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        next = value as StateStateRecord<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord,
    ): StateRecord? {
        val previousRecord = previous as StateStateRecord<T>
        val currentRecord = current as StateStateRecord<T>
        val appliedRecord = applied as StateStateRecord<T>
        return if (policy.equivalent(currentRecord.value, appliedRecord.value)) current
        else {
            val merged =
                policy.merge(previousRecord.value, currentRecord.value, appliedRecord.value)
            if (merged != null) {
                appliedRecord.create(appliedRecord.snapshotId).also { it.value = merged }
            } else {
                null
            }
        }
    }

    override fun toString(): String =
        next.withCurrent { "MutableState(value=${it.value})@${hashCode()}" }

    private class StateStateRecord<T>(snapshotId: SnapshotId, myValue: T) :
        StateRecord(snapshotId) {
        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateStateRecord<T>).value
        }

        override fun create() = StateStateRecord(currentSnapshot().snapshotId, value)

        override fun create(snapshotId: SnapshotId) =
            StateStateRecord(currentSnapshot().snapshotId, value)

        var value: T = myValue
    }

    /**
     * The componentN() operators allow state objects to be used with the property destructuring
     * syntax
     *
     * ```
     * var (foo, setFoo) = remember { mutableStateOf(0) }
     * setFoo(123) // set
     * foo == 123 // get
     * ```
     */
    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T
        @JvmName("getDebuggerDisplayValue") get() = next.withCurrent { it }.value
}

/**
 * Create a instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateListSample
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
public fun <T> mutableStateListOf(): SnapshotStateList<T> = SnapshotStateList<T>()

/**
 * Create an instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
public fun <T> mutableStateListOf(vararg elements: T): SnapshotStateList<T> =
    SnapshotStateList<T>().also { it.addAll(elements.toList()) }

/**
 * Create an instance of [MutableList]<T> from a collection that is observable and can be snapshot.
 */
public fun <T> Collection<T>.toMutableStateList(): SnapshotStateList<T> =
    SnapshotStateList<T>().also { it.addAll(this) }

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateMapSample
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
public fun <K, V> mutableStateMapOf(): SnapshotStateMap<K, V> = SnapshotStateMap<K, V>()

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
public fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>): SnapshotStateMap<K, V> =
    SnapshotStateMap<K, V>().apply { putAll(pairs.toMap()) }

/**
 * Create an instance of [MutableMap]<K, V> from a collection of pairs that is observable and can be
 * snapshot.
 */
@Suppress("unused")
public fun <K, V> Iterable<Pair<K, V>>.toMutableStateMap(): SnapshotStateMap<K, V> =
    SnapshotStateMap<K, V>().also { it.putAll(this.toMap()) }

/**
 * Create a instance of [MutableSet]<T> that is observable and can be snapshot.
 *
 * The returned set iteration order is in the order the items were inserted into the set.
 *
 * @sample androidx.compose.runtime.samples.stateSetSample
 * @see mutableStateOf
 * @see mutableSetOf
 * @see MutableSet
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker public fun <T> mutableStateSetOf(): SnapshotStateSet<T> = SnapshotStateSet<T>()

/**
 * Create an instance of [MutableSet]<T> that is observable and can be snapshot.
 *
 * The returned set iteration order is in the order the items were inserted into the set.
 *
 * @see mutableStateOf
 * @see mutableSetOf
 * @see MutableSet
 * @see Snapshot.takeSnapshot
 */
@StateFactoryMarker
public fun <T> mutableStateSetOf(vararg elements: T): SnapshotStateSet<T> =
    SnapshotStateSet<T>().also { it.addAll(elements.toSet()) }

/**
 * [remember] a [mutableStateOf] [newValue] and update its value to [newValue] on each recomposition
 * of the [rememberUpdatedState] call.
 *
 * [rememberUpdatedState] should be used when parameters or values computed during composition are
 * referenced by a long-lived lambda or object expression. Recomposition will update the resulting
 * [State] without recreating the long-lived lambda or object, allowing that object to persist
 * without cancelling and resubscribing, or relaunching a long-lived operation that may be expensive
 * or prohibitive to recreate and restart. This may be common when working with [DisposableEffect]
 * or [LaunchedEffect], for example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithDisposableEffect
 *
 * [LaunchedEffect]s often describe state machines that should not be reset and restarted if a
 * parameter or event callback changes, but they should have the current value available when
 * needed. For example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithLaunchedEffect
 *
 * By using [rememberUpdatedState] a composable function can update these operations in progress.
 */
@Composable
public fun <T> rememberUpdatedState(newValue: T): State<T> =
    remember { mutableStateOf(newValue) }.apply { value = newValue }
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Effects.kt
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

package androidx.compose.runtime

import androidx.compose.runtime.internal.PlatformOptimizedCancellationException
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.ComposeToolingFlags
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmField
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Schedule [effect] to run when the current composition completes successfully and applies changes.
 * [SideEffect] can be used to apply side effects to objects managed by the composition that are not
 * backed by [snapshots][androidx.compose.runtime.snapshots.Snapshot] so as not to leave those
 * objects in an inconsistent state if the current composition operation fails.
 *
 * [effect] will always be run on the composition's apply dispatcher and appliers are never run
 * concurrent with themselves, one another, applying changes to the composition tree, or running
 * [RememberObserver] event callbacks. [SideEffect]s are always run after [RememberObserver] event
 * callbacks.
 *
 * A [SideEffect] runs after **every** recomposition. To launch an ongoing task spanning potentially
 * many recompositions, see [LaunchedEffect]. To manage an event subscription or other object
 * lifecycle, see [DisposableEffect].
 */
@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
@OptIn(InternalComposeApi::class)
public fun SideEffect(effect: () -> Unit) {
    currentComposer.recordSideEffect(effect)
}

/**
 * Receiver scope for [DisposableEffect] that offers the [onDispose] clause that should be the last
 * statement in any call to [DisposableEffect].
 */
public class DisposableEffectScope {
    /**
     * Provide [onDisposeEffect] to the [DisposableEffect] to run when it leaves the composition or
     * its key changes.
     */
    public inline fun onDispose(crossinline onDisposeEffect: () -> Unit): DisposableEffectResult =
        object : DisposableEffectResult {
            override fun dispose() {
                onDisposeEffect()
            }
        }
}

public interface DisposableEffectResult {
    public fun dispose()
}

private val InternalDisposableEffectScope = DisposableEffectScope()

private class DisposableEffectImpl(
    private val effect: DisposableEffectScope.() -> DisposableEffectResult
) : RememberObserver {
    private var onDispose: DisposableEffectResult? = null

    override fun onRemembered() {
        onDispose = InternalDisposableEffectScope.effect()
    }

    override fun onForgotten() {
        onDispose?.dispose()
        onDispose = null
    }

    override fun onAbandoned() {
        // Nothing to do as [onRemembered] was not called.
    }
}

private const val DisposableEffectNoParamError =
    "DisposableEffect must provide one or more 'key' parameters that define the identity of " +
        "the DisposableEffect and determine when its previous effect should be disposed and " +
        "a new effect started for the new key."

private const val LaunchedEffectNoParamError =
    "LaunchedEffect must provide one or more 'key' parameters that define the identity of " +
        "the LaunchedEffect and determine when its previous effect coroutine should be cancelled " +
        "and a new effect launched for the new key."

/**
 * A side effect of composition that must be reversed or cleaned up if the [DisposableEffect] leaves
 * the composition.
 *
 * It is an error to call [DisposableEffect] without at least one `key` parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Composable
@NonRestartableComposable
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated(DisposableEffectNoParamError, level = DeprecationLevel.ERROR)
public fun DisposableEffect(effect: DisposableEffectScope.() -> DisposableEffectResult): Unit =
    error(DisposableEffectNoParamError)

/**
 * A side effect of composition that must run for any new unique value of [key1] and must be
 * reversed or cleaned up if [key1] changes or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun DisposableEffect(
    key1: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    remember(key1) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1] or [key2] and must
 * be reversed or cleaned up if [key1] or [key2] changes, or if the [DisposableEffect] leaves the
 * composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    remember(key1, key2) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1], [key2] or [key3]
 * and must be reversed or cleaned up if [key1], [key2] or [key3] changes, or if the
 * [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    remember(key1, key2, key3) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [keys] and must be
 * reversed or cleaned up if any [keys] change or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
@Suppress("ArrayReturn")
public fun DisposableEffect(
    vararg keys: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult,
) {
    remember(*keys) { DisposableEffectImpl(effect) }
}

internal class LaunchedEffectImpl(
    private val parentCoroutineContext: CoroutineContext,
    private val task: suspend CoroutineScope.() -> Unit,
) : RememberObserver, CoroutineExceptionHandler {
    private val scope: CoroutineScope
    private var job: Job? = null

    init {
        var context = parentCoroutineContext + this
        @OptIn(ComposeToolingApi::class)
        if (ComposeToolingFlags.isVerboseTracingEnabled) {
            context += LaunchedEffectTracingContext
        }
        scope = CoroutineScope(context)
    }

    override fun onRemembered() {
        // This should never happen but is left here for safety
        job?.cancel("Old job was still running!")
        job = scope.launch(block = task)
    }

    override fun onForgotten() {
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }

    override fun onAbandoned() {
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }

    // CoroutineExceptionHandler implementation to save on allocations
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler.Key

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        context[CompositionErrorContextImpl]?.apply {
            exception.attachComposeStackTrace(this@LaunchedEffectImpl)
        }
        parentCoroutineContext[CoroutineExceptionHandler]?.handleException(context, exception)
            ?: throw exception
    }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] when the [LaunchedEffect]
 * leaves the composition.
 *
 * It is an error to call [LaunchedEffect] without at least one `key` parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Deprecated(LaunchedEffectNoParamError, level = DeprecationLevel.ERROR)
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Composable
public fun LaunchedEffect(block: suspend CoroutineScope.() -> Unit): Unit =
    error(LaunchedEffectNoParamError)

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key1]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
public fun LaunchedEffect(key1: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1] or [key2]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
public fun LaunchedEffect(key1: Any?, key2: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1], [key2] or [key3]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
public fun LaunchedEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.() -> Unit,
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2, key3) { LaunchedEffectImpl(applyContext, block) }
}

private class LeftCompositionCancellationException :
    PlatformOptimizedCancellationException("The coroutine scope left the composition")

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with any different [keys]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@Suppress("ArrayReturn")
@OptIn(InternalComposeApi::class)
public fun LaunchedEffect(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(*keys) { LaunchedEffectImpl(applyContext, block) }
}

// Maintenance note: this class once was used by the inlined implementation of
// rememberCoroutineScope and must be maintained for binary compatibility. The new implementation
// of RememberedCoroutineScope implements RememberObserver directly, since as of this writing the
// compose runtime no longer implicitly treats objects incidentally stored in the slot table (e.g.
// previous parameter values from a skippable invocation, remember keys, etc.) as eligible
// RememberObservers. This dramatically reduces the risk of receiving unexpected RememberObserver
// lifecycle callbacks when a reference to a RememberObserver is leaked into user code and we can
// omit wrapper RememberObservers such as this one.
@PublishedApi
internal class CompositionScopedCoroutineScopeCanceller(val coroutineScope: CoroutineScope) :
    RememberObserver {
    override fun onRemembered() {
        // Nothing to do
    }

    override fun onForgotten() {
        val coroutineScope = coroutineScope
        if (coroutineScope is RememberedCoroutineScope) {
            coroutineScope.cancelIfCreated()
        } else {
            coroutineScope.cancel(LeftCompositionCancellationException())
        }
    }

    override fun onAbandoned() {
        val coroutineScope = coroutineScope
        if (coroutineScope is RememberedCoroutineScope) {
            coroutineScope.cancelIfCreated()
        } else {
            coroutineScope.cancel(LeftCompositionCancellationException())
        }
    }
}

private class CancelledCoroutineContext : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<CancelledCoroutineContext>
}

private class ForgottenCoroutineScopeException :
    PlatformOptimizedCancellationException("rememberCoroutineScope left the composition")

internal class RememberedCoroutineScope(
    private val parentContext: CoroutineContext,
    private val overlayContext: CoroutineContext,
) : CoroutineScope, RememberObserver {
    private val lock = makeSynchronizedObject(this)

    // The goal of this implementation is to make cancellation as cheap as possible if the
    // coroutineContext property was never accessed, consisting only of taking a monitor lock and
    // setting a volatile field.

    @Volatile private var _coroutineContext: CoroutineContext? = null

    override val coroutineContext: CoroutineContext
        get() {
            var localCoroutineContext = _coroutineContext
            if (
                localCoroutineContext == null || localCoroutineContext === CancelledCoroutineContext
            ) {
                val traceContext = parentContext[CompositionErrorContextImpl]
                val exceptionHandler =
                    if (traceContext != null) {
                        // If trace context is present, override exception handler, so all child
                        // jobs would have the composable trace appended.
                        // On exception, call overlay -> parent and throw if neither are present.
                        CoroutineExceptionHandler { c, e ->
                            traceContext.apply {
                                e.attachComposeStackTrace(this@RememberedCoroutineScope)
                            }
                            overlayContext[CoroutineExceptionHandler]?.handleException(c, e)
                                ?: parentContext[CoroutineExceptionHandler]?.handleException(c, e)
                                ?: throw e
                        }
                    } else {
                        EmptyCoroutineContext
                    }

                // Yes, we're leaking our lock here by using the instance of the object
                // that also gets handled by user code as a CoroutineScope as an intentional
                // tradeoff for avoiding the allocation of a dedicated lock object.
                // Since we only use it here for this lazy initialization and control flow
                // does not escape the creation of the CoroutineContext while holding the lock,
                // the splash damage should be acceptable.
                synchronized(lock) {
                    localCoroutineContext = _coroutineContext
                    if (localCoroutineContext == null) {
                        val parentContext = parentContext
                        val childJob = Job(parentContext[Job])
                        localCoroutineContext =
                            parentContext + childJob + overlayContext + exceptionHandler
                    } else if (localCoroutineContext === CancelledCoroutineContext) {
                        // Lazily initialize the child job here, already cancelled.
                        // Assemble the CoroutineContext exactly as otherwise expected.
                        val parentContext = parentContext
                        val cancelledChildJob =
                            Job(parentContext[Job]).apply {
                                cancel(ForgottenCoroutineScopeException())
                            }
                        localCoroutineContext =
                            parentContext + cancelledChildJob + overlayContext + exceptionHandler

                        @OptIn(ComposeToolingApi::class)
                        if (ComposeToolingFlags.isVerboseTracingEnabled) {
                            localCoroutineContext += RememberedCoroutineScopeTracingContext
                        }
                    }
                    _coroutineContext = localCoroutineContext
                }
            }
            return localCoroutineContext!!
        }

    fun cancelIfCreated() {
        // Take the lock unconditionally; this is internal API only used by internal
        // RememberObserver implementations that are not leaked to user code; we can assume
        // this won't be called repeatedly. If this assumption is violated we'll simply create a
        // redundant exception.
        synchronized(lock) {
            val context = _coroutineContext
            if (context == null) {
                _coroutineContext = CancelledCoroutineContext
            } else {
                // Ignore optimizing the case where we might be cancelling an already cancelled job;
                // only internal callers such as RememberObservers will invoke this method.
                context.cancel(ForgottenCoroutineScopeException())
            }
        }
    }

    override fun onRemembered() {
        // Do nothing
    }

    override fun onForgotten() {
        cancelIfCreated()
    }

    override fun onAbandoned() {
        cancelIfCreated()
    }

    companion object {
        @JvmField val CancelledCoroutineContext: CoroutineContext = CancelledCoroutineContext()
    }
}

@PublishedApi
@OptIn(InternalComposeApi::class)
internal fun createCompositionCoroutineScope(
    coroutineContext: CoroutineContext,
    composer: Composer,
): CoroutineScope =
    if (coroutineContext[Job] != null) {
        CoroutineScope(
            Job().apply {
                completeExceptionally(
                    IllegalArgumentException(
                        "CoroutineContext supplied to " +
                            "rememberCoroutineScope may not include a parent job"
                    )
                )
            }
        )
    } else {
        val applyContext = composer.applyCoroutineContext
        RememberedCoroutineScope(applyContext, coroutineContext)
    }

/**
 * Return a [CoroutineScope] bound to this point in the composition using the optional
 * [CoroutineContext] provided by [getContext]. [getContext] will only be called once and the same
 * [CoroutineScope] instance will be returned across recompositions.
 *
 * This scope will be [cancelled][CoroutineScope.cancel] when this call leaves the composition. The
 * [CoroutineContext] returned by [getContext] may not contain a [Job] as this scope is considered
 * to be a child of the composition.
 *
 * The default dispatcher of this scope if one is not provided by the context returned by
 * [getContext] will be the applying dispatcher of the composition's [Recomposer].
 *
 * Use this scope to launch jobs in response to callback events such as clicks or other user
 * interaction where the response to that event needs to unfold over time and be cancelled if the
 * composable managing that process leaves the composition. Jobs should never be launched into
 * **any** coroutine scope as a side effect of composition itself. For scoped ongoing jobs initiated
 * by composition, see [LaunchedEffect].
 *
 * This function will not throw if preconditions are not met, as composable functions do not yet
 * fully support exceptions. Instead the returned scope's [CoroutineScope.coroutineContext] will
 * contain a failed [Job] with the associated exception and will not be capable of launching child
 * jobs.
 */
@Composable
public inline fun rememberCoroutineScope(
    crossinline getContext: @DisallowComposableCalls () -> CoroutineContext = {
        EmptyCoroutineContext
    }
): CoroutineScope {
    val composer = currentComposer
    return remember { createCompositionCoroutineScope(getContext(), composer) }
}

private object LaunchedEffectTracingContext : TracingContext("Compose:LaunchedEffect")

private object RememberedCoroutineScopeTracingContext : TracingContext("Compose:coroutineScope")

internal expect abstract class TracingContext(name: String) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>

    companion object Key : CoroutineContext.Key<TracingContext>
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/DerivedState.kt
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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.SnapshotThreadLocal
import androidx.compose.runtime.internal.identityHashCode
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotId
import androidx.compose.runtime.snapshots.SnapshotIdZero
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.current
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.newWritableRecord
import androidx.compose.runtime.snapshots.sync
import androidx.compose.runtime.snapshots.withCurrent
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.math.min

/**
 * A [State] that is derived from one or more other states.
 *
 * @see derivedStateOf
 */
internal interface DerivedState<T> : State<T> {
    /** Provides a current [Record]. */
    val currentRecord: Record<T>

    /**
     * Mutation policy that controls how changes are handled after state dependencies update. If the
     * policy is `null`, the derived state update is triggered regardless of the value produced and
     * it is up to observer to invalidate it correctly.
     */
    val policy: SnapshotMutationPolicy<T>?

    interface Record<T> {
        /**
         * The value of the derived state retrieved without triggering a notification to read
         * observers.
         */
        val currentValue: T

        /**
         * Map of the dependencies used to produce [value] or [currentValue] to nested read level.
         *
         * This map can be used to determine if the state could affect value of this derived state,
         * when a [StateObject] appears in the apply observer set.
         */
        val dependencies: ObjectIntMap<StateObject>
    }
}

private val calculationBlockNestedLevel = SnapshotThreadLocal<IntRef>()

private inline fun <T> withCalculationNestedLevel(block: (IntRef) -> T): T {
    val ref =
        calculationBlockNestedLevel.get() ?: IntRef(0).also { calculationBlockNestedLevel.set(it) }
    return block(ref)
}

private class DerivedSnapshotState<T>(
    private val calculation: () -> T,
    override val policy: SnapshotMutationPolicy<T>?,
) : StateObjectImpl(), DerivedState<T> {
    private var first: ResultRecord<T> = ResultRecord(currentSnapshot().snapshotId)

    class ResultRecord<T>(snapshotId: SnapshotId) :
        StateRecord(snapshotId), DerivedState.Record<T> {
        companion object {
            val Unset = Any()
        }

        var validSnapshotId: SnapshotId = SnapshotIdZero
        var validSnapshotWriteCount: Int = 0

        override var dependencies: ObjectIntMap<StateObject> = emptyObjectIntMap()
        var result: Any? = Unset
        var resultHash: Int = 0

        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST") val other = value as ResultRecord<T>
            dependencies = other.dependencies
            result = other.result
            resultHash = other.resultHash
        }

        override fun create(): StateRecord = create(currentSnapshot().snapshotId)

        override fun create(snapshotId: SnapshotId): StateRecord = ResultRecord<T>(snapshotId)

        fun isValid(derivedState: DerivedState<*>, snapshot: Snapshot): Boolean {
            val snapshotChanged = sync {
                validSnapshotId != snapshot.snapshotId ||
                    validSnapshotWriteCount != snapshot.writeCount
            }
            val isValid =
                result !== Unset &&
                    (!snapshotChanged || resultHash == readableHash(derivedState, snapshot))

            if (isValid && snapshotChanged) {
                sync {
                    validSnapshotId = snapshot.snapshotId
                    validSnapshotWriteCount = snapshot.writeCount
                }
            }

            return isValid
        }

        fun readableHash(derivedState: DerivedState<*>, snapshot: Snapshot): Int {
            var hash = 7
            val dependencies = sync { dependencies }
            if (dependencies.isNotEmpty()) {
                notifyObservers(derivedState) {
                    dependencies.forEach { stateObject, readLevel ->
                        if (readLevel != 1) {
                            return@forEach
                        }

                        // Find the first record without triggering an observer read.
                        val record =
                            if (stateObject is DerivedSnapshotState<*>) {
                                // eagerly access the parent derived states without recording the
                                // read
                                // that way we can be sure derived states in deps were recalculated,
                                // and are updated to the last values
                                stateObject.current(snapshot)
                            } else {
                                current(stateObject.firstStateRecord, snapshot)
                            }

                        hash = 31 * hash + identityHashCode(record)
                        hash = 31 * hash + record.snapshotId.hashCode()
                    }
                }
            }
            return hash
        }

        override val currentValue: T
            @Suppress("UNCHECKED_CAST") get() = result as T
    }

    /**
     * Get current record in snapshot. Forces recalculation if record is invalid to refresh state
     * value.
     *
     * @return latest state record for the derived state.
     */
    fun current(snapshot: Snapshot): StateRecord =
        currentRecord(current(first, snapshot), snapshot, false, calculation)

    private fun currentRecord(
        readable: ResultRecord<T>,
        snapshot: Snapshot,
        forceDependencyReads: Boolean,
        calculation: () -> T,
    ): ResultRecord<T> {
        if (readable.isValid(this, snapshot)) {
            // If the dependency is not recalculated, emulate nested state reads
            // for correct invalidation later
            if (forceDependencyReads) {
                notifyObservers(this) {
                    val dependencies = readable.dependencies
                    withCalculationNestedLevel { calculationLevelRef ->
                        val invalidationNestedLevel = calculationLevelRef.element
                        dependencies.forEach { dependency, nestedLevel ->
                            calculationLevelRef.element = invalidationNestedLevel + nestedLevel
                            snapshot.readObserver?.invoke(dependency)
                        }
                        calculationLevelRef.element = invalidationNestedLevel
                    }
                }
            }
            return readable
        }

        val newDependencies = MutableObjectIntMap<StateObject>()
        val result = withCalculationNestedLevel { calculationLevelRef ->
            val nestedCalculationLevel = calculationLevelRef.element
            notifyObservers(this) {
                calculationLevelRef.element = nestedCalculationLevel + 1

                val result =
                    Snapshot.observe(
                        {
                            if (it === this) error("A derived state calculation cannot read itself")
                            if (it is StateObject) {
                                val readNestedLevel = calculationLevelRef.element
                                newDependencies[it] =
                                    min(
                                        readNestedLevel - nestedCalculationLevel,
                                        newDependencies.getOrDefault(it, Int.MAX_VALUE),
                                    )
                            }
                        },
                        null,
                        calculation,
                    )

                calculationLevelRef.element = nestedCalculationLevel
                result
            }
        }

        val record = sync {
            val currentSnapshot = Snapshot.current

            if (
                readable.result !== ResultRecord.Unset &&
                    @Suppress("UNCHECKED_CAST") policy?.equivalent(result, readable.result as T) ==
                        true
            ) {
                readable.dependencies = newDependencies
                readable.resultHash = readable.readableHash(this, currentSnapshot)
                readable
            } else {
                val writable = first.newWritableRecord(this, currentSnapshot)
                writable.dependencies = newDependencies
                writable.resultHash = writable.readableHash(this, currentSnapshot)
                writable.result = result
                writable
            }
        }

        if (calculationBlockNestedLevel.get()?.element == 0) {
            Snapshot.notifyObjectsInitialized()

            sync {
                val currentSnapshot = Snapshot.current
                record.validSnapshotId = currentSnapshot.snapshotId
                record.validSnapshotWriteCount = currentSnapshot.writeCount
            }
        }

        return record
    }

    override val firstStateRecord: StateRecord
        get() = first

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        first = value as ResultRecord<T>
    }

    override val value: T
        get() {
            // Unlike most state objects, the record list of a derived state can change during a
            // read
            // because reading updates the cache. To account for this, instead of calling readable,
            // which sends the read notification, the read observer is notified directly and current
            // value is used instead which doesn't notify. This allow the read observer to read the
            // value and only update the cache once.
            Snapshot.current.readObserver?.invoke(this)
            // Read observer could advance the snapshot, so get current snapshot again
            val snapshot = Snapshot.current
            val record = current(first, snapshot)
            @Suppress("UNCHECKED_CAST")
            return currentRecord(record, snapshot, true, calculation).result as T
        }

    override val currentRecord: DerivedState.Record<T>
        get() {
            val snapshot = Snapshot.current
            val record = current(first, snapshot)
            return currentRecord(record, snapshot, false, calculation)
        }

    override fun toString(): String =
        first.withCurrent { "DerivedState(value=${displayValue()})@${hashCode()}" }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T?
        @JvmName("getDebuggerDisplayValue")
        get() =
            first.withCurrent {
                @Suppress("UNCHECKED_CAST")
                if (it.isValid(this, Snapshot.current)) it.result as T else null
            }

    private fun displayValue(): String {
        first.withCurrent {
            if (it.isValid(this, Snapshot.current)) {
                return it.result.toString()
            }
            return "<Not calculated>"
        }
    }
}

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning that
 * this will correctly subscribe to the derived state objects if the value is being read in an
 * observed context such as a [Composable] function. Derived states without mutation policy trigger
 * updates on each dependency change. To avoid invalidation on update, provide suitable
 * [SnapshotMutationPolicy] through [derivedStateOf] overload.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 * @param calculation the calculation to create the value this state object represents.
 */
@StateFactoryMarker
public fun <T> derivedStateOf(calculation: () -> T): State<T> =
    DerivedSnapshotState(calculation, null)

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning that
 * this will correctly subscribe to the derived state objects if the value is being read in an
 * observed context such as a [Composable] function.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 * @param policy mutation policy to control when changes to the [calculation] result trigger update.
 * @param calculation the calculation to create the value this state object represents.
 */
@StateFactoryMarker
public fun <T> derivedStateOf(policy: SnapshotMutationPolicy<T>, calculation: () -> T): State<T> =
    DerivedSnapshotState(calculation, policy)

/** Observe the recalculations performed by derived states. */
internal interface DerivedStateObserver {
    /** Called before a calculation starts. */
    fun start(derivedState: DerivedState<*>)

    /** Called after the started calculation is complete. */
    fun done(derivedState: DerivedState<*>)
}

private val derivedStateObservers = SnapshotThreadLocal<MutableVector<DerivedStateObserver>>()

internal fun derivedStateObservers(): MutableVector<DerivedStateObserver> =
    derivedStateObservers.get()
        ?: MutableVector<DerivedStateObserver>(0).also { derivedStateObservers.set(it) }

private inline fun <R> notifyObservers(derivedState: DerivedState<*>, block: () -> R): R {
    val observers = derivedStateObservers()
    observers.forEach { it.start(derivedState) }
    return try {
        block()
    } finally {
        observers.forEach { it.done(derivedState) }
    }
}

/**
 * Observe the recalculations performed by any derived state that is recalculated during the
 * execution of [block].
 *
 * @param observer called for every calculation of a derived state in the [block].
 * @param block the block of code to observe.
 */
internal inline fun <R> observeDerivedStateRecalculations(
    observer: DerivedStateObserver,
    block: () -> R,
) {
    val observers = derivedStateObservers()
    try {
        observers.add(observer)
        block()
    } finally {
        observers.removeAt(observers.lastIndex)
    }
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/snapshots/Snapshot.kt
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

package androidx.compose.runtime.snapshots

import androidx.collection.MutableScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.checkPrecondition
import androidx.compose.runtime.collection.wrapIntoSet
import androidx.compose.runtime.internal.AtomicInt
import androidx.compose.runtime.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.internal.SnapshotThreadLocal
import androidx.compose.runtime.internal.currentThreadId
import androidx.compose.runtime.platform.SynchronizedObject
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.requirePrecondition
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeMutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeSnapshot
import androidx.compose.runtime.snapshots.tooling.creatingSnapshot
import androidx.compose.runtime.snapshots.tooling.dispatchObserverOnApplied
import androidx.compose.runtime.snapshots.tooling.dispatchObserverOnPreDispose
import androidx.compose.runtime.tooling.verboseTrace
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A snapshot of the values return by mutable states and other state objects. All state object will
 * have the same value in the snapshot as they had when the snapshot was created unless they are
 * explicitly changed in the snapshot.
 *
 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is
 * called). All state objects will return the values associated with this snapshot, locally in the
 * thread, until [enter] returns. All other threads are unaffected.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot].
 *
 * @see takeSnapshot
 * @see takeMutableSnapshot
 * @see androidx.compose.runtime.mutableStateOf
 * @see androidx.compose.runtime.mutableStateListOf
 * @see androidx.compose.runtime.mutableStateMapOf
 */
public sealed class Snapshot(
    snapshotId: SnapshotId,

    /** A set of all the snapshots that should be treated as invalid. */
    internal open var invalid: SnapshotIdSet,
) {
    @Deprecated("Use id: Long constructor instead", level = DeprecationLevel.HIDDEN)
    protected constructor(id: Int, invalid: SnapshotIdSet) : this(id.toSnapshotId(), invalid)

    /**
     * The snapshot id of the snapshot. This is a unique number from a monotonically increasing
     * value for each snapshot taken.
     *
     * [id] will is identical to [snapshotId] if the value of [snapshotId] is less than or equal to
     * [Int.MAX_VALUE]. For [snapshotId] value greater than [Int.MAX_VALUE], this value will return
     * a negative value.
     */
    @Deprecated("Use snapshotId instead", replaceWith = ReplaceWith("snapshotId"))
    public open val id: Int
        get() = snapshotId.toInt()

    /**
     * The snapshot id of the snapshot. This is a unique number from a monotonically increasing
     * value for each snapshot taken.
     */
    public open var snapshotId: SnapshotId = snapshotId
        internal set

    internal open var writeCount: Int
        get() = 0
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            error("Updating write count is not supported for this snapshot")
        }

    /**
     * The root snapshot for this snapshot. For non-nested snapshots this is always `this`. For
     * nested snapshot it is the parent's [root].
     */
    public abstract val root: Snapshot

    /** True if any change to a state object in this snapshot will throw. */
    public abstract val readOnly: Boolean

    /**
     * Dispose the snapshot. Neglecting to dispose a snapshot will result in difficult to diagnose
     * memory leaks as it indirectly causes all state objects to maintain its value for the
     * un-disposed snapshot.
     */
    public open fun dispose() {
        disposed = true
        sync { releasePinnedSnapshotLocked() }
    }

    /**
     * Take a snapshot of the state values in this snapshot. The resulting [Snapshot] is read-only.
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested snapshots are still valid after the parent has
     * been disposed.
     */
    public abstract fun takeNestedSnapshot(readObserver: ((Any) -> Unit)? = null): Snapshot

    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible until
     * the snapshot is applied.
     */
    public abstract fun hasPendingChanges(): Boolean

    /**
     * Enter the snapshot. In [block] all state objects have the value associated with this
     * snapshot. The value of [currentSnapshot] will be this snapshot until this [block] returns or
     * a nested call to [enter] is called. When [block] returns, the previous current snapshot is
     * restored if there was one.
     *
     * All changes to state objects inside [block] are isolated to this snapshot and are not visible
     * to other snapshot or as global state. If this is a [readOnly] snapshot, any changes to state
     * objects will throw an [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot inside [block] can be applied atomically
     * to the global state (or to its parent snapshot if it is a nested snapshot) by calling
     * [MutableSnapshot.apply].
     *
     * @see androidx.compose.runtime.mutableStateOf
     * @see androidx.compose.runtime.mutableStateListOf
     * @see androidx.compose.runtime.mutableStateMapOf
     */
    public inline fun <T> enter(block: () -> T): T {
        val previous = makeCurrent()
        try {
            return block()
        } finally {
            restoreCurrent(previous)
        }
    }

    @PublishedApi
    internal open fun makeCurrent(): Snapshot? {
        val previous = threadSnapshot.get()
        threadSnapshot.set(this)
        return previous
    }

    @PublishedApi
    internal open fun restoreCurrent(snapshot: Snapshot?) {
        threadSnapshot.set(snapshot)
    }

    /**
     * Enter the snapshot, returning the previous [Snapshot] for leaving this snapshot later using
     * [unsafeLeave]. Prefer [enter] or [asContextElement] instead of using [unsafeEnter] directly
     * to prevent mismatched [unsafeEnter]/[unsafeLeave] calls.
     *
     * After returning all state objects have the value associated with this snapshot. The value of
     * [currentSnapshot] will be this snapshot until [unsafeLeave] is called with the returned
     * [Snapshot] or another call to [unsafeEnter] or [enter] is made.
     *
     * All changes to state objects until another snapshot is entered or this snapshot is left are
     * isolated to this snapshot and are not visible to other snapshot or as global state. If this
     * is a [readOnly] snapshot, any changes to state objects will throw an [IllegalStateException].
     *
     * For a [MutableSnapshot], changes made to a snapshot can be applied atomically to the global
     * state (or to its parent snapshot if it is a nested snapshot) by calling
     * [MutableSnapshot.apply].
     */
    public fun unsafeEnter(): Snapshot? = makeCurrent()

    /** Leave the snapshot, restoring the [oldSnapshot] before returning. See [unsafeEnter]. */
    public fun unsafeLeave(oldSnapshot: Snapshot?) {
        checkPrecondition(threadSnapshot.get() === this) {
            "Cannot leave snapshot; $this is not the current snapshot"
        }
        restoreCurrent(oldSnapshot)
    }

    internal var disposed = false

    /*
     * Handle to use when unpinning this snapshot. -1 if this snapshot has been unpinned.
     */
    @Suppress("LeakingThis")
    private var pinningTrackingHandle =
        if (snapshotId != INVALID_SNAPSHOT) trackPinning(snapshotId, invalid) else -1

    internal inline val isPinned
        get() = pinningTrackingHandle >= 0

    /*
     * The read observer for the snapshot if there is one.
     */
    @PublishedApi internal abstract val readObserver: ((Any) -> Unit)?

    /** The write observer for the snapshot if there is one. */
    internal abstract val writeObserver: ((Any) -> Unit)?

    /** Called when a nested snapshot of this snapshot is activated */
    internal abstract fun nestedActivated(snapshot: Snapshot)

    /** Called when a nested snapshot of this snapshot is deactivated */
    internal abstract fun nestedDeactivated(snapshot: Snapshot)

    /** Record that state was modified in the snapshot. */
    internal abstract fun recordModified(state: StateObject)

    /** The set of state objects that have been modified in this snapshot. */
    internal abstract val modified: MutableScatterSet<StateObject>?

    /**
     * Notify the snapshot that all objects created in this snapshot to this point should be
     * considered initialized. If any state object is modified after this point it will appear as
     * modified in the snapshot. Any applicable snapshot write observer will be called for the
     * object and the object will be part of the a set of mutated objects sent to any applicable
     * snapshot apply observer.
     *
     * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
     * considered modified by the snapshot even if they are modified after construction.
     */
    internal abstract fun notifyObjectsInitialized()

    /**
     * Closes the snapshot by removing the snapshot id (an any previous id's) from the list of open
     * snapshots and unpinning snapshots that no longer are referenced by this snapshot.
     */
    internal fun closeAndReleasePinning() {
        sync {
            closeLocked()
            releasePinnedSnapshotsForCloseLocked()
        }
    }

    /**
     * Closes the snapshot by removing the snapshot id (and any previous ids) from the list of open
     * snapshots. Does not release pinned snapshots. See [releasePinnedSnapshotsForCloseLocked] for
     * the second half of [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     */
    internal open fun closeLocked() {
        openSnapshots = openSnapshots.clear(snapshotId)
    }

    /**
     * Releases all pinned snapshots required to perform a clean [closeAndReleasePinning].
     *
     * Call while holding a `sync {}` lock.
     *
     * See [closeAndReleasePinning], [closeLocked].
     */
    internal open fun releasePinnedSnapshotsForCloseLocked() {
        releasePinnedSnapshotLocked()
    }

    internal fun validateNotDisposed() {
        requirePrecondition(!disposed) { "Cannot use a disposed snapshot" }
    }

    internal fun releasePinnedSnapshotLocked() {
        if (pinningTrackingHandle >= 0) {
            releasePinningLocked(pinningTrackingHandle)
            pinningTrackingHandle = -1
        }
    }

    internal fun takeoverPinnedSnapshot(): Int =
        pinningTrackingHandle.also { pinningTrackingHandle = -1 }

    public companion object {
        /**
         * Return the thread's active snapshot. If no thread snapshot is active then the current
         * global snapshot is used.
         */
        public val current: Snapshot
            get() = currentSnapshot()

        /** Return `true` if the thread is currently in the context of a snapshot. */
        public val isInSnapshot: Boolean
            get() = threadSnapshot.get() != null

        /**
         * Returns whether any threads are currently in the process of notifying observers about
         * changes to the global snapshot.
         */
        public val isApplyObserverNotificationPending: Boolean
            get() = pendingApplyObserverCount.get() > 0

        /**
         * All new state objects initial state records should be [PreexistingSnapshotId] which then
         * allows snapshots outside the creating snapshot to access the object with its initial
         * state.
         */
        @Suppress("ConstPropertyName") public const val PreexistingSnapshotId: Int = 1

        /**
         * Take a snapshot of the current value of all state objects. The values are preserved until
         * [Snapshot.dispose] is called on the result.
         *
         * The [readObserver] parameter can be used to track when all state objects are read when in
         * [Snapshot.enter]. A snapshot apply observer can be registered using
         * [Snapshot.registerApplyObserver] to observe modification of state objects.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks values as are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * Composition uses both of these to implicitly subscribe to changes to state object and
         * automatically update the composition when state objects read during composition change.
         *
         * A nested snapshot can be taken of a snapshot which is an independent read-only copy of
         * the snapshot and can be disposed independently. This is used by [takeSnapshot] when in a
         * read-only snapshot for API consistency allowing the result of [takeSnapshot] to be
         * disposed leaving the parent snapshot active.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         *   [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshot.
         * @see Snapshot
         * @see Snapshot.registerApplyObserver
         */
        public fun takeSnapshot(readObserver: ((Any) -> Unit)? = null): Snapshot =
            currentSnapshot().takeNestedSnapshot(readObserver)

        /**
         * Take a snapshot of the current value of all state objects that also allows the state to
         * be changed and later atomically applied when [MutableSnapshot.apply] is called. The
         * values are preserved until [Snapshot.dispose] is called on the result. The global state
         * will either see all the changes made as one atomic change, when [MutableSnapshot .apply]
         * is called, or none of the changes if the mutable state object is disposed before being
         * applied.
         *
         * The values in a snapshot can be modified by calling [Snapshot.enter] and then, in its
         * lambda, modify any state object. The new values of the state objects will only become
         * visible to the global state when [MutableSnapshot.apply] is called.
         *
         * An active snapshot (after it is created but before [Snapshot.dispose] is called) requires
         * resources to track the values in the snapshot. Once a snapshot is no longer needed it
         * should disposed by calling [Snapshot.dispose].
         *
         * Leaving a snapshot active could cause hard to diagnose memory leaks as values are
         * maintained by state objects for these unneeded snapshots. Take care to always call
         * [Snapshot.dispose] on all snapshots when they are no longer needed.
         *
         * A nested snapshot can be taken by calling [Snapshot.takeNestedSnapshot], for a read-only
         * snapshot, or [MutableSnapshot.takeNestedMutableSnapshot] for a snapshot that can be
         * changed. Nested mutable snapshots are applied to the this, the parent snapshot, when
         * their [MutableSnapshot.apply] is called. Their applied changes will be visible to in this
         * snapshot but will not be visible other snapshots (including other nested snapshots) or
         * the global state until this snapshot is applied by calling [MutableSnapshot.apply].
         *
         * Once [MutableSnapshot.apply] is called on this, the parent snapshot, all calls to
         * [MutableSnapshot.apply] on an active nested snapshot will fail.
         *
         * Changes to a mutable snapshot are isolated, using snapshot isolation, from all other
         * snapshots. Their changes are only visible as global state or to new snapshots once
         * [MutableSnapshot.apply] is called.
         *
         * Applying a snapshot can fail if currently visible changes to the state object conflicts
         * with a change made in the snapshot.
         *
         * When in a mutable snapshot, [takeMutableSnapshot] creates a nested snapshot of the
         * current mutable snapshot. If the current snapshot is read-only, an exception is thrown.
         * The current snapshot is the result of calling [currentSnapshot] which is updated by
         * calling [Snapshot.enter] which makes the [Snapshot] the current snapshot while in its
         * lambda.
         *
         * Composition uses mutable snapshots to allow changes made in a [Composable] functions to
         * be temporarily isolated from the global state and is later applied to the global state
         * when the composition is applied. If [MutableSnapshot.apply] fails applying this snapshot,
         * the snapshot and the changes calculated during composition are disposed and a new
         * composition is scheduled to be calculated again.
         *
         * @param readObserver called when any state object is read in the lambda passed to
         *   [Snapshot.enter] or in the [Snapshot.enter] of any nested snapshots.
         *
         * Composition, layout and draw use [readObserver] to implicitly subscribe to changes to
         * state objects to know when to update.
         *
         * @param writeObserver called when a state object is created or just before it is written
         *   to the first time in the snapshot or a nested mutable snapshot. This might be called
         *   several times for the same object if nested mutable snapshots are created.
         *
         * Composition uses [writeObserver] to track when a state object is modified during
         * composition in order to invalidate the reads that have not yet occurred. This allows a
         * single pass of composition for state objects that are written to before they are read
         * (such as modifying the value of a dynamic ambient provider).
         *
         * @see Snapshot.takeSnapshot
         * @see Snapshot
         * @see MutableSnapshot
         */
        public fun takeMutableSnapshot(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null,
        ): MutableSnapshot =
            (currentSnapshot() as? MutableSnapshot)?.takeNestedMutableSnapshot(
                readObserver,
                writeObserver,
            ) ?: error("Cannot create a mutable snapshot of an read-only snapshot")

        /**
         * Escape the current snapshot, if there is one. All state objects will have the value
         * associated with the global while the [block] lambda is executing.
         *
         * @return the result of [block]
         */
        public inline fun <T> global(block: () -> T): T {
            val previous = removeCurrent()
            try {
                return block()
            } finally {
                restoreCurrent(previous)
            }
        }

        /**
         * Take a [MutableSnapshot] and run [block] within it. When [block] returns successfully,
         * attempt to [MutableSnapshot.apply] the snapshot. Returns the result of [block] or throws
         * [SnapshotApplyConflictException] if snapshot changes attempted by [block] could not be
         * applied.
         *
         * Prior to returning, any changes made to snapshot state (e.g. state holders returned by
         * [androidx.compose.runtime.mutableStateOf] are not visible to other threads. When
         * [withMutableSnapshot] returns successfully those changes will be made visible to other
         * threads and any snapshot observers (e.g. [androidx.compose.runtime.snapshotFlow]) will be
         * notified of changes.
         *
         * [block] must not suspend if [withMutableSnapshot] is called from a suspend function.
         */
        // TODO: determine a good way to prevent/discourage suspending in an inlined [block]
        public inline fun <R> withMutableSnapshot(block: () -> R): R =
            takeMutableSnapshot().run {
                var hasError = false
                try {
                    enter(block)
                } catch (e: Throwable) {
                    hasError = true
                    throw e
                } finally {
                    if (!hasError) {
                        apply().check()
                    }
                    dispose()
                }
            }

        /**
         * Observe reads and or write of state objects in the current thread.
         *
         * This only affects the current snapshot (if any) and any new snapshots create from
         * [Snapshot.takeSnapshot] and [takeMutableSnapshot]. It will not affect any snapshots
         * previous created even if [Snapshot.enter] is called in [block].
         *
         * @param readObserver called when any state object is read.
         * @param writeObserver called when a state object is created or just before it is written
         *   to the first time in the snapshot or a nested mutable snapshot. This might be called
         *   several times for the same object if nested mutable snapshots are created.
         * @param block the code the [readObserver] and [writeObserver] will be observing. Once
         *   [block] returns, the [readObserver] and [writeObserver] will no longer be called.
         */
        public fun <T> observe(
            readObserver: ((Any) -> Unit)? = null,
            writeObserver: ((Any) -> Unit)? = null,
            block: () -> T,
        ): T = observeInternal(readObserver, writeObserver, block)

        @Suppress("NOTHING_TO_INLINE")
        // marked as inline to use as part of SnapshotStateObserver without adding extra function
        // call overhead.
        internal inline fun <T> observeInternal(
            noinline readObserver: ((Any) -> Unit)? = null,
            noinline writeObserver: ((Any) -> Unit)? = null,
            noinline block: () -> T,
        ): T {
            if (readObserver == null && writeObserver == null) {
                // No observer change, just execute the block
                return block()
            }

            val previous = threadSnapshot.get()
            if (previous is TransparentObserverMutableSnapshot && previous.canBeReused) {
                // Change observers in place without allocating new snapshots.
                val previousReadObserver = previous.readObserver
                val previousWriteObserver = previous.writeObserver

                try {
                    previous.readObserver = mergedReadObserver(readObserver, previousReadObserver)
                    previous.writeObserver =
                        mergedWriteObserver(writeObserver, previousWriteObserver)
                    return block()
                } finally {
                    previous.readObserver = previousReadObserver
                    previous.writeObserver = previousWriteObserver
                }
            } else {
                // The snapshot is not already transparent, observe in a new transparent snapshot
                val snapshot =
                    when {
                        previous == null || previous is MutableSnapshot -> {
                            TransparentObserverMutableSnapshot(
                                parentSnapshot = previous as? MutableSnapshot,
                                specifiedReadObserver = readObserver,
                                specifiedWriteObserver = writeObserver,
                                mergeParentObservers = true,
                                ownsParentSnapshot = false,
                            )
                        }
                        readObserver == null -> {
                            return block()
                        }
                        else -> {
                            previous.takeNestedSnapshot(readObserver)
                        }
                    }
                try {
                    return snapshot.enter(block)
                } finally {
                    snapshot.dispose()
                }
            }
        }

        @Suppress("unused") // left here for binary compatibility
        @PublishedApi
        internal fun createNonObservableSnapshot(): Snapshot =
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = threadSnapshot.get()
            )

        @PublishedApi
        internal val currentThreadSnapshot: Snapshot?
            get() = threadSnapshot.get()

        private inline val TransparentObserverMutableSnapshot.canBeReused: Boolean
            get() = threadId == currentThreadId()

        private inline val TransparentObserverSnapshot.canBeReused: Boolean
            get() = threadId == currentThreadId()

        @PublishedApi
        internal fun makeCurrentNonObservable(previous: Snapshot?): Snapshot =
            when {
                previous is TransparentObserverMutableSnapshot && previous.canBeReused -> {
                    previous.readObserver = null
                    previous
                }
                previous is TransparentObserverSnapshot && previous.canBeReused -> {
                    previous.readObserver = null
                    previous
                }
                else -> {
                    val snapshot =
                        createTransparentSnapshotWithNoParentReadObserver(
                            previousSnapshot = previous
                        )
                    snapshot.makeCurrent()
                    snapshot
                }
            }

        @PublishedApi
        internal fun restoreNonObservable(
            previous: Snapshot?,
            nonObservable: Snapshot,
            observer: ((Any) -> Unit)?,
        ) {
            if (previous === nonObservable) {
                when (previous) {
                    is TransparentObserverMutableSnapshot -> {
                        previous.readObserver = observer
                    }
                    is TransparentObserverSnapshot -> {
                        previous.readObserver = observer
                    }
                    else -> {
                        error("Non-transparent snapshot was reused: $previous")
                    }
                }
            } else {
                nonObservable.restoreCurrent(previous)
                nonObservable.dispose()
            }
        }

        /**
         * Passed [block] will be run with all the currently set snapshot read observers disabled.
         */
        @Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
        @OptIn(ExperimentalContracts::class)
        public inline fun <T> withoutReadObservation(block: @DisallowComposableCalls () -> T): T {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
            val previousSnapshot = currentThreadSnapshot
            val observer = previousSnapshot?.readObserver
            val newSnapshot = makeCurrentNonObservable(previousSnapshot)
            try {
                return block()
            } finally {
                restoreNonObservable(previousSnapshot, newSnapshot, observer)
            }
        }

        /**
         * Register an apply listener that is called back when snapshots are applied to the global
         * state.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        public fun registerApplyObserver(observer: (Set<Any>, Snapshot) -> Unit): ObserverHandle {
            // Ensure observer does not see changes before this call.
            advanceGlobalSnapshot(emptyLambda)

            sync { applyObservers += observer }
            return ObserverHandle { sync { applyObservers -= observer } }
        }

        /**
         * Register an observer of the first write to the global state of a global state object
         * since the last call to [sendApplyNotifications].
         *
         * Composition uses this to schedule a new composition whenever a state object that was read
         * in composition is modified.
         *
         * State objects can be sent to the apply observer that have not been sent to global write
         * observers. This happens for state objects inside [MutableSnapshot] that is later applied
         * by calling [MutableSnapshot.apply].
         *
         * This should only be used to determine if a call to [sendApplyNotifications] should be
         * scheduled to be called.
         *
         * @return [ObserverHandle] to unregister [observer].
         */
        public fun registerGlobalWriteObserver(observer: ((Any) -> Unit)): ObserverHandle {
            sync { globalWriteObservers += observer }
            advanceGlobalSnapshot()
            return ObserverHandle {
                sync { globalWriteObservers -= observer }
                advanceGlobalSnapshot()
            }
        }

        /**
         * Notify the snapshot that all objects created in this snapshot to this point should be
         * considered initialized. If any state object is are modified passed this point it will
         * appear as modified in the snapshot and any applicable snapshot write observer will be
         * called for the object and the object will be part of the a set of mutated objects sent to
         * any applicable snapshot apply observer.
         *
         * Unless [notifyObjectsInitialized] is called, state objects created in a snapshot are not
         * considered modified by the snapshot even if they are modified after construction.
         *
         * Compose uses this between phases of composition to allow observing changes to state
         * objects create in a previous phase.
         */
        public fun notifyObjectsInitialized(): Unit = currentSnapshot().notifyObjectsInitialized()

        /**
         * Send any pending apply notifications for state objects changed outside a snapshot.
         *
         * Apply notifications for state objects modified outside snapshot are deferred until method
         * is called. This method is implicitly called whenever a non-nested [MutableSnapshot] is
         * applied making its changes visible to all new, non-nested snapshots.
         *
         * Composition schedules this to be called after changes to state objects are detected an
         * observer registered with [registerGlobalWriteObserver].
         */
        public fun sendApplyNotifications() {
            val changes = sync { globalSnapshot.hasPendingChanges() }
            if (changes) advanceGlobalSnapshot()
        }

        @InternalComposeApi public fun openSnapshotCount(): Int = openSnapshots.toList().size

        @PublishedApi
        internal fun removeCurrent(): Snapshot? {
            val previous = threadSnapshot.get()
            if (previous != null) threadSnapshot.set(null)
            return previous
        }

        @PublishedApi
        internal fun restoreCurrent(previous: Snapshot?) {
            if (previous != null) threadSnapshot.set(previous)
        }
    }
}

/**
 * Pin the snapshot and invalid set.
 *
 * @return returns a handle that should be passed to [releasePinningLocked] when the snapshot closes
 *   or is disposed.
 */
internal fun trackPinning(snapshotId: SnapshotId, invalid: SnapshotIdSet): Int {
    val pinned = invalid.lowest(snapshotId)
    return sync { pinningTable.add(pinned) }
}

/** Release the [handle] returned by [trackPinning] */
internal fun releasePinningLocked(handle: Int) {
    pinningTable.remove(handle)
}

/**
 * A snapshot of the values return by mutable states and other state objects. All state object will
 * have the same value in the snapshot as they had when the snapshot was created unless they are
 * explicitly changed in the snapshot.
 *
 * To enter a snapshot call [enter]. The snapshot is the current snapshot as returned by
 * [currentSnapshot] until the control returns from the lambda (or until a nested [enter] is called.
 * All state objects will return the values associated with this snapshot, locally in the thread,
 * until [enter] returns. All other threads are unaffected.
 *
 * All changes made in a [MutableSnapshot] are snapshot isolated from all other snapshots and their
 * changes can only be seen globally, or by new shots, after [MutableSnapshot.apply] as been called.
 *
 * Snapshots can be nested by calling [takeNestedSnapshot] or
 * [MutableSnapshot.takeNestedMutableSnapshot].
 *
 * @see Snapshot.takeMutableSnapshot
 * @see androidx.compose.runtime.mutableStateOf
 * @see androidx.compose.runtime.mutableStateListOf
 * @see androidx.compose.runtime.mutableStateMapOf
 */
public open class MutableSnapshot
internal constructor(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
    override val writeObserver: ((Any) -> Unit)?,
) : Snapshot(snapshotId, invalid) {
    /**
     * Whether there are any pending changes in this snapshot. These changes are not visible until
     * the snapshot is applied.
     */
    override fun hasPendingChanges(): Boolean = modified?.isNotEmpty() == true

    /**
     * Take a mutable snapshot of the state values in this snapshot. Entering this snapshot by
     * calling [enter] allows state objects to be modified that are not visible to the this, the
     * parent snapshot, until the [apply] is called.
     *
     * Applying a nested snapshot, by calling [apply], applies its change to, this, the parent
     * snapshot. For a change to be visible globally, all the parent snapshots need to be applied
     * until the root snapshot is applied to the global state.
     *
     * All nested snapshots need to be disposed by calling [dispose] before resources associated
     * with this snapshot can be collected. Nested active snapshots are still valid after the parent
     * has been disposed but calling [apply] will fail.
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    public open fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)? = null,
        writeObserver: ((Any) -> Unit)? = null,
    ): MutableSnapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        return creatingSnapshot(this, readObserver, writeObserver, readonly = false) {
            actualReadObserver,
            actualWriteObserver ->
            advance {
                sync {
                    val newId = nextSnapshotId
                    nextSnapshotId += 1
                    openSnapshots = openSnapshots.set(newId)
                    val currentInvalid = invalid
                    this.invalid = currentInvalid.set(newId)
                    NestedMutableSnapshot(
                        newId,
                        currentInvalid.addRange(snapshotId + 1, newId),
                        mergedReadObserver(actualReadObserver, this.readObserver),
                        mergedWriteObserver(actualWriteObserver, this.writeObserver),
                        this,
                    )
                }
            }
        }
    }

    /**
     * Apply the changes made to state objects in this snapshot to the global state, or to the
     * parent snapshot if this is a nested mutable snapshot.
     *
     * Once this method returns all changes made to this snapshot are atomically visible as the
     * global state of the state object or to the parent snapshot.
     *
     * While a snapshot is active (after it is created but before [apply] or [dispose] is called)
     * requires resources to track the values in the snapshot. Once a snapshot is no longer needed
     * it should be either applied by calling [apply] or disposed by calling [dispose]. A snapshot
     * that has been had is [apply] called can also have [dispose] called on it. However, calling
     * [apply] after calling [dispose] will throw an exception.
     *
     * Leaving a snapshot active could cause hard to diagnose memory leaks values are maintained by
     * state objects for unneeded snapshots. Take care to always call [dispose] on any snapshot.
     */
    public open fun apply(): SnapshotApplyResult {
        // NOTE: the this algorithm is currently does not guarantee serializable snapshots as it
        // doesn't prevent crossing writes as described here https://arxiv.org/pdf/1412.2324.pdf

        // Just removing the snapshot from the active snapshot set is enough to make it part of the
        // next snapshot, however, this should only be done after first determining that there are
        // no
        // colliding writes are being applied.

        // A write is considered colliding if any write occurred in a state object in a snapshot
        // applied since the snapshot was taken.
        val modified = modified
        val optimisticMerges =
            if (modified != null) {
                val globalSnapshot = globalSnapshot
                optimisticMerges(
                    globalSnapshot.snapshotId,
                    this,
                    openSnapshots.clear(globalSnapshot.snapshotId),
                )
            } else null

        var observers = emptyList<(Set<Any>, Snapshot) -> Unit>()
        var globalModified: MutableScatterSet<StateObject>? = null
        sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeLocked()
                val globalSnapshot = globalSnapshot
                val previousModified = globalSnapshot.modified
                resetGlobalSnapshotLocked(globalSnapshot, emptyLambda)
                if (previousModified != null && previousModified.isNotEmpty()) {
                    observers = applyObservers
                    globalModified = previousModified
                }
            } else {
                val globalSnapshot = globalSnapshot
                val result =
                    innerApplyLocked(
                        nextSnapshotId,
                        modified,
                        optimisticMerges,
                        openSnapshots.clear(globalSnapshot.snapshotId),
                    )
                if (result != SnapshotApplyResult.Success) return result

                closeLocked()

                // Take a new global snapshot that includes this one.
                val previousModified = globalSnapshot.modified
                resetGlobalSnapshotLocked(globalSnapshot, emptyLambda)
                this.modified = null
                globalSnapshot.modified = null

                observers = applyObservers
                globalModified = previousModified
            }
        }

        // Mark as applied
        applied = true

        // Notify any apply observers that changes applied were seen
        if (globalModified != null) {
            val nonNullGlobalModified = globalModified!!.wrapIntoSet()
            if (nonNullGlobalModified.isNotEmpty()) {
                verboseTrace("Compose:applyObservers") {
                    observers.fastForEach { it(nonNullGlobalModified, this) }
                }
            }
        }

        if (modified != null && modified.isNotEmpty()) {
            val modifiedSet = modified.wrapIntoSet()
            verboseTrace("Compose:applyObservers") {
                observers.fastForEach { it(modifiedSet, this) }
            }
        }

        dispatchObserverOnApplied(this, modified)

        // Wait to release pinned snapshots until after running observers.
        // This permits observers to safely take a nested snapshot of the one that was just applied
        // before unpinning records that need to be retained in this case.
        sync {
            releasePinnedSnapshotsForCloseLocked()
            checkAndOverwriteUnusedRecordsLocked()
            globalModified?.forEach { processForUnusedRecordsLocked(it) }
            modified?.forEach { processForUnusedRecordsLocked(it) }
            merged?.fastForEach { processForUnusedRecordsLocked(it) }
            merged = null
        }

        return SnapshotApplyResult.Success
    }

    override val readOnly: Boolean
        get() = false

    override val root: Snapshot
        get() = this

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            nestedDeactivated(this)
            dispatchObserverOnPreDispose(this)
        }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateNotDisposed()
        validateNotAppliedOrPinned()
        val previousId = snapshotId
        return creatingSnapshot(
            if (this is GlobalSnapshot) null else this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            advance {
                sync {
                    val readonlyId = nextSnapshotId.also { nextSnapshotId += 1 }
                    openSnapshots = openSnapshots.set(readonlyId)
                    NestedReadonlySnapshot(
                        snapshotId = readonlyId,
                        invalid = invalid.addRange(previousId + 1, readonlyId),
                        readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                        parent = this,
                    )
                }
            }
        }
    }

    override fun nestedActivated(snapshot: Snapshot) {
        snapshots++
    }

    override fun nestedDeactivated(snapshot: Snapshot) {
        requirePrecondition(snapshots > 0) { "no pending nested snapshots" }
        if (--snapshots == 0) {
            if (!applied) {
                abandon()
            }
        }
    }

    override fun notifyObjectsInitialized() {
        if (applied || disposed) return
        advance()
    }

    override fun closeLocked() {
        // Remove itself and previous ids from the open set.
        openSnapshots = openSnapshots.clear(snapshotId).andNot(previousIds)
    }

    override fun releasePinnedSnapshotsForCloseLocked() {
        releasePreviouslyPinnedSnapshotsLocked()
        super.releasePinnedSnapshotsForCloseLocked()
    }

    private fun validateNotApplied() {
        checkPrecondition(!applied) { "Unsupported operation on a snapshot that has been applied" }
    }

    private fun validateNotAppliedOrPinned() {
        checkPrecondition(!applied || isPinned) {
            "Unsupported operation on a disposed or applied snapshot"
        }
    }

    /**
     * Abandon the snapshot. This does NOT [closeAndReleasePinning], which must be done as an
     * additional step by callers.
     */
    private fun abandon() {
        val modified = modified
        if (modified != null) {
            validateNotApplied()

            // Mark all state records created in this snapshot as invalid. This allows the snapshot
            // id to be forgotten as no state records will refer to it.
            this.modified = null
            val id = snapshotId
            modified.forEach { state ->
                var current: StateRecord? = state.firstStateRecord
                while (current != null) {
                    if (current.snapshotId == id || current.snapshotId in previousIds) {
                        current.snapshotId = INVALID_SNAPSHOT
                    }
                    current = current.next
                }
            }
        }

        // The snapshot can now be closed.
        closeAndReleasePinning()
    }

    internal fun innerApplyLocked(
        nextId: SnapshotId,
        modified: MutableScatterSet<StateObject>,
        optimisticMerges: Map<StateRecord, StateRecord>?,
        invalidSnapshots: SnapshotIdSet,
    ): SnapshotApplyResult {
        // This must be called in a synchronized block

        // If there are modifications, we need to ensure none of the them have collisions.

        // A record is guaranteed not to collide if no other write was performed to it since this
        // snapshot was taken. No writes to a state object occurred if, ignoring this snapshot,
        // the readable records for the snapshots remain the same. If they are different then
        // there is a potential collision, and the state object is asked if it can resolve
        // it. If it can, the updated state record is used for the apply.

        // Determining if there is a collision and resolving it requires finding:
        //  1) the applying record state (i.e., the record being applied by this snapshot)
        //  2) the current record state (i.e., the record seen by the global snapshot)
        //  3) the previous state record (i.e., the record originally copied)

        // The applying record is the readable record this snapshot observes. It is found by calling
        // readable with this snapshot's ignore set and id (which what the state object also does).

        // The current record can be found by asking what would the next snapshot observes. This is
        // found by calling readable with the invalidSnapshots (the set of all currently open
        // snapshots) which is what the next snapshot would have in its invalid, and nextId, which
        // is the id the next snapshot will have.

        // The previous record can be found by looking for the record that this snapshot would
        // observe had it not modified it. This is done by excluding the snapshot itself from
        // invalid (an all previous ids the snapshot had because it advanced) while still using its
        // id to find the record.

        // Once these records are found, a record is in a merge conflict if both the applying record
        // and the current record have a different record and neither of them is the previous
        // record.

        // If the record is not changed outside this snapshot (the most likely scenario), then
        // there is no conflict, and there is no reason to determine the applying record. For this
        // reason, the current and previous are determined first the applied record is only
        // determined if there is a conflict (as it is assumed applied record is different from the
        // previous record since this code would not execute if they were equal).

        // A state object's mutation policy controls how conflicts are resolved. By default, all
        // conflicts cannot be resolved and the snapshot will not be applied. However, given the
        // previous, current, and next records, sometimes conflicts can be resolved (e.g. similar to
        // merge conflicts in a git commit) and, if so, a new value can be provided by the mutation
        // policy that merges the changes. If all changed objects can be merged, then the snapshot
        // will apply but with the new, merged values (e.g., conflict-free data types are an example
        // of types that can be merged).
        var mergedRecords: MutableList<Pair<StateObject, StateRecord>>? = null
        val start = this.invalid.set(this.snapshotId).or(this.previousIds)
        var statesToRemove: MutableList<StateObject>? = null
        modified.forEach { state ->
            val first = state.firstStateRecord
            // If either current or previous cannot be calculated the object was created
            // in a nested snapshot that was committed then changed.
            val current = readable(first, nextId, invalidSnapshots) ?: return@forEach
            val previous = readable(first, this.snapshotId, start) ?: return@forEach
            if (previous.snapshotId == PreexistingSnapshotId.toSnapshotId()) {
                // A previous record might not be found if the state object was created in a
                // nested snapshot that didn't have any other modifications. The `apply()` for
                // a nested snapshot considers such snapshots no-op snapshots and just closes them
                // which allows this object's previous record to be missing or be the record created
                // during initial construction. In these cases taking applied is the right choice
                // this indicates there was no conflicting writes.
                return@forEach
            }
            if (current != previous) {
                val applied = readable(first, this.snapshotId, this.invalid) ?: readError()
                val merged =
                    optimisticMerges?.get(current)
                        ?: run { state.mergeRecords(previous, current, applied) }
                when (merged) {
                    null -> return SnapshotApplyResult.Failure(this)
                    applied -> {
                        // Nothing to do the merge policy says that the current changes
                        // obscure the current value so ignore the conflict
                    }
                    current -> {
                        (mergedRecords
                                ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                    mergedRecords = it
                                })
                            .add(state to current.create(snapshotId))

                        // If we revert to current then the state is no longer modified.
                        (statesToRemove
                                ?: mutableListOf<StateObject>().also { statesToRemove = it })
                            .add(state)
                    }
                    else -> {
                        (mergedRecords
                                ?: mutableListOf<Pair<StateObject, StateRecord>>().also {
                                    mergedRecords = it
                                })
                            .add(
                                if (merged != previous) state to merged
                                else state to previous.create(snapshotId)
                            )
                    }
                }
            }
        }

        mergedRecords?.let {
            // Ensure we have a new snapshot id
            advance()

            // Update all the merged records to have the new id.
            it.fastForEach { merged ->
                val (state, stateRecord) = merged
                stateRecord.snapshotId = nextId
                sync {
                    stateRecord.next = state.firstStateRecord
                    state.prependStateRecord(stateRecord)
                }
            }
        }

        statesToRemove?.let { list ->
            list.fastForEach { modified.remove(it) }
            val mergedList = merged
            merged = if (mergedList == null) list else mergedList + list
        }

        return SnapshotApplyResult.Success
    }

    internal inline fun <T> advance(block: () -> T): T {
        recordPrevious(snapshotId)
        return block().also {
            // Only advance this snapshot if it's possible for it to be applied later,
            // otherwise we don't need to bother.
            // This simplifies tracking of open snapshots when an apply observer takes
            // a nested snapshot of the snapshot that was just applied.
            if (!applied && !disposed) {
                val previousId = snapshotId
                sync {
                    snapshotId = nextSnapshotId.also { nextSnapshotId += 1 }
                    openSnapshots = openSnapshots.set(snapshotId)
                }
                invalid = invalid.addRange(previousId + 1, snapshotId)
            }
        }
    }

    internal fun advance(): Unit = advance {}

    internal fun recordPrevious(id: SnapshotId) {
        sync { previousIds = previousIds.set(id) }
    }

    internal fun recordPreviousPinnedSnapshot(id: Int) {
        if (id >= 0) previousPinnedSnapshots += id
    }

    internal fun recordPreviousPinnedSnapshots(handles: IntArray) {
        // Avoid unnecessary copies implied by the `+` below.
        if (handles.isEmpty()) return
        val pinned = previousPinnedSnapshots
        previousPinnedSnapshots = if (pinned.isEmpty()) handles else pinned + handles
    }

    private fun releasePreviouslyPinnedSnapshotsLocked() {
        for (index in previousPinnedSnapshots.indices) {
            releasePinningLocked(previousPinnedSnapshots[index])
        }
    }

    internal fun recordPreviousList(snapshots: SnapshotIdSet) {
        sync { previousIds = previousIds.or(snapshots) }
    }

    override fun recordModified(state: StateObject) {
        (modified ?: mutableScatterSetOf<StateObject>().also { modified = it }).add(state)
    }

    override var writeCount: Int = 0

    override var modified: MutableScatterSet<StateObject>? = null

    internal var merged: List<StateObject>? = null

    /**
     * A set of the id's previously associated with this snapshot. When this snapshot closes then
     * these ids must be removed from the global as well.
     */
    internal var previousIds: SnapshotIdSet = SnapshotIdSet.EMPTY

    /** A list of the pinned snapshots handles that must be released by this snapshot */
    internal var previousPinnedSnapshots: IntArray = EmptyIntArray

    /**
     * The number of pending nested snapshots of this snapshot. To simplify the code, this snapshot
     * it, itself, counted as its own nested snapshot.
     */
    private var snapshots = 1

    /** Tracks whether the snapshot has been applied. */
    internal var applied = false

    private companion object {
        private val EmptyIntArray = IntArray(0)
    }
}

/**
 * The result of a applying a mutable snapshot. [Success] indicates that the snapshot was
 * successfully applied and is now visible as the global state of the state object (or visible in
 * the parent snapshot for a nested snapshot). [Failure] indicates one or more state objects were
 * modified by both this snapshot and in the global (or parent) snapshot, and the changes from this
 * snapshot are **not** visible in the global or parent snapshot.
 */
public sealed class SnapshotApplyResult {
    /**
     * Check the result of an apply. If the result is [Success] then this does does nothing. If the
     * result is [Failure] then a [SnapshotApplyConflictException] exception is thrown. Once [check]
     * as been called the snapshot is disposed.
     */
    public abstract fun check()

    /** True if the result is [Success]. */
    public abstract val succeeded: Boolean

    public object Success : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Success] result is a noop.
         */
        override fun check() {}

        override val succeeded: Boolean
            get() = true
    }

    public class Failure(public val snapshot: Snapshot) : SnapshotApplyResult() {
        /**
         * Check the result of a snapshot apply. Calling [check] on a [Failure] result throws a
         * [SnapshotApplyConflictException] exception.
         */
        override fun check() {
            snapshot.dispose()
            throw SnapshotApplyConflictException(snapshot)
        }

        override val succeeded: Boolean
            get() = false
    }
}

/**
 * The type returned by observer registration methods that unregisters the observer when it is
 * disposed.
 */
@Suppress("CallbackName")
public fun interface ObserverHandle {
    /** Dispose the observer causing it to be unregistered from the snapshot system. */
    public fun dispose()
}

/**
 * Return the thread's active snapshot. If no thread snapshot is active then the current global
 * snapshot is used.
 */
internal fun currentSnapshot(): Snapshot = threadSnapshot.get() ?: globalSnapshot

/**
 * An exception that is thrown when [SnapshotApplyResult.check] is called on a result of a
 * [MutableSnapshot.apply] that fails to apply.
 */
public class SnapshotApplyConflictException(@Suppress("unused") public val snapshot: Snapshot) :
    Exception()

/** Snapshot local value of a state object. */
public abstract class StateRecord(
    /** The snapshot id of the snapshot in which the record was created. */
    internal var snapshotId: SnapshotId
) {
    public constructor() : this(currentSnapshot().snapshotId)

    @Deprecated("Use snapshotId: Long constructor instead")
    public constructor(id: Int) : this(id.toSnapshotId())

    /**
     * Reference of the next state record. State records are stored in a linked list.
     *
     * Changes to [next] must preserve all existing records to all threads even during
     * intermediately changes. For example, it is safe to add the beginning or end of the list but
     * adding to the middle requires care. First the new record must have its [next] updated then
     * the [next] of its new predecessor can then be set to point to it. This implies that records
     * that are already in the list cannot be moved in the list as this the change must be atomic to
     * all threads that cannot happen without a lock which this list cannot afford.
     *
     * It is unsafe to remove a record as it might be in the process of being reused (see
     * [usedLocked]). If a record is removed care must be taken to ensure that it is not being
     * claimed by some other thread. This would require changes to [usedLocked].
     */
    internal var next: StateRecord? = null

    /** Copy the value into this state record from another for the same state object. */
    public abstract fun assign(value: StateRecord)

    /**
     * Create a new state record for the same state object. Consider also implementing the [create]
     * overload that provides snapshotId for faster record construction when snapshot id is known.
     */
    public abstract fun create(): StateRecord

    /**
     * Create a new state record for the same state object and provided [snapshotId]. This allows to
     * implement an optimized version of [create] to avoid accessing [currentSnapshot] when snapshot
     * id is known. The default implementation provides a backwards compatible behavior, and should
     * be overridden if [StateRecord] subclass supports this optimization.
     */
    @Deprecated("Use snapshotId: Long version instead", level = DeprecationLevel.HIDDEN)
    public open fun create(snapshotId: Int): StateRecord =
        create().also { it.snapshotId = snapshotId.toSnapshotId() }

    /**
     * Create a new state record for the same state object and provided [snapshotId]. This allows to
     * implement an optimized version of [create] to avoid accessing [currentSnapshot] when snapshot
     * id is known. The default implementation provides a backwards compatible behavior, and should
     * be overridden if [StateRecord] subclass supports this optimization.
     */
    public open fun create(snapshotId: SnapshotId): StateRecord =
        create().also { it.snapshotId = snapshotId }
}

/**
 * Interface implemented by all snapshot aware state objects. Used by this module to maintain the
 * state records of a state object.
 */
@JvmDefaultWithCompatibility
public interface StateObject {
    /** The first state record in a linked list of state records. */
    public val firstStateRecord: StateRecord

    /**
     * Add a new state record to the beginning of a list. After this call [firstStateRecord] should
     * be [value].
     */
    public fun prependStateRecord(value: StateRecord)

    /**
     * Produce a merged state based on the conflicting state changes.
     *
     * This method must not modify any of the records received and should treat the state records as
     * immutable, even the [applied] record.
     *
     * @param previous the state record that was used to create the [applied] record and is a state
     *   that also (though indirectly) produced the [current] record.
     * @param current the state record of the parent snapshot or global state.
     * @param applied the state record that is being applied of the parent snapshot or global state.
     * @return the modified state or `null` if the values cannot be merged. If the states cannot be
     *   merged the current apply will fail. Any of the parameters can be returned as a result. If
     *   it is not one of the parameter values then it *must* be a new value that is created by
     *   calling [StateRecord.create] on one of the records passed and then can be modified to have
     *   the merged value before being returned. If a new record is returned [MutableSnapshot.apply]
     *   will update the internal snapshot id and call [prependStateRecord] if the record is used.
     */
    public fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord,
    ): StateRecord? = null
}

/**
 * A snapshot whose state objects cannot be modified. If a state object is modified when in a
 * read-only snapshot a [IllegalStateException] is thrown.
 */
internal class ReadonlySnapshot
internal constructor(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
) : Snapshot(snapshotId, invalid) {
    /**
     * The number of nested snapshots that are active. To simplify the code, this snapshot counts
     * itself as a nested snapshot.
     */
    private var snapshots = 1
    override val readOnly: Boolean
        get() = true

    override val root: Snapshot
        get() = this

    override fun hasPendingChanges(): Boolean = false

    override val writeObserver: ((Any) -> Unit)?
        get() = null

    override var modified: MutableScatterSet<StateObject>?
        get() = null
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        validateOpen(this)
        return creatingSnapshot(
            parent = this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            NestedReadonlySnapshot(
                snapshotId = snapshotId,
                invalid = invalid,
                readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                parent = this,
            )
        }
    }

    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }

    override fun dispose() {
        if (!disposed) {
            nestedDeactivated(this)
            super.dispose()
            dispatchObserverOnPreDispose(this)
        }
    }

    override fun nestedActivated(snapshot: Snapshot) {
        snapshots++
    }

    override fun nestedDeactivated(snapshot: Snapshot) {
        if (--snapshots == 0) {
            // A read-only snapshot can be just be closed as it has no modifications.
            closeAndReleasePinning()
        }
    }

    override fun recordModified(state: StateObject) {
        reportReadonlySnapshotWrite()
    }
}

internal class NestedReadonlySnapshot(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    override val readObserver: ((Any) -> Unit)?,
    val parent: Snapshot,
) : Snapshot(snapshotId, invalid) {
    init {
        parent.nestedActivated(this)
    }

    override val readOnly
        get() = true

    override val root: Snapshot
        get() = parent.root

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?) =
        creatingSnapshot(
            parent = this,
            readObserver = readObserver,
            writeObserver = null,
            readonly = true,
        ) { actualReadObserver, _ ->
            NestedReadonlySnapshot(
                snapshotId = snapshotId,
                invalid = invalid,
                readObserver = mergedReadObserver(actualReadObserver, this.readObserver),
                parent = parent,
            )
        }

    override fun notifyObjectsInitialized() {
        // Nothing to do for read-only snapshots
    }

    override fun hasPendingChanges(): Boolean = false

    override fun dispose() {
        if (!disposed) {
            if (snapshotId != parent.snapshotId) {
                closeAndReleasePinning()
            }
            parent.nestedDeactivated(this)
            super.dispose()
            dispatchObserverOnPreDispose(this)
        }
    }

    override val modified: MutableScatterSet<StateObject>?
        get() = null

    override val writeObserver: ((Any) -> Unit)?
        get() = null

    override fun recordModified(state: StateObject) = reportReadonlySnapshotWrite()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()

    override fun nestedActivated(snapshot: Snapshot) = unsupported()
}

private val emptyLambda: (invalid: SnapshotIdSet) -> Unit = {}

/**
 * A snapshot object that simplifies the code by treating the global state as a mutable snapshot.
 */
internal class GlobalSnapshot(snapshotId: SnapshotId, invalid: SnapshotIdSet) :
    MutableSnapshot(
        snapshotId,
        invalid,
        null,
        { state -> sync { globalWriteObservers.fastForEach { it(state) } } },
    ) {

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot =
        creatingSnapshot(
            parent = null,
            readonly = true,
            readObserver = readObserver,
            writeObserver = null,
        ) { actualReadObserver, _ ->
            takeNewSnapshot { invalid ->
                ReadonlySnapshot(
                    snapshotId = sync { nextSnapshotId.also { nextSnapshotId += 1 } },
                    invalid = invalid,
                    readObserver = actualReadObserver,
                )
            }
        }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?,
    ): MutableSnapshot =
        creatingSnapshot(
            parent = null,
            readonly = false,
            readObserver = readObserver,
            writeObserver = writeObserver,
        ) { actualReadObserver, actualWriteObserver ->
            takeNewSnapshot { invalid ->
                MutableSnapshot(
                    snapshotId = sync { nextSnapshotId.also { nextSnapshotId += 1 } },
                    invalid = invalid,

                    // It is intentional that the global read observers are not merged with mutable
                    // snapshots read observers.
                    readObserver = actualReadObserver,

                    // It is intentional that global write observers are not merged with mutable
                    // snapshots write observers.
                    writeObserver = actualWriteObserver,
                )
            }
        }

    override fun notifyObjectsInitialized() {
        advanceGlobalSnapshot()
    }

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()

    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun apply(): SnapshotApplyResult =
        error("Cannot apply the global snapshot directly. Call Snapshot.advanceGlobalSnapshot")

    override fun dispose() {
        sync { releasePinnedSnapshotLocked() }
    }
}

/** A nested mutable snapshot created by [MutableSnapshot.takeNestedMutableSnapshot]. */
internal class NestedMutableSnapshot(
    snapshotId: SnapshotId,
    invalid: SnapshotIdSet,
    readObserver: ((Any) -> Unit)?,
    writeObserver: ((Any) -> Unit)?,
    val parent: MutableSnapshot,
) : MutableSnapshot(snapshotId, invalid, readObserver, writeObserver) {
    private var deactivated = false

    init {
        parent.nestedActivated(this)
    }

    override val root: Snapshot
        get() = parent.root

    override fun dispose() {
        if (!disposed) {
            super.dispose()
            deactivate()
        }
    }

    override fun apply(): SnapshotApplyResult {
        if (parent.applied || parent.disposed) return SnapshotApplyResult.Failure(this)

        // Applying a nested mutable snapshot applies its changes to the parent snapshot.

        // See MutableSnapshot.apply() for implantation notes.

        // The apply observer notification are for applying to the global scope so it is elided
        // here making this code a bit simpler than MutableSnapshot.apply.

        val modified = modified
        val id = snapshotId
        val optimisticMerges =
            if (modified != null) optimisticMerges(parent.snapshotId, this, parent.invalid)
            else null
        sync {
            validateOpen(this)
            if (modified == null || modified.size == 0) {
                closeAndReleasePinning()
            } else {
                val result =
                    innerApplyLocked(parent.snapshotId, modified, optimisticMerges, parent.invalid)
                if (result != SnapshotApplyResult.Success) return result

                parent.modified?.apply { addAll(modified) }
                    ?: modified.also {
                        // Ensure modified reference is only used by one snapshot
                        parent.modified = it
                        this.modified = null
                    }
            }

            // Ensure the parent is newer than the current snapshot
            if (parent.snapshotId < id) {
                parent.advance()
            }

            // Make the snapshot visible in the parent snapshot
            parent.invalid = parent.invalid.clear(id).andNot(previousIds)

            // Ensure the ids associated with this snapshot are also applied by the parent.
            parent.recordPrevious(id)
            parent.recordPreviousPinnedSnapshot(takeoverPinnedSnapshot())
            parent.recordPreviousList(previousIds)
            parent.recordPreviousPinnedSnapshots(previousPinnedSnapshots)
        }

        applied = true
        deactivate()
        dispatchObserverOnApplied(this, modified)
        return SnapshotApplyResult.Success
    }

    private fun deactivate() {
        if (!deactivated) {
            deactivated = true
            parent.nestedDeactivated(this)
        }
    }
}

/** A pseudo snapshot that doesn't introduce isolation but does introduce observers. */
internal class TransparentObserverMutableSnapshot(
    private val parentSnapshot: MutableSnapshot?,
    specifiedReadObserver: ((Any) -> Unit)?,
    specifiedWriteObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean,
    private val ownsParentSnapshot: Boolean,
) :
    MutableSnapshot(
        INVALID_SNAPSHOT,
        SnapshotIdSet.EMPTY,
        mergedReadObserver(
            specifiedReadObserver,
            parentSnapshot?.readObserver ?: globalSnapshot.readObserver,
            mergeParentObservers,
        ),
        mergedWriteObserver(
            specifiedWriteObserver,
            parentSnapshot?.writeObserver ?: globalSnapshot.writeObserver,
        ),
    ) {
    override var readObserver: ((Any) -> Unit)? = super.readObserver
    override var writeObserver: ((Any) -> Unit)? = super.writeObserver

    internal val threadId: Long = currentThreadId()

    private val currentSnapshot: MutableSnapshot
        get() = parentSnapshot ?: globalSnapshot

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
        if (ownsParentSnapshot) {
            parentSnapshot?.dispose()
        }
    }

    override var snapshotId: SnapshotId
        get() = currentSnapshot.snapshotId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            unsupported()
        }

    override var invalid
        get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableScatterSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override var writeCount: Int
        get() = currentSnapshot.writeCount
        set(value) {
            currentSnapshot.writeCount = value
        }

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun apply(): SnapshotApplyResult = currentSnapshot.apply()

    override fun recordModified(state: StateObject) = currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                previousSnapshot = currentSnapshot.takeNestedSnapshot(null),
                readObserver = mergedReadObserver,
                ownsPreviousSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun takeNestedMutableSnapshot(
        readObserver: ((Any) -> Unit)?,
        writeObserver: ((Any) -> Unit)?,
    ): MutableSnapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        val mergedWriteObserver = mergedWriteObserver(writeObserver, this.writeObserver)
        return if (!mergeParentObservers) {
            val nestedSnapshot =
                currentSnapshot.takeNestedMutableSnapshot(
                    readObserver = null,
                    writeObserver = mergedWriteObserver,
                )
            TransparentObserverMutableSnapshot(
                parentSnapshot = nestedSnapshot,
                specifiedReadObserver = mergedReadObserver,
                specifiedWriteObserver = mergedWriteObserver,
                mergeParentObservers = false,
                ownsParentSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedMutableSnapshot(mergedReadObserver, mergedWriteObserver)
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

/** A pseudo snapshot that doesn't introduce isolation but does introduce observers. */
internal class TransparentObserverSnapshot(
    private val parentSnapshot: Snapshot?,
    specifiedReadObserver: ((Any) -> Unit)?,
    private val mergeParentObservers: Boolean,
    private val ownsParentSnapshot: Boolean,
) : Snapshot(INVALID_SNAPSHOT, SnapshotIdSet.EMPTY) {
    override var readObserver: ((Any) -> Unit)? =
        mergedReadObserver(
            specifiedReadObserver,
            parentSnapshot?.readObserver ?: globalSnapshot.readObserver,
            mergeParentObservers,
        )
    override val writeObserver: ((Any) -> Unit)? = null

    internal val threadId: Long = currentThreadId()

    override val root: Snapshot = this

    private val currentSnapshot: Snapshot
        get() = parentSnapshot ?: globalSnapshot

    override fun dispose() {
        // Explicitly don't call super.dispose()
        disposed = true
        if (ownsParentSnapshot) {
            parentSnapshot?.dispose()
        }
    }

    override var snapshotId: SnapshotId
        get() = currentSnapshot.snapshotId
        @Suppress("UNUSED_PARAMETER")
        set(value) {
            unsupported()
        }

    override var invalid
        get() = currentSnapshot.invalid
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override fun hasPendingChanges(): Boolean = currentSnapshot.hasPendingChanges()

    override var modified: MutableScatterSet<StateObject>?
        get() = currentSnapshot.modified
        @Suppress("UNUSED_PARAMETER") set(value) = unsupported()

    override val readOnly: Boolean
        get() = currentSnapshot.readOnly

    override fun recordModified(state: StateObject) = currentSnapshot.recordModified(state)

    override fun takeNestedSnapshot(readObserver: ((Any) -> Unit)?): Snapshot {
        val mergedReadObserver = mergedReadObserver(readObserver, this.readObserver)
        return if (!mergeParentObservers) {
            createTransparentSnapshotWithNoParentReadObserver(
                currentSnapshot.takeNestedSnapshot(null),
                mergedReadObserver,
                ownsPreviousSnapshot = true,
            )
        } else {
            currentSnapshot.takeNestedSnapshot(mergedReadObserver)
        }
    }

    override fun notifyObjectsInitialized() = currentSnapshot.notifyObjectsInitialized()

    /** Should never be called. */
    override fun nestedActivated(snapshot: Snapshot) = unsupported()

    override fun nestedDeactivated(snapshot: Snapshot) = unsupported()
}

private fun createTransparentSnapshotWithNoParentReadObserver(
    previousSnapshot: Snapshot?,
    readObserver: ((Any) -> Unit)? = null,
    ownsPreviousSnapshot: Boolean = false,
): Snapshot =
    if (previousSnapshot is MutableSnapshot || previousSnapshot == null) {
        TransparentObserverMutableSnapshot(
            parentSnapshot = previousSnapshot as? MutableSnapshot,
            specifiedReadObserver = readObserver,
            specifiedWriteObserver = null,
            mergeParentObservers = false,
            ownsParentSnapshot = ownsPreviousSnapshot,
        )
    } else {
        TransparentObserverSnapshot(
            parentSnapshot = previousSnapshot,
            specifiedReadObserver = readObserver,
            mergeParentObservers = false,
            ownsParentSnapshot = ownsPreviousSnapshot,
        )
    }

internal fun mergedReadObserver(
    readObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?,
    mergeReadObserver: Boolean = true,
): ((Any) -> Unit)? {
    @Suppress("NAME_SHADOWING") val parentObserver = if (mergeReadObserver) parentObserver else null
    return if (readObserver != null && parentObserver != null && readObserver !== parentObserver) {
        { state: Any ->
            readObserver(state)
            parentObserver(state)
        }
    } else readObserver ?: parentObserver
}

internal fun mergedWriteObserver(
    writeObserver: ((Any) -> Unit)?,
    parentObserver: ((Any) -> Unit)?,
): ((Any) -> Unit)? =
    if (writeObserver != null && parentObserver != null && writeObserver !== parentObserver) {
        { state: Any ->
            writeObserver(state)
            parentObserver(state)
        }
    } else writeObserver ?: parentObserver

/**
 * Snapshot id of `0` is reserved as invalid and no state record with snapshot `0` is considered
 * valid.
 *
 * The value `0` was chosen as it is the default value of the Int snapshot id type and records
 * initially created will naturally have a snapshot id of 0. If this wasn't considered invalid
 * adding such a record to a state object will make the state record immediately visible to the
 * snapshots instead of being born invalid. Using `0` ensures all state records are created invalid
 * and must be explicitly marked as valid in to be visible in a snapshot.
 */
private val INVALID_SNAPSHOT = SnapshotIdZero

/** Current thread snapshot */
private val threadSnapshot = SnapshotThreadLocal<Snapshot>()

/**
 * A global synchronization object. This synchronization object should be taken before modifying any
 * of the fields below.
 */
@PublishedApi internal val lock: SynchronizedObject = makeSynchronizedObject()

@Suppress("BanInlineOptIn", "LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
@PublishedApi
internal inline fun <T> sync(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return synchronized(lock, block)
}

// The following variables should only be written when sync is taken

/**
 * A set of snapshots that are currently open and should be considered invalid for new snapshots.
 */
private var openSnapshots = SnapshotIdSet.EMPTY

/** The first snapshot created must be at least on more than the [Snapshot.PreexistingSnapshotId] */
private var nextSnapshotId = Snapshot.PreexistingSnapshotId.toSnapshotId() + 1

/**
 * A tracking table for pinned snapshots. A pinned snapshot is the lowest snapshot id that the
 * snapshot is ignoring by considering them invalid. This is used to calculate when a snapshot
 * record can be reused.
 */
private val pinningTable = SnapshotDoubleIndexHeap()

/**
 * The set of objects who have more than one active state record. These are traversed during apply
 * of mutable snapshots and when the global snapshot is advanced to determine if any of the records
 * can be cleared.
 */
private val extraStateObjects = SnapshotWeakSet<StateObject>()

/** A list of apply observers */
private var applyObservers = emptyList<(Set<Any>, Snapshot) -> Unit>()

/** A list of observers of writes to the global state. */
private var globalWriteObservers = emptyList<(Any) -> Unit>()

private val globalSnapshot =
    GlobalSnapshot(
            snapshotId = nextSnapshotId.also { nextSnapshotId += 1 },
            invalid = SnapshotIdSet.EMPTY,
        )
        .also { openSnapshots = openSnapshots.set(it.snapshotId) }

// Unused, kept for API compat
@Suppress("unused") @PublishedApi internal val snapshotInitializer: Snapshot = globalSnapshot

private fun <T> resetGlobalSnapshotLocked(
    globalSnapshot: GlobalSnapshot,
    block: (invalid: SnapshotIdSet) -> T,
): T {
    val snapshotId = globalSnapshot.snapshotId
    val result = block(openSnapshots.clear(snapshotId))

    val nextGlobalSnapshotId = nextSnapshotId
    nextSnapshotId += 1

    openSnapshots = openSnapshots.clear(snapshotId)
    globalSnapshot.snapshotId = nextGlobalSnapshotId
    globalSnapshot.invalid = openSnapshots
    globalSnapshot.writeCount = 0
    globalSnapshot.modified = null
    globalSnapshot.releasePinnedSnapshotLocked()
    openSnapshots = openSnapshots.set(nextGlobalSnapshotId)

    return result
}

/**
 * Counts the number of threads currently inside `advanceGlobalSnapshot`, notifying observers of
 * changes to the global snapshot.
 */
private var pendingApplyObserverCount = AtomicInt(0)

private fun <T> advanceGlobalSnapshot(block: (invalid: SnapshotIdSet) -> T): T {
    val globalSnapshot = globalSnapshot

    val modified: MutableScatterSet<StateObject>?
    val result = sync {
        modified = globalSnapshot.modified
        if (modified != null) {
            pendingApplyObserverCount.add(1)
        }
        resetGlobalSnapshotLocked(globalSnapshot, block)
    }

    // If the previous global snapshot had any modified states then notify the registered apply
    // observers.
    modified?.let {
        try {
            val observers = applyObservers
            val modifiedSet = it.wrapIntoSet()
            verboseTrace("Compose:applyObservers") {
                observers.fastForEach { observer -> observer(modifiedSet, globalSnapshot) }
            }
        } finally {
            pendingApplyObserverCount.add(-1)
        }
    }

    sync {
        checkAndOverwriteUnusedRecordsLocked()
        modified?.forEach { processForUnusedRecordsLocked(it) }
    }

    return result
}

private fun advanceGlobalSnapshot() = advanceGlobalSnapshot(emptyLambda)

private fun <T : Snapshot> takeNewSnapshot(block: (invalid: SnapshotIdSet) -> T): T =
    advanceGlobalSnapshot { invalid ->
        val result = block(invalid)
        sync { openSnapshots = openSnapshots.set(result.snapshotId) }
        result
    }

private fun validateOpen(snapshot: Snapshot) {
    val openSnapshots = openSnapshots
    if (!openSnapshots.get(snapshot.snapshotId)) {
        error(
            "Snapshot is not open: snapshotId=${
                snapshot.snapshotId
            }, disposed=${
                snapshot.disposed
            }, applied=${
                (snapshot as? MutableSnapshot)?.applied ?: "read-only"
            }, lowestPin=${
                sync { pinningTable.lowestOrDefault(SnapshotIdInvalidValue) }
            }"
        )
    }
}

/**
 * A candidate snapshot is valid if the it is less than or equal to the current snapshot and it
 * wasn't specifically marked as invalid when the snapshot started.
 *
 * All snapshot active at when the snapshot was taken considered invalid for the snapshot (they have
 * not been applied and therefore are considered invalid).
 *
 * All snapshots taken after the current snapshot are considered invalid since they where taken
 * after the current snapshot was taken.
 *
 * INVALID_SNAPSHOT is reserved as an invalid snapshot id.
 */
private fun valid(
    currentSnapshot: SnapshotId,
    candidateSnapshot: SnapshotId,
    invalid: SnapshotIdSet,
): Boolean {
    return candidateSnapshot != INVALID_SNAPSHOT &&
        candidateSnapshot <= currentSnapshot &&
        !invalid.get(candidateSnapshot)
}

// Determine if the given data is valid for the snapshot.
private fun valid(data: StateRecord, snapshot: SnapshotId, invalid: SnapshotIdSet): Boolean {
    return valid(snapshot, data.snapshotId, invalid)
}

private fun <T : StateRecord> readable(r: T, id: SnapshotId, invalid: SnapshotIdSet): T? {
    // The readable record is the valid record with the highest snapshotId
    var current: StateRecord? = r
    var candidate: StateRecord? = null
    while (current != null) {
        if (valid(current, id, invalid)) {
            candidate =
                if (candidate == null) current
                else if (candidate.snapshotId < current.snapshotId) current else candidate
        }
        current = current.next
    }
    if (candidate != null) {
        @Suppress("UNCHECKED_CAST")
        return candidate as T
    }
    return null
}

/**
 * Return the current readable state record for the current snapshot. It is assumed that [this] is
 * the first record of [state]
 */
public fun <T : StateRecord> T.readable(state: StateObject): T {
    val snapshot = Snapshot.current
    snapshot.readObserver?.invoke(state)
    return readable(this, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Readable can return null when the global snapshot has been advanced by another thread
            // and state written to the object was overwritten while this thread was paused.
            // Repeating the read is valid here as either this will return the same result as
            // the previous call or will find a valid record. Being in a sync block prevents other
            // threads from writing to this state object until the read completes.
            val syncSnapshot = Snapshot.current
            @Suppress("UNCHECKED_CAST")
            readable(state.firstStateRecord as T, syncSnapshot.snapshotId, syncSnapshot.invalid)
                ?: readError()
        }
}

// unused, still here for API compat.
/**
 * Return the current readable state record for the [snapshot]. It is assumed that [this] is the
 * first record of [state]
 */
public fun <T : StateRecord> T.readable(state: StateObject, snapshot: Snapshot): T {
    // invoke the observer associated with the current snapshot.
    snapshot.readObserver?.invoke(state)
    return readable(this, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Readable can return null when the global snapshot has been advanced by another thread
            // See T.readable(state: StateObject) for more info.
            val syncSnapshot = Snapshot.current
            @Suppress("UNCHECKED_CAST")
            readable(state.firstStateRecord as T, syncSnapshot.snapshotId, syncSnapshot.invalid)
                ?: readError()
        }
}

private fun readError(): Nothing {
    error(
        "Reading a state that was created after the snapshot was taken or in a snapshot that " +
            "has not yet been applied"
    )
}

/**
 * A record can be reused if no other snapshot will see it as valid. This is always true for a
 * record created in an abandoned snapshot. It is also true if the record is valid in the previous
 * snapshot and is obscured by another record also valid in the previous state record.
 */
private fun usedLocked(state: StateObject): StateRecord? {
    var current: StateRecord? = state.firstStateRecord
    var validRecord: StateRecord? = null
    val reuseLimit = pinningTable.lowestOrDefault(nextSnapshotId) - 1
    val invalid = SnapshotIdSet.EMPTY
    while (current != null) {
        val currentId = current.snapshotId
        if (currentId == INVALID_SNAPSHOT) {
            // Any records that were marked invalid by an abandoned snapshot or is marked reachable
            // can be used immediately.
            return current
        }
        if (valid(current, reuseLimit, invalid)) {
            if (validRecord == null) {
                validRecord = current
            } else {
                // If we have two valid records one must obscure the other. Return the
                // record with the lowest id
                return if (current.snapshotId < validRecord.snapshotId) current else validRecord
            }
        }
        current = current.next
    }
    return null
}

/**
 * Clear records that cannot be selected in any currently open snapshot.
 *
 * This method uses the same technique as [usedLocked] which uses the [pinningTable] to determine
 * lowest id in the invalid set for all snapshots. Only the record with the greatest id of all
 * records less or equal to this lowest id can possibly be selected in any snapshot and all other
 * records below that number can be overwritten.
 *
 * However, this technique doesn't find all records that will not be selected by any open snapshot
 * as a record that has an id above that number could be reusable but will not be found.
 *
 * For example if snapshot 1 is open and 2 is created and modifies [state] then is applied, 3 is
 * open and then 4 is open, and then 1 is applied. When 3 modifies [state] and then applies, as 1 is
 * pinned by 4, it is uncertain whether the record for 2 is needed by 4 so it must be kept even if 4
 * also modified [state] and would not select 2. Accurately determine if a record is selectable
 * would require keeping a list of all open [Snapshot] instances which currently is not kept and
 * traversing that list for each record.
 *
 * If any such records are possible this method returns true. In other words, this method returns
 * true if any records might be reusable but this function could not prove there were or not.
 */
private fun overwriteUnusedRecordsLocked(state: StateObject): Boolean {
    var current: StateRecord? = state.firstStateRecord
    var overwriteRecord: StateRecord? = null
    var validRecord: StateRecord? = null
    val reuseLimit = pinningTable.lowestOrDefault(nextSnapshotId)
    var retainedRecords = 0

    while (current != null) {
        val currentId = current.snapshotId
        if (currentId != INVALID_SNAPSHOT) {
            if (currentId < reuseLimit) {
                if (validRecord == null) {
                    // If any records are below [reuseLimit] then we must keep the highest one
                    // so the lowest snapshot can select it.
                    validRecord = current
                    retainedRecords++
                } else {
                    // If [validRecord] is from an earlier snapshot, overwrite it instead
                    val recordToOverwrite =
                        if (current.snapshotId < validRecord.snapshotId) {
                            current
                        } else {
                            // We cannot use `.also { }` here as it prevents smart casting of other
                            // uses of [validRecord].
                            val result = validRecord
                            validRecord = current
                            result
                        }
                    if (overwriteRecord == null) {
                        // Find a record we will definitely keep
                        overwriteRecord =
                            state.firstStateRecord.findYoungestOr { it.snapshotId >= reuseLimit }
                    }
                    recordToOverwrite.snapshotId = INVALID_SNAPSHOT
                    recordToOverwrite.assign(overwriteRecord)
                }
            } else {
                retainedRecords++
            }
        }
        current = current.next
    }

    return retainedRecords > 1
}

private inline fun StateRecord.findYoungestOr(predicate: (StateRecord) -> Boolean): StateRecord {
    var current: StateRecord? = this
    var youngest = this
    while (current != null) {
        if (predicate(current)) return current
        if (youngest.snapshotId < current.snapshotId) youngest = current
        current = current.next
    }
    return youngest
}

private fun checkAndOverwriteUnusedRecordsLocked() {
    extraStateObjects.removeIf { !overwriteUnusedRecordsLocked(it) }
}

private fun processForUnusedRecordsLocked(state: StateObject) {
    if (overwriteUnusedRecordsLocked(state)) {
        extraStateObjects.add(state)
    }
}

@PublishedApi
internal fun <T : StateRecord> T.writableRecord(state: StateObject, snapshot: Snapshot): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.snapshotId
    val readData = readable(this, id, snapshot.invalid) ?: readError()

    // If the readable data was born in this snapshot, it is writable.
    if (readData.snapshotId == snapshot.snapshotId) return readData

    // Otherwise, make a copy of the readable data and mark it as born in this snapshot, making it
    // writable.
    @Suppress("UNCHECKED_CAST")
    val newData =
        sync {
            // Verify that some other thread didn't already create this.
            val newReadData = readable(state.firstStateRecord, id, snapshot.invalid) ?: readError()
            if (newReadData.snapshotId == id) newReadData
            else newReadData.newWritableRecordLocked(state, snapshot)
        }
            as T

    if (readData.snapshotId != Snapshot.PreexistingSnapshotId.toSnapshotId()) {
        snapshot.recordModified(state)
    }

    return newData
}

internal fun <T : StateRecord> T.overwritableRecord(
    state: StateObject,
    snapshot: Snapshot,
    candidate: T,
): T {
    if (snapshot.readOnly) {
        // If the snapshot is read-only, use the snapshot recordModified to report it.
        snapshot.recordModified(state)
    }
    val id = snapshot.snapshotId

    if (candidate.snapshotId == id) return candidate

    val newData = sync { newOverwritableRecordLocked(state) }
    newData.snapshotId = id

    if (candidate.snapshotId != Snapshot.PreexistingSnapshotId.toSnapshotId()) {
        snapshot.recordModified(state)
    }

    return newData
}

internal fun <T : StateRecord> T.newWritableRecord(state: StateObject, snapshot: Snapshot) = sync {
    newWritableRecordLocked(state, snapshot)
}

private fun <T : StateRecord> T.newWritableRecordLocked(state: StateObject, snapshot: Snapshot): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    val newData = newOverwritableRecordLocked(state)
    newData.assign(this)
    newData.snapshotId = snapshot.snapshotId
    return newData
}

internal fun <T : StateRecord> T.newOverwritableRecordLocked(state: StateObject): T {
    // Calling used() on a state object might return the same record for each thread calling
    // used() therefore selecting the record to reuse should be guarded.

    // Note: setting the snapshotId to Int.MAX_VALUE will make it invalid for all snapshots.
    // This means the lock can be released as used() will no longer select it. Using id could
    // also be used but it puts the object into a state where the reused value appears to be
    // the current valid value for the snapshot. This is not an issue if the snapshot is only
    // being read from a single thread but using Int.MAX_VALUE allows multiple readers,
    // single writer, of a snapshot. Note that threads reading a mutating snapshot should not
    // cache the result of readable() as the mutating thread calls to writable() can change the
    // result of readable().
    @Suppress("UNCHECKED_CAST")
    return (usedLocked(state) as T?)?.apply { snapshotId = SnapshotIdMax }
        ?: create(SnapshotIdMax).apply {
            this.next = state.firstStateRecord
            state.prependStateRecord(this as T)
        } as T
}

@PublishedApi
internal fun notifyWrite(snapshot: Snapshot, state: StateObject) {
    snapshot.writeCount += 1
    snapshot.writeObserver?.invoke(state)
}

/**
 * Call [block] with a writable state record for [snapshot] of the given record. It is assumed that
 * this is called for the first state record in a state object. If the snapshot is read-only calling
 * this will throw.
 */
public inline fun <T : StateRecord, R> T.writable(
    state: StateObject,
    snapshot: Snapshot,
    block: T.() -> R,
): R {
    // A writable record will always be the readable record (as all newer records are invalid it
    // must be the newest valid record). This means that if the readable record is not from the
    // current snapshot, a new record must be created. To create a new writable record, a record
    // can be reused, if possible, and the readable record is applied to it. If a record cannot
    // be reused, a new record is created and the readable record is applied to it. Once the
    // values are correct the record is made live by giving it the current snapshot id.

    // Writes need to be in a `sync` block as all writes in flight must be completed before a new
    // snapshot is take. Writing in a sync block ensures this is the case because new snapshots
    // are also in a sync block.
    return sync { this.writableRecord(state, snapshot).block() }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is called
 * for the first state record in a state object. A record is writable if it was created in the
 * current mutable snapshot.
 */
public inline fun <T : StateRecord, R> T.writable(state: StateObject, block: T.() -> R): R {
    val snapshot: Snapshot
    return sync {
            snapshot = Snapshot.current
            this.writableRecord(state, snapshot).block()
        }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Call [block] with a writable state record for the given record. It is assumed that this is called
 * for the first state record in a state object. A record is writable if it was created in the
 * current mutable snapshot. This should only be used when the record will be overwritten in its
 * entirety (such as having only one field and that field is written to).
 *
 * WARNING: If the caller doesn't overwrite all the fields in the state record the object will be
 * inconsistent and the fields not written are almost guaranteed to be incorrect. If it is possible
 * that [block] will not write to all the fields use [writable] instead.
 *
 * @param state The object that has this record in its record list.
 * @param candidate The current for the snapshot record returned by [withCurrent]
 * @param block The block that will mutate all the field of the record.
 */
internal inline fun <T : StateRecord, R> T.overwritable(
    state: StateObject,
    candidate: T,
    block: T.() -> R,
): R {
    val snapshot: Snapshot
    return sync {
            snapshot = Snapshot.current
            this.overwritableRecord(state, snapshot, candidate).block()
        }
        .also { notifyWrite(snapshot, state) }
}

/**
 * Produce a set of optimistic merges of the state records, this is performed outside the a
 * synchronization block to reduce the amount of time taken in the synchronization block reducing
 * the thread contention of merging state values.
 *
 * How sets and ids are used to determine a merged record is explained in
 * [MutableSnapshot.innerApplyLocked].
 *
 * @see MutableSnapshot.innerApplyLocked
 */
private fun optimisticMerges(
    currentSnapshotId: SnapshotId,
    applyingSnapshot: MutableSnapshot,
    invalidSnapshots: SnapshotIdSet,
): Map<StateRecord, StateRecord>? {
    val modified = applyingSnapshot.modified
    if (modified == null) return null
    val applyingSnapshotId = applyingSnapshot.snapshotId
    val start = applyingSnapshot.invalid.set(applyingSnapshotId).or(applyingSnapshot.previousIds)
    var result: MutableMap<StateRecord, StateRecord>? = null
    modified.forEach { state ->
        val first = state.firstStateRecord
        val current = readable(first, currentSnapshotId, invalidSnapshots) ?: return@forEach
        val previous = readable(first, applyingSnapshotId, start) ?: return@forEach
        if (current != previous) {
            // Try to produce a merged state record
            val applied =
                readable(first, applyingSnapshotId, applyingSnapshot.invalid) ?: readError()
            val merged = state.mergeRecords(previous, current, applied)
            if (merged != null) {
                (result ?: hashMapOf<StateRecord, StateRecord>().also { result = it })[current] =
                    merged
            } else {
                // If one fails don't bother calculating the others as they are likely not going
                // to be used. There is an unlikely case that a optimistic merge cannot be
                // produced but the snapshot will apply because, once the synchronization is taken,
                // the current state can be merge. This routine errors on the side of reduced
                // overall work by not performing work that is likely to be ignored.
                return null
            }
        }
    }
    return result
}

private fun reportReadonlySnapshotWrite(): Nothing {
    error("Cannot modify a state object in a read-only snapshot")
}

/** Returns the current record without notifying any read observers. */
@PublishedApi
internal fun <T : StateRecord> current(r: T, snapshot: Snapshot): T =
    readable(r, snapshot.snapshotId, snapshot.invalid)
        ?: sync {
            // Global snapshot could have been advanced
            // see StateRecord.readable for more details
            readable(r, snapshot.snapshotId, snapshot.invalid)
        }
        ?: readError()

@PublishedApi
internal fun <T : StateRecord> current(r: T): T =
    Snapshot.current.let { snapshot ->
        readable(r, snapshot.snapshotId, snapshot.invalid)
            ?: sync {
                // Global snapshot could have been advanced
                // see StateRecord.readable for more details
                Snapshot.current.let { syncSnapshot ->
                    readable(r, syncSnapshot.snapshotId, syncSnapshot.invalid)
                }
            }
            ?: readError()
    }

/**
 * Provides a [block] with the current record, without notifying any read observers.
 *
 * @see readable
 */
public inline fun <T : StateRecord, R> T.withCurrent(block: (r: T) -> R): R = block(current(this))

/** Helper routine to add a range of values ot a snapshot set */
internal fun SnapshotIdSet.addRange(from: SnapshotId, until: SnapshotId): SnapshotIdSet {
    var result = this
    var invalidId = from
    while (invalidId < until) {
        result = result.set(invalidId)
        invalidId += 1
    }
    return result
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/snapshots/SnapshotStateList.kt
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.requirePrecondition

/**
 * An implementation of [MutableList] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateListOf].
 *
 * This class closely implements the same semantics as [ArrayList].
 *
 * @see androidx.compose.runtime.mutableStateListOf
 */
@Stable
public expect class SnapshotStateList<T> internal constructor(persistentList: PersistentList<T>) :
    StateObject, MutableList<T>, RandomAccess {
    public constructor()

    override var firstStateRecord: StateRecord
        private set

    override fun prependStateRecord(value: StateRecord)

    /**
     * Return a list containing all the elements of this list.
     *
     * The list returned is immutable and returned will not change even if the content of the list
     * is changed in the same snapshot. It also will be the same instance until the content is
     * changed. It is not, however, guaranteed to be the same instance for the same list as adding
     * and removing the same item from the this list might produce a different instance with the
     * same content.
     *
     * This operation is O(1) and does not involve a physically copying the list. It instead returns
     * the underlying immutable list used internally to store the content of the list.
     *
     * It is recommended to use [toList] when using returning the value of this list from
     * [androidx.compose.runtime.snapshotFlow].
     */
    public fun toList(): List<T>

    override val size: Int

    override fun contains(element: T): Boolean

    override fun containsAll(elements: Collection<T>): Boolean

    override fun get(index: Int): T

    override fun indexOf(element: T): Int

    override fun isEmpty(): Boolean

    override fun iterator(): MutableIterator<T>

    override fun lastIndexOf(element: T): Int

    override fun listIterator(): MutableListIterator<T>

    override fun listIterator(index: Int): MutableListIterator<T>

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T>

    override fun add(element: T): Boolean

    override fun add(index: Int, element: T)

    override fun addAll(elements: Collection<T>): Boolean

    override fun addAll(index: Int, elements: Collection<T>): Boolean

    override fun clear()

    override fun remove(element: T): Boolean

    override fun removeAll(elements: Collection<T>): Boolean

    override fun removeAt(index: Int): T

    override fun retainAll(elements: Collection<T>): Boolean

    override fun set(index: Int, element: T): T

    public fun removeRange(fromIndex: Int, toIndex: Int)

    internal fun retainAllInRange(elements: Collection<T>, start: Int, end: Int): Int
}

internal inline fun <R, T> SnapshotStateList<T>.writable(
    block: StateListStateRecord<T>.() -> R
): R =
    @Suppress("UNCHECKED_CAST") (firstStateRecord as StateListStateRecord<T>).writable(this, block)

internal inline fun <R, T> SnapshotStateList<T>.withCurrent(
    block: StateListStateRecord<T>.() -> R
): R = @Suppress("UNCHECKED_CAST") (firstStateRecord as StateListStateRecord<T>).withCurrent(block)

internal fun <T> SnapshotStateList<T>.mutateBoolean(block: (MutableList<T>) -> Boolean): Boolean =
    mutate(block)

internal inline fun <R, T> SnapshotStateList<T>.mutate(block: (MutableList<T>) -> R): R {
    var result: R
    while (true) {
        var oldList: PersistentList<T>? = null
        var currentModification = 0
        synchronized(sync) {
            val current = withCurrent { this }
            currentModification = current.modification
            oldList = current.list
        }
        val builder = oldList!!.builder()
        result = block(builder)
        val newList = builder.build()
        if (
            newList == oldList ||
                writable { attemptUpdate(currentModification, newList, structural = true) }
        )
            break
    }
    return result
}

internal inline fun <T> SnapshotStateList<T>.update(
    structural: Boolean = true,
    block: (PersistentList<T>) -> PersistentList<T>,
) {
    conditionalUpdate(structural, block)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> SnapshotStateList<T>.clearImpl() {
    writable {
        synchronized(sync) {
            list = persistentListOf()
            modification++
            structuralChange++
        }
    }
}

internal inline fun <T> SnapshotStateList<T>.conditionalUpdate(
    structural: Boolean = true,
    block: (PersistentList<T>) -> PersistentList<T>,
) = run {
    val result: Boolean
    while (true) {
        var oldList: PersistentList<T>? = null
        var currentModification = 0
        synchronized(sync) {
            val current = withCurrent { this }
            currentModification = current.modification
            oldList = current.list
        }
        val newList = block(oldList!!)
        if (newList == oldList) {
            result = false
            break
        }
        if (writable { attemptUpdate(currentModification, newList, structural) }) {
            result = true
            break
        }
    }
    result
}

// NOTE: do not inline this method to avoid class verification failures, see b/369909868
internal fun <T> StateListStateRecord<T>.attemptUpdate(
    currentModification: Int,
    newList: PersistentList<T>,
    structural: Boolean,
): Boolean =
    synchronized(sync) {
        if (modification == currentModification) {
            list = newList
            if (structural) structuralChange++
            modification++
            true
        } else false
    }

internal fun <T> SnapshotStateList<T>.stateRecordWith(list: PersistentList<T>): StateRecord {
    val snapshot = currentSnapshot()
    return StateListStateRecord(snapshot.snapshotId, list).also {
        if (snapshot !is GlobalSnapshot) {
            it.next = StateListStateRecord(Snapshot.PreexistingSnapshotId.toSnapshotId(), list)
        }
    }
}

internal val <T> SnapshotStateList<T>.structure: Int
    get() = withCurrent { structuralChange }

@Suppress("UNCHECKED_CAST")
internal val <T> SnapshotStateList<T>.readable: StateListStateRecord<T>
    get() = (firstStateRecord as StateListStateRecord<T>).readable(this)

/**
 * Creates a new snapshot state list with the specified [size], where each element is calculated by
 * calling the specified [init] function.
 *
 * The function [init] is called for each list element sequentially starting from the first one. It
 * should return the value for a list element given its index.
 */
public fun <T> SnapshotStateList(size: Int, init: (index: Int) -> T): SnapshotStateList<T> {
    if (size == 0) {
        return SnapshotStateList()
    }

    val builder = persistentListOf<T>().builder()
    for (i in 0 until size) {
        builder.add(init(i))
    }
    return SnapshotStateList(builder.build())
}

/** This is an internal implementation class of [SnapshotStateList]. Do not use. */
internal class StateListStateRecord<T>
internal constructor(snapshotId: SnapshotId, internal var list: PersistentList<T>) :
    StateRecord(snapshotId) {
    internal var modification = 0
    internal var structuralChange = 0

    override fun assign(value: StateRecord) {
        synchronized(sync) {
            @Suppress("UNCHECKED_CAST")
            list = (value as StateListStateRecord<T>).list
            modification = value.modification
            structuralChange = value.structuralChange
        }
    }

    override fun create(): StateRecord = create(currentSnapshot().snapshotId)

    override fun create(snapshotId: SnapshotId): StateRecord =
        StateListStateRecord(snapshotId, list)
}

/**
 * This lock is used to ensure that the value of modification and the list in the state record, when
 * used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the list. This avoid additional allocations but introduces some contention
 * between lists. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired last to avoid deadlocks. In other words, the
 * lock must be taken in the `writable` lambda, if `writable` is used.
 */
private val sync = makeSynchronizedObject()

private fun modificationError(): Nothing = error("Cannot modify a state list through an iterator")

private fun validateRange(index: Int, size: Int) {
    if (index !in 0 until size) {
        throw IndexOutOfBoundsException("index ($index) is out of bound of [0, $size)")
    }
}

private fun invalidIteratorSet(): Nothing =
    error(
        "Cannot call set before the first call to next() or previous() " +
            "or immediately after a call to add() or remove()"
    )

internal class StateListIterator<T>(val list: SnapshotStateList<T>, offset: Int) :
    MutableListIterator<T> {
    private var index = offset - 1
    private var lastRequested = -1
    private var structure = list.structure

    override fun hasPrevious() = index >= 0

    override fun nextIndex() = index + 1

    override fun previous(): T {
        validateModification()
        validateRange(index, list.size)
        lastRequested = index
        return list[index].also { index-- }
    }

    override fun previousIndex(): Int = index

    override fun add(element: T) {
        validateModification()
        list.add(index + 1, element)
        lastRequested = -1
        index++
        structure = list.structure
    }

    override fun hasNext() = index < list.size - 1

    override fun next(): T {
        validateModification()
        val newIndex = index + 1
        lastRequested = newIndex
        validateRange(newIndex, list.size)
        return list[newIndex].also { index = newIndex }
    }

    override fun remove() {
        validateModification()
        list.removeAt(lastRequested)
        index--
        lastRequested = -1
        structure = list.structure
    }

    override fun set(element: T) {
        validateModification()
        if (lastRequested < 0) invalidIteratorSet()
        list.set(lastRequested, element)
        structure = list.structure
    }

    private fun validateModification() {
        if (list.structure != structure) {
            throw ConcurrentModificationException()
        }
    }
}

internal class SubList<T>(val parentList: SnapshotStateList<T>, fromIndex: Int, toIndex: Int) :
    MutableList<T> {
    private val offset = fromIndex
    private var structure = parentList.structure
    override var size = toIndex - fromIndex
        private set

    override fun contains(element: T): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): T {
        validateModification()
        validateRange(index, size)
        return parentList[offset + index]
    }

    override fun indexOf(element: T): Int {
        validateModification()
        (offset until offset + size).forEach { if (element == parentList[it]) return it - offset }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun lastIndexOf(element: T): Int {
        validateModification()
        var index = offset + size - 1
        while (index >= offset) {
            if (element == parentList[index]) return index - offset
            index--
        }
        return -1
    }

    override fun add(element: T): Boolean {
        validateModification()
        parentList.add(offset + size, element)
        size++
        structure = parentList.structure
        return true
    }

    override fun add(index: Int, element: T) {
        validateModification()
        parentList.add(offset + index, element)
        size++
        structure = parentList.structure
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateModification()
        val result = parentList.addAll(index + offset, elements)
        if (result) {
            size += elements.size
            structure = parentList.structure
        }
        return result
    }

    override fun addAll(elements: Collection<T>): Boolean = addAll(size, elements)

    override fun clear() {
        if (size > 0) {
            validateModification()
            parentList.removeRange(offset, offset + size)
            size = 0
            structure = parentList.structure
        }
    }

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        validateModification()
        var current = index - 1
        return object : MutableListIterator<T> {
            override fun hasPrevious() = current >= 0

            override fun nextIndex(): Int = current + 1

            override fun previous(): T {
                val oldCurrent = current
                validateRange(oldCurrent, size)
                current = oldCurrent - 1
                return this@SubList[oldCurrent]
            }

            override fun previousIndex(): Int = current

            override fun add(element: T) = modificationError()

            override fun hasNext(): Boolean = current < size - 1

            override fun next(): T {
                val newCurrent = current + 1
                validateRange(newCurrent, size)
                current = newCurrent
                return this@SubList[newCurrent]
            }

            override fun remove() = modificationError()

            override fun set(element: T) = modificationError()
        }
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        return if (index >= 0) {
            removeAt(index)
            true
        } else false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        for (element in elements) {
            removed = remove(element) || removed
        }
        return removed
    }

    override fun removeAt(index: Int): T {
        validateModification()
        return parentList.removeAt(offset + index).also {
            size--
            structure = parentList.structure
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        validateModification()
        val removed = parentList.retainAllInRange(elements, offset, offset + size)
        if (removed > 0) {
            structure = parentList.structure
            size -= removed
        }
        return removed > 0
    }

    override fun set(index: Int, element: T): T {
        validateRange(index, size)
        validateModification()
        val result = parentList.set(index + offset, element)
        structure = parentList.structure
        return result
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        requirePrecondition(fromIndex in 0..toIndex && toIndex <= size) {
            "fromIndex or toIndex are out of bounds"
        }
        validateModification()
        return SubList(parentList, fromIndex + offset, toIndex + offset)
    }

    private fun validateModification() {
        if (parentList.structure != structure) {
            throw ConcurrentModificationException()
        }
    }
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/snapshots/SnapshotStateMap.kt
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Stable
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentHashMapOf
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import kotlin.jvm.JvmName

/**
 * An implementation of [MutableMap] that can be observed and snapshot. This is the result type
 * created by [androidx.compose.runtime.mutableStateMapOf].
 *
 * This class closely implements the same semantics as [HashMap].
 *
 * @see androidx.compose.runtime.mutableStateMapOf
 */
@Stable
public class SnapshotStateMap<K, V> : StateObject, MutableMap<K, V> {
    override var firstStateRecord: StateRecord =
        persistentHashMapOf<K, V>().let { map ->
            val snapshot = currentSnapshot()
            StateMapStateRecord(snapshot.snapshotId, map).also {
                if (snapshot !is GlobalSnapshot) {
                    it.next =
                        StateMapStateRecord(Snapshot.PreexistingSnapshotId.toSnapshotId(), map)
                }
            }
        }
        private set

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        firstStateRecord = value as StateMapStateRecord<K, V>
    }

    /**
     * Returns an immutable map containing all key-value pairs from the original map.
     *
     * The content of the map returned will not change even if the content of the map is changed in
     * the same snapshot. It also will be the same instance until the content is changed. It is not,
     * however, guaranteed to be the same instance for the same content as adding and removing the
     * same item from the this map might produce a different instance with the same content.
     *
     * This operation is O(1) and does not involve a physically copying the map. It instead returns
     * the underlying immutable map used internally to store the content of the map.
     *
     * It is recommended to use [toMap] when using returning the value of this map from
     * [androidx.compose.runtime.snapshotFlow].
     */
    public fun toMap(): Map<K, V> = readable.map

    override val size: Int
        get() = readable.map.size

    override fun containsKey(key: K): Boolean = readable.map.containsKey(key)

    override fun containsValue(value: V): Boolean = readable.map.containsValue(value)

    override fun get(key: K): V? = readable.map[key]

    override fun isEmpty(): Boolean = readable.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = SnapshotMapEntrySet(this)
    override val keys: MutableSet<K> = SnapshotMapKeySet(this)
    override val values: MutableCollection<V> = SnapshotMapValueSet(this)

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String =
        (firstStateRecord as StateMapStateRecord<K, V>).withCurrent {
            "SnapshotStateMap(value=${it.map})@${hashCode()}"
        }

    override fun clear(): Unit = update { persistentHashMapOf() }

    override fun put(key: K, value: V): V? = mutate { it.put(key, value) }

    override fun putAll(from: Map<out K, V>): Unit = mutate { it.putAll(from) }

    override fun remove(key: K): V? = mutate { it.remove(key) }

    internal val modification
        get() = readable.modification

    internal fun removeValue(value: V) =
        entries
            .firstOrNull { it.value == value }
            ?.let {
                remove(it.key)
                true
            } == true

    @Suppress("UNCHECKED_CAST")
    internal val readable: StateMapStateRecord<K, V>
        get() = (firstStateRecord as StateMapStateRecord<K, V>).readable(this)

    internal inline fun removeIf(predicate: (MutableMap.MutableEntry<K, V>) -> Boolean): Boolean {
        var removed = false
        mutate {
            for (entry in this.entries) {
                if (predicate(entry)) {
                    it.remove(entry.key)
                    removed = true
                }
            }
        }
        return removed
    }

    internal inline fun any(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        for (entry in readable.map.entries) {
            if (predicate(entry)) return true
        }
        return false
    }

    internal inline fun all(predicate: (Map.Entry<K, V>) -> Boolean): Boolean {
        for (entry in readable.map.entries) {
            if (!predicate(entry)) return false
        }
        return true
    }

    /**
     * An internal function used by the debugger to display the value of the current value of the
     * mutable state object without triggering read observers.
     */
    @Suppress("unused")
    internal val debuggerDisplayValue: Map<K, V>
        @JvmName("getDebuggerDisplayValue") get() = withCurrent { map }

    private inline fun <R> withCurrent(block: StateMapStateRecord<K, V>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateMapStateRecord<K, V>).withCurrent(block)

    private inline fun <R> writable(block: StateMapStateRecord<K, V>.() -> R): R =
        @Suppress("UNCHECKED_CAST")
        (firstStateRecord as StateMapStateRecord<K, V>).writable(this, block)

    private inline fun <R> mutate(block: (MutableMap<K, V>) -> R): R {
        var result: R
        while (true) {
            var oldMap: PersistentMap<K, V>? = null
            var currentModification = 0
            synchronized(sync) {
                val current = withCurrent { this }
                oldMap = current.map
                currentModification = current.modification
            }
            val builder = oldMap!!.builder()
            result = block(builder)
            val newMap = builder.build()
            if (newMap == oldMap || writable { attemptUpdate(currentModification, newMap) }) break
        }
        return result
    }

    private fun StateMapStateRecord<K, V>.attemptUpdate(
        currentModification: Int,
        newMap: PersistentMap<K, V>,
    ) =
        synchronized(sync) {
            if (modification == currentModification) {
                map = newMap
                modification++
                true
            } else false
        }

    private inline fun update(block: (PersistentMap<K, V>) -> PersistentMap<K, V>) = withCurrent {
        val newMap = block(map)
        if (newMap !== map) writable { commitUpdate(newMap) }
    }

    // NOTE: do not inline this method to avoid class verification failures, see b/369909868
    private fun StateMapStateRecord<K, V>.commitUpdate(newMap: PersistentMap<K, V>) =
        synchronized(sync) {
            map = newMap
            modification++
        }

    /** Implementation class of [SnapshotStateMap]. Do not use. */
    internal class StateMapStateRecord<K, V>
    internal constructor(snapshotId: SnapshotId, internal var map: PersistentMap<K, V>) :
        StateRecord(snapshotId) {
        internal var modification = 0

        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST") val other = (value as StateMapStateRecord<K, V>)
            synchronized(sync) {
                map = other.map
                modification = other.modification
            }
        }

        override fun create(): StateRecord = StateMapStateRecord(currentSnapshot().snapshotId, map)

        override fun create(snapshotId: SnapshotId): StateRecord =
            StateMapStateRecord(snapshotId, map)
    }
}

private abstract class SnapshotMapSet<K, V, E>(val map: SnapshotStateMap<K, V>) : MutableSet<E> {
    override val size: Int
        get() = map.size

    override fun clear() = map.clear()

    override fun isEmpty() = map.isEmpty()
}

private class SnapshotMapEntrySet<K, V>(map: SnapshotStateMap<K, V>) :
    SnapshotMapSet<K, V, MutableMap.MutableEntry<K, V>>(map) {
    override fun add(element: MutableMap.MutableEntry<K, V>) = unsupported()

    override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>) = unsupported()

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> =
        StateMapMutableEntriesIterator(map, map.readable.map.entries.iterator())

    override fun remove(element: MutableMap.MutableEntry<K, V>) = map.remove(element.key) != null

    override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        var removed = false
        for (element in elements) {
            removed = map.remove(element.key) != null || removed
        }
        return removed
    }

    override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        val entries = elements.associate { it.key to it.value }
        return map.removeIf { !entries.containsKey(it.key) || entries[it.key] != it.value }
    }

    override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
        return map[element.key] == element.value
    }

    override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
        return elements.all { contains(it) }
    }
}

private class SnapshotMapKeySet<K, V>(map: SnapshotStateMap<K, V>) : SnapshotMapSet<K, V, K>(map) {
    override fun add(element: K) = unsupported()

    override fun addAll(elements: Collection<K>) = unsupported()

    override fun iterator() = StateMapMutableKeysIterator(map, map.readable.map.entries.iterator())

    override fun remove(element: K): Boolean = map.remove(element) != null

    override fun removeAll(elements: Collection<K>): Boolean {
        var removed = false
        elements.forEach { removed = map.remove(it) != null || removed }
        return removed
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.key !in set }
    }

    override fun contains(element: K) = map.contains(element)

    override fun containsAll(elements: Collection<K>): Boolean = elements.all { map.contains(it) }
}

private class SnapshotMapValueSet<K, V>(map: SnapshotStateMap<K, V>) :
    SnapshotMapSet<K, V, V>(map) {
    override fun add(element: V) = unsupported()

    override fun addAll(elements: Collection<V>) = unsupported()

    override fun iterator() =
        StateMapMutableValuesIterator(map, map.readable.map.entries.iterator())

    override fun remove(element: V): Boolean = map.removeValue(element)

    override fun removeAll(elements: Collection<V>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.value in set }
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        val set = elements.toSet()
        return map.removeIf { it.value !in set }
    }

    override fun contains(element: V) = map.containsValue(element)

    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { map.containsValue(it) }
    }
}

/**
 * This lock is used to ensure that the value of modification and the map in the state record, when
 * used together, are atomically read and written.
 *
 * A global sync object is used to avoid having to allocate a sync object and initialize a monitor
 * for each instance the map. This avoids additional allocations but introduces some contention
 * between maps. As there is already contention on the global snapshot lock to write so the
 * additional contention introduced by this lock is nominal.
 *
 * In code the requires this lock and calls `writable` (or other operation that acquires the
 * snapshot global lock), this lock *MUST* be acquired last to avoid deadlocks. In other words, the
 * lock must be taken in the `writable` lambda, if `writable` is used.
 */
private val sync = makeSynchronizedObject()

private abstract class StateMapMutableIterator<K, V>(
    val map: SnapshotStateMap<K, V>,
    val iterator: Iterator<Map.Entry<K, V>>,
) {
    protected var modification = map.modification
    protected var current: Map.Entry<K, V>? = null
    protected var next: Map.Entry<K, V>? = null

    init {
        advance()
    }

    fun remove() = modify {
        val value = current

        if (value != null) {
            map.remove(value.key)
            current = null
        } else {
            throw IllegalStateException()
        }
    }

    fun hasNext() = next != null

    protected fun advance() {
        current = next
        next = if (iterator.hasNext()) iterator.next() else null
    }

    protected inline fun <T> modify(block: () -> T): T {
        if (map.modification != modification) {
            throw ConcurrentModificationException()
        }
        return block().also { modification = map.modification }
    }
}

private class StateMapMutableEntriesIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>,
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<MutableMap.MutableEntry<K, V>> {
    override fun next(): MutableMap.MutableEntry<K, V> {
        advance()
        if (current != null) {
            return object : MutableMap.MutableEntry<K, V> {
                override val key = current!!.key
                override var value = current!!.value

                override fun setValue(newValue: V): V = modify {
                    val result = value
                    map[key] = newValue
                    value = newValue
                    return result
                }
            }
        } else {
            throw IllegalStateException()
        }
    }
}

private class StateMapMutableKeysIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>,
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<K> {
    override fun next(): K {
        val result = next ?: throw IllegalStateException()
        advance()
        return result.key
    }
}

private class StateMapMutableValuesIterator<K, V>(
    map: SnapshotStateMap<K, V>,
    iterator: Iterator<Map.Entry<K, V>>,
) : StateMapMutableIterator<K, V>(map, iterator), MutableIterator<V> {
    override fun next(): V {
        val result = next ?: throw IllegalStateException()
        advance()
        return result.value
    }
}

internal fun unsupported(): Nothing {
    throw UnsupportedOperationException()
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/ComposeVersion.kt
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

package androidx.compose.runtime

@Suppress("unused")
internal object ComposeVersion {
    /**
     * This version number is used by the compose compiler in order to verify that the compiler and
     * the runtime are compatible with one another.
     *
     * Every release should increase this number to a multiple of 100, which provides for the
     * opportunity to use the last two digits for releases made out-of-band.
     *
     * IMPORTANT: Whenever updating this value, please make sure to also update `versionTable` and
     * `minimumRuntimeVersionInt` in `VersionChecker.kt` of the compiler.
     */
    const val version: Int = 13000
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Applier.kt
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

package androidx.compose.runtime

import androidx.compose.runtime.internal.JvmDefaultWithCompatibility

/**
 * An Applier is responsible for applying the tree-based operations that get emitted during a
 * composition. Every [Composer] has an [Applier] which it uses to emit a [ComposeNode].
 *
 * A custom [Applier] implementation will be needed in order to utilize Compose to build and
 * maintain a tree of a novel type.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @see AbstractApplier
 * @see Composition
 * @see Composer
 * @see ComposeNode
 */
@JvmDefaultWithCompatibility
public interface Applier<N> {
    /**
     * The node that operations will be applied on at any given time. It is expected that the value
     * of this property will change as [down] and [up] are called.
     */
    public val current: N

    /**
     * Called when the [Composer] is about to begin applying changes using this applier.
     * [onEndChanges] will be called when changes are complete.
     */
    public fun onBeginChanges() {}

    /**
     * Called when the [Composer] is finished applying changes using this applier. A call to
     * [onBeginChanges] will always precede a call to [onEndChanges].
     */
    public fun onEndChanges() {}

    /**
     * Indicates that the applier is getting traversed "down" the tree. When this gets called,
     * [node] is expected to be a child of [current], and after this operation, [node] is expected
     * to be the new [current].
     */
    public fun down(node: N)

    /**
     * Indicates that the applier is getting traversed "up" the tree. After this operation
     * completes, the [current] should return the "parent" of the [current] node at the beginning of
     * this operation.
     */
    public fun up()

    /**
     * Indicates that [instance] should be inserted as a child to [current] at [index]. An applier
     * should insert the node into the tree either in [insertTopDown] or [insertBottomUp], not both.
     *
     * The [insertTopDown] method is called before the children of [instance] have been created and
     * inserted into it. [insertBottomUp] is called after all children have been created and
     * inserted.
     *
     * Some trees are faster to build top-down, in which case the [insertTopDown] method should be
     * used to insert the [instance]. Other trees are faster to build bottom-up in which case
     * [insertBottomUp] should be used.
     *
     * To give example of building a tree top-down vs. bottom-up consider the following tree,
     * ```
     *      R
     *      |
     *      B
     *     / \
     *    A   C
     *  ```
     *
     * where the node `B` is being inserted into the tree at `R`. Top-down building of the tree
     * first inserts `B` into `R`, then inserts `A` into `B` followed by inserting `C` into B`. For
     * example,
     *
     *  ```
     *      1           2           3
     *      R           R           R
     *      |           |           |
     *      B           B           B
     *                 /           / \
     *                A           A   C
     * ```
     *
     * A bottom-up building of the tree starts with inserting `A` and `C` into `B` then inserts `B`
     * tree into `R`.
     *
     * ```
     *    1           2           3
     *    B           B           R
     *    |          / \          |
     *    A         A   C         B
     *                           / \
     *                          A   C
     * ```
     *
     * To see how building top-down vs. bottom-up can differ significantly in performance consider a
     * tree where whenever a child is added to the tree all parent nodes, up to the root, are
     * notified of the new child entering the tree. If the tree is built top-down,
     * 1. `R` is notified of `B` entering.
     * 2. `B` is notified of `A` entering, `R` is notified of `A` entering.
     * 3. `B` is notified of `C` entering, `R` is notified of `C` entering.
     *
     * for a total of 5 notifications. The number of notifications grows exponentially with the
     * number of inserts.
     *
     * For bottom-up, the notifications are,
     * 1. `B` is notified `A` entering.
     * 2. `B` is notified `C` entering.
     * 3. `R` is notified `B` entering.
     *
     * The notifications are linear to the number of nodes inserted.
     *
     * If, on the other hand, all children are notified when the parent enters a tree, then the
     * notifications are, for top-down,
     * 1. `B` is notified it is entering `R`.
     * 2. `A` is notified it is entering `B`.
     * 3. `C` is notified it is entering `B`.
     *
     * which is linear to the number of nodes inserted.
     *
     * For bottom-up, the notifications look like,
     * 1. `A` is notified it is entering `B`.
     * 2. `C` is notified it is entering `B`.
     * 3. `B` is notified it is entering `R`, `A` is notified it is entering `R`, `C` is notified it
     *    is entering `R`.
     *
     *    which exponential to the number of nodes inserted.
     */
    public fun insertTopDown(index: Int, instance: N)

    /**
     * Indicates that [instance] should be inserted as a child of [current] at [index]. An applier
     * should insert the node into the tree either in [insertTopDown] or [insertBottomUp], not both.
     * See the description of [insertTopDown] to which describes when to implement [insertTopDown]
     * and when to use [insertBottomUp].
     */
    public fun insertBottomUp(index: Int, instance: N)

    /**
     * Indicates that the children of [current] from [index] to [index] + [count] should be removed.
     */
    public fun remove(index: Int, count: Int)

    /**
     * Indicates that [count] children of [current] should be moved from index [from] to index [to].
     *
     * The [to] index is relative to the position before the change, so, for example, to move an
     * element at position 1 to after the element at position 2, [from] should be `1` and [to]
     * should be `3`. If the elements were A B C D E, calling `move(1, 3, 1)` would result in the
     * elements being reordered to A C B D E.
     */
    public fun move(from: Int, to: Int, count: Int)

    /**
     * Move to the root and remove all nodes from the root, preparing both this [Applier] and its
     * root to be used as the target of a new composition in the future.
     */
    public fun clear()

    /** Apply a change to the current node. */
    public fun apply(block: N.(Any?) -> Unit, value: Any?) {
        current.block(value)
    }

    /** Notify [current] is is being reused in reusable content. */
    public fun reuse() {
        (current as? ComposeNodeLifecycleCallback)?.onReuse()
    }
}

/**
 * An abstract [Applier] implementation.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @see Applier
 * @see Composition
 * @see Composer
 * @see ComposeNode
 */
public abstract class AbstractApplier<T>(public val root: T) : Applier<T> {
    private val stack = Stack<T>()

    override var current: T = root
        protected set

    override fun down(node: T) {
        stack.push(current)
        current = node
    }

    override fun up() {
        current = stack.pop()
    }

    final override fun clear() {
        stack.clear()
        current = root
        onClear()
    }

    /** Called to perform clearing of the [root] when [clear] is called. */
    protected abstract fun onClear()

    protected fun MutableList<T>.remove(index: Int, count: Int) {
        if (count == 1) {
            removeAt(index)
        } else {
            subList(index, index + count).clear()
        }
    }

    protected fun MutableList<T>.move(from: Int, to: Int, count: Int) {
        val dest = if (from > to) to else to - count
        if (count == 1) {
            if (from == to + 1 || from == to - 1) {
                // Adjacent elements, perform swap to avoid backing array manipulations.
                val fromEl = get(from)
                val toEl = set(to, fromEl)
                set(from, toEl)
            } else {
                val fromEl = removeAt(from)
                add(dest, fromEl)
            }
        } else {
            val subView = subList(from, from + count)
            val subCopy = subView.toMutableList()
            subView.clear()
            addAll(dest, subCopy)
        }
    }
}

internal class OffsetApplier<N>(private val applier: Applier<N>, private val offset: Int) :
    Applier<N> {
    private var nesting = 0
    override val current: N
        get() = applier.current

    override fun down(node: N) {
        nesting++
        applier.down(node)
    }

    override fun up() {
        runtimeCheck(nesting > 0) { "OffsetApplier up called with no corresponding down" }
        nesting--
        applier.up()
    }

    override fun insertTopDown(index: Int, instance: N) {
        applier.insertTopDown(index + if (nesting == 0) offset else 0, instance)
    }

    override fun insertBottomUp(index: Int, instance: N) {
        applier.insertBottomUp(index + if (nesting == 0) offset else 0, instance)
    }

    override fun remove(index: Int, count: Int) {
        applier.remove(index + if (nesting == 0) offset else 0, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        val effectiveOffset = if (nesting == 0) offset else 0
        applier.move(from + effectiveOffset, to + effectiveOffset, count)
    }

    override fun clear() {
        composeImmediateRuntimeError("Clear is not valid on OffsetApplier")
    }

    override fun apply(block: N.(Any?) -> Unit, value: Any?) {
        applier.apply(block, value)
    }

    override fun reuse() {
        applier.reuse()
    }
}

/**
 * A stub of [Applier] that does not implement any operations and throws when called into. Used to
 * apply pending changes that do not result in a change to the composition hierarchy and therefore
 * do not need a real application phase after completing the composition.
 */
internal object ThrowingApplierStub : Applier<Any?> {
    override val current: Any
        get() = throwIllegalOperationException()

    override fun up() = throwIllegalOperationException()

    override fun remove(index: Int, count: Int) = throwIllegalOperationException()

    override fun move(from: Int, to: Int, count: Int) = throwIllegalOperationException()

    override fun clear() = throwIllegalOperationException()

    override fun insertBottomUp(index: Int, instance: Any?) = throwIllegalOperationException()

    override fun insertTopDown(index: Int, instance: Any?) = throwIllegalOperationException()

    override fun down(node: Any?) = throwIllegalOperationException()

    private fun throwIllegalOperationException() {
        composeImmediateRuntimeError(
            "ChangeList cannot call the Applier when " +
                "executing pending changes outside of the applier phase."
        )
    }
}
```

## File: compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/RecomposeScopeImpl.kt
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

package androidx.compose.runtime

import androidx.collection.MutableObjectIntMap
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterSet
import androidx.compose.runtime.composer.gapbuffer.GapAnchor
import androidx.compose.runtime.composer.gapbuffer.SlotTable
import androidx.compose.runtime.composer.gapbuffer.SlotWriter
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope

/**
 * Represents a recomposable scope or section of the composition hierarchy. Can be used to manually
 * invalidate the scope to schedule it for recomposition.
 */
public interface RecomposeScope {
    /**
     * Invalidate the corresponding scope, requesting the composer recompose this scope.
     *
     * This method is thread safe.
     */
    public fun invalidate()
}

private const val changedLowBitMask = 0b001_001_001_001_001_001_001_001_001_001_0
private const val changedHighBitMask = changedLowBitMask shl 1
private const val changedMask = (changedLowBitMask or changedHighBitMask).inv()

/**
 * A compiler plugin utility function to change $changed flags from Different(10) to Same(01) for
 * when captured by restart lambdas. All parameters are passed with the same value as it was
 * previously invoked with and the changed flags should reflect that.
 */
@PublishedApi
internal fun updateChangedFlags(flags: Int): Int {
    val lowBits = flags and changedLowBitMask
    val highBits = flags and changedHighBitMask
    return ((flags and changedMask) or
        (lowBits or (highBits shr 1)) or
        ((lowBits shl 1) and highBits))
}

private const val UsedFlag = 0x001
private const val DefaultsInScopeFlag = 0x002
private const val DefaultsInvalidFlag = 0x004
private const val RequiresRecomposeFlag = 0x008
private const val SkippedFlag = 0x010
private const val RereadingFlag = 0x020
private const val ForcedRecomposeFlag = 0x040
private const val ForceReusing = 0x080
private const val Paused = 0x100
private const val Resuming = 0x200
private const val ResetReusing = 0x400

internal interface RecomposeScopeOwner {
    fun invalidate(scope: RecomposeScopeImpl, instance: Any?): InvalidationResult

    fun recomposeScopeReleased(scope: RecomposeScopeImpl)

    fun recordReadOf(value: Any)
}

/**
 * A RecomposeScope is created for a region of the composition that can be recomposed independently
 * of the rest of the composition. The composer will position the slot table to the location stored
 * in [anchor] and call [block] when recomposition is requested. It is created by
 * [Composer.startRestartGroup] and is used to track how to restart the group.
 */
@OptIn(ComposeToolingApi::class)
internal class RecomposeScopeImpl(internal var owner: RecomposeScopeOwner?) :
    ScopeUpdateScope, RecomposeScope, IdentifiableRecomposeScope {

    /** The backing store for the boolean flags tracked by the recompose scope. */
    private var flags: Int = 0

    /**
     * An anchor to the location in the slot table that start the group associated with this
     * recompose scope.
     */
    var anchor: Anchor? = null

    /** Access to anchor from tooling */
    @ComposeToolingApi
    override val identity: Any?
        get() = anchor

    /**
     * Return whether the scope is valid. A scope becomes invalid when the slots it updates are
     * removed from the slot table. For example, if the scope is in the then clause of an if
     * statement that later becomes false.
     */
    val valid: Boolean
        get() = owner != null && anchor?.valid ?: false

    val canRecompose: Boolean
        get() = block != null

    /**
     * Used is set when the [RecomposeScopeImpl] is used by, for example, [currentRecomposeScope].
     * This is used as the result of [Composer.endRestartGroup] and indicates whether the lambda
     * that is stored in [block] will be used.
     */
    var used: Boolean
        get() = getFlag(UsedFlag)
        set(value) {
            setFlag(UsedFlag, value)
        }

    /**
     * Used to force a scope to the reusing state when a composition is paused while reusing
     * content.
     */
    var reusing: Boolean
        get() = getFlag(ForceReusing)
        set(value) {
            setFlag(ForceReusing, value)
        }

    /**
     * Used to restore the reusing state after unpausing a composition that was paused in a reusing
     * state.
     */
    var resetReusing: Boolean
        get() = getFlag(ResetReusing)
        set(value) {
            setFlag(ResetReusing, value)
        }

    /** Used to flag a scope as paused for pausable compositions */
    var paused: Boolean
        get() = getFlag(Paused)
        set(value) {
            setFlag(Paused, value)
        }

    /** Used to flag a scope as paused for pausable compositions */
    var resuming: Boolean
        get() = getFlag(Resuming)
        set(value) {
            setFlag(Resuming, value)
        }

    /**
     * Set to true when the there are function default calculations in the scope. These are treated
     * as a special case to avoid having to create a special scope for them. If these change the
     * this scope needs to be recomposed but the default values can be skipped if they where not
     * invalidated.
     */
    var defaultsInScope: Boolean
        get() = getFlag(DefaultsInScopeFlag)
        set(value) {
            setFlag(DefaultsInScopeFlag, value)
        }

    /**
     * Tracks whether any of the calculations in the default values were changed. See
     * [defaultsInScope] for details.
     */
    var defaultsInvalid: Boolean
        get() = getFlag(DefaultsInvalidFlag)
        set(value) {
            setFlag(DefaultsInvalidFlag, value)
        }

    /**
     * Tracks whether the scope was invalidated directly but was recomposed because the caller was
     * recomposed. This ensures that a scope invalidated directly will recompose even if its
     * parameters are the same as the previous recomposition.
     */
    var requiresRecompose: Boolean
        get() = getFlag(RequiresRecomposeFlag)
        set(value) {
            setFlag(RequiresRecomposeFlag, value)
        }

    /** The lambda to call to restart the scopes composition. */
    private var block: ((Composer, Int) -> Unit)? = null

    /**
     * Restart the scope's composition. It is an error if [block] was not updated. The code
     * generated by the compiler ensures that when the recompose scope is used then [block] will be
     * set but it might occur if the compiler is out-of-date (or ahead of the runtime) or incorrect
     * direct calls to [Composer.startRestartGroup] and [Composer.endRestartGroup].
     */
    fun compose(composer: Composer) {
        block?.invoke(composer, 1) ?: error("Invalid restart scope")
    }

    /**
     * Invalidate the group which will cause [owner] to request this scope be recomposed, and an
     * [InvalidationResult] will be returned.
     */
    fun invalidateForResult(value: Any?): InvalidationResult =
        owner?.invalidate(this, value) ?: InvalidationResult.IGNORED

    /**
     * Release the recompose scope. This is called when the recompose scope has been removed by the
     * compostion because the part of the composition it was tracking was removed.
     */
    fun release() {
        owner?.recomposeScopeReleased(this)
        owner = null
        trackedInstances = null
        trackedDependencies = null
        block = null
    }

    /**
     * Called when the data tracked by this recompose scope moves to a different composition when
     * for example, the movable content it is part of has moved.
     */
    fun adoptedBy(owner: RecomposeScopeOwner) {
        this.owner = owner
    }

    /**
     * Invalidate the group which will cause [owner] to request this scope be recomposed.
     *
     * Unlike [invalidateForResult], this method is thread safe and calls the thread safe invalidate
     * on the composer.
     */
    override fun invalidate() {
        owner?.invalidate(this, null)
    }

    /**
     * Update [block]. The scope is returned by [Composer.endRestartGroup] when [used] is true and
     * implements [ScopeUpdateScope].
     */
    override fun updateScope(block: (Composer, Int) -> Unit) {
        this.block = block
    }

    private var currentToken = 0
    private var trackedInstances: MutableObjectIntMap<Any>? = null
    private var trackedDependencies: MutableScatterMap<DerivedState<*>, Any?>? = null
    private var rereading: Boolean
        get() = getFlag(RereadingFlag)
        set(value) {
            setFlag(RereadingFlag, value)
        }

    /**
     * Used to explicitly force recomposition. This is used during live edit to force a recompose
     * scope that doesn't have a restart callback to recompose as its parent (or some parent above
     * it) was invalidated and the path to this scope has also been forced.
     */
    var forcedRecompose: Boolean
        get() = getFlag(ForcedRecomposeFlag)
        set(value) {
            setFlag(ForcedRecomposeFlag, value)
        }

    /** Indicates whether the scope was skipped (e.g. [scopeSkipped] was called. */
    internal var skipped: Boolean
        get() = getFlag(SkippedFlag)
        private set(value) {
            setFlag(SkippedFlag, value)
        }

    /**
     * Called when composition start composing into this scope. The [token] is a value that is
     * unique everytime this is called. This is currently the snapshot id but that shouldn't be
     * relied on.
     */
    fun start(token: Int) {
        currentToken = token
        skipped = false
    }

    fun scopeSkipped() {
        if (!reusing) {
            skipped = true
        }
    }

    /**
     * Track instances that were read in scope.
     *
     * @return whether the value was already read in scope during current pass
     */
    fun recordRead(instance: Any): Boolean {
        if (rereading) return false // Re-reading should force composition to update its tracking

        val trackedInstances =
            trackedInstances ?: MutableObjectIntMap<Any>().also { trackedInstances = it }

        val token = trackedInstances.put(instance, currentToken, default = -1)
        if (token == currentToken) {
            return true
        }

        return false
    }

    fun recordDerivedStateValue(instance: DerivedState<*>, value: Any?) {
        val trackedDependencies =
            trackedDependencies
                ?: MutableScatterMap<DerivedState<*>, Any?>().also { trackedDependencies = it }

        trackedDependencies[instance] = value
    }

    /**
     * Returns true if the scope is observing derived state which might make this scope
     * conditionally invalidated.
     */
    val isConditional: Boolean
        get() = trackedDependencies != null

    /**
     * Determine if the scope should be considered invalid.
     *
     * @param instances The set of objects reported as invalidating this scope.
     */
    fun isInvalidFor(instances: Any? /* State | ScatterSet<State> | null */): Boolean {
        // If a non-empty instances exists and contains only derived state objects with their
        // default values, then the scope should not be considered invalid. Otherwise the scope
        // should if it was invalidated by any other kind of instance.
        if (instances == null) return true
        val trackedDependencies = trackedDependencies ?: return true

        return when (instances) {
            is DerivedState<*> -> {
                instances.checkDerivedStateChanged(trackedDependencies)
            }
            is ScatterSet<*> -> {
                instances.isNotEmpty() &&
                    instances.any {
                        it !is DerivedState<*> || it.checkDerivedStateChanged(trackedDependencies)
                    }
            }
            else -> true
        }
    }

    private fun DerivedState<*>.checkDerivedStateChanged(
        dependencies: MutableScatterMap<DerivedState<*>, Any?>
    ): Boolean {
        @Suppress("UNCHECKED_CAST")
        this as DerivedState<Any?>
        val policy = policy ?: structuralEqualityPolicy()
        return !policy.equivalent(currentRecord.currentValue, dependencies[this])
    }

    fun rereadTrackedInstances() {
        owner?.let { owner ->
            trackedInstances?.let { trackedInstances ->
                rereading = true
                try {
                    trackedInstances.forEach { value, _ -> owner.recordReadOf(value) }
                } finally {
                    rereading = false
                }
            }
        }
    }

    /**
     * Called when composition is completed for this scope. The [token] is the same token passed in
     * the previous call to [start]. If [end] returns a non-null value the lambda returned will be
     * called during [ControlledComposition.applyChanges].
     */
    fun end(token: Int): ((Composition) -> Unit)? {
        return trackedInstances?.let { instances ->
            // If any value previous observed was not read in this current composition
            // schedule the value to be removed from the observe scope and removed from the
            // observations tracked by the composition.
            // [skipped] is true if the scope was skipped. If the scope was skipped we should
            // leave the observations unmodified.
            if (!skipped && instances.any { _, instanceToken -> instanceToken != token })
                { composition ->
                    if (
                        currentToken == token &&
                            instances == trackedInstances &&
                            composition is CompositionImpl
                    ) {
                        instances.removeIf { instance, instanceToken ->
                            val shouldRemove = instanceToken != token
                            if (shouldRemove) {
                                composition.removeObservation(instance, this)
                                if (instance is DerivedState<*>) {
                                    composition.removeDerivedStateObservation(instance)
                                    trackedDependencies?.remove(instance)
                                }
                            }
                            shouldRemove
                        }
                    }
                }
            else null
        }
    }

    @Suppress("NOTHING_TO_INLINE") private inline fun getFlag(flag: Int) = flags and flag != 0

    @Suppress("NOTHING_TO_INLINE")
    private inline fun setFlag(flag: Int, value: Boolean) {
        val existingFlags = flags
        flags =
            if (value) {
                existingFlags or flag
            } else {
                existingFlags and flag.inv()
            }
    }

    companion object {
        internal fun adoptAnchoredScopes(
            slots: SlotWriter,
            anchors: List<GapAnchor>,
            newOwner: RecomposeScopeOwner,
        ) {
            if (anchors.isNotEmpty()) {
                anchors.fastForEach { anchor ->
                    // The recompose scope is always at slot 0 of a restart group.
                    val recomposeScope = slots.slot(anchor, 0) as? RecomposeScopeImpl
                    // Check for null as the anchor might not be for a recompose scope
                    recomposeScope?.adoptedBy(newOwner)
                }
            }
        }

        internal fun hasAnchoredRecomposeScopes(slots: SlotTable, anchors: List<GapAnchor>) =
            anchors.isNotEmpty() &&
                anchors.fastAny {
                    slots.ownsAnchor(it) &&
                        slots.slot(slots.anchorIndex(it), 0) is RecomposeScopeImpl
                }
    }
}
```

