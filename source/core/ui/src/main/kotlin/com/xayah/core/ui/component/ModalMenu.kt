package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.DropdownMenuContent
import com.xayah.core.ui.material3.DropdownMenuPositionProvider
import com.xayah.core.ui.material3.calculateTransformOrigin
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.toShape
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.material3.tokens.ShapeKeyTokens
import com.xayah.core.ui.material3.window.PopupProperties
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun ModalActionDropdownMenu(
    expanded: Boolean,
    actionList: List<ActionMenuItem>,
    maxDisplay: Int? = null,
    onDismissRequest: () -> Unit,
) {
    ModalDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        var itemHeightPx by remember { mutableIntStateOf(0) }
        val scrollState = rememberScrollState()
        var targetList by remember { mutableStateOf(actionList) }
        AnimatedContent(
            modifier = Modifier.limitMaxDisplay(itemHeightPx = itemHeightPx, maxDisplay = maxDisplay, scrollState = scrollState),
            targetState = targetList,
            label = AnimationTokens.AnimatedContentLabel
        ) { targetState ->
            Column {
                targetState.forEach { item ->
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(item.backgroundColor.toColor())
                            .onSizeChanged { itemHeightPx = it.height },
                        text = {
                            Text(
                                modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                text = item.title.value,
                                color = item.color.toColor()
                            )
                        },
                        enabled = item.enabled,
                        onClick = {
                            if (item.secondaryMenu.isNotEmpty()) {
                                targetList = item.secondaryMenu
                            } else if (item.title == StringResourceToken.fromStringId(R.string.word_return)) {
                                targetList = actionList
                            } else {
                                item.onClick.invoke()
                            }
                        },
                        leadingIcon = {
                            item.icon?.apply {
                                Icon(imageVector = item.icon.value, tint = item.color.toColor(), contentDescription = null)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ModalStringListDropdownMenu(
    expanded: Boolean,
    selectedIndex: Int,
    selectedIcon: ImageVectorToken = ImageVectorToken.fromVector(Icons.Rounded.Done),
    list: List<String>,
    maxDisplay: Int? = null,
    onSelected: (index: Int, selected: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        var itemHeightPx by remember { mutableIntStateOf(0) }
        val scrollState = rememberScrollState()
        if (maxDisplay != null) {
            LaunchedEffect(expanded, list) {
                if (expanded && list.isNotEmpty()) {
                    // Scroll to selected item.
                    val itemValue = scrollState.maxValue / list.size
                    scrollState.scrollTo(itemValue * selectedIndex)
                }
            }
        }
        AnimatedContent(
            modifier = Modifier.limitMaxDisplay(itemHeightPx = itemHeightPx, maxDisplay = maxDisplay, scrollState = scrollState),
            targetState = list.isEmpty(),
            label = AnimationTokens.AnimatedContentLabel
        ) { targetState ->
            Column {
                if (targetState) {
                    repeat(2) {
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(ColorSchemeKeyTokens.OnPrimary.toColor())
                                .onSizeChanged { itemHeightPx = it.height },
                            text = {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shimmer(),
                                    text = ""
                                )
                            },
                            onClick = {},
                        )
                    }
                } else {
                    list.forEachIndexed { index, item ->
                        val selected = index == selectedIndex
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(if (selected) ColorSchemeKeyTokens.PrimaryContainer.toColor() else ColorSchemeKeyTokens.OnPrimary.toColor())
                                .onSizeChanged { itemHeightPx = it.height },
                            text = {
                                Text(
                                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                                    text = item,
                                    color = if (selected) ColorSchemeKeyTokens.Primary.toColor() else Color.Unspecified
                                )
                            },
                            onClick = {
                                onSelected(index, list[index])
                            },
                            trailingIcon = {
                                if (selected) Icon(
                                    imageVector = selectedIcon.value,
                                    contentDescription = null,
                                    tint = ColorSchemeKeyTokens.Primary.toColor()
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

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
                shape = ShapeKeyTokens.CornerLarge.toShape(),
                verticalPadding = PaddingTokens.Level0,
                content = content
            )
        }
    }
}
