package com.xayah.core.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xayah.core.ui.material3.window.LocalPopupTestTag
import com.xayah.core.ui.material3.window.PopupLayoutHelper
import com.xayah.core.ui.material3.window.PopupLayoutHelperImpl
import com.xayah.core.ui.material3.window.PopupLayoutHelperImpl29
import com.xayah.core.ui.material3.window.PopupProperties
import com.xayah.core.ui.material3.window.SecureFlagPolicy
import com.xayah.core.ui.material3.window.SimpleStack
import com.xayah.core.ui.material3.window.isFlagSecureEnabled
import com.xayah.core.ui.material3.window.shouldApplySecureFlag
import com.xayah.core.ui.material3.window.toIntBounds
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
internal class ModalPopupLayout(
    private var onDismissRequest: (() -> Unit)?,
    private var properties: PopupProperties,
    var testTag: String,
    private val composeView: View,
    density: Density,
    initialPositionProvider: PopupPositionProvider,
    popupId: UUID,
    private val popupLayoutHelper: PopupLayoutHelper = if (Build.VERSION.SDK_INT >= 29) {
        PopupLayoutHelperImpl29()
    } else {
        PopupLayoutHelperImpl()
    },
) : AbstractComposeView(composeView.context),
    ViewRootForInspector {
    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @VisibleForTesting
    internal val params = createLayoutParams()

    /** The logic of positioning the popup relative to its parent. */
    var positionProvider = initialPositionProvider

    // Position params
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    var popupContentSize: IntSize? by mutableStateOf(null)
    private var parentLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)
    private var parentBounds: IntRect? = null

    /** Track parent coordinates and content size; only show popup once we have both. */
    val canCalculatePosition by derivedStateOf {
        parentLayoutCoordinates != null && popupContentSize != null
    }

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    // The window visible frame used for the last popup position calculation.
    private val previousWindowVisibleFrame = Rect()

    override val subCompositionView: AbstractComposeView get() = this

    init {
        id = android.R.id.content
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(R.id.compose_view_saveable_id_tag, "Popup:$popupId")

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
        // Allocate space for elevation
        with(density) { elevation = maxSupportedElevation.toPx() }
        // Simple outline to force window manager to allocate space for shadow.
        // Note that the outline affects clickable area for the dismiss listener. In case of shapes
        // like circle the area for dismiss might be to small (rectangular outline consuming clicks
        // outside of the circle).
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, result: Outline) {
                result.setRect(0, 0, view.width, view.height)
                // We set alpha to 0 to hide the view's shadow and let the composable to draw its
                // own shadow. This still enables us to get the extra space needed in the surface.
                result.alpha = 0f
            }
        }
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    private val scrim: View = View(context).apply {
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        setBackgroundResource(android.R.color.transparent)
    }

    private val scrimParams = WindowManager.LayoutParams().apply {
        // Flags specific to modal popup.
        flags = flags and (WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM).inv()
        flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_DIM_BEHIND

        type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL

        // Get the Window token from the parent view
        token = composeView.applicationWindowToken

        // Match the whole window size.
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT

        format = PixelFormat.TRANSLUCENT

        // Hardcode the dim value.
        dimAmount = 0.2f

        // Set the dim animation.
        windowAnimations = androidx.appcompat.R.style.Animation_AppCompat_Dialog


        // accessibilityTitle is not exposed as a public API therefore we set popup window
        // title which is used as a fallback by a11y services
        title = composeView.context.resources.getString(androidx.compose.ui.R.string.default_popup_window_title)
    }

    fun show() {
        windowManager.addView(scrim, scrimParams)
        windowManager.addView(this, params)
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    @Composable
    @UiComposable
    override fun Content() {
        content()
    }

//    override fun internalOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        if (properties.usePlatformDefaultWidth) {
//            super.internalOnMeasure(widthMeasureSpec, heightMeasureSpec)
//        } else {
//            // usePlatformDefaultWidth false, so don't want to limit the popup width to the Android
//            // platform default. Therefore, we create a new measure spec for width, which
//            // corresponds to the full screen width. We do the same for height, even if
//            // ViewRootImpl gives it to us from the first measure.
//            val displayWidthMeasureSpec = makeMeasureSpec(displayWidth, MeasureSpec.AT_MOST)
//            val displayHeightMeasureSpec = makeMeasureSpec(displayHeight, MeasureSpec.AT_MOST)
//            super.internalOnMeasure(displayWidthMeasureSpec, displayHeightMeasureSpec)
//        }
//    }

//    override fun internalOnLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
//        super.internalOnLayout(changed, left, top, right, bottom)
//        // Now set the content size as fixed layout params, such that ViewRootImpl knows
//        // the exact window size.
//        if (!properties.usePlatformDefaultWidth) {
//            val child = getChildAt(0) ?: return
//            params.width = child.measuredWidth
//            params.height = child.measuredHeight
//            popupLayoutHelper.updateViewLayout(windowManager, this, params)
//        }
//    }

    private val displayWidth: Int
        get() {
            val density = context.resources.displayMetrics.density
            return (context.resources.configuration.screenWidthDp * density).roundToInt()
        }

    private val displayHeight: Int
        get() {
            val density = context.resources.displayMetrics.density
            return (context.resources.configuration.screenHeightDp * density).roundToInt()
        }

    /**
     * Taken from PopupWindow
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && properties.dismissOnBackPress) {
            if (keyDispatcherState == null) {
                return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = keyDispatcherState
                state?.startTracking(event, this)
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = keyDispatcherState
                if (state != null && state.isTracking(event) && !event.isCanceled) {
                    onDismissRequest?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Set whether the popup can grab a focus and support dismissal.
     */
    private fun setIsFocusable(isFocusable: Boolean) = applyNewFlags(
        if (!isFocusable) {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.flags and (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv())
        }
    )

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        applyNewFlags(
            if (secureFlagEnabled) {
                params.flags or WindowManager.LayoutParams.FLAG_SECURE
            } else {
                params.flags and (WindowManager.LayoutParams.FLAG_SECURE.inv())
            }
        )
    }

    private fun setClippingEnabled(clippingEnabled: Boolean) = applyNewFlags(
        if (clippingEnabled) {
            params.flags and (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv())
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
    )

    fun updateParameters(
        onDismissRequest: (() -> Unit)?,
        properties: PopupProperties,
        testTag: String,
        layoutDirection: LayoutDirection,
    ) {
        this.onDismissRequest = onDismissRequest
        if (properties.usePlatformDefaultWidth && !this.properties.usePlatformDefaultWidth) {
            // Undo fixed size in internalOnLayout, which would suppress size changes when
            // usePlatformDefaultWidth is true.
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            popupLayoutHelper.updateViewLayout(windowManager, this, params)
        }
        this.properties = properties
        this.testTag = testTag
        setIsFocusable(properties.focusable)
        setSecurePolicy(properties.securePolicy)
        setClippingEnabled(properties.clippingEnabled)
        superSetLayoutDirection(layoutDirection)
    }

    private fun applyNewFlags(flags: Int) {
        params.flags = flags
        popupLayoutHelper.updateViewLayout(windowManager, this, params)
    }

    /**
     * Updates the [LayoutCoordinates] object that is used by [updateParentBounds] to calculate
     * the position of the popup. If the new [LayoutCoordinates] reports new parent bounds, calls
     * [updatePosition].
     */
    fun updateParentLayoutCoordinates(parentLayoutCoordinates: LayoutCoordinates) {
        this.parentLayoutCoordinates = parentLayoutCoordinates
        updateParentBounds()
    }

    /**
     * Used by [pollForLocationOnScreenChange] to read the [composeView]'s absolute position
     * on screen. The array is stored as a field instead of allocated in the method because it's
     * called on every frame.
     */
    private val locationOnScreen = IntArray(2)

    /**
     * Returns true if the absolute location of the [composeView] on the screen has changed since
     * the last call. This method asks the view for its location instead of using Compose APIs like
     * [LayoutCoordinates] because it does less work, and this method is intended to be called on
     * every frame.
     *
     * The location can change without any callbacks being fired if, for example, the soft keyboard
     * is shown or hidden when the window is in `adjustPan` mode. In that case, the window's root
     * view (`ViewRootImpl`) will "scroll" the view hierarchy in a special way that doesn't fire any
     * callbacks.
     */
    fun pollForLocationOnScreenChange() {
        val (oldX, oldY) = locationOnScreen
        composeView.getLocationOnScreen(locationOnScreen)
        if (oldX != locationOnScreen[0] || oldY != locationOnScreen[1]) {
            updateParentBounds()
        }
    }

    /**
     * Re-calculates the bounds of the parent layout node that this popup is anchored to. If they've
     * changed since the last call, calls [updatePosition] to actually calculate the popup's new
     * position and update the window.
     */
    @VisibleForTesting
    internal fun updateParentBounds() {
        val coordinates = parentLayoutCoordinates ?: return
        val layoutSize = coordinates.size

        val position = coordinates.positionInWindow()
        val layoutPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())

        val newParentBounds = IntRect(layoutPosition, layoutSize)
        if (newParentBounds != parentBounds) {
            this.parentBounds = newParentBounds
            updatePosition()
        }
    }

    /**
     * Updates the position of the popup based on current position properties.
     */
    fun updatePosition() {
        val parentBounds = parentBounds ?: return
        val popupContentSize = popupContentSize ?: return

        val windowSize = previousWindowVisibleFrame.let {
            popupLayoutHelper.getWindowVisibleDisplayFrame(composeView, it)
            val bounds = it.toIntBounds()
            IntSize(width = bounds.width, height = bounds.height)
        }

        val popupPosition = positionProvider.calculatePosition(
            parentBounds,
            windowSize,
            parentLayoutDirection,
            popupContentSize
        )

        params.x = popupPosition.x
        params.y = popupPosition.y

        if (properties.excludeFromSystemGesture) {
            // Resolve conflict with gesture navigation back when dragging this handle view on the
            // edge of the screen.
            popupLayoutHelper.setGestureExclusionRects(this, windowSize.width, windowSize.height)
        }

        popupLayoutHelper.updateViewLayout(windowManager, this, params)
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        setViewTreeLifecycleOwner(null)
        windowManager.removeViewImmediate(scrim)
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!properties.dismissOnClickOutside) {
            return super.onTouchEvent(event)
        }
        // Note that this implementation is taken from PopupWindow. It actually does not seem to
        // matter whether we return true or false as some upper layer decides on whether the
        // event is propagated to other windows or not. So for focusable the event is consumed but
        // for not focusable it is propagated to other windows.
        if ((event?.action == MotionEvent.ACTION_DOWN) &&
            ((event.x < 0) || (event.x >= width) || (event.y < 0) || (event.y >= height))
        ) {
            onDismissRequest?.invoke()
            return true
        } else if (event?.action == MotionEvent.ACTION_OUTSIDE) {
            onDismissRequest?.invoke()
            return true
        }

        return super.onTouchEvent(event)
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        // Do nothing. ViewRootImpl will call this method attempting to set the layout direction
        // from the context's locale, but we have one already from the parent composition.
    }

    // Sets the "real" layout direction for our content that we obtain from the parent composition.
    private fun superSetLayoutDirection(layoutDirection: LayoutDirection) {
        val direction = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
        super.setLayoutDirection(direction)
    }

    /**
     * Initialize the LayoutParams specific to [android.widget.PopupWindow].
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            // Start to position the popup in the top left corner, a new position will be calculated
            gravity = Gravity.START or Gravity.TOP

            // Flags specific to android.widget.PopupWindow
            flags = flags and (
                    WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                            WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    ).inv()

            // Enables us to intercept outside clicks even when popup is not focusable
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT

            // accessibilityTitle is not exposed as a public API therefore we set popup window
            // title which is used as a fallback by a11y services
            title = composeView.context.resources.getString(R.string.default_popup_window_title)
        }
    }
}

@Composable
fun ModalPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val testTag = LocalPopupTestTag.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val modalPopupLayout = remember {
        ModalPopupLayout(
            onDismissRequest = onDismissRequest,
            properties = properties,
            testTag = testTag,
            composeView = view,
            density = density,
            initialPositionProvider = popupPositionProvider,
            popupId = popupId
        ).apply {
            setContent(parentComposition) {
                SimpleStack(
                    Modifier
                        .semantics { this.popup() }
                        // Get the size of the content
                        .onSizeChanged {
                            popupContentSize = it
                            updatePosition()
                        }
                        // Hide the popup while we can't position it correctly
                        .alpha(if (canCalculatePosition) 1f else 0f)
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(modalPopupLayout) {
        modalPopupLayout.show()
        modalPopupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            testTag = testTag,
            layoutDirection = layoutDirection
        )
        onDispose {
            modalPopupLayout.disposeComposition()
            // Remove the window
            modalPopupLayout.dismiss()
        }
    }

    SideEffect {
        modalPopupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            testTag = testTag,
            layoutDirection = layoutDirection
        )
    }

    DisposableEffect(popupPositionProvider) {
        modalPopupLayout.positionProvider = popupPositionProvider
        modalPopupLayout.updatePosition()
        onDispose {}
    }

    // The parent's bounds can change on any frame without onGloballyPositioned being called, if
    // e.g. the soft keyboard changes visibility. For that reason, we need to check if we've moved
    // on every frame. However, we don't need to handle all moves – most position changes will be
    // handled by onGloballyPositioned. This polling loop only needs to handle the case where the
    // view's absolute position on the screen has changed, so we do a quick check to see if it has,
    // and only do the other position calculations in that case.
    LaunchedEffect(modalPopupLayout) {
        while (isActive) {
            withInfiniteAnimationFrameNanos {}
            modalPopupLayout.pollForLocationOnScreenChange()
        }
    }

    // TODO(soboleva): Look at module arrangement so that Box can be
    //  used instead of this custom Layout
    // Get the parent's position, size and layout direction
    Layout(
        content = {},
        modifier = Modifier
            .onGloballyPositioned { childCoordinates ->
                // This callback is best-effort – the screen coordinates of this layout node can
                // change at any time without this callback being fired (e.g. during IME visibility
                // change). For that reason, updating the position in this callback is not
                // sufficient, and the coordinates are also re-calculated on every frame.
                val parentCoordinates = childCoordinates.parentLayoutCoordinates!!
                modalPopupLayout.updateParentLayoutCoordinates(parentCoordinates)
            }
    ) { _, _ ->
        modalPopupLayout.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }
}
