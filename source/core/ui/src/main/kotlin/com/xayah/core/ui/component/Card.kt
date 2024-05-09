package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.xayah.core.model.OperationState
import com.xayah.core.ui.material3.CardColors
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.CardElevation
import com.xayah.core.ui.material3.ShapeDefaults
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.material3.tokens.OutlinedCardTokens
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.StateView
import com.xayah.core.ui.util.value

@Composable
fun outlinedCardBorder(enabled: Boolean = true, borderColor: Color? = null): BorderStroke {
    val tint = borderColor ?: OutlinedCardTokens.OutlineColor.toColor()
    val color = if (enabled) {
        tint
    } else {
        tint.copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
            .compositeOver(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    OutlinedCardTokens.DisabledContainerElevation
                )
            )
    }
    return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    performHapticFeedback: Boolean = false,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = rememberRipple(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = onClick,
        onLongClick = {
            if (performHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick?.invoke()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = colors.contentColor(enabled).value,
        tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
        indication = indication,
    ) {
        Column(content = content)
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    progress: Float,
    title: StringResourceToken,
    defExpanded: Boolean = false,
    flagExpanded: Boolean = false,
    maxDisplayNum: Int = -1,
    items: List<ProcessingCardItem>,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var _expanded by remember(flagExpanded) { mutableStateOf(defExpanded || flagExpanded) }
    var secondary by remember { mutableStateOf(false) }
    var _secondaryIndex by remember { mutableIntStateOf(0) }
    var _secondaryStartIndex by remember { mutableIntStateOf(0) }
    val _secondaryItems by remember(items, _secondaryIndex) { mutableStateOf(if (items.size <= _secondaryIndex) listOf() else items[_secondaryIndex].secondaryItems) }
    val _items by remember(items, secondary, _secondaryItems) { mutableStateOf(if (secondary) _secondaryItems else items) }
    var _title by remember(title) { mutableStateOf(title) }
    var _maxDisplayNum by remember(maxDisplayNum) { mutableIntStateOf(maxDisplayNum) }
    val successCount by remember(_items) { mutableIntStateOf(_items.count { it.state == OperationState.DONE }) }
    val failedCount by remember(_items) { mutableIntStateOf(_items.count { it.state == OperationState.ERROR }) }
    val totalCount by remember(_items.size) { mutableIntStateOf(_items.size) }
    val _state by remember(successCount, failedCount, totalCount) {
        mutableStateOf(
            if (failedCount != 0) OperationState.ERROR
            else if (successCount + failedCount == 0) OperationState.IDLE
            else if (successCount == totalCount) OperationState.DONE
            else OperationState.PROCESSING
        )
    }
    val displayItems by remember(_maxDisplayNum, _items) {
        mutableStateOf(
            if (_maxDisplayNum == -1 || _items.size <= _maxDisplayNum) {
                _secondaryStartIndex = 0
                _items
            } else {
                val processingIndex = _items.indexOfFirst { it.state == OperationState.PROCESSING }
                val halfCount = (_maxDisplayNum - 1) / 2
                val startIndex: Int
                val endIndex: Int
                if (processingIndex - halfCount < 0) {
                    startIndex = 0
                    endIndex = _maxDisplayNum
                } else if (processingIndex + halfCount > _items.size - 1) {
                    startIndex = _items.size - _maxDisplayNum
                    endIndex = _items.size
                } else {
                    startIndex = processingIndex - halfCount
                    endIndex = processingIndex + _maxDisplayNum / 2 + 1
                }
                _secondaryStartIndex = startIndex
                _items.subList(startIndex, endIndex)
            }
        )
    }

    val backToPrimary: () -> Unit = remember(maxDisplayNum, items, title) {
        {
            secondary = false
            _maxDisplayNum = maxDisplayNum
            _title = title
        }
    }

    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = (if (_expanded) ColorSchemeKeyTokens.SurfaceVariantDim else ColorSchemeKeyTokens.Transparent).toColor(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = (if (_expanded) ColorSchemeKeyTokens.SurfaceVariant else ColorSchemeKeyTokens.Surface).toColor(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {
                        if (totalCount != 0 && flagExpanded.not()) {
                            _expanded = _expanded.not()
                            backToPrimary()
                        }
                    }
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        if (secondary) {
                            ArrowBackButton {
                                backToPrimary()
                            }
                        } else {
                            _state.StateView(enabled = enabled, expanded = _expanded, progress = progress)
                        }

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = _title.value,
                            color = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                        )
                        actions()
                        if (totalCount != 0) {
                            LabelSmallText(text = "${successCount + failedCount}/${totalCount}", color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                            Icon(
                                imageVector = if (_expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                            )
                        }
                    }
                }
                AnimatedVisibility(_expanded) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(count = displayItems.size, key = { "$it-${displayItems[it].title}" }) {
                            val item = displayItems[it]
                            Surface(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .fillMaxWidth(),
                                enabled = enabled,
                                color = ColorSchemeKeyTokens.Transparent.toColor(enabled),
                                onClick = {
                                    if (item.secondaryItems.isNotEmpty()) {
                                        _secondaryIndex = _secondaryStartIndex + it
                                        _maxDisplayNum = -1
                                        secondary = true
                                        _title = item.title
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(SizeTokens.Level16),
                                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item.state.StateView(enabled = enabled, expanded = false, progress = item.progress)
                                    TitleSmallText(modifier = Modifier.weight(1f), text = item.title.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                                    LabelSmallText(text = item.content.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(enabled))
                                    if (item.secondaryItems.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = ColorSchemeKeyTokens.OnSurface.toColor(enabled)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
