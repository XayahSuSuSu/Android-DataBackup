package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.DropdownMenuContent
import com.xayah.core.ui.material3.DropdownMenuPositionProvider
import com.xayah.core.ui.material3.calculateTransformOrigin
import com.xayah.core.ui.material3.toShape
import com.xayah.core.ui.material3.tokens.ShapeKeyTokens
import com.xayah.core.ui.material3.window.PopupProperties
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.SizeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun ModalActionDropdownMenu(
    expanded: Boolean,
    actionList: List<ActionMenuItem>,
    maxDisplay: Int? = null,
    onClick: ((index: Int) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var processingIndex by remember { mutableIntStateOf(-1) }
    val processing by remember(processingIndex) { mutableStateOf(processingIndex != -1) }
    ModalDropdownMenu(expanded = expanded, onDismissRequest = { if (processing.not()) onDismissRequest.invoke() }) {
        var itemHeightPx by remember { mutableIntStateOf(0) }
        val scrollState = rememberScrollState()
        var targetList by remember { mutableStateOf(actionList) }
        AnimatedContent(
            modifier = Modifier.limitMaxDisplay(itemHeightPx = itemHeightPx, maxDisplay = maxDisplay, scrollState = scrollState),
            targetState = targetList,
            label = AnimationTokens.AnimatedContentLabel
        ) { targetState ->
            Column {
                targetState.forEachIndexed { index, item ->
                    val countdown by remember(item.countdown) {
                        flow {
                            var countdown = item.countdown
                            while (countdown != 0) {
                                delay(500)
                                countdown--
                                emit(countdown)
                            }
                        }
                    }.collectAsStateWithLifecycle(initialValue = item.countdown)
                    val enabled = remember(item.enabled, countdown, processing) { item.enabled && countdown == 0 && processing.not() }

                    DropdownMenuItem(
                        modifier = Modifier
                            .background(item.backgroundColor.value.withState(enabled))
                            .onSizeChanged { itemHeightPx = it.height },
                        text = {
                            Text(
                                modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                                text = item.title,
                                color = item.color.value.withState(enabled)
                            )
                        },
                        enabled = enabled,
                        onClick = {
                            if (processing.not()) {
                                if (item.secondaryMenu.isNotEmpty()) {
                                    targetList = item.secondaryMenu
                                } else if (item.title == context.getString(R.string.word_return) && item.onClick == null) {
                                    targetList = actionList
                                } else if (onClick != null) {
                                    onClick(index)
                                } else if (item.onClick != null) {
                                    scope.launch {
                                        processingIndex = index
                                        item.onClick.invoke()
                                        processingIndex = -1
                                        if (item.dismissOnClick) onDismissRequest.invoke()
                                    }
                                }
                            }
                        },
                        leadingIcon = if (countdown != 0) {
                            {
                                Icon(
                                    imageVector = when (countdown) {
                                        3 -> ImageVector.vectorResource(id = R.drawable.ic_rounded_counter_3)
                                        2 -> ImageVector.vectorResource(id = R.drawable.ic_rounded_counter_2)
                                        1 -> ImageVector.vectorResource(id = R.drawable.ic_rounded_counter_1)
                                        else -> Icons.Rounded.Pending
                                    },
                                    tint = item.color.value.withState(enabled),
                                    contentDescription = null
                                )
                            }
                        } else if (item.icon == null) null else {
                            {
                                item.icon.apply {
                                    Icon(imageVector = item.icon, tint = item.color.value.withState(enabled), contentDescription = null)
                                }
                            }
                        },
                        trailingIcon = if (processingIndex != index) null else {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(SizeTokens.Level18),
                                    color = item.color.value.withState(enabled),
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
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
    selectedIcon: ImageVector = Icons.Rounded.Done,
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
                                .background(ThemedColorSchemeKeyTokens.OnPrimary.value)
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
                                .background(if (selected) ThemedColorSchemeKeyTokens.PrimaryContainer.value else ThemedColorSchemeKeyTokens.OnPrimary.value)
                                .onSizeChanged { itemHeightPx = it.height },
                            text = {
                                Text(
                                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                                    text = item,
                                    color = if (selected) ThemedColorSchemeKeyTokens.Primary.value else Color.Unspecified
                                )
                            },
                            onClick = {
                                onSelected(index, list[index])
                            },
                            trailingIcon = {
                                if (selected) Icon(
                                    imageVector = selectedIcon,
                                    contentDescription = null,
                                    tint = ThemedColorSchemeKeyTokens.Primary.value
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
fun ModalStringListMultipleSelectionDropdownMenu(
    expanded: Boolean,
    selectedIndexList: List<Int>,
    selectedIcon: ImageVector = Icons.Rounded.Done,
    list: List<String>,
    maxDisplay: Int? = null,
    onSelected: (indexList: List<Int>) -> Unit,
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
                    scrollState.scrollTo(itemValue * (selectedIndexList.maxOrNull() ?: 0))
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
                                .background(ThemedColorSchemeKeyTokens.OnPrimary.value)
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
                        val selected = index in selectedIndexList
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(if (selected) ThemedColorSchemeKeyTokens.PrimaryContainer.value else ThemedColorSchemeKeyTokens.OnPrimary.value)
                                .onSizeChanged { itemHeightPx = it.height },
                            text = {
                                Text(
                                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level4),
                                    text = item,
                                    color = if (selected) ThemedColorSchemeKeyTokens.Primary.value else Color.Unspecified
                                )
                            },
                            onClick = {
                                if (selected)
                                    onSelected(selectedIndexList.toMutableList().apply { remove(index) }.toList())
                                else
                                    onSelected(selectedIndexList.toMutableList().apply { add(index) }.toList())
                            },
                            trailingIcon = {
                                if (selected) Icon(
                                    imageVector = selectedIcon,
                                    contentDescription = null,
                                    tint = ThemedColorSchemeKeyTokens.Primary.value
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
fun <S> AnimatedModalDropdownMenu(
    targetState: S,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.(targetState: S) -> Unit,
) {
    ModalDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier, offset = offset, properties = properties) {
        AnimatedContent(
            targetState = targetState,
            label = AnimationTokens.AnimatedContentLabel
        ) { targetState ->
            Column {
                content(targetState)
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

@Composable
fun ContentWithActions(modifier: Modifier = Modifier, actions: (MutableState<Boolean>) -> List<ActionMenuItem>, content: @Composable (MutableState<Boolean>) -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = modifier.wrapContentSize(Alignment.Center)) {
        content(expanded)

        ModalActionDropdownMenu(expanded = expanded.value, actionList = actions(expanded), onDismissRequest = { expanded.value = false })
    }
}
