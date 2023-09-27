package com.xayah.databackup.ui.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.MenuTokens
import com.xayah.librootservice.util.ExceptionUtil
import kotlin.math.max
import kotlin.math.min

private fun calculateTransformOrigin(
    parentBounds: IntRect,
    menuBounds: IntRect,
): TransformOrigin {
    val pivotX = when {
        menuBounds.left >= parentBounds.right -> 0f
        menuBounds.right <= parentBounds.left -> 1f
        menuBounds.width == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                        max(parentBounds.left, menuBounds.left) +
                                min(parentBounds.right, menuBounds.right)
                        ) / 2
            (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
        }
    }
    val pivotY = when {
        menuBounds.top >= parentBounds.bottom -> 0f
        menuBounds.bottom <= parentBounds.top -> 1f
        menuBounds.height == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                        max(parentBounds.top, menuBounds.top) +
                                min(parentBounds.bottom, menuBounds.bottom)
                        ) / 2
            (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
        }
    }
    return TransformOrigin(pivotX, pivotY)
}

/**
 * Calculates the position of a Material [ModalDropdownMenu].
 */
// TODO(popam): Investigate if this can/should consider the app window size rather than screen size
@Immutable
private data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> },
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuTokens.MenuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                    it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

@Composable
private fun DropdownMenuContent(
    expandedStates: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Menu open/close animation.
    val transition = updateTransition(expandedStates, "DropDownMenu")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = MenuTokens.InTransitionDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = MenuTokens.OutTransitionDuration - 1
                )
            }
        }, label = MenuTokens.AnimationLabel
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0.8f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = MenuTokens.OutTransitionDuration)
            }
        }, label = MenuTokens.AnimationLabel
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0f
        }
    }
    val shape = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(MenuTokens.RoundedCornerShapeSize)).extraSmall
    Surface(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = transformOriginState.value
        },
        shape = shape,
        color = ColorScheme.surface(),
        tonalElevation = MenuTokens.ContainerElevation,
        shadowElevation = MenuTokens.ContainerElevation
    ) {
        Column(
            modifier = modifier
                .padding(vertical = MenuTokens.DropdownMenuVerticalPadding)
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

/**
 * A [ModalDropdownMenu] behaves similarly to a [DropdownMenu], but will include modal features.
 */
@Composable
fun ModalDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider = DropdownMenuPositionProvider(
            offset,
            density
        ) { parentBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
        }
        ModalPopup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties
        ) {
            DropdownMenuContent(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
                modifier = modifier,
                content = content
            )
        }
    }
}


@Composable
fun ModalStringListDropdownMenu(
    expanded: Boolean,
    selectedIndex: Int,
    selectedIcon: ImageVector = Icons.Rounded.Done,
    list: List<String>,
    maxDisplay: Int? = null,
    onSelected: (index: Int, selected: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        var itemHeightPx by remember { mutableIntStateOf(0) }
        var modifier: Modifier = Modifier
        if (maxDisplay != null) {
            val scrollState = rememberScrollState()
            LaunchedEffect(expanded) {
                if (expanded) {
                    // Scroll to selected item.
                    val itemValue = scrollState.maxValue / list.size
                    scrollState.scrollTo(itemValue * selectedIndex)
                }
            }
            with(LocalDensity.current) {
                /**
                 * If [maxDisplay] is non-null, limit the max height.
                 */
                modifier = Modifier
                    .heightIn(max = ((itemHeightPx * maxDisplay).toDp()))
                    .verticalScroll(scrollState)
            }
        }
        Column(modifier = modifier) {
            list.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                DropdownMenuItem(
                    modifier = Modifier
                        .background(if (selected) ColorScheme.primaryContainer() else ColorScheme.onPrimary())
                        .onSizeChanged { itemHeightPx = it.height },
                    text = {
                        Text(
                            modifier = Modifier.paddingHorizontal(MenuTokens.ModalDropdownMenuPadding),
                            text = item,
                            color = if (selected) ColorScheme.primary() else Color.Unspecified
                        )
                    },
                    onClick = {
                        onSelected(index, list[index])
                    },
                    trailingIcon = { if (selected) Icon(imageVector = selectedIcon, contentDescription = null, tint = ColorScheme.primary()) }
                )
            }
        }
    }
}

@Composable
fun ChipDropdownMenu(
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    defaultSelectedIndex: Int = 0,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
    onClick: () -> Unit,
) {
    var selectedIndex by remember(defaultSelectedIndex) { mutableIntStateOf(defaultSelectedIndex) }
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            onClick = {
                onClick()
                if (list.isNotEmpty()) expanded = true
            },
            label = { Text(text = label ?: list[selectedIndex]) },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            } else null,
            trailingIcon = if (trailingIcon != null) {
                {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            } else null
        )

        ModalStringListDropdownMenu(
            expanded = expanded,
            selectedIndex = selectedIndex,
            list = list,
            maxDisplay = MenuTokens.DefaultMaxDisplay,
            onSelected = { index, selected ->
                expanded = false
                onSelected(index, selected)
                selectedIndex = index
            },
            onDismissRequest = { expanded = false }
        )
    }
}

enum class SortState {
    ASCENDING,
    DESCENDING;

    companion object {
        fun of(state: String?): SortState {
            return ExceptionUtil.tryOn(
                block = {
                    SortState.valueOf(state!!.uppercase())
                },
                onException = {
                    ASCENDING
                })
        }
    }
}

@Composable
fun SortStateChipDropdownMenu(
    icon: ImageVector,
    defaultSelectedIndex: Int = 0,
    defaultSortState: SortState = SortState.ASCENDING,
    list: List<String>,
    onSelected: (index: Int, selected: String, state: SortState) -> Unit,
    onClick: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(defaultSelectedIndex) }
    var expanded by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(defaultSortState) }
    val selectedIcon = if (state == SortState.ASCENDING) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        AssistChip(
            onClick = {
                onClick()
                if (list.isNotEmpty()) expanded = true
            },
            label = { Text(text = list[selectedIndex]) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )

        ModalStringListDropdownMenu(
            expanded = expanded,
            selectedIndex = selectedIndex,
            selectedIcon = if (state == SortState.ASCENDING) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            list = list,
            maxDisplay = MenuTokens.DefaultMaxDisplay,
            onSelected = { index, selected ->
                if (selectedIndex == index) state = if (state == SortState.ASCENDING) SortState.DESCENDING else SortState.ASCENDING
                else selectedIndex = index
                onSelected(index, selected, state)
            },
            onDismissRequest = { expanded = false }
        )
    }
}
